
import { test, expect } from '@playwright/test';

test.describe('Palette A11y Enhancements', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/?mock_user=true');
    // Wait for the page to settle
    await page.waitForTimeout(500);
  });

  test('Avatar text color should contrast with background', async ({ page }) => {
    // Navigate to PulseLink panel
    await page.locator('.nav-item[title="PulseLink"]').click();

    // Locate the display name input
    // The label text is "Display name"
    const nameInput = page.getByLabel('Display name');
    await expect(nameInput).toBeVisible();

    // Type a name that produces a light pink background (#DFBCD7)
    await nameInput.fill('User 150');

    // Locate the avatar
    const avatar = page.locator('.profile-avatar-preview .profile-avatar-img');
    await expect(avatar).toBeVisible();
    await expect(avatar).toHaveText('US'); // Initials

    // Verify background color is indeed what we expect (approximately)
    // #DFBCD7 is rgb(223, 188, 215)
    await expect(avatar).toHaveCSS('background-color', 'rgb(223, 188, 215)');

    // Verify text color is BLACK (rgb(0, 0, 0)) for high contrast
    // Currently it is white (rgb(255, 255, 255)), so this expectation will fail initially.
    await expect(avatar).toHaveCSS('color', 'rgb(0, 0, 0)');
  });
});
