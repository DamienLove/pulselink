## 2024-05-22 - Linting Surprises
**Learning:** Found a duplicate `aria-label` prop in `web/src/App.jsx` that was breaking the lint check, even though it wasn't related to my changes.
**Action:** Always run `npm lint` locally before submitting, even if you think your changes are small. It catches pre-existing issues that might block the PR.

## 2024-05-24 - Eager useMemo Performance Traps
**Learning:** `useMemo` executes eagerly on every dependency change. For O(N) operations like search indexing (processing thousands of strings), this blocks the main thread on every render/update, even if the user isn't searching.
**Action:** Use lazy evaluation (like the `useLazySearchIndex` pattern) for expensive derivations that are only needed during specific interactions.

## 2024-05-27 - Array Allocation in Render Loops
**Learning:** `[...a, ...b].filter(Boolean).join(' • ')` is a common pattern for joining strings with separators, but it creates multiple intermediate arrays (spread, filter result) on every render.
**Action:** For simple string joining in hot paths (like list items), use imperative string concatenation or template literals to avoid unnecessary object allocation.

## 2024-05-28 - Firestore Snapshot Optimization in React
**Learning:** Naive `snapshot.docs.map()` in Firestore listeners creates new object references for *every* document on every update, breaking `React.memo` downstream even for unchanged items.
**Action:** Use `snapshot.docChanges()` with a local `Map` cache inside the `useEffect` closure to maintain stable object references for unchanged documents, significantly reducing re-renders in large lists.
