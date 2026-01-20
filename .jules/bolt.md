## 2024-05-22 - Linting Surprises
**Learning:** Found a duplicate `aria-label` prop in `web/src/App.jsx` that was breaking the lint check, even though it wasn't related to my changes.
**Action:** Always run `npm lint` locally before submitting, even if you think your changes are small. It catches pre-existing issues that might block the PR.

## 2024-05-24 - Eager useMemo Performance Traps
**Learning:** `useMemo` executes eagerly on every dependency change. For O(N) operations like search indexing (processing thousands of strings), this blocks the main thread on every render/update, even if the user isn't searching.
**Action:** Use lazy evaluation (like the `useLazySearchIndex` pattern) for expensive derivations that are only needed during specific interactions.

## 2024-05-24 - Unstable Props Defeating React.memo
**Learning:** `React.memo` on `Sidebar` was being defeated by a single unstable prop (`openCommandPalette` passed as inline arrow function). This caused the entire sidebar (including thread lists) to re-render on every `App` render (e.g. typing in inputs), negating the memoization benefit.
**Action:** Always ensure all props passed to `memo`-ized components are stable (using `useCallback`, `useMemo`, or stable references).
