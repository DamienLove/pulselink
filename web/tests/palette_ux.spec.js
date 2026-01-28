
import { test, expect } from '@playwright/test';

test.describe('Palette UX Enhancements', () => {
  test.beforeEach(async ({ page }) => {
    // Enable mock user mode
    await page.goto('/?mock_user=true');
    await page.waitForTimeout(1000);
  });

  test('Sidebar search should have shortcut hint', async ({ page }) => {
    // Navigate to Beacon panel where sidebar search is visible
    await page.locator('.nav-item[title="Beacon"]').click();

    const searchInput = page.locator('.sidebar-search-input-field');
    const hint = page.locator('.sidebar-search-wrapper .shortcut-hint');

    await expect(searchInput).toBeVisible();
    await expect(searchInput).toHaveAttribute('placeholder', 'Search');
    await expect(hint).toBeVisible();
    await expect(hint).toHaveText('/');
    await expect(hint).toHaveAttribute('aria-hidden', 'true');
  });

  test('Contacts search should have shortcut hint', async ({ page }) => {
    await page.locator('.nav-item[title="Contacts"]').click();

    const searchInput = page.locator('.settings-search-input');
    const hint = page.locator('.settings-search-container .shortcut-hint');

    await expect(searchInput).toBeVisible();
    await expect(searchInput).toHaveAttribute('placeholder', 'Search by name, phone, or email');
    await expect(hint).toBeVisible();
    await expect(hint).toHaveText('/');
    await expect(hint).toHaveAttribute('aria-hidden', 'true');
  });

  test('Themes search should have shortcut hint', async ({ page }) => {
    await page.locator('.nav-item[title="Themes"]').click();

    const searchInput = page.locator('.settings-search-input');
    const hint = page.locator('.settings-search-container .shortcut-hint');

    await expect(searchInput).toBeVisible();
    await expect(searchInput).toHaveAttribute('placeholder', 'Search by name or creator');
    await expect(hint).toBeVisible();
    await expect(hint).toHaveText('/');
    await expect(hint).toHaveAttribute('aria-hidden', 'true');
  });

  test('Settings search should have shortcut hint', async ({ page }) => {
    await page.locator('.nav-item[title="Settings"]').click();

    const searchInput = page.locator('.settings-search-input');
    const hint = page.locator('.settings-search-container .shortcut-hint');

    await expect(searchInput).toBeVisible();
    await expect(searchInput).toHaveAttribute('placeholder', 'Search settings');
    await expect(hint).toBeVisible();
    await expect(hint).toHaveText('/');
    await expect(hint).toHaveAttribute('aria-hidden', 'true');
  });

  test('Avatar text should have sufficient contrast', async ({ page }) => {
    // 1. Check "Alice" in Contacts (Expect Black text on Light BG)
    await page.locator('.nav-item[title="Contacts"]').click();

    // Find Alice's row
    const aliceRow = page.locator('.contact-row', { hasText: 'Alice' });
    await expect(aliceRow).toBeVisible();

    // In DeviceContactItem, Avatar is not explicitly rendered, but wait...
    // DeviceContactItem source:
    /*
    const DeviceContactItem = memo(({ contact }) => {
      // ...
      return (
        <div className="contact-row contact-row--stacked">
          { // No Avatar component here! It just shows text. }
          <div className="contact-main">
            <div className="contact-name">{contact.displayName || 'Unnamed contact'}</div>
    */
    // Ah! DeviceContactItem does NOT use Avatar component in the current code I read!
    // Let me check App.jsx again.

    // Sidebar ThreadItem USES Avatar.
    // ThreadItem: <Avatar name={name} />

    // "Test Contact" (thread) is in mock data.
    // Name: Test Contact, BG: #9A3E92, Text: #FFFFFF (White)

    // I need a thread with a name that produces Black text.
    // "Alice" -> Black.
    // Is there a thread for Alice?

    // Mock data:
    // setLegacyThreads([{ id: 'thread_1', address: '+15559998888', display_name: 'Test Contact', ... }]);

    // I can modify the mock data in the test via window.debugSetLegacyThreads if I'm in DEV mode.
    // But playwright runs against built app? Or dev server?
    // Usually dev server.

    // Let's try to verify "Test Contact" (White text) first.
    await page.locator('.nav-item[title="Beacon"]').click();
    const threadItem = page.locator('.thread-item', { hasText: 'Test Contact' });
    await expect(threadItem).toBeVisible();

    const avatar = threadItem.locator('.thread-avatar');
    await expect(avatar).toHaveCSS('color', 'rgb(255, 255, 255)');

    // Now let's try to inject a thread for "Alice" (Black text)
    await page.evaluate(() => {
        if (window.debugSetLegacyThreads) {
            window.debugSetLegacyThreads([{
                id: 'thread_alice',
                address: '+15550001111',
                display_name: 'Alice',
                date: Date.now(),
                snippet: 'Hello',
                pinned: false,
                archived: false
            }]);
        }
    });

    const aliceItem = page.locator('.thread-item', { hasText: 'Alice' });
    await expect(aliceItem).toBeVisible();
    const aliceAvatar = aliceItem.locator('.thread-avatar');
    // Alice BG is #C6A660 -> Black text
    await expect(aliceAvatar).toHaveCSS('color', 'rgb(0, 0, 0)');
  });
});
