# Web Client Verification Tests

This directory contains Playwright tests for verifying the PulseLink web client.

## Running Tests

1.  Ensure dependencies are installed:
    ```bash
    cd web
    pnpm install
    npx playwright install chromium
    ```

2.  Run the tests:
    ```bash
    npx playwright test tests/verification.spec.cjs
    ```

## Test Scope

*   `verification.spec.cjs`: Verifies that the application loads and displays the login screen for unauthenticated users.
*   *Note*: Testing authenticated features (Beacon Inbox, Settings, etc.) requires a test user or a mocked authentication provider.
