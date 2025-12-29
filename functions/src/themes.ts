import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

if (admin.apps.length === 0) {
  admin.initializeApp();
}

/**
 * A callable Cloud Function to approve a theme submission.
 *
 * This function is protected and can only be successfully called by a user
 * who has the `admin: true` custom claim.
 *
 * @param {object} data The data passed to the function.
 * @param {string} data.themeId The ID of the theme in themes_submissions.
 * @param {functions.https.CallableContext} context The context of the call.
 */
export const approveTheme = functions.https.onCall(async (data, context) => {
  // 1. Authentication Check
  if (!context.auth) {
    throw new functions.https.HttpsError(
        "unauthenticated",
        "You must be authenticated to call this function.",
    );
  }

  // 2. Admin Check
  const callerClaims = context.auth.token;
  if (callerClaims.admin !== true) {
    throw new functions.https.HttpsError(
        "permission-denied",
        "You must be an admin to approve themes.",
    );
  }

  // 3. Data Validation
  const themeId = data.themeId;
  if (!themeId || typeof themeId !== "string") {
    throw new functions.https.HttpsError(
        "invalid-argument",
        "Please provide a valid themeId.",
    );
  }

  const db = admin.firestore();

  try {
    // 4. Get the submission
    const submissionRef = db.collection("themes_submissions").doc(themeId);
    const submissionSnap = await submissionRef.get();

    if (!submissionSnap.exists) {
      throw new functions.https.HttpsError(
          "not-found",
          `Theme submission '${themeId}' not found.`,
      );
    }

    const themeData = submissionSnap.data();
    if (!themeData) {
      throw new functions.https.HttpsError("internal", "Theme data is empty.");
    }

    // 5. Promote to public
    const publicRef = db.collection("themes_public").doc(themeId);

    // Sanitize/Update status
    const publicData = {
      ...themeData,
      status: "approved",
      approvedBy: context.auth.uid,
      approvedAt: admin.firestore.FieldValue.serverTimestamp(),
    };

    await db.runTransaction(async (t) => {
      t.set(publicRef, publicData);
      t.delete(submissionRef);
    });

    console.log(`Admin ${context.auth.uid} approved theme ${themeId}`);

    return {
      status: "success",
      message: `Theme ${themeId} approved and published.`,
    };
  } catch (error: unknown) {
    console.error("Error approving theme:", error);
    if (error instanceof functions.https.HttpsError) {
      throw error;
    }
    throw new functions.https.HttpsError(
        "internal",
        "An unexpected error occurred while approving the theme.",
    );
  }
});
