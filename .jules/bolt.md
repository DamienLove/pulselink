## 2024-05-23 - React List Virtualization & Memoization
**Learning:** Large lists in React (like contact lists) that are rendered inline within a complex parent component (`App.jsx`) will strictly re-render every item whenever the parent re-renders, even if the list data hasn't changed.
**Action:** Extract list items into separate `memo`ized components. This allows React to skip re-rendering individual rows if their props (data) haven't changed, significantly reducing the main thread blocking time during interactions with other parts of the UI (like typing in inputs).

## 2025-02-23 - Double Rendering in Map & Gallery
**Learning:** I discovered that the Theme Gallery and Map Alert lists were being rendered TWICE. Once via the `ThemeGalleryItem`/`MapAlertItem` components, and then IMMEDIATELY AGAIN via an inline map block right next to it. This effectively doubled the DOM nodes for these lists and caused duplicate key warnings.
**Action:** Always check adjacent blocks of code when refactoring. It seems a previous refactor introduced the component but forgot to delete the old inline code.

## 2024-05-22 - State Colocation & React.memo
**Learning:** Lifting state to the root component (`App.jsx`) for high-frequency inputs (like a message composer) forces the entire component tree to re-render on every keystroke. This causes noticeable lag, especially when the tree contains expensive lists or maps.
**Action:** Extract high-frequency state into a smaller, dedicated child component (e.g., `MessageComposer`) and wrap it in `React.memo`. This isolates the re-renders to just that small component, leaving the heavy parent and siblings untouched during typing.
