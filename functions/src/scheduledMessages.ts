import {onSchedule} from "firebase-functions/v2/scheduler";
import * as admin from "firebase-admin";
import * as logger from "firebase-functions/logger";

if (admin.apps.length === 0) {
  admin.initializeApp();
}

/**
 * Scheduled function running every minute to process messages in scheduled_outbox.
 * Moves messages with scheduledAt <= now to the main outbox for delivery.
 */
export const processScheduledMessages = onSchedule("every 1 minutes", async (event) => {
  const db = admin.firestore();
  const now = admin.firestore.Timestamp.now();

  try {
    // collectionGroup query to find all scheduled messages across all users
    // Sentinel: Ensure index exists for scheduled_outbox (scheduledAt ASC)
    const snapshot = await db.collectionGroup("scheduled_outbox")
      .where("scheduledAt", "<=", now)
      .limit(200) // Limit to avoid batch overflow (500 ops max)
      .get();

    if (snapshot.empty) {
      return;
    }

    const batch = db.batch();
    let count = 0;

    for (const doc of snapshot.docs) {
      const data = doc.data();
      const uid = doc.ref.parent.parent?.id;

      if (!uid) {
        logger.warn(`Could not determine UID for scheduled message ${doc.id}`);
        continue;
      }

      // Prepare the payload for the real outbox
      // We remove scheduledAt and status="scheduled", reset status to "pending"
      const {scheduledAt, ...rest} = data;
      const outboxPayload = {
        ...rest,
        status: "pending",
        createdAt: admin.firestore.FieldValue.serverTimestamp(), // Update creation time to now so it's fresh for relay
        scheduledFrom: scheduledAt, // Audit trail
      };

      // Add to outbox
      const outboxRef = db.collection("users").doc(uid).collection("outbox").doc();
      batch.set(outboxRef, outboxPayload);

      // Delete from scheduled_outbox
      batch.delete(doc.ref);

      count++;
    }

    if (count > 0) {
      await batch.commit();
      logger.info(`Processed ${count} scheduled messages.`);
    }
  } catch (error) {
    logger.error("Error processing scheduled messages", error);
  }
});
