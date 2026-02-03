const { chromium } = require('playwright');

(async () => {
  const browser = await chromium.launch();
  const page = await browser.newPage();

  // Use mock_user=true to bypass auth and load mock data
  await page.goto('http://localhost:5173/?mock_user=true');

  // Click on Beacon in the sidebar
  await page.locator('.sidebar button[title="Beacon"]').click();

  // Wait for the thread to appear
  await page.getByText('Test Contact').waitFor();

  // Click the thread
  await page.getByText('Test Contact').click();

  // Wait for chat header
  await page.locator('.chat-header').waitFor();

  // Take screenshot
  await page.screenshot({ path: 'verification/beacon_inbox.png' });

  await browser.close();
})();
