
import { test, expect } from '@playwright/test';

test.describe('Extensions Store', () => {
  test.beforeEach(async ({ page }) => {
    // Load with mock user to bypass auth and populate default settings (where beaconEnabled is true by default)
    await page.goto('/?mock_user=true');
    // Wait for the app to settle
    await expect(page.locator('.sidebar')).toBeVisible();
  });

  test('can toggle Beacon Web feature on and off', async ({ page }) => {
    // 1. Verify Beacon is initially visible in Sidebar
    const beaconSidebarItem = page.locator('.sidebar .nav-item[title="Beacon"]');
    await expect(beaconSidebarItem).toBeVisible();

    // 2. Navigate to Features (Extensions) panel
    await page.click('.sidebar .nav-item[title="Features"]');

    // Verify we are on the features panel
    await expect(page.locator('.panel-header h3')).toHaveText('Features');

    // 3. Find the Beacon Web toggle (initially "Remove" because it's enabled)
    // The button aria-label should be "Remove Beacon Web"
    const beaconToggleBtn = page.locator('button[aria-label="Remove Beacon Web"]');
    await expect(beaconToggleBtn).toBeVisible();
    await expect(beaconToggleBtn).toHaveText('Remove');

    // 4. Toggle OFF
    await beaconToggleBtn.click();

    // 5. Verify button changes to "Install"
    const installBtn = page.locator('button[aria-label="Install Beacon Web"]');
    await expect(installBtn).toBeVisible();
    await expect(installBtn).toHaveText('Install');

    // 6. Verify Beacon is GONE from Sidebar
    await expect(beaconSidebarItem).toBeHidden();

    // 7. Toggle ON
    await installBtn.click();

    // 8. Verify button changes back to "Remove"
    await expect(beaconToggleBtn).toBeVisible();

    // 9. Verify Beacon REAPPEARS in Sidebar
    await expect(beaconSidebarItem).toBeVisible();
  });

  test('can toggle Contacts feature on and off', async ({ page }) => {
    // 1. Verify Contacts is initially visible
    const contactsSidebarItem = page.locator('.sidebar .nav-item[title="Contacts"]');
    await expect(contactsSidebarItem).toBeVisible();

    // 2. Navigate to Features
    await page.click('.sidebar .nav-item[title="Features"]');

    // 3. Toggle Contacts OFF
    const removeBtn = page.locator('button[aria-label="Remove Contacts Manager"]');
    await removeBtn.click();

    // 4. Verify gone from sidebar
    await expect(contactsSidebarItem).toBeHidden();

    // 5. Toggle Contacts ON
    const installBtn = page.locator('button[aria-label="Install Contacts Manager"]');
    await installBtn.click();

    // 6. Verify back in sidebar
    await expect(contactsSidebarItem).toBeVisible();
  });
});
