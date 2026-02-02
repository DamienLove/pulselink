import { test, expect } from '@playwright/test';

test.describe('Contacts Interaction', () => {
  test.beforeEach(async ({ page }) => {
    // Enable mock user mode to get pre-populated contacts
    await page.goto('/?mock_user=true');
    // Wait for initial load
    await page.waitForTimeout(2000);
  });

  test('clicking a device contact should open composer with that contact', async ({ page }) => {
    // 1. Navigate to Contacts panel
    // Wait for the home grid to appear
    await expect(page.locator('.home-grid')).toBeVisible();

    // Click the Contacts card on Home
    // Use a more robust selector if possible, but h3 text works for now based on App.jsx
    await page.locator('.home-card:has(h3:text("Contacts"))').click();

    // 2. Verify we are in Contacts panel
    await expect(page.locator('.contacts-panel')).toBeVisible();

    // 3. Find a contact item. Mock user usually has "Alice"
    // The mock data setup in App.jsx:
    // setDeviceContacts([{ id: 'dev_1', displayName: 'Alice', phoneNumber: '+15553334444' }]);
    const contactItem = page.locator('.contact-row', { hasText: 'Alice' }).first();
    await expect(contactItem).toBeVisible();

    // Verify it has accessibility attributes
    await expect(contactItem).toHaveAttribute('role', 'button');
    await expect(contactItem).toHaveAttribute('tabindex', '0');

    // 4. Click the contact
    await contactItem.click();

    // 5. Verify we are redirected to Beacon (messaging)
    // The active panel switches to 'beacon'.
    // We can verify the presence of the composer.
    const composer = page.locator('.composer');
    await expect(composer).toBeVisible();

    // 6. Verify the 'To' field is populated with the contact's number
    const toInput = page.locator('#compose-address');
    await expect(toInput).toHaveValue('+15553334444');
  });
});
