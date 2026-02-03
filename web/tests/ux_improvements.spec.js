import { test, expect } from '@playwright/test';

test.describe('UX Improvements Verification', () => {
  test.beforeEach(async ({ page }) => {
    // Enable mock user mode
    await page.goto('/?mock_user=true');
    // Wait for the app to settle
    await page.waitForTimeout(2000);
  });

  test('should display full date tooltip on message timestamp', async ({ page }) => {
    // Navigate to Beacon Inbox
    await page.locator('.home-card h3', { hasText: 'Beacon Inbox' }).click();

    // Select the first thread
    const thread = page.locator('.thread-item').first();
    await expect(thread).toBeVisible();
    await thread.click();

    // Inject a message with a specific timestamp
    // Using a fixed timestamp to verify formatting
    const testDate = new Date('2023-12-25T12:00:00Z').getTime();

    await page.evaluate((date) => {
        if (window.debugSetMessages) {
            window.debugSetMessages([
                {
                    id: 'msg_ux_test',
                    body: 'UX Test Message',
                    date: date,
                    type: 2, // sent
                }
            ]);
        }
    }, testDate);

    // Get the message time element
    const timeEl = page.locator('.message-time').first();
    await expect(timeEl).toBeVisible();

    // Check for the title attribute
    const title = await timeEl.getAttribute('title');
    console.log('Found timestamp title:', title);

    expect(title).toBeTruthy();
    // The exact format depends on locale, but it should definitely contain the year
    expect(title).toContain('2023');
  });
});
