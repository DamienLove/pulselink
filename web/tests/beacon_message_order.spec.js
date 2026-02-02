import { test, expect } from '@playwright/test';

test.describe('Beacon Message Ordering', () => {
  test.beforeEach(async ({ page }) => {
    // Enable mock user mode
    await page.goto('/?mock_user=true');
    // Navigate to Beacon Inbox
    await page.locator('.home-card h3', { hasText: 'Beacon Inbox' }).click();
    // Wait for thread list
    await expect(page.locator('.thread-item').first()).toBeVisible();
  });

  test('should display messages in reverse chronological order (newest at top)', async ({ page }) => {
    // Select the first thread
    await page.locator('.thread-item').first().click();

    // Inject messages in the order Firestore would return them (Newest First)
    // Since App.jsx relies on Firestore sorting, we simulate that here.
    await page.evaluate(() => {
        const now = Date.now();
        const messages = [
            { id: 'msg_3', body: 'Newest Message', date: now, type: 2 },
            { id: 'msg_2', body: 'Middle Message', date: now - 5000, type: 1 },
            { id: 'msg_1', body: 'Oldest Message', date: now - 10000, type: 2 }
        ];
        if (window.debugSetMessages) {
            window.debugSetMessages(messages);
        }
    });

    // Wait for messages to render
    await expect(page.locator('.message')).toHaveCount(3);

    // Verify order: Newest (Top) -> Oldest (Bottom)
    const firstMsg = page.locator('.message').first();
    await expect(firstMsg).toContainText('Newest Message');

    const lastMsg = page.locator('.message').last();
    await expect(lastMsg).toContainText('Oldest Message');
  });

  test('should add sent message to the top immediately', async ({ page }) => {
    // Select the first thread
    await page.locator('.thread-item').first().click();

    // Type a new message
    const messageBody = 'Brand new message ' + Date.now();
    await page.locator('.composer-textarea').fill(messageBody);

    // Send
    await page.locator('button', { hasText: 'Send' }).click();

    // Verify it appears at the TOP (first element)
    const firstMsg = page.locator('.message').first();
    await expect(firstMsg).toContainText(messageBody);
  });
});
