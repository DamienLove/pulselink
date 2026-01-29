from playwright.sync_api import sync_playwright

def run():
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page()
        # Use mock_user=true to bypass login
        page.goto("http://localhost:5173/?mock_user=true")

        # Wait for PulseGuide FAB
        page.wait_for_selector(".pulse-guide-fab")

        # Click it to open
        page.click(".pulse-guide-fab")

        # Wait for card
        page.wait_for_selector(".pulse-guide-card")

        # Screenshot
        page.screenshot(path="verification/pulse_guide.png")
        browser.close()

if __name__ == "__main__":
    run()
