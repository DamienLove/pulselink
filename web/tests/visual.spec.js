import { test, expect } from '@playwright/test';

test('visual verification of login page', async ({ page }) => {
  // Navigate to the app (assuming it's running on localhost:5173 or the configured base URL)
  // For local testing, we assume the dev server is running.
  // In a CI environment, baseURL would be set.
  await page.goto('/');

  // Wait for the app shell to load
  await page.waitForSelector('.app-shell');

  // Verify key elements are visible with correct classes
  await expect(page.locator('.login-card')).toBeVisible();
  await expect(page.locator('.brand-logo')).toBeVisible();
  await expect(page.getByText('PulseLink Web')).toBeVisible();

  // Verify Fonts are loaded (Inter and Space Grotesk)
  // This is a basic check to see if the computed style matches
  const heading = page.getByRole('heading', { name: 'PulseLink Web' });
  await expect(heading).toHaveCSS('font-family', /Space Grotesk/);

  const body = page.locator('body');
  await expect(body).toHaveCSS('font-family', /Inter/);

  // Take a screenshot for manual review or visual regression tools
  // Note: Actual visual regression testing (toMatchSnapshot) requires a baseline.
  // This test currently ensures the page renders without error and captures the state.
  await page.screenshot({ path: 'login-visual.png', fullPage: true });
});
