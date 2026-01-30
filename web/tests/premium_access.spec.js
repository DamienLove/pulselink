import { test, expect } from '@playwright/test';

test.describe('Premium Access Gating', () => {

  test('Non-premium user should see upsell', async ({ page }) => {
    // Navigate with mock_user=true and premium=false
    await page.goto('/?mock_user=true&premium=false&pro=false');
    await page.waitForTimeout(1000); // Wait for React to settle

    // Navigate to Beacon Inbox
    await page.locator('.home-card h3', { hasText: 'Beacon Inbox' }).click();

    // Verify Upsell content
    await expect(page.getByRole('heading', { name: 'Beacon Inbox' })).toBeVisible();
    await expect(page.locator('.badge-premium')).toHaveText('Premium Feature');
    await expect(page.getByText('Upgrade to PulseLink Pro or Premium')).toBeVisible();

    // Ensure no messages are shown (MessageComposer should not be visible)
    // Note: The empty state for premium users also has "Beacon Inbox" text but different structure.
    // Premium empty state has "Select a thread or start a new message".
    await expect(page.getByText('Select a thread or start a new message')).not.toBeVisible();
    await expect(page.getByPlaceholder('Type a message...')).not.toBeVisible();
  });

  test('Premium user should see inbox', async ({ page }) => {
    // Navigate with mock_user=true (default premium=true)
    await page.goto('/?mock_user=true');
    await page.waitForTimeout(1000);

    // Navigate to Beacon Inbox
    await page.locator('.home-card h3', { hasText: 'Beacon Inbox' }).click();

    // Verify Inbox content
    // We expect the MessageComposer or the "Select a thread" prompt (if empty)
    // Since mock data injects threads, we should see threads or at least not the upsell.

    await expect(page.locator('.badge-premium')).not.toBeVisible();
    await expect(page.getByText('Upgrade to PulseLink Pro or Premium')).not.toBeVisible();

    // Should show composer or thread list
    // Mock user usually has threads injected.
    const thread = page.locator('.thread-item').first();
    // Wait for thread or composer
    await Promise.any([
        expect(thread).toBeVisible(),
        expect(page.getByPlaceholder('Type a message...')).toBeVisible()
    ]);
  });

});
