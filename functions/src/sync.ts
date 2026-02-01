import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

export const onUserUpdated = functions.firestore
  .document("users/{userId}")
  .onUpdate(async (change, context) => {
    const userId = context.params.userId;
    const before = change.before.data();
    const after = change.after.data();

    // Check for changes that should trigger a sync
    const premiumChanged =
      before.premiumSubscriptionStatus !== after.premiumSubscriptionStatus &&
      (after.premiumSubscriptionStatus === "SUBSCRIPTION_STATE_ACTIVE" ||
       after.premiumSubscriptionStatus === "SUBSCRIPTION_STATE_IN_GRACE_PERIOD");

    const accessChanged =
      !before.remoteWebAccessEnabled && after.remoteWebAccessEnabled;

    const syncRequested =
      before.syncRequestedAt !== after.syncRequestedAt && after.syncRequestedAt;

    if (premiumChanged || accessChanged || syncRequested) {
      console.log(`Triggering sync for user ${userId}`);

      const db = admin.firestore();
      // Find user's devices with FCM tokens
      const devicesQuery = await db.collection("devices")
        .where("uid", "==", userId)
        .get();

      if (devicesQuery.empty) {
        console.log(`No devices found for user ${userId}`);
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
        console.log(`No FCM tokens found for user ${userId}`);
        return;
      }

      const payload = {
        data: {
          type: "SYNC_REQUEST",
          timestamp: String(Date.now()),
        },
        tokens: tokens,
        android: {
          priority: "high" as const, // Wake up the device
        },
      };

      try {
        const response = await admin.messaging().sendMulticast(payload);
        console.log(
          `Sent SYNC_REQUEST to ${response.successCount} devices for user ${userId}`
        );
      } catch (error) {
        console.error("Error sending SYNC_REQUEST notification", error);
      }
    }
  });
