from playwright.sync_api import sync_playwright

def verify_features(page):
    page.goto("http://localhost:5173")

    # Check Sidebar "Features"
    page.wait_for_selector('button[aria-label="Features"]')
    print("Found Features button in Sidebar")

    # Check Home "Features" card
    features_card = page.get_by_role("button", name="Features Manage built-in capabilities and add-ons.")
    if features_card.is_visible():
        print("Found Features card on Home")
    else:
        print("Features card not visible")

    # Navigate to Beacon to check premium button
    page.get_by_role("button", name="Beacon").first.click()

    # Wait for empty state or upgrade prompt
    page.wait_for_selector(".badge-premium", timeout=5000)

    # Check for "Restore Purchase" button
    restore_btn = page.get_by_role("button", name="Restore Purchase / Refresh Status")
    if restore_btn.is_visible():
        print("Found Restore Purchase button")
    else:
        print("Restore Purchase button not found")

    page.screenshot(path="verification/features_verification.png")

if __name__ == "__main__":
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page()
        try:
            verify_features(page)
        except Exception as e:
            print(f"Error: {e}")
            page.screenshot(path="verification/error.png")
        finally:
            browser.close()
