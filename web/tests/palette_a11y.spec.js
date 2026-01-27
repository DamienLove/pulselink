
import { test, expect } from '@playwright/test';

test.describe('Palette A11y Enhancements', () => {
  test.beforeEach(async ({ page }) => {
    // Enable mock user mode
    await page.goto('/?mock_user=true');
    await page.waitForTimeout(1000);
  });

  test('Contacts search results should have live region attributes', async ({ page }) => {
    await page.locator('.nav-item[title="Contacts"]').click();

    // Verify contact count has live region attributes
    const contactCount = page.locator('.contact-count');
    await expect(contactCount).toBeVisible();
    await expect(contactCount).toHaveAttribute('aria-live', 'polite');
    await expect(contactCount).toHaveAttribute('aria-atomic', 'true');

    // Search for non-existent contact
    const searchInput = page.locator('.settings-search-input');
    await searchInput.fill('NonExistentContactXYZ');

    // Verify empty state has role="status"
    const emptyState = page.locator('.settings-note[role="status"]');
    await expect(emptyState).toBeVisible();
    await expect(emptyState).toContainText('No contacts match that search');
  });

  test('Sidebar search empty state should have role status', async ({ page }) => {
    await page.locator('.nav-item[title="Beacon"]').click();

    const searchInput = page.locator('.sidebar-search-input-field');
    await searchInput.fill('NonExistentThreadXYZ');

    const emptyState = page.locator('.sidebar-placeholder[role="status"]');
    await expect(emptyState).toBeVisible();
    await expect(emptyState).toContainText('No matches found');
  });

  test('Themes search empty state should have role status', async ({ page }) => {
    await page.locator('.nav-item[title="Themes"]').click();

    const searchInput = page.locator('.settings-search-input');
    await searchInput.fill('NonExistentThemeXYZ');

    const emptyState = page.locator('.theme-empty[role="status"]');
    await expect(emptyState).toBeVisible();
    await expect(emptyState).toContainText('No themes found');
  });
});
