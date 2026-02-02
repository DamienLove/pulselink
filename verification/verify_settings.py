from playwright.sync_api import sync_playwright
import time

def verify_settings():
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        # Increase viewport size to see full panel
        page = browser.new_page(viewport={'width': 1280, 'height': 800})

        try:
            # 1. Navigate to Home with mock user
            print("Navigating to app...")
            page.goto("http://localhost:5173/?mock_user=true")
            page.wait_for_load_state("networkidle")

            # 2. Click Settings in Sidebar
            print("Clicking Settings...")
            page.click("button[title='Settings']")
            time.sleep(1) # Wait for animation

            # 3. Take Screenshot of General Tab (Default)
            print("Capturing General Settings...")
            page.screenshot(path="verification/settings_general.png")

            # 4. Click PulseLink Tab (ensure we click the tab, not the sidebar)
            print("Clicking PulseLink Tab...")
            # Use specific selector for the tab inside settings-tabs
            page.click(".settings-tabs button:has-text('PulseLink')")
            time.sleep(0.5)
            page.screenshot(path="verification/settings_pulselink.png")

            # 5. Click Data Tab
            print("Clicking Data Tab...")
            page.click(".settings-tabs button:has-text('Data & Privacy')")
            time.sleep(0.5)
            page.screenshot(path="verification/settings_data.png")

            print("Verification complete.")

        except Exception as e:
            print(f"Error: {e}")
            page.screenshot(path="verification/error.png")
        finally:
            browser.close()

if __name__ == "__main__":
    verify_settings()
