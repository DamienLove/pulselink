import { test, expect } from '@playwright/test';

test('verify extensions store and gating', async ({ page }) => {
  // 1. Navigate to app
  await page.goto('http://localhost:5173');

  // 2. Mock User Login
  await page.evaluate(() => {
    const mockUser = {
      uid: 'test-user',
      email: 'test@example.com',
      displayName: 'Test User',
      getIdTokenResult: () => Promise.resolve({ claims: { premium: true } })
    };
    if (window.debugSetUser) {
        window.debugSetUser(mockUser);
    }
  });

  await page.waitForTimeout(2000);

  // 3. Mock Remote Settings (All Enabled)
  await page.evaluate(() => {
    if (window.debugSetRemoteSettings) {
        window.debugSetRemoteSettings({
            remoteWebAccessEnabled: true,
            autoUpdateContactInfo: true,
            timeFormat: 'AUTO',
            thirdPartyExtensionsEnabled: true,
            beaconLauncherEnabled: true,
            ringerSongEnabled: true,
            mapEnabled: true,
            contactsEnabled: true,
            themesEnabled: true
        });
    }
  });

  await page.waitForTimeout(1000);

  // Debug screenshot
  await page.screenshot({ path: '/home/jules/verification/debug_state.png' });

  // 4. Verify Home Cards visible
  await expect(page.locator('.home-card').filter({ hasText: 'Beacon Inbox' })).toBeVisible();
  await expect(page.locator('.home-card').filter({ hasText: 'RingerSong' })).toBeVisible();

  // Screenshot 1: All Features Enabled
  await page.screenshot({ path: '/home/jules/verification/home_all_enabled.png' });

  // 5. Navigate to Extensions Store
  // Sidebar "Extensions" button
  await page.click('button[title="Extensions"]');
  await expect(page.locator('text=Enhance your PulseLink experience')).toBeVisible();

  // Search for Beacon
  await page.fill('input[placeholder="Search extensions..."]', 'Beacon');
  await page.waitForTimeout(500); // Wait for filter

  // Verify filtered list
  await expect(page.locator('.home-card').filter({ hasText: 'Beacon Inbox' })).toBeVisible();
  // Ensure RingerSong is filtered out (hidden)
  const ringerVisible = await page.locator('.home-card').filter({ hasText: 'RingerSong' }).isVisible();
  expect(ringerVisible).toBeFalsy();

  // Screenshot 2: Extensions Search
  await page.screenshot({ path: '/home/jules/verification/extensions_search.png' });

  // Clear search
  await page.fill('input[placeholder="Search extensions..."]', '');
  await page.waitForTimeout(500);

  // Toggle Beacon Inbox OFF
  // Find card with "Beacon Inbox"
  const beaconCard = page.locator('.home-card').filter({ hasText: 'Beacon Inbox' });
  await beaconCard.getByRole('button', { name: 'Remove' }).click();

  // Toggle RingerSong OFF
  const ringerCard = page.locator('.home-card').filter({ hasText: 'RingerSong' });
  await ringerCard.getByRole('button', { name: 'Remove' }).click();

  // 6. Navigate Home and Verify Gating
  await page.click('button[title="Home"]'); // Sidebar Home icon

  // Wait for state update
  await page.waitForTimeout(1000);

  // Verify cards hidden
  await expect(page.locator('.home-card').filter({ hasText: 'Beacon Inbox' })).toBeHidden();
  await expect(page.locator('.home-card').filter({ hasText: 'RingerSong' })).toBeHidden();

  // Verify Sidebar items hidden
  await expect(page.locator('.nav-item').filter({ hasText: 'Beacon' })).toBeHidden();
  await expect(page.locator('.nav-item').filter({ hasText: 'RingerSong' })).toBeHidden();

  // Screenshot 3: Gated Home
  await page.screenshot({ path: '/home/jules/verification/home_gated.png' });
});
