import { test, expect } from '@playwright/test';

test.describe('Accessibility features', () => {

  test('skip to main content link works', async ({ page }) => {
    // Go to the app
    await page.goto('http://localhost:5173');

    // Find the skip link
    const skipLink = page.getByText('Skip to main content');
    const mainContent = page.locator('#main-content');

    // Initially it should be hidden visually (e.g. top: -100px)
    await expect(skipLink).not.toBeInViewport();

    // Focus the link (simulating Tab key)
    await skipLink.focus();

    // Now it should be visible in viewport
    await expect(skipLink).toBeInViewport();

    // Click it (simulating Enter)
    await page.keyboard.press('Enter');

    // URL hash should change
    expect(page.url()).toContain('#main-content');

    // Focus should move to main content
    await expect(mainContent).toBeFocused();
  });

  test('login inputs have accessible labels', async ({ page }) => {
    await page.goto('http://localhost:5173');

    // Check Email input
    // The label text is "Email", but the input might be associated via nesting or htmlFor
    const emailInput = page.getByLabel('Email', { exact: false });
    await expect(emailInput).toBeVisible();

    // Check Password input
    // We target the input explicitly to avoid confusion with the toggle button
    const passwordInput = page.locator('input[type="password"]');
    // Ensure it has an accessible name
    await expect(passwordInput).toHaveAttribute('id', 'login-password');
    // Verify the label exists
    await expect(page.locator('label[for="login-password"]')).toHaveText('Password');
  });
});
