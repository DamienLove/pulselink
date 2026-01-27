
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

  test('Smart character count should show segments', async ({ page }) => {
    await page.locator('.nav-item[title="Beacon"]').click();
    await page.getByText('Test Contact').first().click();

    const textarea = page.locator('.composer-textarea');
    await expect(textarea).toBeVisible();

    await textarea.fill('Hello world');
    const counter = page.locator('#message-char-count');
    await expect(counter).toHaveText('11');

    const longText = 'a'.repeat(165);
    await textarea.fill(longText);
    await expect(counter).toHaveText('165 / 2');
  });
});
