## 2024-05-24 - Improved Async Feedback
**Learning:** Users lack confidence in async operations (like saving profiles) when buttons don't provide immediate visual feedback (disabling/loading state).
**Action:** Always wrap async handlers in a `try/finally` block that toggles an `isProcessing` state, and use this state to disable buttons and update their labels (e.g., "Save" -> "Saving...").

## 2024-05-24 - Empty States for Lists
**Learning:** Empty lists (like zero threads) in sidebars can look like broken UI or loading errors to new users.
**Action:** Always provide a semantic empty state (even if simple text) for lists that can be legitimately empty, guiding the user on what to expect or do next.

## 2024-05-25 - Semantic Active States
**Learning:** Visual active states (CSS classes) often get implemented without the corresponding semantic HTML attributes (`aria-current`), leaving screen reader users guessing which view is active.
**Action:** Ensure navigation components always pair `.active` visual classes with `aria-current="page"` (for tabs/nav) or `aria-current="true"` (for lists) to expose state to assistive technology.

## 2024-05-25 - Reusable Icon Buttons
**Learning:** Micro-interactions (like copy buttons) are frequently reused but often reimplemented with inconsistent inline styles.
**Action:** Standardize small action buttons using a utility class (e.g., `.ghost-btn.icon-only`) to enforce consistent touch targets (32px+) and alignment without cluttering JSX.
