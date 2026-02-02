import { test, expect } from '@playwright/test';

test.describe('Message Composer Focus', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/?mock_user=true');
    // Navigate to Beacon panel
    await page.locator('.nav-item[title="Beacon"]').click();
  });

  test('should focus "To" input when switching to new message', async ({ page }) => {
    // First, select a thread to ensure we are NOT in new message mode
    await page.locator('.thread-item').first().click();
    await expect(page.locator('.composer-textarea')).toBeFocused();

    // Now Click "New" button in sidebar
    await page.locator('button[aria-label="Start new conversation"]').click();

    // Check if address input is focused
    await expect(page.locator('#compose-address')).toBeFocused();
  });

  test('should focus message body when selecting a thread', async ({ page }) => {
    // Click the first thread item
    await page.locator('.thread-item').first().click();

    // Check if textarea is focused
    await expect(page.locator('.composer-textarea')).toBeFocused();
  });

  test('should blur message body when pressing Escape', async ({ page }) => {
    // Select a thread to focus textarea
    await page.locator('.thread-item').first().click();
    await expect(page.locator('.composer-textarea')).toBeFocused();

    // Press Escape
    await page.keyboard.press('Escape');

    // Check if textarea is NOT focused
    await expect(page.locator('.composer-textarea')).not.toBeFocused();
  });
});
