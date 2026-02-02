/**
 * Import function triggers from their respective submodules:
 *
 * import {onCall} from "firebase-functions/v2/https";
 * import {onDocumentWritten} from "firebase-functions/v2/firestore";
 *
 * See a full list of supported triggers at https://firebase.google.com/docs/functions
 */

import {setGlobalOptions} from "firebase-functions/v2";
import * as admin from "firebase-admin";

if (admin.apps.length === 0) {
  admin.initializeApp();
}

// Start writing functions
// https://firebase.google.com/docs/functions/typescript

setGlobalOptions({maxInstances: 10});

export {naturalLanguageQuery} from "./naturalLanguageInterface";
export {menuSuggestion} from "./genkit-sample";
export {summarizeSmsThread, composeSmsAssist, classifySmsUrgency} from "./ai";
export {alertRelay, alertRelayHttp} from "./alertRelay";
export {onMessageCreated, onOutboxCreated} from "./messaging";
export {sendEmailNotification} from "./email";
export {findUser, deleteAccount, onUserUpdated} from "./users";
export {approveTheme, onThemeSubmitted} from "./themes";
export {
  verifySubscription,
  getPremiumStatus,
  handlePlayStoreRTDN,
} from "./billing";
export {getSpotifyAccessToken} from "./spotify";
export {submitExtension, onExtensionSubmitted} from "./extensions";
