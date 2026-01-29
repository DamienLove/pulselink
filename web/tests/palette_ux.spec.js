
import { test, expect } from '@playwright/test';

test.describe('Palette UX Enhancements', () => {
  test.beforeEach(async ({ page }) => {
    // Enable mock user mode
    await page.goto('/?mock_user=true');
    await page.waitForTimeout(1000);
  });

  test('Sidebar search should have shortcut hint', async ({ page }) => {
    // Navigate to Beacon panel where sidebar search is visible
    await page.locator('.nav-item[title="Beacon"]').click();

    const searchInput = page.locator('.sidebar-search-input-field');
    const hint = page.locator('.sidebar-search-wrapper .shortcut-hint');

    await expect(searchInput).toBeVisible();
    await expect(searchInput).toHaveAttribute('placeholder', 'Search');
    await expect(hint).toBeVisible();
    await expect(hint).toHaveText('/');
    await expect(hint).toHaveAttribute('aria-hidden', 'true');
  });

  test('Contacts search should have shortcut hint', async ({ page }) => {
    await page.locator('.nav-item[title="Contacts"]').click();

    const searchInput = page.locator('.settings-search-input');
    const hint = page.locator('.settings-search-container .shortcut-hint');

    await expect(searchInput).toBeVisible();
    await expect(searchInput).toHaveAttribute('placeholder', 'Search by name, phone, or email');
    await expect(hint).toBeVisible();
    await expect(hint).toHaveText('/');
    await expect(hint).toHaveAttribute('aria-hidden', 'true');
  });

  test('Themes search should have shortcut hint', async ({ page }) => {
    await page.locator('.nav-item[title="Themes"]').click();

    const searchInput = page.locator('.settings-search-input');
    const hint = page.locator('.settings-search-container .shortcut-hint');

    await expect(searchInput).toBeVisible();
    await expect(searchInput).toHaveAttribute('placeholder', 'Search by name or creator');
    await expect(hint).toBeVisible();
    await expect(hint).toHaveText('/');
    await expect(hint).toHaveAttribute('aria-hidden', 'true');
  });

  test('Settings search should have shortcut hint', async ({ page }) => {
    await page.locator('.nav-item[title="Settings"]').click();

    const searchInput = page.locator('.settings-search-input');
    const hint = page.locator('.settings-search-container .shortcut-hint');

    await expect(searchInput).toBeVisible();
    await expect(searchInput).toHaveAttribute('placeholder', 'Search settings');
    await expect(hint).toBeVisible();
    await expect(hint).toHaveText('/');
    await expect(hint).toHaveAttribute('aria-hidden', 'true');
  });

  test('Avatar text contrast should adjust based on background', async ({ page }) => {
    // Navigate to PulseLink panel to access Profile settings
    await page.locator('.nav-item[title="PulseLink"]').click();

    // The Avatar component is used in the profile preview
    // Note: The App uses className="profile-avatar-img" for this specific instance, overriding the default "thread-avatar"
    const avatar = page.locator('.profile-avatar-preview .profile-avatar-img');
    const nameInput = page.locator('label:has-text("Display name") input');

    // 1. Test Light Background -> Black Text
    // "Alice" -> #C6A660 (Luminance ~167 > 128) -> Text should be Black
    await nameInput.fill('Alice');
    // Wait for react state update
    await page.waitForTimeout(100);
    await expect(avatar).toHaveCSS('color', 'rgb(0, 0, 0)');

    // 2. Test Dark Background -> White Text
    // "Mom" -> #012EEB (Luminance low) -> Text should be White
    await nameInput.fill('Mom');
    await page.waitForTimeout(100);
    await expect(avatar).toHaveCSS('color', 'rgb(255, 255, 255)');
  });
});
