
/* eslint-env node */
import { test, expect } from '@playwright/test';

test.describe('Premium Access Gate', () => {
  test('should load the app', async ({ page }) => {
    // Basic smoke test to ensure the app builds and routes
    await page.goto('http://localhost:5173');
    await expect(page).toHaveTitle(/PulseLink/);
  });
});
