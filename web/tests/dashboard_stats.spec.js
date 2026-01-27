import { test, expect } from '@playwright/test';

test('Dashboard stats and status widget are visible', async ({ page }) => {
  // Use mock_user query param to bypass auth and inject mock data
  // Assuming baseURL is set in playwright.config.js, otherwise relative path should work if served
  await page.goto('/?mock_user=true');

  // Wait for the app to load
  await expect(page.locator('.home-panel')).toBeVisible();

  // Verify Status Widget
  const statusWidget = page.locator('.status-widget');
  await expect(statusWidget).toBeVisible();
  await expect(statusWidget.getByText('Web Relay')).toBeVisible();
  await expect(statusWidget.getByText('Sync Status')).toBeVisible();
  await expect(statusWidget.getByText('Premium')).toBeVisible();

  // Verify Beacon Stats (Thread Count)
  // Mock data in App.jsx sets 1 synced thread. lineThreads is empty by default in mock mode.
  const beaconCard = page.locator('.home-card', { hasText: 'Beacon Inbox' });
  await expect(beaconCard).toBeVisible();
  await expect(beaconCard.locator('.stat-number')).toHaveText('1');

  // Verify Contacts Stats (Device Contacts)
  // Mock data sets 1 device contact.
  // Use exact match or subtext to distinguish from 'Trusted Contacts' in PulseLink card
  const contactsCard = page.locator('.home-card').filter({ hasText: 'Device contacts' });
  await expect(contactsCard.locator('.stat-number')).toHaveText('1');

  // Verify PulseLink Stats (Trusted Contacts)
  // Mock data sets 1 trusted contact.
  const pulselinkCard = page.locator('.home-card').filter({ hasText: 'PulseLink' }).filter({ hasText: 'Trusted contacts' });
  await expect(pulselinkCard.locator('.stat-number')).toHaveText('1');

  // Verify Map Stats (Active Alerts)
  // Mock data does not set alerts initially, so 0.
  const mapCard = page.locator('.home-card', { hasText: 'Emergency Map' });
  await expect(mapCard.locator('.stat-number')).toHaveText('0');

  // Inject an alert using the debug hook to verify dynamic update
  await page.evaluate(() => {
    if (window.debugSetAlertLocations) {
      window.debugSetAlertLocations([
        { id: 'a1', severity: 'emergency', address: 'Test Alert', date: Date.now() }
      ]);
    }
  });

  // Should update to 1
  await expect(mapCard.locator('.stat-number')).toHaveText('1');
});
