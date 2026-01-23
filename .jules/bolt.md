## 2024-05-22 - Linting Surprises
**Learning:** Found a duplicate `aria-label` prop in `web/src/App.jsx` that was breaking the lint check, even though it wasn't related to my changes.
**Action:** Always run `npm lint` locally before submitting, even if you think your changes are small. It catches pre-existing issues that might block the PR.

## 2024-05-24 - Eager useMemo Performance Traps
**Learning:** `useMemo` executes eagerly on every dependency change. For O(N) operations like search indexing (processing thousands of strings), this blocks the main thread on every render/update, even if the user isn't searching.
**Action:** Use lazy evaluation (like the `useLazySearchIndex` pattern) for expensive derivations that are only needed during specific interactions.

## 2024-05-25 - React Monolith Optimization
**Learning:** In a large, monolithic component like `App.jsx`, performance wins often come from stabilizing props passed to memoized children and optimizing data transformation loops. Found that `contactMapper` was creating excessive intermediate arrays (filter/map/join) for every contact on every re-index.
**Action:** Replace inefficient array methods (filter/map) with a single imperative loop or string concatenation when processing large lists for search indexing. Move utility functions (like `toMillis`) to module scope to avoid re-definition and ensure consistent logic across the file.
