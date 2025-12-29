import * as functions from "firebase-functions";
import * as admin from "firebase-admin";
import * as nodemailer from "nodemailer";
import {escapeHtml} from "./security";

// Email invitation interface
interface EmailInvitationData {
  recipientEmail: string;
  senderName: string;
  senderDeviceId: string;
  invitationCode: string;
  contactId: number;
}

// Initialize Nodemailer transporter using environment variables
const createTransporter = () => {
  const config = functions.config();

  if (!config.email) {
    throw new Error("Email configuration not found. Please set Firebase Functions config.");
  }

  return nodemailer.createTransport({
    host: config.email.host,
    port: parseInt(config.email.port),
    secure: false, // true for 465, false for other ports
    auth: {
      user: config.email.user,
      pass: config.email.password,
    },
  });
};

// Generate HTML email template
const generateEmailTemplate = (
    senderName: string,
    invitationCode: string,
    deepLinkBase: string,
    playStoreUrl: string,
    appStoreUrl: string,
): string => {
  const safeSenderName = escapeHtml(senderName);
  const safeInvitationCode = escapeHtml(invitationCode);
  const inviteLink = `${deepLinkBase}/invite?code=${encodeURIComponent(invitationCode)}`;

  return `
    <!DOCTYPE html>
    <html>
    <head>
      <meta charset="utf-8">
      <meta name="viewport" content="width=device-width, initial-scale=1.0">
      <title>PulseLink Invitation</title>
      <style>
        body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; padding: 20px; }
        .header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 30px; text-align: center; border-radius: 8px 8px 0 0; }
        .content { background: #ffffff; padding: 30px; border: 1px solid #e0e0e0; border-radius: 0 0 8px 8px; }
        .button { display: inline-block; padding: 14px 28px; background: #667eea; color: white !important; text-decoration: none; border-radius: 6px; font-weight: 600; margin: 20px 0; }
        .button:hover { background: #5568d3; }
        .app-links { display: flex; gap: 15px; justify-content: center; margin-top: 25px; }
        .app-link { display: inline-block; }
        .code { background: #f5f5f5; padding: 12px; border-radius: 6px; font-family: 'Courier New', monospace; font-size: 18px; font-weight: bold; text-align: center; margin: 20px 0; color: #667eea; }
        .footer { text-align: center; margin-top: 30px; color: #666; font-size: 14px; }
      </style>
    </head>
    <body>
      <div class="header">
        <h1 style="margin: 0; font-size: 28px;">🔗 PulseLink Invitation</h1>
      </div>
      <div class="content">
        <p style="font-size: 18px; margin-top: 0;">Hi there!</p>
        <p><strong>${safeSenderName}</strong> wants to connect with you on <strong>PulseLink</strong>, the safety app that keeps your loved ones informed and protected.</p>
        
        <p style="margin-top: 25px;">With PulseLink, you can:</p>
        <ul style="padding-left: 20px;">
          <li>Share real-time location updates with trusted contacts</li>
          <li>Send and receive safety alerts instantly</li>
          <li>Check in with loved ones automatically</li>
          <li>Stay connected even in emergencies</li>
        </ul>

        <div style="text-align: center; margin: 30px 0;">
          <a href="${inviteLink}" class="button">Accept Invitation</a>
        </div>

        <p style="margin-top: 25px;">Or use this invitation code:</p>
        <div class="code">${safeInvitationCode}</div>

        <p style="font-size: 14px; color: #666; margin-top: 30px;">Don't have PulseLink yet? Download it now:</p>
        <div class="app-links">
          <a href="${playStoreUrl}" class="app-link">
            <img src="https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png" alt="Get it on Google Play" height="60">
          </a>
          ${appStoreUrl ? `<a href="${appStoreUrl}" class="app-link"><img src="https://developer.apple.com/assets/elements/badges/download-on-the-app-store.svg" alt="Download on App Store" height="60"></a>` : ""}
        </div>
      </div>
      
      <div class="footer">
        <p>This invitation will expire in 7 days.</p>
        <p style="font-size: 12px; margin-top: 15px;">PulseLink - Your Safety Network<br>
If you didn't expect this invitation, you can safely ignore this email.</p>
      </div>
    </body>
    </html>
  `;
};

