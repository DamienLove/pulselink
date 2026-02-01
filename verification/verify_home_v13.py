import os
import time
from playwright.sync_api import sync_playwright, expect

def verify_home_v13():
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page(viewport={"width": 1280, "height": 800})

        # Navigate to the app with mock user to bypass login
        url = "http://localhost:5174?mock_user=true"
        print(f"Navigating to {url}")
        try:
            page.goto(url, timeout=30000)
        except Exception as e:
            print(f"Failed to load page: {e}")
            browser.close()
            return

        # Wait for the System Status widget to appear
        system_status = page.locator(".system-status-widget")
        try:
            expect(system_status).to_be_visible(timeout=10000)
            print("System Status widget is visible.")
        except AssertionError:
            print("System Status widget NOT found.")
            page.screenshot(path="/home/jules/verification/failed_home_v13.png")
            browser.close()
            return

        # Check for holographic card class on Home Cards
        home_cards = page.locator(".home-card.holographic-card").first
        expect(home_cards).to_be_visible()
        print("Holographic cards found.")

        # Hover over a card to trigger effects (optional, hard to see in static screenshot but good for logic)
        home_cards.hover()
        time.sleep(0.5) # Allow transition

        # Take a screenshot of the Home Panel
        os.makedirs("/home/jules/verification", exist_ok=True)
        screenshot_path = "/home/jules/verification/home_v13.png"
        page.screenshot(path=screenshot_path)
        print(f"Screenshot taken: {screenshot_path}")

        browser.close()

if __name__ == "__main__":
    verify_home_v13()
