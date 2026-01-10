## 2024-05-24 - Firestore Data Enumeration Prevention
**Vulnerability:** The `callerIdCache` collection in `firestore.rules` used `allow read`, which implicitly grants `list` permission. This allowed any authenticated user to download the entire dataset of cached phone numbers and names, a potential privacy leak.
**Learning:** `allow read` is a shorthand for `get` and `list`. For collections containing user data or PII that are accessed via key-value lookups (like caches or user profiles), `allow get` is safer than `allow read`.
**Prevention:** Always verify if `list` permission is actually required. If the app only looks up documents by ID, restrict the rule to `allow get`.
