
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

  test('Theme editor should have hex code inputs', async ({ page }) => {
    await page.locator('.nav-item[title="Themes"]').click();

    // Verify Primary color input group
    const primaryColorLabel = page.locator('label.login-field', { hasText: 'Primary color' });
    const colorInput = primaryColorLabel.locator('input[type="color"]');
    const textInput = primaryColorLabel.locator('input[type="text"]');

    await expect(colorInput).toBeVisible();
    await expect(textInput).toBeVisible();
    await expect(textInput).toHaveAttribute('placeholder', '#000000');

    // Check synchronization (assuming default theme is active initially or mocked)
    const value = await textInput.inputValue();
    expect(value).toMatch(/^#[0-9A-Fa-f]{6}$/);
    const colorValue = await colorInput.inputValue();
    expect(colorValue.toLowerCase()).toBe(value.toLowerCase());
  });
});
