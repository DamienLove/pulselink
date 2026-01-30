import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

export const onUserUpdated = functions.firestore
    .document("users/{userId}")
    .onUpdate(async (change, context) => {
      const before = change.before.data();
      const after = change.after.data();

      // Check if syncRequestedAt changed
      const beforeSync = before?.syncRequestedAt;
      const afterSync = after?.syncRequestedAt;

      // Check if remoteWebAccessEnabled changed to true
      const beforeAccess = before?.remoteWebAccessEnabled;
      const afterAccess = after?.remoteWebAccessEnabled;
      const accessEnabled = !beforeAccess && afterAccess;

      const syncRequested = afterSync && (!beforeSync || !beforeSync.isEqual(afterSync));

      if (!syncRequested && !accessEnabled) return;

      const userId = context.params.userId;
      console.log(`Sync requested for user ${userId} (manual: ${syncRequested}, auto-enable: ${accessEnabled})`);

      const db = admin.firestore();
      // Find devices
      const devicesQuery = await db.collection("devices")
          .where("uid", "==", userId)
          .get();

      if (devicesQuery.empty) return;

      const tokens: string[] = [];
      devicesQuery.forEach((doc) => {
        const data = doc.data();
        if (data.fcmToken) {
          tokens.push(data.fcmToken);
        }
      });

      if (tokens.length === 0) return;

      const payload = {
        data: {
          type: "SMS_RELAY",
          userId: userId,
          reason: "SYNC_REQUEST",
          timestamp: String(Date.now()),
        },
        tokens: tokens,
        android: {
          priority: "high" as const,
        },
      };

      try {
        await admin.messaging().sendMulticast(payload);
        console.log(`Sent sync trigger to ${tokens.length} devices`);
      } catch (error) {
        console.error("Error sending sync trigger", error);
      }
    });
