import { test, expect } from '@playwright/test';

test('Future Deep theme renders correctly', async ({ page }) => {
  // Go to login page
  await page.goto('/');

  // Wait for the app shell to load
  await page.waitForSelector('.app-shell');

  // Check login card background (glassmorphism)
  const loginCard = page.locator('.login-card');
  await expect(loginCard).toBeVisible();

  // Check primary button color (Indigo)
  const primaryBtn = page.locator('.primary-btn').first();
  await expect(primaryBtn).toBeVisible();
  // Computed style might vary slightly depending on browser, but checking for the variable or roughly the RGB
  // We can check if it's computed to rgb(99, 102, 241) which is #6366f1
  await expect(primaryBtn).toHaveCSS('background-color', 'rgb(99, 102, 241)');

  // Check background color of the app shell
  const appShell = page.locator('.app-shell');
  await expect(appShell).toHaveCSS('background-color', 'rgb(3, 4, 7)'); // #030407
});
