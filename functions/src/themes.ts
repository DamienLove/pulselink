import * as functions from "firebase-functions";
import * as admin from "firebase-admin";
import {onDocumentWritten} from "firebase-functions/v2/firestore";
import * as logger from "firebase-functions/logger";
import {isValidUrl} from "./security";

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

/**
 * Trigger to auto-approve themes that don't contain images.
 * This restores the "instant publish" functionality securely.
 */
export const onThemeSubmitted = onDocumentWritten(
    "themes_submissions/{themeId}",
    async (event) => {
      if (!event.data) return; // Delete
      const snapshot = event.data.after;
      if (!snapshot.exists) return; // Deletion

      const data = snapshot.data();
      if (!data) return;

      // Only process pending themes
      if (data.status !== "pending") return;

      const themeId = event.params.themeId;
      const db = admin.firestore();

      // 1. Verify "Safe Content" and Check for Images
      let hasImages = false;
      const theme = data.theme || {};

      // Check background image
      const bgUrl = theme.backgroundImageUrl ?
        String(theme.backgroundImageUrl).trim() : "";
      if (bgUrl.length > 0) {
        if (!isValidUrl(bgUrl)) {
          logger.warn(`Theme ${themeId} rejected: Invalid background URL.`);
          // Reject invalid URLs immediately to prevent XSS in admin panel
          await snapshot.ref.delete();
          return;
        }
        hasImages = true;
      }

      // Check icon overrides
      const iconOverrides = theme.iconOverrides || {};
      for (const key of Object.keys(iconOverrides)) {
        const val = iconOverrides[key];
        const iconUrl = val ? String(val).trim() : "";
        if (iconUrl.length > 0) {
          if (!isValidUrl(iconUrl)) {
            logger.warn(`Theme ${themeId} rejected: Invalid icon URL.`);
            await snapshot.ref.delete();
            return;
          }
          hasImages = true;
        }
      }

      // Sentinel: Scan all theme values for CSS injection (url(), etc.)
      // Any usage of url() in color fields constitutes an image/external
      // resource and must be flagged for review to prevent privacy leaks
      // or CSS injection.
      for (const [key, value] of Object.entries(theme)) {
        if (typeof value === "string") {
          // Check for CSS url() syntax: url(...)
          // This captures both url('...') and url("...") and url(...)
          // Case-insensitive to catch URL(...) etc.
          if (/url\s*\(/i.test(value)) {
            logger.warn(
                `Theme ${themeId} flagged: CSS url() detected in field ${key}.`,
            );
            hasImages = true;
          }
        }
      }

      if (hasImages) {
        logger.info(`Theme ${themeId} requires review (contains images).`);
        return;
      }

      // 2. Validate Text Content (Sanity Check)
      if ((data.name?.length || 0) > 50) {
        logger.warn(`Theme ${themeId} rejected: Name too long.`);
        await snapshot.ref.delete();
        return;
      }

      // 3. Auto-Approve if safe
      try {
        const publicRef = db.collection("themes_public").doc(themeId);

        const publicData = {
          ...data,
          status: "approved",
          approvedBy: "system",
          approvedAt: admin.firestore.FieldValue.serverTimestamp(),
        };

        await db.runTransaction(async (t) => {
          t.set(publicRef, publicData);
          t.delete(snapshot.ref);
        });

        logger.info(`Auto-approved theme ${themeId} (no images).`);
      } catch (e) {
        logger.error(`Failed to auto-approve theme ${themeId}`, e);
      }
    },
);
