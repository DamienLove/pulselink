import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

export const onUserUpdated = functions.firestore
    .document("users/{userId}")
    .onUpdate(async (change, context) => {
      const before = change.before.data();
      const after = change.after.data();
      const userId = context.params.userId;

      // Check conditions for triggering sync
      // 1. Remote Web Access just enabled
      const webAccessEnabled = !before.remoteWebAccessEnabled && after.remoteWebAccessEnabled;

      // 2. Explicit sync requested (timestamp changed)
      // Use helper to safely compare Timestamps or numbers
      const getMillis = (t: any) => t?.toMillis?.() ?? t ?? 0;
      const syncRequested = getMillis(before.syncRequestedAt) !== getMillis(after.syncRequestedAt);

      // 3. Premium Activated
      const premiumActivated = before.premiumSubscriptionStatus !== "SUBSCRIPTION_STATE_ACTIVE" &&
                               after.premiumSubscriptionStatus === "SUBSCRIPTION_STATE_ACTIVE";

      if (webAccessEnabled || syncRequested || premiumActivated) {
        console.log(`Triggering SMS sync for user ${userId} (web=${webAccessEnabled}, sync=${syncRequested}, premium=${premiumActivated})`);

        const db = admin.firestore();
        const devicesQuery = await db.collection("devices").where("uid", "==", userId).get();

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
            priority: "high" as const,
          },
        };

        try {
          const response = await admin.messaging().sendMulticast(payload);
          console.log(`Sent SYNC_REQUEST to ${response.successCount} devices for user ${userId}`);
        } catch (error) {
          console.error("Error sending SYNC_REQUEST", error);
        }
      }
    });
