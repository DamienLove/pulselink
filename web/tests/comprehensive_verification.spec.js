import { test, expect } from '@playwright/test';

test.describe('Comprehensive Feature Verification', () => {
  test.beforeEach(async ({ page }) => {
    // Enable mock user mode
    await page.goto('/?mock_user=true');
    // Wait for the app to settle
    await page.waitForTimeout(2000);
  });

  test('should verify Unified Navigation branding', async ({ page }) => {
    // Enable merged experience via debug hook
    await page.evaluate(() => {
        if (window.debugSetRemoteSettings) {
            window.debugSetRemoteSettings(prev => ({ ...prev, mergedExperienceEnabled: true }));
        }
    });

    // Check for the "PulseLink Unified" text in the sidebar
    // We wait a bit for React to re-render
    await expect(page.locator('.brand-title')).toHaveText('PulseLink Unified', { timeout: 5000 });
  });

  test('should verify Beacon message attachments', async ({ page }) => {
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

  test('should verify Beacon message sending', async ({ page }) => {
    await page.locator('.home-card h3', { hasText: 'Beacon Inbox' }).click();

    // Select a thread
    const thread = page.locator('.thread-item').first();
    await expect(thread).toBeVisible();
    await thread.click();

    // Find composer
    const textarea = page.locator('.composer-textarea');
    await expect(textarea).toBeVisible();

    // Type message
    await textarea.fill('Test message via automation');

    // Click send
    const sendBtn = page.getByTitle('Send (Ctrl+Enter)');
    await expect(sendBtn).toBeVisible();
    await sendBtn.click();

    // Verify interaction
    // The button should show "Sending..." state or a status message should appear
    // We race these conditions because depending on network/mock speed, "Sending..." might flash too fast or persist
    await Promise.race([
        expect(page.getByRole('button', { name: 'Sending...' })).toBeVisible(),
        expect(page.locator('.compose-status')).toBeVisible()
    ]);
  });

  test('should verify Features (Extensions) functionality', async ({ page }) => {
    // Navigate to Features (Extensions)
    const btn = page.locator('.nav-item[title="Features"]');
    await btn.click();

    await expect(page.getByRole('heading', { name: 'Features', exact: true })).toBeVisible();

    // Check for "Crash Detection" card presence
    const crashCard = page.locator('.home-card h3', { hasText: 'Crash Detection' });
    await expect(crashCard).toBeVisible();

    // Find Truecaller card to test toggle
    const truecallerCard = page.locator('.home-card').filter({ hasText: 'Truecaller Caller ID' });
    await expect(truecallerCard).toBeVisible();

    const toggleBtn = truecallerCard.locator('button');
    await expect(toggleBtn).toBeVisible();

    const initialText = await toggleBtn.innerText();
    // Click to toggle
    await toggleBtn.click();

    // Expect text to change (Install <-> Remove)
    await expect(toggleBtn).not.toHaveText(initialText, { timeout: 5000 });
  });

  test('should verify Settings search and visibility', async ({ page }) => {
    await page.locator('.nav-item[title="Settings"]').click();

    await expect(page.getByRole('heading', { name: 'Settings', exact: true })).toBeVisible();

    // Search for "Web"
    const searchInput = page.locator('.settings-search-input');
    await searchInput.fill('Web');

    // "Web preferences" section should remain visible
    await expect(page.getByText('Web preferences')).toBeVisible();

    // "Account" section should be hidden as it doesn't match "Web"
    await expect(page.getByText('User ID')).toBeHidden();

    // Clear search by clicking the clear button
    const clearBtn = page.locator('.settings-search-container .ghost-btn.icon-only');
    await clearBtn.click();

    // "User ID" should be visible again
    await expect(page.getByText('User ID')).toBeVisible();
  });

  test('should verify Contacts list and search', async ({ page }) => {
    await page.locator('.nav-item[title="Contacts"]').click();
    await expect(page.getByRole('heading', { name: 'Contacts' })).toBeVisible();

    // Verify mock contact is present (Alice)
    await expect(page.getByText('Alice')).toBeVisible();

    // Test Search
    const searchInput = page.locator('.settings-search-input');
    await searchInput.fill('Alice');
    await expect(page.getByText('Alice')).toBeVisible();

    await searchInput.fill('ZNonExistent');
    await expect(page.getByText('Alice')).toBeHidden();
    await expect(page.getByText('No contacts match that search')).toBeVisible();
  });

  test('should verify RingerSong functionality', async ({ page }) => {
    // Navigate to RingerSong
    await page.locator('.nav-item[title="RingerSong"]').click();
    await expect(page.locator('.ringersong-header h3')).toHaveText('RingerSong');

    // Check search input
    const searchInput = page.getByPlaceholder('Search Spotify for songs...');
    await expect(searchInput).toBeVisible();

    // Check empty state or playlist
    // In mock mode we start with empty playlist
    await expect(page.getByText('Your playlist is empty')).toBeVisible();
  });

  test('should verify Emergency Map functionality', async ({ page }) => {
     // Navigate to Map
     await page.locator('.nav-item[title="Map"]').click();
     await expect(page.getByRole('heading', { name: 'Emergency map' })).toBeVisible();

     // Check map container exists
     await expect(page.locator('.map-canvas')).toBeVisible();

     // Check controls
     await expect(page.getByText('Incoming only')).toBeVisible();
     await expect(page.getByText('Alert type')).toBeVisible();
  });

  test('should verify Themes Gallery', async ({ page }) => {
    await page.locator('.nav-item[title="Themes"]').click();
    await expect(page.getByRole('heading', { name: 'Theme Gallery' })).toBeVisible();

    // Check for "Publish your theme" card
    await expect(page.getByText('Publish your theme')).toBeVisible();

    // Check for "Quick presets"
    await expect(page.getByText('Quick presets')).toBeVisible();
  });

  test('should verify PulseLink Profile', async ({ page }) => {
    await page.locator('.nav-item[title="PulseLink"]').click();
    await expect(page.getByRole('heading', { name: 'PulseLink', exact: true })).toBeVisible();

    // Check Public profile section
    await expect(page.getByRole('heading', { name: 'Public profile' })).toBeVisible();

    // Check Display Name input
    // Finding input with value "Test User" (from mock)
    // We use getByLabel because the input is wrapped in a label with text "Display name"
    await expect(page.getByLabel('Display name')).toHaveValue('Test User');
  });
});
