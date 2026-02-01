import os
from playwright.sync_api import sync_playwright, expect

def verify_mobile_v13():
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        # iPhone 12 Pro viewport
        page = browser.new_page(viewport={"width": 390, "height": 844})

        # Navigate to the app with mock user
        url = "http://localhost:5174?mock_user=true"
        print(f"Navigating to {url} (Mobile)")
        try:
            page.goto(url, timeout=30000)
        except Exception as e:
            print(f"Failed to load page: {e}")
            browser.close()
            return

        # Check for System Status widget
        system_status = page.locator(".system-status-widget")
        try:
            expect(system_status).to_be_visible(timeout=10000)
            print("System Status widget is visible on mobile.")
        except AssertionError:
            print("System Status widget NOT found on mobile.")
            page.screenshot(path="/home/jules/verification/failed_mobile_v13.png")
            browser.close()
            return

        # Take a screenshot
        os.makedirs("/home/jules/verification", exist_ok=True)
        screenshot_path = "/home/jules/verification/mobile_v13.png"
        page.screenshot(path=screenshot_path)
        print(f"Screenshot taken: {screenshot_path}")

        browser.close()

if __name__ == "__main__":
    verify_mobile_v13()
