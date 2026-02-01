import { test, expect } from '@playwright/test';

test.describe('Thread Timestamp', () => {
  test('should display timestamp in thread list', async ({ page }) => {
    // Navigate with mock_user
    await page.goto('/?mock_user=true');

    // Ensure we are on the Beacon panel (it might be default or click needed)
    // Just to be safe, click the Beacon nav item
    await page.locator('.nav-item[title="Beacon"]').click();

    // Check for thread item
    const threadItem = page.locator('.thread-item').first();
    await expect(threadItem).toBeVisible();

    // Check for date element
    const dateEl = threadItem.locator('.thread-date');
    await expect(dateEl).toBeVisible();

    // Verify it contains a time/date string
    // Since mock data uses Date.now(), it should match time format (e.g. 10:30 AM)
    const text = await dateEl.textContent();
    console.log('Thread date text:', text);

    // Regex matches:
    // 10:30 (Time)
    // Yesterday
    // Oct 24 (Date)
    expect(text).toMatch(/(\d{1,2}:\d{2})|(Yesterday)|(\w{3}\s\d{1,2})/);
  });
});
