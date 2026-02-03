// web/tests/beacon_inbox_verification.spec.js
/* eslint-env node */
import { test, expect } from '@playwright/test';

test.describe('Beacon Inbox Verification', () => {
  test('should display synced messages for premium user', async ({ page }) => {
    // Use mock_user=true to bypass Firestore side-effects and load default mock data
    await page.goto('/?mock_user=true');

    // Click on Beacon in the sidebar to view messages
    // Use specific selector to avoid ambiguity with home screen tiles
    await page.locator('.sidebar button[title="Beacon"]').click();

    // Wait for the thread to appear in the sidebar
    await expect(page.getByText('Test Contact')).toBeVisible({ timeout: 10000 });
    await expect(page.getByText('Hello World')).toBeVisible();

    // Click the thread to open it
    await page.getByText('Test Contact').click();

    // Verify chat header matches the phone number/name
    await expect(page.locator('.chat-header')).toContainText('+15559998888');

    // Verify composer is present
    const composer = page.locator('.composer-textarea');
    await expect(composer).toBeVisible();
    await composer.fill('This is a test message');

    // Verify send button is active
    const sendBtn = page.getByRole('button', { name: 'Send' });
    await expect(sendBtn).toBeEnabled();
  });
});
