import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

if (admin.apps.length === 0) {
  admin.initializeApp();
}

const db = admin.firestore();

export const findUser = functions.https.onCall(async (data, context) => {
  // Auth check
  if (!context.auth) {
    throw new functions.https.HttpsError(
        "unauthenticated",
        "User must be logged in.",
    );
  }

  const {phoneNumber, email} = data;
  if (!phoneNumber && !email) {
    throw new functions.https.HttpsError(
        "invalid-argument",
        "Must provide phoneNumber or email.",
    );
  }

  try {
    let userRecord: admin.auth.UserRecord | null = null;

    if (phoneNumber) {
      try {
        userRecord = await admin.auth().getUserByPhoneNumber(phoneNumber);
      } catch (error) {
        const e = error as { code?: string };
        // If not found or invalid format, try adding '+' if missing
        if (
          (e.code === "auth/user-not-found" ||
            e.code === "auth/invalid-phone-number") &&
          !phoneNumber.startsWith("+")
        ) {
          try {
            userRecord = await admin.auth()
                .getUserByPhoneNumber(`+${phoneNumber}`);
          } catch {
            // Still not found via phone
          }
        }
      }
    }

    // If not found by phone (or phone not provided), try email
    if (!userRecord && email) {
      try {
        userRecord = await admin.auth().getUserByEmail(email);
      } catch {
        // Not found via email either
      }
    }

    if (!userRecord) {
      return {found: false};
    }

    const uid = userRecord.uid;
    // Fetch deviceId from Firestore users/{uid}
    const userDoc = await db.collection("users").doc(uid).get();
    if (!userDoc.exists) {
      return {found: false, message: "User profile not found."};
    }

    const userData = userDoc.data();
    const deviceId = userData?.deviceId;
    const displayName = userData?.ownerName || userRecord.displayName || "";
    const avatarUrl = userData?.avatarUrl || userRecord.photoURL || "";

    if (!deviceId) {
      return {found: false, message: "User has no active device."};
    }

    return {
      found: true,
      uid: uid,
      deviceId: deviceId,
      displayName: displayName,
      // Sentinel: Removed PII (email/phone) to prevent enumeration leaks
      avatarUrl: avatarUrl,
    };
  } catch (error) {
    console.error("Error finding user", error);
    return {found: false};
  }
});

export const deleteAccount = functions.https.onCall(async (_data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError("unauthenticated", "User must be logged in.");
  }

  const uid = context.auth.uid;
  const email = (context.auth.token.email as string | undefined) ?? null;
  const db = admin.firestore();

  try {
    const userDocRef = db.collection("users").doc(uid);
    const userSnap = await userDocRef.get();
    const deviceId = userSnap.exists ? (userSnap.data()?.deviceId as string | undefined) : undefined;

    // Delete user profile + subcollections
    await db.recursiveDelete(userDocRef);

    // Delete device registrations
    const deviceDeletes: FirebaseFirestore.WriteBatch[] = [];
    let batch = db.batch();
    let ops = 0;
    const queueDelete = (ref: FirebaseFirestore.DocumentReference) => {
      batch.delete(ref);
      ops += 1;
      if (ops >= 450) {
        deviceDeletes.push(batch);
        batch = db.batch();
        ops = 0;
      }
    };

    const deviceQuery = await db.collection("devices").where("uid", "==", uid).get();
    deviceQuery.forEach((doc) => queueDelete(doc.ref));
    if (deviceId) {
      queueDelete(db.collection("devices").doc(deviceId));
    }
    if (ops > 0) deviceDeletes.push(batch);
    for (const b of deviceDeletes) {
      await b.commit();
    }

    // Delete beta agreement tied to device ID (if present)
    if (deviceId) {
      await db.collection("betaAgreements").doc(deviceId).delete().catch(() => undefined);
    }

    // Delete link invites sent by or targeted to the user
    const inviteDeletes: Promise<FirebaseFirestore.WriteResult>[] = [];
    const inviteCollection = db.collection("linkEmailInvites");
    const senderInvites = await inviteCollection.where("senderUid", "==", uid).get();
    senderInvites.forEach((doc) => inviteDeletes.push(doc.ref.delete()));
    if (email) {
      const targetInvites = await inviteCollection
          .where("targetEmailLowercase", "==", email.toLowerCase())
          .get();
      targetInvites.forEach((doc) => inviteDeletes.push(doc.ref.delete()));
    }
    await Promise.all(inviteDeletes);

    // Delete link docs containing this uid
    const linksSnap = await db.collection("links").where("uids", "array-contains", uid).get();
    const linkDeletes = linksSnap.docs.map((doc) => doc.ref.delete());
    await Promise.all(linkDeletes);

    // Finally, delete the Auth user
    await admin.auth().deleteUser(uid);

    return {success: true};
  } catch (error: any) {
    console.error("Account deletion failed", error);
    throw new functions.https.HttpsError(
        "internal",
        "Unable to delete account. Please contact support.",
    );
  }
});
