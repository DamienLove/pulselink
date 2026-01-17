## 2024-05-23 - Input Validation for User-Provided URLs
**Vulnerability:** User input for 'Background Image URL' and 'Extension Endpoint' was not validated, allowing `javascript:` URIs (XSS) or internal network paths (SSRF).
**Learning:** React renders `style={{ backgroundImage: url(...) }}` which can execute JS in some contexts if not sanitized, and `fetch()` can be used for SSRF/CSRF from client context.
**Prevention:** Added `isValidHttpUrl` helper to enforce `http:` or `https:` protocol using the `URL` API.
