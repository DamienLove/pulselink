## 2024-01-05 - AI Prompt Injection via Scalar Fields
**Vulnerability:** Indirect Prompt Injection (Instruction Hijacking) was possible via the `contactName` field in the AI summary flow. Although HTML-escaped, the field allowed newlines, enabling attackers to inject fake "System:" instructions or mock data blocks outside the intended context.
**Learning:** `escapeHtml` is insufficient for LLM security because it protects against XSS (browser interpretation) but not against structural manipulation of the prompt (LLM interpretation). Newlines are semantic delimiters in many prompt templates.
**Prevention:** Use `sanitizeScalar` to strip newlines and control characters from simple text fields before embedding them in prompts. Treat all user input as untrusted data, not just for HTML tags but for prompt structure.

## 2024-05-23 - [Insecure Default in Extension Approval]
**Vulnerability:** The `onExtensionSubmitted` function was configured to auto-approve all submitted extensions by default ("for now") in what was intended as a dev-only convenience, but without strict environment checks.
**Learning:** Temporary "dev-only" shortcuts often lack robust guards (like checking `FUNCTIONS_EMULATOR`) and can easily slip into production or become permanent features if not caught.
**Prevention:** Avoid "allow all" defaults even in development. Implement the actual security check (e.g., admin role) immediately, or use strict environment variable checks if a bypass is truly needed.

## 2024-05-24 - [Missing Security Headers]
**Vulnerability:** The web application hosting configuration (`firebase.json`) lacked standard security headers (HSTS, X-Frame-Options, X-Content-Type-Options), leaving the app vulnerable to Clickjacking, MIME sniffing, and SSL stripping.
**Learning:** Single Page Applications (SPAs) hosted on static CDNs (like Firebase Hosting) do not inherit security headers by default; they must be explicitly configured in the hosting config file.
**Prevention:** Always audit `firebase.json` (or equivalent) for a `headers` section. Enforce `X-Frame-Options: DENY` for main applications and `Strict-Transport-Security` for all production domains.
## 2024-05-24 - Firestore Data Enumeration Prevention
**Vulnerability:** The `callerIdCache` collection in `firestore.rules` used `allow read`, which implicitly grants `list` permission. This allowed any authenticated user to download the entire dataset of cached phone numbers and names, a potential privacy leak.
**Learning:** `allow read` is a shorthand for `get` and `list`. For collections containing user data or PII that are accessed via key-value lookups (like caches or user profiles), `allow get` is safer than `allow read`.
**Prevention:** Always verify if `list` permission is actually required. If the app only looks up documents by ID, restrict the rule to `allow get`.
