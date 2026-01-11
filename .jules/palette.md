## 2025-05-20 - Focus Management for Inline Confirmation
**Learning:** Replaced buttons (like "Remove" -> "Confirm?") lose focus immediately in React, forcing keyboard users to re-navigate the DOM.
**Action:** Use `autoFocus` on the new button that appears conditionally to ensure continuity of interaction. Add `onKeyDown` for Escape key cancellation to handle the "cancel" intent explicitly.

## 2025-05-20 - Focus Management in Inline Confirmation
**Learning:** Replacing a button with a confirmation state (Confirm/Cancel) traps keyboard users if focus isn't managed. The 'Confirm' button needs `autoFocus` to be discoverable.
**Action:** Always add `autoFocus` to conditional confirmation buttons and handle `Escape` to revert state.
