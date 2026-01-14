## 2024-05-23 - Settings Search UX Consistency
**Learning:** Users expect search inputs to behave consistently across an application. If one search bar (Contacts) has a "Clear" button, others (Settings) should too. Accessibility labels are also critical for search inputs that might rely solely on placeholders visually.
**Action:** When adding new search or filter inputs, always include a mechanism to clear the input easily and ensure `aria-label` is present. Check for existing patterns in the app to maintain consistency.
