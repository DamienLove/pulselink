import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

export const onUserUpdated = functions.firestore
    .document("users/{userId}")
    .onUpdate(async (change, context) => {
      const newData = change.after.data();
      const oldData = change.before.data();

      // Check if syncRequestedAt changed
      const newSyncRequest = newData.syncRequestedAt;
      const oldSyncRequest = oldData.syncRequestedAt;

      // Also check if remoteWebAccessEnabled turned ON
      const newWebAccess = newData.remoteWebAccessEnabled;
      const oldWebAccess = oldData.remoteWebAccessEnabled;

      // Compare Timestamps
      let syncRequested = false;
      if (newSyncRequest && !oldSyncRequest) {
        syncRequested = true;
      } else if (newSyncRequest && oldSyncRequest) {
        // Use isEqual if available, otherwise compare seconds/nanoseconds
        if (typeof newSyncRequest.isEqual === "function") {
          syncRequested = !newSyncRequest.isEqual(oldSyncRequest);
        } else {
          syncRequested = newSyncRequest.seconds !== oldSyncRequest.seconds ||
                          newSyncRequest.nanoseconds !== oldSyncRequest.nanoseconds;
        }
      }

      const webAccessEnabled = (newWebAccess === true && oldWebAccess !== true);

      if (syncRequested || webAccessEnabled) {
        const userId = context.params.userId;
        console.log(`Sync requested for user ${userId}`);

        const db = admin.firestore();
        // Find user's devices with FCM tokens
        const devicesQuery = await db.collection("devices")
            .where("uid", "==", userId)
            .get();

        if (devicesQuery.empty) return;

        const tokens: string[] = [];
        devicesQuery.forEach((doc) => {
          const d = doc.data();
          if (d.fcmToken) {
            tokens.push(d.fcmToken);
          }
        });

        if (tokens.length > 0) {
          const payload = {
            data: {
              type: "SYNC_REQUEST",
              reason: "web_request",
              timestamp: String(Date.now()),
            },
            tokens: tokens,
            android: {
              priority: "high" as const,
            },
          };

          try {
            const response = await admin.messaging().sendMulticast(payload);
            console.log(`Sent SYNC_REQUEST to ${response.successCount} devices.`);
          } catch (e) {
            console.error("Failed to send SYNC_REQUEST", e);
          }
        }
      }
    });
