## 2024-05-22 - Linting Surprises
**Learning:** Found a duplicate `aria-label` prop in `web/src/App.jsx` that was breaking the lint check, even though it wasn't related to my changes.
**Action:** Always run `npm lint` locally before submitting, even if you think your changes are small. It catches pre-existing issues that might block the PR.

## 2024-05-24 - Eager useMemo Performance Traps
**Learning:** `useMemo` executes eagerly on every dependency change. For O(N) operations like search indexing (processing thousands of strings), this blocks the main thread on every render/update, even if the user isn't searching.
**Action:** Use lazy evaluation (like the `useLazySearchIndex` pattern) for expensive derivations that are only needed during specific interactions.

## 2024-05-25 - Mocking User State in Playwright
**Learning:** `window.debugSetUser` bypasses `onAuthStateChanged`, leaving `activePanel` in its default state ('beacon'). Tests asserting on authenticated UI ('home') fail blindly.
**Action:** Always pair `window.debugSetUser(user)` with `window.debugSetActivePanel('home')` (or target panel) in test setup.
