from playwright.sync_api import sync_playwright
import time

def run():
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page(viewport={"width": 1280, "height": 720})

        # Navigate to Home with mock user
        print("Navigating to Home...")
        page.goto("http://localhost:5173/?mock_user=true")
        page.wait_for_selector(".home-panel")
        time.sleep(1) # Wait for animation

        # Screenshot Home
        print("Taking screenshot: home_v13.png")
        page.screenshot(path="verification/home_v13.png")

        # Hover over a card
        print("Hovering over Beacon card...")
        page.hover(".home-card")
        time.sleep(0.5)
        page.screenshot(path="verification/home_v13_hover.png")

        # Navigate to Features via Sidebar
        print("Navigating to Features...")
        # Use selector for sidebar item
        page.click(".nav-item[title='Features']")
        page.wait_for_selector(".pulselink-panel")
        time.sleep(1) # Wait for slideUp animation

        # Screenshot Features
        print("Taking screenshot: features_v13.png")
        page.screenshot(path="verification/features_v13.png")

        browser.close()

if __name__ == "__main__":
    run()
