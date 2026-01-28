
import { test, expect } from '@playwright/test';

test.describe('Palette Focus UX', () => {
  test.beforeEach(async ({ page }) => {
    // Enable mock user mode
    await page.goto('/?mock_user=true');
    // Wait for the app to load
    await page.waitForSelector('.nav-item');
  });

  test('Initial State Focus', async ({ page }) => {
    // Navigate to Beacon
    await page.locator('.nav-item[title="Beacon"]').click();

    // Wait for composer to appear
    await page.waitForSelector('#compose-address');

    // Check if address input is focused
    await expect(page.locator('#compose-address')).toBeFocused();
  });

  test('Thread Selection Focus', async ({ page }) => {
    // Navigate to Beacon
    await page.locator('.nav-item[title="Beacon"]').click();

    // Wait for thread list
    await page.waitForSelector('.thread-item');

    // Click on the first thread
    await page.locator('.thread-item').first().click();

    // Wait for composer textarea
    await page.waitForSelector('.composer-textarea');

    // Check if textarea is focused
    await expect(page.locator('.composer-textarea')).toBeFocused();
  });

  test('New Conversation Focus', async ({ page }) => {
    // Navigate to Beacon
    await page.locator('.nav-item[title="Beacon"]').click();

    // Select a thread first
    await page.waitForSelector('.thread-item');
    await page.locator('.thread-item').first().click();

    // Click 'New' button
    await page.locator('button[aria-label="Start new conversation"]').click();

    // Check if address input is focused
    await expect(page.locator('#compose-address')).toBeFocused();
  });
});
