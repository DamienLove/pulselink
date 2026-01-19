## 2024-05-24 - Search Keyboard Shortcuts
**Learning:** Power users often rely on keyboard shortcuts like `Ctrl+K` or `/` to navigate quickly. Adding these shortcuts reduces friction and aligns with common productivity app patterns.
**Action:** Implemented global shortcut listeners in the Sidebar to focus the search input, and added a visual hint in the placeholder.
## 2024-05-23 - Message Composer Character Count
**Learning:** Users composing SMS/messages often lose track of length. While modern carriers handle concatenation, a subtle indicator provides confidence and precision, especially for "old school" SMS users.
**Action:** Implemented a non-intrusive character count that appears when typing begins. It's positioned absolutely within the input wrapper to save vertical space.
## 2024-05-21 - Extensions Store: Missing Labels on Action Buttons
**Learning:** Lists of items with identical action buttons (e.g., "Install", "Remove") create a confusing experience for screen reader users if the buttons don't mention the item name. Users navigating by buttons hear a repetitive "Install, Install, Remove" without context.
**Action:** Always append the item name to the button's `aria-label` in lists (e.g., `aria-label="Install Beacon Inbox"`).
