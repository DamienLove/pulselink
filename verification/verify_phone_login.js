import { chromium } from 'playwright';

(async () => {
  const browser = await chromium.launch();
  const page = await browser.newPage();
  // Ensure the dev server is running on port 5173
  await page.goto('http://localhost:5173');

  // Wait for the login card
  await page.waitForSelector('.login-card');

  // Click 'Sign in with Phone'
  await page.getByRole('button', { name: 'Sign in with Phone' }).click();

  // Wait for phone input
  await page.waitForSelector('input[type="tel"]');

  // Take screenshot
  await page.screenshot({ path: 'verification/phone_login.png' });

  await browser.close();
})();
