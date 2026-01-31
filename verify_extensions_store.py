from playwright.sync_api import sync_playwright

def verify_extensions_store():
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page()

        # Enable mock user
        page.goto("http://localhost:5173/?mock_user=true")
        page.wait_for_timeout(2000)

        # Navigate to Features using Sidebar
        page.locator('.nav-item[title="Features"]').click()
        page.wait_for_timeout(1000)

        # Verify Heading
        if not page.get_by_role("heading", name="Features Store").is_visible():
            print("Heading 'Features Store' not found!")

        # Verify Quick Setup
        if not page.get_by_text("Quick Setup").is_visible():
            print("Quick Setup section not found!")

        # Verify Categories
        categories = ["Core", "PulseLink Apps", "Safety & Security", "Smart Features", "Integrations"]
        for cat in categories:
            if not page.get_by_role("heading", name=cat).is_visible():
                print(f"Category '{cat}' not found!")

        # Test Search
        page.get_by_placeholder("Search features...").fill("RingerSong")
        page.wait_for_timeout(500)

        # Take Screenshot
        page.screenshot(path="verification_extensions_store.png")
        print("Screenshot saved to verification_extensions_store.png")

        browser.close()

if __name__ == "__main__":
    verify_extensions_store()
