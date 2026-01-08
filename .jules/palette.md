## 2024-05-24 - List Item Loading States
**Learning:** Managing async actions (like deletion) in parent components for large lists causes expensive full-list re-renders and UI jank.
**Action:** Implement local `isLoading` / `isDeleting` state within `memo`-ized list item components to provide immediate, isolated feedback (spinner + disabled state) without re-rendering siblings.
