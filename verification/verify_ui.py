from playwright.sync_api import sync_playwright

def run():
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page()
        page.goto("http://localhost:5173/?mock_user=true")

        # Wait for dashboard
        page.wait_for_selector(".home-panel")

        # Wait a bit for animations
        page.wait_for_timeout(1000)

        # Take screenshot of the home panel
        page.screenshot(path="verification/dashboard_stats.png", full_page=True)
        browser.close()

if __name__ == "__main__":
    run()
