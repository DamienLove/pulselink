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

## 2024-05-26 - Auto-expanding Text Inputs
**Learning:** Fixed-height textareas in chat interfaces force users to scroll unnecessarily while composing long messages, breaking their train of thought and hiding context.
**Action:** Implemented an auto-resize mechanism using `scrollHeight` to dynamically adjust the textarea height as the user types, improving the composition experience significantly.

## 2024-05-27 - Visual Required Field Indicators
**Learning:** HTML `required` attribute provides validation but lacks immediate visual feedback for users scanning a form. Screen readers announce "required", but sighted users might miss it until submission fails.
**Action:** Created a reusable `<RequiredIndicator />` component (red asterisk with `aria-hidden="true"`) to pair with `required` inputs, ensuring visual clarity without redundant screen reader announcements.

## 2024-05-28 - Image Load Error Fallbacks
**Learning:** User-provided URLs for avatars often break (404s), resulting in ugly browser-default broken image icons that degrade perceived app quality. Relying solely on "if URL exists" logic is insufficient.
**Action:** Implemented a `useState` + `onError` handler pattern in the `Avatar` component to detect load failures and automatically revert to the deterministic initial fallback, maintaining UI elegance even with bad data.

## 2024-05-29 - Context-Aware Auto-Focus
**Learning:** In chat applications, switching threads or starting a new conversation carries an implicit intent to type. Forcing users to manually click the input field adds unnecessary friction.
**Action:** Implemented logic to automatically focus the message body when selecting a thread, and the "To" address field when starting a new conversation, while disabling this behavior on mobile to prevent virtual keyboard intrusion.
