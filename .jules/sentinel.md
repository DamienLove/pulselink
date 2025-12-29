## 2024-05-24 - Firestore Identity Spoofing Fix
**Vulnerability:** The `users_by_phone` collection allowed any authenticated user to create a mapping for *any* phone number to their own UID, enabling identity spoofing.
**Learning:** Firestore rules do not automatically link `request.auth.token` attributes to document paths. You must explicitly enforce that the path variable (e.g., `phoneNumber`) matches the auth token claim (`request.auth.token.phone_number`).
**Prevention:** Always verify that user-claimable identifiers in document paths match the immutable claims in the auth token.

## 2024-05-25 - Shared Cache Poisoning
**Vulnerability:** The `callerIdCache` collection allowed any authenticated user to write to any document, enabling a malicious user to poison the global shared caller ID cache with false information (e.g., marking a safe number as spam).
**Learning:** Client-side "crowdsourcing" of shared data (like global caches) is inherently insecure if implemented via direct database writes (`allow write: if request.auth != null`). Malicious clients can always bypass client-side logic.
**Prevention:** Shared/global data structures must be read-only for clients. Writes must be mediated by a trusted backend (Cloud Function) that validates the source and content, or the feature must be redesigned to be user-specific (personal cache).

## 2024-05-25 - Secrets Bundled in Client Code
**Vulnerability:** The `VITE_SPOTIFY_CLIENT_SECRET` environment variable was used in `App.jsx`, causing the secret credential to be bundled into the public frontend code. This allows any user to extract the secret and impersonate the application.
**Learning:** Frontend build tools (Vite, Webpack) embed `VITE_*` or `REACT_APP_*` variables into the static JS bundle. Secrets that must remain private (like API client secrets) must NEVER be accessed in frontend code, even via env vars.
**Prevention:** Move all third-party API interactions requiring secrets to a trusted backend environment (Cloud Functions). The frontend should call the backend function, which handles the authentication securely.
