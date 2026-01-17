## 2024-05-23 - React List Virtualization & Memoization
**Learning:** Large lists in React (like contact lists) that are rendered inline within a complex parent component (`App.jsx`) will strictly re-render every item whenever the parent re-renders, even if the list data hasn't changed.
**Action:** Extract list items into separate `memo`ized components. This allows React to skip re-rendering individual rows if their props (data) haven't changed, significantly reducing the main thread blocking time during interactions with other parts of the UI (like typing in inputs).

## 2025-02-23 - Double Rendering in Map & Gallery
**Learning:** I discovered that the Theme Gallery and Map Alert lists were being rendered TWICE. Once via the `ThemeGalleryItem`/`MapAlertItem` components, and then IMMEDIATELY AGAIN via an inline map block right next to it. This effectively doubled the DOM nodes for these lists and caused duplicate key warnings.
**Action:** Always check adjacent blocks of code when refactoring. It seems a previous refactor introduced the component but forgot to delete the old inline code.

## 2024-05-22 - State Colocation & React.memo
**Learning:** Lifting state to the root component (`App.jsx`) for high-frequency inputs (like a message composer) forces the entire component tree to re-render on every keystroke. This causes noticeable lag, especially when the tree contains expensive lists or maps.
**Action:** Extract high-frequency state into a smaller, dedicated child component (e.g., `MessageComposer`) and wrap it in `React.memo`. This isolates the re-renders to just that small component, leaving the heavy parent and siblings untouched during typing.

## 2025-02-24 - Search Index Pre-computation
**Learning:** Filtering large arrays (like `deviceContacts`) on every keystroke using complex inline string manipulation (multiple `.toString().toLowerCase()` calls per item) causes significant main thread work.
**Action:** Pre-compute a "search index" (e.g., a normalized string) in a separate `useMemo` that only updates when the source data changes. The filter step then becomes a simple `includes()` check, keeping the UI responsive during typing.

## 2025-02-25 - Sidebar Search Isolation
**Learning:** The `Sidebar` search state was lifted to `App.jsx`, causing the entire application (including heavy Map and Message lists) to re-render on every keystroke of the thread search input.
**Action:** Moved the search state and filtering logic *inside* the `Sidebar` component. This isolates the high-frequency re-renders to the Sidebar itself, preventing the global app re-render, while still accepting the raw data from the parent.

## 2025-02-26 - Key-based State Reset
**Learning:** Using `useEffect` to sync local component state with props (e.g., updating a form when `selectedItem` changes) causes a "double render" effect (Parent render -> Child render -> Effect -> Child state update -> Child re-render).
**Action:** Use the `key` prop on the child component (e.g., `key={selectedItem.id}`) to force React to unmount and remount the component when the selection changes. This allows initializing state directly from props in the constructor/useState, avoiding the `useEffect` entirely and saving one full render cycle.
