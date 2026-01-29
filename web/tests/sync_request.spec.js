import { test, expect } from '@playwright/test';

test.describe('Sync Request Feature', () => {
  test('should show Request Phone Sync button when premium user has no messages', async ({ page }) => {
    // Navigate with mock user (defaults to Premium)
    await page.goto('http://localhost:5173/?mock_user=true');

    // Wait for app to load
    await expect(page.locator('.sidebar')).toBeVisible();

    // Use debug hook to clear lines and threads to simulate empty state
    await page.evaluate(() => {
      window.debugSetLines([]);
      window.debugSetLegacyThreads([]);
      window.debugSetActivePanel('beacon');
    });

    // Check for the new empty state in the main panel
    await expect(page.locator('text=Welcome to Beacon Inbox')).toBeVisible();
    await expect(page.locator('button:has-text("Request Sync from Phone")').first()).toBeVisible();

    // Check for the button in the sidebar (might need to expand sidebar if collapsed, but default is expanded)
    // The sidebar text also contains "Request Phone Sync"
    await expect(page.locator('.sidebar-placeholder button:has-text("Request Phone Sync")')).toBeVisible();
  });
});
