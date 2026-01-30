
import { test, expect } from '@playwright/test';

test.describe('Command Palette UX', () => {
  test.beforeEach(async ({ page }) => {
    // Enable mock user mode
    await page.goto('/?mock_user=true');
    await page.waitForTimeout(1000); // Allow app to hydrate
  });

  test('should restore focus to the trigger element on close', async ({ page }) => {
    // Navigate to Beacon panel to see the Command Palette button
    await page.getByLabel('Beacon').click();

    // 1. Locate the Command Palette button in the Sidebar
    const paletteBtn = page.getByLabel('Open command palette (Ctrl+K)');
    await expect(paletteBtn).toBeVisible();

    // 2. Focus the button explicitly to simulate keyboard navigation or click
    await paletteBtn.focus();
    await expect(paletteBtn).toBeFocused();

    // 3. Open the palette
    await paletteBtn.click();

    // 4. Verify focus moves to the Command Palette input
    const paletteInput = page.getByRole('combobox', { name: 'Command input' });
    await expect(paletteInput).toBeVisible();
    await expect(paletteInput).toBeFocused();

    // 5. Close the palette via Escape key
    await page.keyboard.press('Escape');

    // 6. Verify focus is restored to the trigger button
    await expect(paletteInput).not.toBeVisible();

    // This expectation is expected to fail initially because focus restoration isn't implemented
    await expect(paletteBtn).toBeFocused();
  });
});