// Main Cloud Function to send email invitation
export const sendEmailInvitation = functions.https.onCall(
    async (data: EmailInvitationData, context) => {
      try {
      // Validate authentication
        if (!context.auth) {
          throw new functions.https.HttpsError(
              "unauthenticated",
              "User must be authenticated to send invitations",
          );
        }

        // Validate input data
        const {recipientEmail, senderName, senderDeviceId, invitationCode, contactId} = data;

        if (!recipientEmail || !senderName || !senderDeviceId || !invitationCode) {
          throw new functions.https.HttpsError(
              "invalid-argument",
              "Missing required fields: recipientEmail, senderName, senderDeviceId, or invitationCode",
          );
        }

        // Email validation
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        if (!emailRegex.test(recipientEmail)) {
          throw new functions.https.HttpsError(
              "invalid-argument",
              "Invalid email address format",
          );
        }

        // Get configuration
        const config = functions.config();
        const deepLinkBase = config.app?.deep_link_base || "https://pulselink.app";
        const playStoreUrl = config.app?.play_store_url || "https://play.google.com/store/apps/details?id=com.pulselink";
        const appStoreUrl = config.app?.app_store_url || "";

        // Create invitation document in Firestore
        const expiresAt = new Date();
        expiresAt.setDate(expiresAt.getDate() + 7); // 7 days from now

        const invitationDoc = {
          invitationCode,
          senderDeviceId,
          senderName,
          recipientEmail,
          contactId,
          createdAt: admin.firestore.FieldValue.serverTimestamp(),
          expiresAt: admin.firestore.Timestamp.fromDate(expiresAt),
          status: "PENDING",
          senderUid: context.auth.uid,
        };

        const invitationRef = await admin
            .firestore()
            .collection("emailInvitations")
            .add(invitationDoc);

        functions.logger.info("Invitation document created", {invitationId: invitationRef.id});

        // Send email
        const transporter = createTransporter();
        const emailFrom = config.email?.from_name || "PulseLink Team";

        const mailOptions = {
          from: `${emailFrom} <${config.email.user}>`,
          to: recipientEmail,
          subject: `${senderName} wants to connect on PulseLink`,
          html: generateEmailTemplate(
              senderName,
              invitationCode,
              deepLinkBase,
              playStoreUrl,
              appStoreUrl,
          ),
        };

        const info = await transporter.sendMail(mailOptions);
        functions.logger.info("Email sent successfully", {messageId: info.messageId, recipientEmail});

        return {
          success: true,
          invitationId: invitationRef.id,
          message: "Invitation email sent successfully",
        };
      } catch (error) {
        functions.logger.error("Error sending email invitation", error);

        if (error instanceof functions.https.HttpsError) {
          throw error;
        }

        throw new functions.https.HttpsError(
            "internal",
            "Failed to send invitation email",
        error instanceof Error ? error.message : "Unknown error",
        );
      }
    },
);

// Function to check invitation status
export const checkInvitationStatus = functions.https.onCall(
    async (data: { invitationCode: string }, context) => {
      try {
        if (!context.auth) {
          throw new functions.https.HttpsError(
              "unauthenticated",
              "User must be authenticated",
          );
        }

        const {invitationCode} = data;

        if (!invitationCode) {
          throw new functions.https.HttpsError(
              "invalid-argument",
              "Invitation code is required",
          );
        }

        // Query Firestore for the invitation
        const invitationsSnapshot = await admin
            .firestore()
            .collection("emailInvitations")
            .where("invitationCode", "==", invitationCode)
            .limit(1)
            .get();

        if (invitationsSnapshot.empty) {
          throw new functions.https.HttpsError(
              "not-found",
              "Invitation not found",
          );
        }

        const invitationDoc = invitationsSnapshot.docs[0];
        const invitation = invitationDoc.data();

        // Check if expired
        const now = new Date();
        const expiresAt = invitation.expiresAt.toDate();

        if (now > expiresAt) {
          await invitationDoc.ref.update({status: "EXPIRED"});
          throw new functions.https.HttpsError(
              "failed-precondition",
              "This invitation has expired",
          );
        }

        // Check if already accepted
        if (invitation.status === "ACCEPTED") {
          throw new functions.https.HttpsError(
              "already-exists",
              "This invitation has already been accepted",
          );
        }

        return {
          success: true,
          invitation: {
            senderName: invitation.senderName,
            senderDeviceId: invitation.senderDeviceId,
            status: invitation.status,
            createdAt: invitation.createdAt,
          },
        };
      } catch (error) {
        functions.logger.error("Error checking invitation status", error);

        if (error instanceof functions.https.HttpsError) {
          throw error;
        }

        throw new functions.https.HttpsError(
            "internal",
            "Failed to check invitation status",
        );
      }
    },
);

// Function to mark invitation as accepted
export const acceptInvitation = functions.https.onCall(
    async (data: { invitationCode: string }, context) => {
      try {
        if (!context.auth) {
          throw new functions.https.HttpsError(
              "unauthenticated",
              "User must be authenticated",
          );
        }

        const {invitationCode} = data;

        const invitationsSnapshot = await admin
            .firestore()
            .collection("emailInvitations")
            .where("invitationCode", "==", invitationCode)
            .where("status", "==", "PENDING")
            .limit(1)
            .get();

        if (invitationsSnapshot.empty) {
          throw new functions.https.HttpsError(
              "not-found",
              "Valid pending invitation not found",
          );
        }

        const invitationDoc = invitationsSnapshot.docs[0];

        await invitationDoc.ref.update({
          status: "ACCEPTED",
          acceptedAt: admin.firestore.FieldValue.serverTimestamp(),
          acceptedByUid: context.auth.uid,
        });

        functions.logger.info("Invitation accepted", {invitationCode, uid: context.auth.uid});

        return {
          success: true,
          message: "Invitation accepted successfully",
        };
      } catch (error) {
        functions.logger.error("Error accepting invitation", error);

        if (error instanceof functions.https.HttpsError) {
          throw error;
        }

        throw new functions.https.HttpsError(
            "internal",
            "Failed to accept invitation",
        );
      }
    },
);
