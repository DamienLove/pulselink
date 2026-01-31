import * as admin from "firebase-admin";
import {onDocumentWritten} from "firebase-functions/v2/firestore";

if (admin.apps.length === 0) {
  admin.initializeApp();
}

export const onUserUpdated = onDocumentWritten("users/{uid}", async (event) => {
  const beforeData = event.data?.before.data();
  const afterData = event.data?.after.data();

  if (!afterData) {
    // Document deleted
    return;
  }

  const uid = event.params.uid;

  // Helper to safely get timestamp millis
  const getMillis = (data: any, field: string): number => {
    const val = data?.[field];
    if (!val) return 0;
    // Check if it's a Firestore Timestamp (has toMillis method)
    if (typeof val.toMillis === "function") {
      return val.toMillis();
    }
    // Check if it's a number
    if (typeof val === "number") {
      return val;
    }
    // Check if it's a Date object
    if (val instanceof Date) {
      return val.getTime();
    }
    return 0;
  };

  const beforeStatus = beforeData?.premiumSubscriptionStatus;
  const afterStatus = afterData.premiumSubscriptionStatus;
  const activeStates = [
    "SUBSCRIPTION_STATE_ACTIVE",
    "SUBSCRIPTION_STATE_IN_GRACE_PERIOD",
  ];

  const premiumStatusChanged =
      (beforeStatus !== afterStatus);

  const webAccessChanged =
      !beforeData?.remoteWebAccessEnabled && afterData.remoteWebAccessEnabled;

  const beforeSyncTime = getMillis(beforeData, "syncRequestedAt");
  const afterSyncTime = getMillis(afterData, "syncRequestedAt");
  const syncRequested = afterSyncTime > beforeSyncTime;

  if (premiumStatusChanged || webAccessChanged || syncRequested) {
    console.log(`Triggering sync for user ${uid}. Reason: ${
        premiumStatusChanged ? "Premium Status" :
        webAccessChanged ? "Web Access" : "Sync Request"
    }`);

    const db = admin.firestore();
    const devicesQuery = await db.collection("devices")
        .where("uid", "==", uid)
        .get();

    if (devicesQuery.empty) {
      console.log(`No devices found for user ${uid}`);
      return;
    }

    const tokens: string[] = [];
    devicesQuery.forEach((doc) => {
      const data = doc.data();
      if (data.fcmToken) {
        tokens.push(data.fcmToken);
      }
    });

    if (tokens.length === 0) {
      console.log(`No FCM tokens found for user ${uid}`);
      return;
    }

    const payload = {
      data: {
        type: "SYNC_REQUEST",
        uid: uid,
        timestamp: String(Date.now()),
      },
      tokens: tokens,
      android: {
        priority: "high" as const,
      },
    };

    try {
      const response = await admin.messaging().sendMulticast(payload);
      console.log(`Sent SYNC_REQUEST to ${response.successCount} devices`);
    } catch (error) {
      console.error("Error sending SYNC_REQUEST notification", error);
    }
  }
});
