import * as admin from "firebase-admin";
import {onCall, HttpsError} from "firebase-functions/v2/https";
import {onDocumentWritten} from "firebase-functions/v2/firestore";

// Simple extension submission function
export const submitExtension = onCall(async (request) => {
  if (!request.auth) {
    throw new HttpsError(
        "unauthenticated",
        "User must be logged in to submit an extension.",
    );
  }

  const {manifest} = request.data;
  if (!manifest || !manifest.id || !manifest.name || !manifest.entry_point) {
    throw new HttpsError("invalid-argument", "Invalid manifest format.");
  }

  // Basic validation
  if (manifest.id.length > 50 || manifest.name.length > 50) {
    throw new HttpsError("invalid-argument", "ID or Name too long.");
  }

  // Store in firestore
  const db = admin.firestore();
  const extensionRef = db.collection("extensions_store").doc(manifest.id);

  // Check if exists and ownership
  const doc = await extensionRef.get();
  if (doc.exists) {
    const data = doc.data();
    if (data?.ownerUid !== request.auth.uid) {
      throw new HttpsError(
          "permission-denied",
          "You do not own this extension ID.",
      );
    }
  }

  await extensionRef.set({
    ...manifest,
    ownerUid: request.auth.uid,
    updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    status: "submitted", // Requires review
  }, {merge: true});

  return {success: true, message: "Extension submitted for review."};
});

// Auto-approve extensions from trusted developers (e.g. the owner)
export const onExtensionSubmitted = onDocumentWritten(
    "extensions_store/{extensionId}",
    async (event) => {
      if (!event.data) return; // Delete
      const after = event.data.after.data();
      if (!after || after.status !== "submitted") return;

      // TODO: Add allowlist logic here
      // For now, auto-approve everything in dev environment
      return event.data.after.ref.update({status: "approved"});
    },
);
