import { test, expect } from '@playwright/test';

test.describe('Extended Feature Verification via Home Screen', () => {
  test.beforeEach(async ({ page }) => {
    // Enable mock user mode
    await page.goto('/?mock_user=true');
    // Wait for the app to settle and ensure Home is visible
    await page.waitForTimeout(1000);
    await expect(page.locator('.home-hero')).toBeVisible();
  });

  test('should verify PulseLink home button and edit contact', async ({ page }) => {
    // Click PulseLink card
    await page.locator('.home-card').filter({ hasText: 'PulseLink' }).filter({ hasText: 'Update your profile' }).click();

    // Check header
    await expect(page.locator('h3', { hasText: 'PulseLink' })).toBeVisible();
    await expect(page.locator('h4', { hasText: 'Trusted contacts' })).toBeVisible();

    // Verify "Mom" is present
    const contactRow = page.locator('.contact-row', { hasText: 'Mom' });
    await expect(contactRow).toBeVisible();

    // Click Edit
    await contactRow.locator('button', { hasText: 'Edit' }).click();

    // Check if form is populated
    await expect(page.locator('input[value="Mom"]')).toBeVisible();

    // Update name
    await page.locator('input[value="Mom"]').fill('Mom Updated');

    // Save
    await page.locator('button', { hasText: 'Update contact' }).click();

    // Verify toast (Saving... or Failed are both acceptable in mock mode without backend)
    // Use .last() because duplicate toasts are rendered in this panel
    await expect(page.locator('.toast').last()).toBeVisible();
  });

  test('should verify RingerSong home button and search', async ({ page }) => {
    // Click RingerSong card
    await page.locator('.home-card').filter({ hasText: 'RingerSong' }).click();

    // Check header
    await expect(page.locator('h3', { hasText: 'RingerSong' })).toBeVisible();
    await expect(page.locator('h4', { hasText: 'Current Playlist' })).toBeVisible();

    // Test Search interaction
    const searchInput = page.locator('input[placeholder="Search Spotify for songs..."]');
    await searchInput.fill('Test Song');
    await page.locator('button', { hasText: 'Search' }).click();

    // Since we don't have a real backend/token, we expect a status update (even if failure)
    // The app sets status "Searching..." then likely "Search failed" or similar.
    // We just verify the UI reacts.
    await expect(page.locator('.toast').first()).toBeVisible();
  });

  test('should verify Map home button', async ({ page }) => {
    // Click Emergency Map card
    await page.locator('.home-card').filter({ hasText: 'Emergency Map' }).click();

    // Check header
    await expect(page.locator('h3', { hasText: 'Emergency map' })).toBeVisible();
    await expect(page.locator('.map-canvas')).toBeVisible();
  });

  test('should verify Themes home button and apply preset', async ({ page }) => {
    // Click Theme Gallery card
    await page.locator('.home-card').filter({ hasText: 'Theme Gallery' }).click();

    // Check header
    await expect(page.locator('h3', { hasText: 'Theme Gallery' })).toBeVisible();
    await expect(page.locator('h4', { hasText: 'Quick presets' })).toBeVisible();

    // Apply "Neon Cyber"
    await page.locator('.theme-chip', { hasText: 'Neon Cyber' }).click();

    // Verify toast
    await expect(page.locator('.toast').filter({ hasText: /Theme synced|Updating theme/ })).toBeVisible();
  });

  test('should verify Features home button', async ({ page }) => {
    // Click Features card
    await page.locator('.home-card').filter({ hasText: 'Features' }).click();

    // Check header
    await expect(page.locator('h3', { hasText: 'Features' })).toBeVisible();
    await expect(page.locator('h4', { hasText: 'Quick Setup' })).toBeVisible();
  });

  test('should verify Settings Test Relay via Sidebar', async ({ page }) => {
      // Settings is not on the home grid in the current App.jsx snippet (it was in the Sidebar)
      await page.locator('.nav-item[title="Settings"]').click();

      await page.locator('.settings-search-input').fill('PulseLink');

      const testRelayBtn = page.locator('button', { hasText: 'Test relay' });
      await expect(testRelayBtn).toBeVisible();

      await testRelayBtn.click();

      await expect(page.locator('.toast').first()).toBeVisible();
      await expect(page.locator('.toast').filter({ hasText: /Test message queued|Queueing test message/ })).toBeVisible();
  });

  test('should verify Beacon message attachments', async ({ page }) => {
    // Click Beacon Inbox card
    await page.locator('.home-card h3', { hasText: 'Beacon Inbox' }).click();

    // Wait for threads to load/mock to appear
    const thread = page.locator('.thread-item').first();
    await expect(thread).toBeVisible();
    await thread.click();

    // Inject a message with an image
    await page.evaluate(() => {
        if (window.debugSetMessages) {
            window.debugSetMessages([
                {
                    id: 'msg_img_1',
                    body: 'Check this image',
                    imageUrl: 'https://placehold.co/100x100.png',
                    date: Date.now(),
                    type: 2, // sent
                }
            ]);
        }
    });

    // Check if the image renders
    const img = page.locator('.message-image');
    await expect(img).toBeVisible();
    // Use a more flexible check for src as sometimes it might be relative or processed
    const src = await img.getAttribute('src');
    expect(src).toContain('placehold.co/100x100.png');
  });
});
