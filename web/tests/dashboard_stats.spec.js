import { test, expect } from '@playwright/test';

test('Dashboard displays live stats with mock user', async ({ page }) => {
  // 1. Load app with mock user
  await page.goto('/?mock_user=true');

  // 2. Wait for dashboard to load
  await expect(page.locator('.home-hero')).toBeVisible();

  // 3. Check Beacon Card for Stat
  // targeting the card that contains "Beacon Inbox"
  const beaconCard = page.locator('.home-card').filter({ hasText: 'Beacon Inbox' });
  await expect(beaconCard).toBeVisible();
  // We expect a .card-stat element to exist and contain a number
  await expect(beaconCard.locator('.card-stat')).toBeVisible();
  await expect(beaconCard.locator('.card-stat')).toHaveText(/^\d+$/);

  // 4. Check Contacts Card
  const contactsCard = page.locator('.home-card').filter({ hasText: 'Contacts' });
  await expect(contactsCard.locator('.card-stat')).toBeVisible();
  await expect(contactsCard.locator('.card-stat')).toHaveText(/^\d+$/);

  // 5. Check RingerSong Card
  const ringerCard = page.locator('.home-card').filter({ hasText: 'RingerSong' });
  await expect(ringerCard.locator('.card-stat')).toBeVisible();

  // 6. Check Map Card
  const mapCard = page.locator('.home-card').filter({ hasText: 'Emergency Map' });
  await expect(mapCard.locator('.card-stat')).toBeVisible();
});
