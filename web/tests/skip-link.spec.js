import { test, expect } from '@playwright/test';

test.describe('Skip to main content link', () => {
  test('skip link appears on keyboard focus and navigates to main content', async ({ page }) => {
    // Navigate to the application
    await page.goto('/');

    // The skip link should not be visible initially
    const skipLink = page.locator('.skip-link');
    await expect(skipLink).toBeAttached();

    // Press Tab to focus the skip link
    await page.keyboard.press('Tab');

    // The skip link should now be visible (transform: translateY(0))
    await expect(skipLink).toBeFocused();
    await expect(skipLink).toHaveCSS('transform', 'matrix(1, 0, 0, 1, 0, 0)'); // translateY(0)

    // Click the skip link
    await skipLink.click();

    // Verify that the main content area receives focus or is scrolled to
    const mainContent = page.locator('#main-content');
    await expect(mainContent).toBeInViewport();
  });

  test('skip link has correct accessibility attributes', async ({ page }) => {
    await page.goto('/');

    const skipLink = page.locator('.skip-link');

    // Verify the link text
    await expect(skipLink).toHaveText('Skip to main content');

    // Verify it's a proper link
    await expect(skipLink).toHaveAttribute('href', '#main-content');
  });

  test('skip link keyboard navigation flow', async ({ page }) => {
    await page.goto('/');

    // First Tab should focus the skip link
    await page.keyboard.press('Tab');
    const skipLink = page.locator('.skip-link');
    await expect(skipLink).toBeFocused();

    // Pressing Enter should activate the link
    await page.keyboard.press('Enter');

    // Main content should be in view
    const mainContent = page.locator('#main-content');
    await expect(mainContent).toBeInViewport();
  });

  test('skip link is hidden when not focused', async ({ page }) => {
    await page.goto('/');

    const skipLink = page.locator('.skip-link');

    // Check that the skip link has translateY(-100%) when not focused
    const transform = await skipLink.evaluate((el) => {
      return window.getComputedStyle(el).transform;
    });

    // Should be translated off-screen
    expect(transform).not.toBe('matrix(1, 0, 0, 1, 0, 0)');
  });
});
