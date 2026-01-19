import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

export const onMessageCreated = functions.firestore
    .document("linkChannels/{channelId}/messages/{messageId}")
    .onCreate(async (snapshot, context) => {
      const message = snapshot.data();
      const receiverId = message.receiverId;
      const senderId = message.senderId;
      const type = message.type || "manual";

      // Ensure newly created messages carry an initial SENT status for clients.
      await snapshot.ref.set({status: "SENT"}, {merge: true});

      if (!receiverId) {
        console.log("No receiverId found in message");
        return;
      }

      const db = admin.firestore();
      // Get receiver's FCM token from devices collection
      const deviceDoc = await db.collection("devices").doc(receiverId).get();

      if (!deviceDoc.exists) {
        console.log(`No device document found for receiver ${receiverId}`);
        return;
      }

      const fcmToken = deviceDoc.data()?.fcmToken;
      if (!fcmToken) {
        console.log(`No FCM token found for receiver ${receiverId}`);
        return;
      }

      // FCM data payload - all values must be strings
      const dataPayload: {[key: string]: string} = {
        type: type,
        senderId: senderId,
        timestamp: String(message.timestamp || Date.now()),
        messageId: context.params.messageId,
        channelId: context.params.channelId,
      };

      // Add optional fields if present
      if (message.body) {
        // Sentinel: Truncate body to prevent FCM payload size limit errors
        // (4KB limit). If the body is too large, notifications will fail.
        const maxBodyLength = 1000;
        dataPayload.body = message.body.length > maxBodyLength ?
            message.body.substring(0, maxBodyLength) + "..." :
            message.body;
      }
      if (message.linkCode) dataPayload.linkCode = message.linkCode;

      const payload = {
        data: dataPayload,
        token: fcmToken,
      };

      try {
        await admin.messaging().send(payload);
        console.log(`FCM notification sent to ${receiverId}`);
      } catch (error) {
        console.error("Error sending FCM notification", error);
      }
    });

export const onOutboxCreated = functions.firestore
    .document("users/{userId}/outbox/{messageId}")
    .onCreate(async (snapshot, context) => {
      const userId = context.params.userId;
      const messageId = context.params.messageId;

      console.log(`New outbox message ${messageId} for user ${userId}`);

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
          type: "SMS_RELAY",
          messageId: messageId,
          userId: userId,
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
          `Sent outbox notification to ${response.successCount} devices`
        );
      } catch (error) {
        console.error("Error sending outbox notification", error);
      }
    });
