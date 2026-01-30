
from playwright.sync_api import sync_playwright

def verify_command_palette():
    with sync_playwright() as p:
        browser = p.chromium.launch()
        page = browser.new_page()
        # Mock user to bypass login
        page.goto("http://localhost:5173/?mock_user=true")

        # Wait for app to load
        page.wait_for_timeout(2000)

        # Open Command Palette
        page.keyboard.press("Control+k")

        # Wait for palette animation
        page.wait_for_timeout(1000)

        # Take screenshot
        page.screenshot(path="verification/command_palette.png")
        browser.close()

if __name__ == "__main__":
    verify_command_palette()
