
import { test, expect } from '@playwright/test';

test('Full Verification Suite', async ({ page }) => {
  // 1. Initial Load & Login
  await test.step('Login and Setup', async () => {
    await page.goto('/');

    // Wait for App to initialize
    await page.waitForLoadState('networkidle');

    // Inject Mock User
    await page.evaluate(() => {
      if (!window.debugSetUser) throw new Error("Debug hooks not found! Ensure we are in DEV mode.");

      window.debugSetUser({
        uid: 'test_user_verify',
        email: 'tester@pulselink.app',
        displayName: 'Test User',
        getIdTokenResult: async () => ({ claims: { premium: true } })
      });

      const settings = {
        remoteWebAccessEnabled: true,
        thirdPartyExtensionsEnabled: true,
        mergedExperienceEnabled: false,
        mapEnabled: true,
        contactsEnabled: true,
        themesEnabled: true,
        ringerSongEnabled: true,
        beaconEnabled: true,
        trustedContactsEnabled: true
      };

      // Inject Mock User Data (Settings)
      window.debugSetUserData({
        subscriptionStatus: 'premium',
        ...settings
      });

      // Inject Remote Settings to ensure Sidebar shows items
      window.debugSetRemoteSettings(settings);

      // Force navigation to Home (activePanel defaults to 'beacon')
      window.debugSetActivePanel('home');
    });

    // Verify Home Screen
    await expect(page.locator('.home-hero h2')).toHaveText(/Good (morning|afternoon|evening)/);
    // Use specific locators for Home Cards
    await expect(page.locator('.home-card h3', { hasText: 'Beacon Inbox' })).toBeVisible();
    await expect(page.locator('.home-card h3', { hasText: 'Contacts' })).toBeVisible();
  });

  // 2. Navigation Verification
  await test.step('Sidebar Navigation', async () => {
    // Go to Contacts
    await page.locator('.sidebar-nav button[title="Contacts"]').click();
    await expect(page.locator('.panel-header h3')).toHaveText('Contacts');

    // Go to Settings
    await page.locator('.sidebar-nav button[title="Settings"]').click();
    await expect(page.locator('.settings-header h3')).toHaveText('Settings');

    // Go back to Home
    await page.locator('.sidebar-nav button[title="Home"]').click();
    await expect(page.locator('.home-hero')).toBeVisible();
  });

  // 3. Unified Experience Feature
  await test.step('Unified Experience Toggle', async () => {
    // Enable Unified Experience via Remote Settings
    await page.evaluate(() => {
      window.debugSetRemoteSettings(prev => ({ ...prev, mergedExperienceEnabled: true }));
    });

    // Check for branding change in Sidebar
    await expect(page.locator('.brand-title')).toHaveText('PulseLink Unified');

    // Revert
    await page.evaluate(() => {
      window.debugSetRemoteSettings(prev => ({ ...prev, mergedExperienceEnabled: false }));
    });
    await expect(page.locator('.brand-title')).toHaveText('PulseLink Suite');
  });

  // 4. Contacts Verification
  await test.step('Contacts Functionality', async () => {
    await page.locator('.sidebar-nav button[title="Contacts"]').click();

    // Inject Mock Contacts
    await page.evaluate(() => {
      window.debugSetDeviceContacts([
        { id: 'c1', displayName: 'Alice Smith', phoneNumber: '+15550101' },
        { id: 'c2', displayName: 'Bob Jones', email: 'bob@example.com' }
      ]);
    });

    // Verify list
    await expect(page.getByText('Alice Smith')).toBeVisible();
    await expect(page.getByText('+15550101')).toBeVisible();
    await expect(page.getByText('Bob Jones')).toBeVisible();

    // Verify Search
    await page.fill('.settings-search-input', 'Alice');
    await expect(page.getByText('Alice Smith')).toBeVisible();
    await expect(page.getByText('Bob Jones')).not.toBeVisible();

    // Clear search
    await page.locator('.settings-search-container button[title="Clear search"]').click();
    await expect(page.getByText('Bob Jones')).toBeVisible();
  });

  // 5. Beacon Inbox & Attachments
  await test.step('Beacon Inbox & Attachments', async () => {
    await page.locator('.sidebar-nav button[title="Beacon"]').click();

    // Inject Thread and Messages
    await page.evaluate(async () => {
      const thread = { id: 't1', address: '+15550199', display_name: 'Charlie', snippet: 'Image attached' };
      window.debugSetLegacyThreads([thread]);
      window.debugSetIsLoadingThreads(false); // Force loading off

      // Select the thread
      window.debugSetSelectedThread(thread);

      // Wait for React to process state change (simulated)
      await new Promise(r => setTimeout(r, 100));

      // Inject Messages
      window.debugSetMessages([
        { id: 'm1', body: 'Hey look at this!', date: Date.now() - 10000, type: 2 },
        { id: 'm2', body: '', imageUrl: 'https://via.placeholder.com/150', date: Date.now(), type: 1 }
      ]);
    });

    // Verify Thread List
    await expect(page.locator('.thread-item').first()).toContainText('Charlie');

    // Verify Message List
    await expect(page.getByText('Hey look at this!')).toBeVisible();

    // Verify Attachment
    await expect(page.locator('.message-image')).toBeVisible();
    await expect(page.locator('.message-image')).toHaveAttribute('src', 'https://via.placeholder.com/150');
  });

  // 6. Settings Verification
  await test.step('Settings Menu', async () => {
    await page.locator('.sidebar-nav button[title="Settings"]').click();

    // Verify basic sections using explicit headings
    await expect(page.getByRole('heading', { name: 'Account', exact: true })).toBeVisible();
    await expect(page.getByRole('heading', { name: 'PulseLink settings' })).toBeVisible();

    // Search for specific setting "scroll"
    await page.fill('.settings-search-input', 'scroll');
    await expect(page.getByRole('heading', { name: 'Web preferences' })).toBeVisible();

    // Account section should be hidden
    await expect(page.getByRole('heading', { name: 'Account', exact: true })).not.toBeVisible();
  });

  // 7. Features
  await test.step('Features Page', async () => {
      await page.locator('.sidebar-nav button[title="Features"]').click();
      await expect(page.getByRole('heading', { name: 'Quick Setup' })).toBeVisible();
      await expect(page.locator('.home-card h3', { hasText: 'Beacon Inbox' })).toBeVisible();
  });

  // 8. PulseLink Emergency Features (Map & Trusted Contacts)
  await test.step('Emergency Features', async () => {
    // A. Trusted Contacts
    await page.locator('.sidebar-nav button[title="PulseLink"]').click();
    await expect(page.getByRole('heading', { name: 'Trusted contacts' })).toBeVisible();

    await page.evaluate(() => {
      window.debugSetTrustedContacts([
        { id: 'tc1', displayName: 'Mom', phoneNumber: '+15559999' }
      ]);
    });
    await expect(page.getByText('Mom')).toBeVisible();
    await expect(page.getByText('+15559999')).toBeVisible();

    // B. Emergency Map
    await page.locator('.sidebar-nav button[title="Map"]').click();
    await expect(page.getByRole('heading', { name: 'Emergency map' })).toBeVisible();

    await page.evaluate(() => {
      window.debugSetAlertLocations([
        {
          id: 'alert1',
          lat: 40.7128,
          lng: -74.0060,
          severity: 'emergency',
          address: 'New York, NY',
          date: Date.now(),
          incoming: true // Important: default filter is incomingOnly=true
        }
      ]);
    });

    // Check for map alert list item
    await expect(page.locator('.map-item-title')).toHaveText('New York, NY');
    await expect(page.locator('.map-badge')).toHaveText('Emergency');
  });
});
