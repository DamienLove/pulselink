import { test, expect } from '@playwright/test';

test.describe('Inbox Flow', () => {
  test.beforeEach(async ({ page }) => {
    // 1. Visit the app with mock user
    await page.goto('/?mock_user=true');

    // Wait for Home screen
    await expect(page.getByText('Choose what you want to manage')).toBeVisible();

    // Click Beacon Inbox card
    await page.getByText('Beacon Inbox').click();

    // Wait for the sidebar search to be visible, indicating Beacon panel loaded
    await expect(page.locator('.sidebar-search-input-field')).toBeVisible();
  });

  test('should focus address input when clicking New', async ({ page }) => {
    // Click "New" button in sidebar (secondary-btn)
    await page.getByRole('button', { name: 'New' }).click();

    // Expect focus to be on the "To" input
    await expect(page.locator('#compose-address')).toBeFocused();
  });

  test('should focus message body when selecting a thread', async ({ page }) => {
    // Wait for threads to load (mock user provides them)
    await expect(page.locator('.thread-item').first()).toBeVisible();

    // Click the first thread item
    await page.locator('.thread-item').first().click();

    // Expect focus to be on the textarea
    await expect(page.locator('.composer-textarea')).toBeFocused();
  });

  test('should show optimistic message immediately', async ({ page }) => {
     // Wait for threads
     await expect(page.locator('.thread-item').first()).toBeVisible();

     // Select a thread first
     await page.locator('.thread-item').first().click();

     // Wait for textarea to be visible
     const textarea = page.locator('.composer-textarea');
     await expect(textarea).toBeVisible();

     // Type a message
     const testMessage = `Test message ${Date.now()}`;
     await textarea.fill(testMessage);

     // Click send
     await page.getByRole('button', { name: 'Send' }).click();

     // Verify it appears in the list (message bubble)
     await expect(page.locator('.message-bubble', { hasText: testMessage })).toBeVisible();

     // Verify textarea is cleared (optional, but good check for success)
     await expect(textarea).toBeEmpty();
  });
});
