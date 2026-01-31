from playwright.sync_api import sync_playwright

def run():
    with sync_playwright() as p:
        browser = p.chromium.launch()
        page = browser.new_page()
        # Use mock_user=true to bypass login
        page.goto("http://localhost:5173/?mock_user=true")
        page.wait_for_selector(".home-panel")

        # Take a screenshot of the home panel
        page.screenshot(path="verification/home_v13.png", full_page=True)

        # Navigate to Settings to see the glass cards
        page.click("button[title='Settings']")
        page.wait_for_selector(".settings-panel")
        page.screenshot(path="verification/settings_v13.png", full_page=True)

        browser.close()

if __name__ == "__main__":
    run()
