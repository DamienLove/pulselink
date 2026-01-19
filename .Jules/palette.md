## 2024-05-24 - Search Keyboard Shortcuts
**Learning:** Power users often rely on keyboard shortcuts like `Ctrl+K` or `/` to navigate quickly. Adding these shortcuts reduces friction and aligns with common productivity app patterns.
**Action:** Implemented global shortcut listeners in the Sidebar to focus the search input, and added a visual hint in the placeholder.
## 2024-05-23 - Message Composer Character Count
**Learning:** Users composing SMS/messages often lose track of length. While modern carriers handle concatenation, a subtle indicator provides confidence and precision, especially for "old school" SMS users.
**Action:** Implemented a non-intrusive character count that appears when typing begins. It's positioned absolutely within the input wrapper to save vertical space.
## 2024-05-21 - Extensions Store: Missing Labels on Action Buttons
**Learning:** Lists of items with identical action buttons (e.g., "Install", "Remove") create a confusing experience for screen reader users if the buttons don't mention the item name. Users navigating by buttons hear a repetitive "Install, Install, Remove" without context.
**Action:** Always append the item name to the button's `aria-label` in lists (e.g., `aria-label="Install Beacon Inbox"`).
## 2024-05-25 - Accessible Icon Buttons & Input Helpers
**Learning:** Icon-only buttons (like Sidebar toggle) are invisible to screen readers without `aria-label`. Visual cues like character counts are missed unless linked via `aria-describedby`.
**Action:** Audit all icon buttons for `aria-label` and programmatically link input helper text using `aria-describedby`.

## 2024-05-26 - Unstyled Interactive State Confusion
**Learning:** Interactive elements like avatar selection buttons that lack visual feedback (active state, hover state) and accessibility attributes (`aria-pressed`) create a confusing experience where users cannot verify their selection.
**Action:** Always ensure interactive selection groups have distinct visual active states and appropriate ARIA attributes (`aria-pressed` or `aria-checked`) to communicate state to all users.
