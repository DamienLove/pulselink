
import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

// Ensure Firebase Admin is initialized, but only once.
if (admin.apps.length === 0) {
  admin.initializeApp();
}

/**
 * A callable Cloud Function to grant a user 'pro' access.
 *
 * This function is protected and can only be successfully called by a user
 * who has the `admin: true` custom claim.
 *
 * @param {object} data The data passed to the function.
 * @param {string} data.email The email of the user to grant pro access to.
 * @param {functions.https.CallableContext} context The context of the call.
 */
export const grantProAccess = functions.https.onCall(async (data, context) => {
  // 1. Authentication Check: Ensure the user calling the function is authenticated.
  if (!context.auth) {
    throw new functions.https.HttpsError(
      "unauthenticated",
      "You must be authenticated to call this function."
    );
  }

  // 2. Admin Check: Ensure the calling user is an admin.
  const callerClaims = context.auth.token;
  if (callerClaims.admin !== true) {
    throw new functions.https.HttpsError(
      "permission-denied",
      "You must be an admin to grant pro access."
    );
  }

  // 3. Data Validation: Ensure the target email is provided.
  const targetEmail = data.email;
  if (!targetEmail || typeof targetEmail !== "string") {
    throw new functions.https.HttpsError(
      "invalid-argument",
      "Please provide a valid email address."
    );
  }

  try {
    // 4. Grant Pro Access: Get the target user and set their custom claim.
    console.log(`Admin user '${context.auth.token.email}' is attempting to grant pro access to '${targetEmail}'.`);
    
    const userToUpgrade = await admin.auth().getUserByEmail(targetEmail);
    await admin.auth().setCustomUserClaims(userToUpgrade.uid, { pro: true });

    console.log(`Successfully granted pro access to ${targetEmail} (UID: ${userToUpgrade.uid})`);
    
    return {
      status: "success",
      message: `Pro access has been granted to ${targetEmail}.`,
    };

  } catch (error: any) {
    console.error("Error in grantProAccess function:", error);
    
    // Provide a more specific error to the client if the user isn't found.
    if (error.code === "auth/user-not-found") {
       throw new functions.https.HttpsError(
        "not-found",
        `The user with email ${targetEmail} was not found. Please ensure they have signed in at least once.`
      );
    }

    throw new functions.https.HttpsError(
      "internal",
      "An unexpected error occurred while processing your request."
    );
  }
});
