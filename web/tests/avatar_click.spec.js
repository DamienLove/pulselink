import { test, expect } from '@playwright/test';

test('Avatar click in Beacon Inbox should open Contact Edit form', async ({ page }) => {
  // 1. Load app with mock user
  await page.goto('http://localhost:5173/?mock_user=true');

  // 2. Navigate to Beacon panel
  await page.click('button[title="Beacon"]');

  // 3. Wait for threads to load
  const threadItem = page.locator('.thread-item').first();
  await expect(threadItem).toBeVisible();

  // 4. Click the Avatar specifically
  // The avatar is the first child or class .thread-avatar
  const avatar = threadItem.locator('.thread-avatar');
  await avatar.click();

  // 5. Verify we are navigated to the PulseLink panel (Contact Edit)
  // The panel header should say "PulseLink"
  const panelHeader = page.locator('.panel-header h3');
  await expect(panelHeader).toHaveText('PulseLink');

  // 6. Verify the form is populated or ready
  // Since mock user has 'Test Contact' with '+15559998888' in legacy threads
  // And trusted contacts has 'Mom' (+15551112222)
  // If we click the 'Test Contact' (legacy thread), it might not be in trusted list.
  // So it should open "Add trusted contact" with phone number pre-filled.

  // Let's check the phone input value
  const phoneInput = page.locator('input[value="+15559998888"]');
  // OR check if the input with label "Phone" has the value
  // We can just check if any input has the value.

  // Actually, let's see which thread is first.
  // In App.jsx mock data:
  // setLegacyThreads([{ id: 'thread_1', address: '+15559998888', display_name: 'Test Contact', ... }]);
  // This thread should be visible.

  // After clicking avatar:
  // Expect "Add trusted contact" header (since it's not in trustedContacts mock list)
  const formHeader = page.locator('.settings-card h4').filter({ hasText: /Add trusted contact|Edit trusted contact/ });
  await expect(formHeader).toBeVisible();

  const phoneField = page.locator('input[value="+15559998888"]');
  await expect(phoneField).toBeVisible();
});
