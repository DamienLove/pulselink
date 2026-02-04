// web/tests/features.spec.js
/* eslint-env node */
import { test, expect } from '@playwright/test';

test.describe('New Features', () => {
  test.beforeEach(async ({ page }) => {
    // Navigate with mock user
    await page.goto('/?mock_user=true');
    // Go to Beacon using specific selector to avoid ambiguity
    await page.locator('.sidebar button[title="Beacon"]').click();
  });

  test('Scheduled Messages UI', async ({ page }) => {
    // Select the first thread (mocked in App.jsx)
    await page.locator('.thread-item').first().click();

    // Verify Composer is visible
    await expect(page.getByPlaceholder('Type a message...')).toBeVisible();

    // Click Schedule button (Clock Icon) - uses title/aria-label "Schedule message"
    await page.locator('button[title="Schedule message"]').click();

    // Verify Date Picker is visible
    await expect(page.locator('input[type="datetime-local"]')).toBeVisible();

    // Fill date to trigger button text change
    // Note: We don't need to actually fill it validly for the text to change if logic is just based on value presence
    // But datetime-local input requires specific format
    // Just type something
    await page.locator('input[type="datetime-local"]').fill('2026-01-01T12:00');

    // Verify "Schedule" button text changes (Send button becomes Schedule)
    // Use exact match to avoid matching the "Schedule message" icon button
    await expect(page.getByRole('button', { name: 'Schedule', exact: true })).toBeVisible();

    // Cancel schedule
    await page.getByTitle('Cancel schedule').click();
    await expect(page.locator('input[type="datetime-local"]')).not.toBeVisible();
    await expect(page.getByRole('button', { name: 'Send' })).toBeVisible();
  });

  test('Smart Replies', async ({ page }) => {
    // Select thread
    await page.locator('.thread-item').first().click();

    // Inject a received message ending with "?"
    await page.evaluate(() => {
      window.debugSetMessages([
        {
          id: 'msg1',
          body: 'Where are you?',
          date: Date.now(),
          type: 1 // Received
        }
      ]);
    });

    // Verify chips appear
    await expect(page.getByRole('button', { name: 'At home' })).toBeVisible();
    await expect(page.getByRole('button', { name: 'On my way' })).toBeVisible();

    // Click a chip
    await page.getByRole('button', { name: 'On my way' }).click();

    // Verify text inserted
    await expect(page.getByPlaceholder('Type a message...')).toHaveValue('On my way');
  });

  test('Rich Link Previews', async ({ page }) => {
    // Mock the Cloud Function call
    await page.route('**/getLinkPreview', async route => {
      const json = {
        data: {
          title: 'Example Domain',
          description: 'This domain is for use in illustrative examples.',
          image: 'https://example.com/image.png',
          url: 'https://example.com'
        }
      };
      await route.fulfill({ json });
    });

    // Select thread
    await page.locator('.thread-item').first().click();

    // Inject message with URL
    await page.evaluate(() => {
      window.debugSetMessages([
        {
          id: 'msg2',
          body: 'Check this out: https://example.com',
          date: Date.now(),
          type: 2 // Sent
        }
      ]);
    });

    // Verify Link Preview renders
    // It might take a moment or need to wait for the mocked response
    await expect(page.locator('.link-preview-card')).toBeVisible();
    await expect(page.locator('.link-preview-title')).toHaveText('Example Domain');
    await expect(page.locator('.link-preview-domain')).toHaveText('example.com');
  });
});
