# PulseLink Beacon – Firebase & AI Kickoff

Scope for branch `feature/app-beacon-setup`: register the Beacon Android app with Firebase, add Remote Config + Firestore scaffolding, and outline the AI backend (Cloud Functions + Vertex AI) for SMS triage.

## 1) Firebase project + app registration
- Project: `pulselink-24899` (already exists).
- Register a new Android app with package `com.pulselink.beacon`. Use the existing SHA-256 from your signing key when available so Play-integrity/App Check can be enabled later.
- Download the generated `google-services.json` and place it at `app-beacon/google-services.json` (file is git-ignored).

## 2) Android Gradle changes (app-beacon)
- Apply plugin: add `id("com.google.gms.google-services")` to `app-beacon/build.gradle.kts` plugins block.
- Add Firebase BOM and SDKs under `dependencies`:
  - `implementation(platform("com.google.firebase:firebase-bom:<latest>"))`
  - `implementation("com.google.firebase:firebase-analytics-ktx")`
  - `implementation("com.google.firebase:firebase-config-ktx")`
  - `implementation("com.google.firebase:firebase-firestore-ktx")`
  - If we reuse auth from Safety/Android app: `implementation("com.google.firebase:firebase-auth-ktx")`
- Keep minSdk 26; Remote Config and Firestore are supported.

## 3) Remote Config (global theming defaults)
Create parameters in the Firebase Console with safe defaults:
- `app_theme_style` (string): `WhatsApp`
- `primary_color_hex` (string): `#008069`
- `font_family` (string): `Roboto`
- `bubble_style_id` (string): `default`
- Fetch strategy: `fetchAndActivate()` on app start with `minimumFetchIntervalInSeconds = 3600` for prod; shorter intervals only in debug builds.
- Mirror these defaults locally in code (e.g., `remoteConfig.setDefaultsAsync(mapOf(...))`) so UI renders before network returns.

## 4) Firestore (per-user preferences + AI results)
- Structure:
  - `users/{uid}/preferences/appPreferences` document with fields:
    - `selectedTheme` (string)
    - `customColors.primary` / `customColors.secondary` (hex strings)
    - `customFont` (string)
    - `bubbleStyleId` (string or int)
  - `users/{uid}/conversations/{threadId}` document for AI output:
    - `summary` (string)
    - `urgency` (enum: normal|important|urgent|emergency)
    - `scores` (object: sentiment, priority, toxicity if needed)
    - `updatedAt` (timestamp)
- Security rules (draft):
  ```
  rules_version = '2';
  service cloud.firestore {
    match /databases/{database}/documents {
      match /users/{userId}/{collection=**}/{docId} {
        allow read, write: if request.auth != null && request.auth.uid == userId;
      }
    }
  }
  ```
- Indexes: none required initially; add composite indexes if we later query by urgency.

## 5) Android runtime wiring
- On launch:
  - Initialize Firebase (`FirebaseApp.initializeApp` if not auto-initialized).
  - Kick off Remote Config fetch; store activated values in a small `ThemeDefaults` data class.
- After auth:
  - Attach a Firestore listener to `users/{uid}/preferences/appPreferences` for live theme changes.
  - Attach listeners to `users/{uid}/conversations/{threadId}` to react to AI classifications (ringer/DND handling).
- UI apply:
  - Map Remote Config + Firestore values into Compose `ColorScheme`, typography, and bubble styles.
  - When the user edits preferences, write back to Firestore and also update Remote Config defaults if we want global templates.

## 6) AI backend (Cloud Functions + Vertex AI)
- Endpoint: HTTPS callable function `classifySms` in `functions/src/beacon/classifySms.ts`.
- Flow:
  1) Android posts `{ uid, threadId, messageId, text, locale }`.
  2) Function checks App Check token (once enabled) and auth context.
  3) Run analysis:
     - Start with Google Cloud Natural Language API for sentiment + entities.
     - (Phase 2) Call a Vertex AI text model (e.g., `text-bison` or custom fine-tune) with a prompt to return `{summary, urgency, confidence}`.
  4) Persist to Firestore at `users/{uid}/conversations/{threadId}`.
  5) Return the payload so the client can optimistically update UI.
- Env/config:
  - Use `functions.config().beacon.gcp_project`, `vertex.location`, and service account with Vertex + NL API scopes.
  - Keep PII out of logs; redact phone numbers in structured logs.

## 7) Analytics & monitoring
- Add Firebase Analytics events:
  - `beacon_theme_changed`, `beacon_pref_saved`, `beacon_ai_urgency` (params: `level`, `confidence`), `beacon_dnd_override`.
- Enable Crashlytics later if needed (plugin already available at root).

## 8) Verification checklist
- [ ] `google-services.json` present in `app-beacon/`.
- [ ] `./gradlew :app-beacon:assembleDebug` succeeds.
- [ ] Remote Config fetch returns defaults on first launch; overrides apply after activation.
- [ ] Firestore listener updates UI without restart.
- [ ] Cloud Function `classifySms` deployed to `pulselink-24899` and writes to Firestore.
- [ ] DND/ringer adjustments respect Android permission model (POST_NOTIFICATIONS, READ_SMS/RECEIVE_SMS where applicable, plus DND access).
