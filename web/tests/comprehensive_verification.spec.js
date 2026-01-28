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
    await page.locator('.nav-item[title="Beacon"]').click();

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

  test('should verify Beacon message composition', async ({ page }) => {
    await page.locator('.nav-item[title="Beacon"]').click();
    const thread = page.locator('.thread-item').first();
    await expect(thread).toBeVisible();
    await thread.click();

    const textarea = page.locator('textarea[placeholder*="Type a message"]');
    await expect(textarea).toBeVisible();
    await textarea.fill('Hello World');
    await expect(textarea).toHaveValue('Hello World');

    // Check char counter
    const counter = page.locator('#message-char-count');
    await expect(counter).toHaveText('11');
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

    // Verify mock contact is present (Alice is usually in the mock data from App.jsx)
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
    await page.locator('.nav-item[title="RingerSong"]').click();
    await expect(page.getByRole('heading', { name: 'RingerSong', exact: true })).toBeVisible();

    // Verify playlist header
    await expect(page.getByText('Current Playlist')).toBeVisible();

    // Verify search input
    const searchInput = page.locator('input[placeholder="Search Spotify for songs..."]');
    await expect(searchInput).toBeVisible();

    // Mock search results via evaluate since we can't easily mock fetch in this context without more setup
    // But we can test the UI interaction
    await searchInput.fill('Test Song');
    const searchBtn = page.getByRole('button', { name: 'Search' });
    await expect(searchBtn).toBeVisible();
    // We won't click search as it would fail without a real token, but verification of UI is good.
  });

  test('should verify Emergency Map functionality', async ({ page }) => {
    await page.locator('.nav-item[title="Map"]').click();

    // Check map canvas existence
    await expect(page.locator('.map-canvas')).toBeVisible();

    // Check alert list container
    await expect(page.locator('.map-list')).toBeVisible();

    // Inject a mock alert location
    await page.evaluate(() => {
        if (window.debugSetAlertLocations) {
            window.debugSetAlertLocations([
                {
                    id: 'alert_1',
                    lat: 39.5,
                    lng: -98.35,
                    severity: 'emergency',
                    incoming: true,
                    address: 'Test Location',
                    body: 'Help me',
                    date: Date.now()
                }
            ]);
        }
    });

    // Verify alert item appears in list
    await expect(page.locator('.map-item-title', { hasText: 'Test Location' })).toBeVisible();
    await expect(page.locator('.map-badge', { hasText: 'Emergency' })).toBeVisible();
  });

  test('should verify PulseLink Trusted Contacts', async ({ page }) => {
    await page.locator('.nav-item[title="PulseLink"]').click();
    await expect(page.getByRole('heading', { name: 'PulseLink', exact: true })).toBeVisible();

    // Check Trusted Contacts section
    await expect(page.getByRole('heading', { name: 'Trusted contacts' })).toBeVisible();

    // Verify existing mock contact (Mom)
    await expect(page.getByText('Mom')).toBeVisible();

    // Test adding a contact UI (fill form)
    // Use exact match to distinguish from "Display name"
    const nameInput = page.getByRole('textbox', { name: 'Name', exact: true });
    await nameInput.fill('New Friend');

    // There are multiple "Phone" inputs (Profile and Contact), so we need to be specific or scope it
    // The Contact phone input is inside the same card as the Name input we just found?
    // Actually, let's scope to the settings card that contains "Add trusted contact" to be safe
    const contactCard = page.locator('.settings-card', { hasText: 'Add trusted contact' });
    // Use getByRole with exact match to avoid "Extra phones"
    const phoneInput = contactCard.getByRole('textbox', { name: 'Phone', exact: true });
    await phoneInput.fill('+15550000000');

    const addBtn = page.getByRole('button', { name: 'Add contact' });
    await expect(addBtn).toBeVisible();

    // Note: Clicking add might fail if Firestore is not mocked perfectly for writes in this environment,
    // but we verified the UI elements exist.
  });
});
