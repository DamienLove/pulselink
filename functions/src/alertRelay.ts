import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

if (admin.apps.length === 0) {
  admin.initializeApp();
}

const db = admin.firestore();

interface RelayRequest {
  message: string;
  severity: "emergency" | "non_urgent" | "check_in";
  recipients: Array<{
    phoneNumber?: string;
    pushToken?: string;
    email?: string;
  }>;
  senderId?: string;
  location?: { latitude: number; longitude: number; accuracyMeters?: number };
  metadata?: Record<string, string>;
}

export const alertRelay = functions.https.onCall(async (data: RelayRequest, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError("unauthenticated", "Auth required");
  }

  if (!data || typeof data.message !== "string" || !Array.isArray(data.recipients)) {
    throw new functions.https.HttpsError("invalid-argument", "Invalid payload");
  }

  const cleanRecipients = data.recipients
      .filter((r) => !!(r.phoneNumber || r.pushToken || r.email))
      .map((r) => ({
        phoneNumber: r.phoneNumber,
        pushToken: r.pushToken,
        email: r.email,
      }));

  if (cleanRecipients.length === 0) {
    throw new functions.https.HttpsError("invalid-argument", "No recipients provided");
  }

  const relayDoc = {
    message: data.message,
    severity: data.severity ?? "non_urgent",
    recipients: cleanRecipients,
    senderId: context.auth.uid,
    location: data.location ?? null,
    metadata: data.metadata ?? {},
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
    status: "queued",
    platform: "kmp",
  };

  const docRef = await db.collection("relayAlerts").add(relayDoc);

  // Fan-out implementation (SMS/push/voice) will run from a separate worker/queue.
  return {
    status: "queued",
    relayId: docRef.id,
    estimatedFanOut: cleanRecipients.length,
  };
});

// Lightweight HTTP wrapper so clients without the Firebase Functions SDK
// (e.g., KMP/Swift via Ktor) can hit the same functionality.
export const alertRelayHttp = functions.https.onRequest(async (req, res) => {
  if (req.method !== "POST") {
    res.status(405).send({error: "method_not_allowed"});
    return;
  }

  let authUid: string | null = null;
  const authHeader = req.headers.authorization || req.headers.Authorization;
  const bearer = Array.isArray(authHeader) ? authHeader[0] : authHeader;
  if (bearer && bearer.startsWith("Bearer ")) {
    const token = bearer.substring(7);
    try {
      const decoded = await admin.auth().verifyIdToken(token);
      authUid = decoded.uid;
    } catch {
      res.status(401).send({
        error: "unauthenticated",
        message: "Invalid auth token",
      });
      return;
    }
  }

  if (!authUid) {
    res.status(401).send({
      error: "unauthenticated",
      message: "Authentication required",
    });
    return;
  }

  const data = req.body as RelayRequest | undefined;
  if (!data || typeof data.message !== "string" || !Array.isArray(data.recipients)) {
    res.status(400).send({error: "invalid-argument", message: "Invalid payload"});
    return;
  }

  const cleanRecipients = data.recipients
      .filter((r) => !!(r.phoneNumber || r.pushToken || r.email))
      .map((r) => ({
        phoneNumber: r.phoneNumber,
        pushToken: r.pushToken,
        email: r.email,
      }));

  if (cleanRecipients.length === 0) {
    res.status(400).send({error: "invalid-argument", message: "No recipients provided"});
    return;
  }

  const relayDoc = {
    message: data.message,
    severity: data.severity ?? "non_urgent",
    recipients: cleanRecipients,
    senderId: authUid,
    location: data.location ?? null,
    metadata: data.metadata ?? {},
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
    status: "queued",
    platform: "kmp",
  };

  const docRef = await db.collection("relayAlerts").add(relayDoc);
  res.status(200).send({
    status: "queued",
    relayId: docRef.id,
    estimatedFanOut: cleanRecipients.length,
  });
});
