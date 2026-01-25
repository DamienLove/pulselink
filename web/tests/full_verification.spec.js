
/* eslint-env node */
import { test, expect } from '@playwright/test';

test.describe('Full Feature Verification', () => {
  test.beforeEach(async ({ page }) => {
    // Enable mock user mode
    await page.goto('/?mock_user=true');
    // Wait for the app to settle
    await page.waitForTimeout(2000);
  });

  test('should display Home screen with all feature cards', async ({ page }) => {
    await expect(page.locator('.home-hero')).toBeVisible();

    const cardTitles = [
      'Beacon Inbox',
      'PulseLink',
      'Contacts',
      'RingerSong',
      'Emergency Map',
      'Theme Gallery',
      'Features'
    ];

    for (const title of cardTitles) {
      await expect(page.locator('.home-card h3', { hasText: title }).first()).toBeVisible();
    }
  });

  test('should navigate to Beacon Inbox and verify composer', async ({ page }) => {
    await page.locator('.home-card h3', { hasText: 'Beacon Inbox' }).click();
    await expect(page.locator('.nav-item.active')).toHaveText(/Beacon/);

    // Check if we have threads (mocked)
    const thread = page.locator('.thread-item').first();
    if (await thread.isVisible()) {
        await thread.click();
        await expect(page.locator('.chat-header')).toBeVisible();
        await expect(page.locator('.composer')).toBeVisible();
    } else {
        await expect(page.locator('.empty-state')).toBeVisible();
    }

    await expect(page.getByRole('button', { name: 'Start new conversation' })).toBeVisible();
  });

  test('should navigate to PulseLink and verify profile', async ({ page }) => {
    await page.locator('.home-card h3', { hasText: 'PulseLink' }).first().click();

    await expect(page.getByRole('heading', { name: 'Public profile' })).toBeVisible();

    const profileCard = page.locator('.settings-card').filter({ hasText: 'Public profile' });
    await expect(profileCard.getByLabel('Display name')).toHaveValue('Test User');
    await expect(profileCard.getByLabel('Phone')).toHaveValue('+15550101010');
  });

  test('should navigate to Contacts and verify list', async ({ page }) => {
    await page.locator('.home-card h3', { hasText: 'Contacts' }).click();
    await expect(page.getByRole('heading', { name: 'Contacts' })).toBeVisible();
    await expect(page.getByText('Alice')).toBeVisible();
    await expect(page.getByText('+15553334444')).toBeVisible();
  });

  test('should navigate to Map and verify container', async ({ page }) => {
    await page.locator('.home-card h3', { hasText: 'Emergency Map' }).click();
    await expect(page.getByRole('heading', { name: 'Emergency map' })).toBeVisible();
    await expect(page.locator('.map-canvas')).toBeVisible();
  });

  test('should navigate to Themes and verify gallery', async ({ page }) => {
    await page.locator('.home-card h3', { hasText: 'Theme Gallery' }).click();
    await expect(page.getByRole('heading', { name: 'Theme Gallery' })).toBeVisible();
    await expect(page.getByText('Future Deep V11')).toBeVisible();
  });

  test('should navigate to Features and verify list', async ({ page }) => {
    // Target the card in the grid specifically
    const btn = page.locator('.home-grid .home-card').filter({ hasText: 'Features' }).first();

    if (await btn.isDisabled()) {
        console.log('Features button is disabled in UI.');
        return;
    }

    await btn.click();
    await expect(page.getByRole('heading', { name: 'Features', exact: true })).toBeVisible();
    await expect(page.getByText('Beacon Inbox')).toBeVisible();
    await expect(page.getByText('Firebase Relay')).toBeVisible();
  });

  test('should navigate to Settings and verify menu', async ({ page }) => {
    await page.locator('.nav-item[title="Settings"]').click();
    await expect(page.getByRole('heading', { name: 'Settings', exact: true })).toBeVisible();
    await expect(page.getByText('Signed in as')).toBeVisible();
    await expect(page.getByText('test@example.com')).toBeVisible();
    await expect(page.getByText('Enable remote web access')).toBeVisible();
  });
});
