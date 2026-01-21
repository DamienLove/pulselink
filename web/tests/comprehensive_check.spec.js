import { test, expect } from '@playwright/test';

test('Comprehensive Feature Verification', async ({ page }) => {
  // 1. Setup: Load App
  await page.goto('/');

  // Wait for app to initialize (login screen)
  await expect(page.locator('.login-container')).toBeVisible();

  // Wait for debug hooks to be injected
  await page.waitForFunction(() => window.debugSetUser);

  // 2. Login Mock
  await page.evaluate(() => {
    const mockUser = {
      uid: 'test-user-123',
      email: 'test@example.com',
      displayName: 'Test User',
      getIdTokenResult: async () => ({
        claims: { premium: true }
      })
    };
    window.debugSetUser(mockUser);
  });

  // Wait for Home Panel
  await expect(page.locator('.home-panel')).toBeVisible();

  // 3. Enable All Features
  await page.evaluate(() => {
    window.debugSetRemoteSettings({
      remoteWebAccessEnabled: true,
      beaconLauncherEnabled: true,
      mapEnabled: true,
      contactsEnabled: true,
      themesEnabled: true,
      ringerSongEnabled: true,
      thirdPartyExtensionsEnabled: true,
      mergedExperienceEnabled: false
    });
    // Stop loading skeleton
    window.debugSetIsLoadingThreads(false);
  });

  // 4. Verify Home Screen Buttons (Grid)
  const features = ['Beacon Inbox', 'Contacts', 'RingerSong', 'Emergency Map', 'Theme Gallery', 'Extensions'];
  for (const feature of features) {
    // Scoping to .home-grid to avoid conflict with Sidebar
    await expect(page.locator('.home-grid .home-card').filter({ hasText: feature }).first()).toBeVisible();
  }

  // 5. Verify Beacon Inbox & Attachments
  // Click the card on the home screen
  await page.locator('.home-grid .home-card').filter({ hasText: 'Beacon Inbox' }).click();

  // Verify active panel changed (Beacon sidebar item should be active)
  await expect(page.locator('.nav-item.active')).toContainText('Beacon');

  // Inject mock thread
  await page.evaluate(() => {
    const thread = { id: 't1', address: '+15550100', snippet: 'Photo', date: Date.now() };
    window.debugSetLegacyThreads([thread]);
  });

  // Select the thread
  await page.click('.thread-item');

  // Wait for chat header to ensure selection logic processed
  await expect(page.locator('.chat-header h3')).toContainText('+15550100');

  // Inject message with attachment (AFTER selection to avoid clearMessages)
  await page.evaluate(() => {
    window.debugSetMessages([
      {
        id: 'm1',
        body: 'Check this out',
        imageUrl: 'https://via.placeholder.com/150',
        date: Date.now(),
        type: 1
      }
    ]);
  });

  // Verify message content and attachment
  await expect(page.locator('.message-bubble')).toContainText('Check this out');
  await expect(page.locator('.message-image')).toBeVisible();
  await expect(page.locator('.message-image')).toHaveAttribute('src', 'https://via.placeholder.com/150');

  // 6. Verify Contacts
  // Use Sidebar to navigate
  await page.locator('.sidebar-nav .nav-item').filter({ hasText: 'Contacts' }).click();

  // Inject mock contact
  await page.evaluate(() => {
    window.debugSetDeviceContacts([
      { id: 'c1', displayName: 'John Doe', phoneNumber: '+15559999' }
    ]);
  });

  await expect(page.locator('.contact-name')).toContainText('John Doe');
  await expect(page.locator('.contact-meta')).toContainText('+15559999');

  // 7. Verify Settings
  await page.locator('.sidebar-nav .nav-item').filter({ hasText: 'Settings' }).click();
  await expect(page.locator('h3')).toContainText('Settings');
  await expect(page.getByText('Signed in as')).toBeVisible();

  // 8. Verify Map
  await page.locator('.sidebar-nav .nav-item').filter({ hasText: 'Map' }).click();
  await expect(page.locator('.map-panel')).toBeVisible();
  // Inject an alert to see if list populates
  await page.evaluate(() => {
    window.debugSetAlertLocations([
      { id: 'a1', lat: 0, lng: 0, severity: 'emergency', address: '123 Main St', date: Date.now(), incoming: true }
    ]);
  });
  await expect(page.locator('.map-item-title')).toContainText('123 Main St');

  // 9. Verify Extensions
  await page.locator('.sidebar-nav .nav-item').filter({ hasText: 'Extensions' }).click();
  await expect(page.getByText('Extensions', { exact: false }).first()).toBeVisible();
  // Check for one of the extension cards
  await expect(page.locator('.home-card h3').filter({ hasText: 'Beacon Inbox' })).toBeVisible();

  // 10. Unified Navigation (Command Palette)
  await page.keyboard.press('Control+k');
  await expect(page.getByPlaceholder('Type a command or search...')).toBeVisible();
  // Close it
  await page.keyboard.press('Escape');
});
