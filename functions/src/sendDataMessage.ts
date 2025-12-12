import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

if (admin.apps.length === 0) {
  admin.initializeApp();
}

interface DataMessageRequest {
  token: string;
  data: Record<string, string>;
}

export const sendDataMessage = functions.https.onCall(async (req: DataMessageRequest, context) => {
  if (!req || typeof req.token !== "string" || typeof req.data !== "object") {
    throw new functions.https.HttpsError("invalid-argument", "token and data required");
  }

  if (!context.auth) {
    throw new functions.https.HttpsError("unauthenticated", "Auth required");
  }

  const payload: admin.messaging.TokenMessage = {
    token: req.token,
    data: Object.fromEntries(
      Object.entries(req.data || {}).map(([k, v]) => [k, typeof v === "string" ? v : String(v)])
    ),
    android: {
      priority: "high",
    },
    apns: {
      headers: {
        "apns-priority": "10",
      },
    },
  };

  await admin.messaging().send(payload);
  return { status: "ok" };
});
