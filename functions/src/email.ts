import * as functions from "firebase-functions";
import * as nodemailer from "nodemailer";
import {escapeHtml} from "./security";

const transporter = nodemailer.createTransport({
  service: "gmail",
  auth: {
    user: process.env.EMAIL_USER,
    pass: process.env.EMAIL_PASS,
  },
});

export const sendEmailNotification = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError("unauthenticated", "User must be authenticated");
  }

  const {email, messageType, senderName, payload} = data;

  if (!email) {
    throw new functions.https.HttpsError("invalid-argument", "Email is required");
  }

  // Sentinel: Sanitize inputs to prevent HTML injection
  const safeSenderName = escapeHtml(senderName || "Unknown");

  let subject = "PulseLink Notification";
  let text = `You have a new message from ${senderName}`;
  let html = `<p>You have a new message from <strong>${safeSenderName}</strong></p>`;

  if (messageType === "LinkRequest") {
    subject = "PulseLink Request";
    const code = payload.code || "unknown";
    // Sentinel: Encode URI component for link and escape for HTML
    const deepLink = `https://pulselink.app/link?code=${encodeURIComponent(code)}`;
    const safeCode = escapeHtml(code);

    text = `${senderName} wants to link with you on PulseLink.\n\nLink Code: ${code}\n\nTap here to accept: ${deepLink}`;
    html = `
            <p><strong>${safeSenderName}</strong> wants to link with you on PulseLink.</p>
            <p>Link Code: <strong>${safeCode}</strong></p>
            <p><a href="${deepLink}">Tap here to accept</a></p>
        `;
  } else if (messageType === "ManualMessage") {
    const body = payload.body || "Alert";
    subject = `PulseLink Alert from ${senderName}`;
    text = `${senderName}: ${body}`;

    // Sentinel: Escape body content
    const safeBody = escapeHtml(body);
    html = `<p><strong>${safeSenderName}</strong>: ${safeBody}</p>`;
  }

  const mailOptions = {
    from: `PulseLink <${process.env.EMAIL_USER}>`,
    to: email,
    subject: subject,
    text: text,
    html: html,
  };

  try {
    await transporter.sendMail(mailOptions);
    return {success: true};
  } catch (error) {
    console.error("Error sending email", error);
    throw new functions.https.HttpsError("internal", "Failed to send email");
  }
});
