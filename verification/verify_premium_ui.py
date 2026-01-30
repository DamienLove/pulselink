from playwright.sync_api import sync_playwright, expect
import time
import os

def verify_premium_ui():
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        context = browser.new_context(viewport={'width': 1280, 'height': 720})
        page = context.new_page()

        try:
            # 1. Non-Premium State
            print("Verifying Non-Premium State...")
            page.goto("http://localhost:5173/?mock_user=true&premium=false&pro=false")
            # Wait for load
            page.wait_for_timeout(2000)

            # Click Beacon
            page.locator('.home-card h3', has_text='Beacon Inbox').click()
            page.wait_for_timeout(1000)

            # Screenshot Upsell
            screenshot_path_upsell = "verification/upsell_state.png"
            page.screenshot(path=screenshot_path_upsell)
            print(f"Upsell screenshot saved to {screenshot_path_upsell}")

            # 2. Premium State
            print("Verifying Premium State...")
            page.goto("http://localhost:5173/?mock_user=true&premium=true")
            page.wait_for_timeout(2000)

            # Click Beacon
            page.locator('.home-card h3', has_text='Beacon Inbox').click()
            page.wait_for_timeout(1000)

            # Screenshot Inbox
            screenshot_path_inbox = "verification/inbox_state.png"
            page.screenshot(path=screenshot_path_inbox)
            print(f"Inbox screenshot saved to {screenshot_path_inbox}")

        except Exception as e:
            print(f"Error: {e}")
            raise e
        finally:
            browser.close()

if __name__ == "__main__":
    verify_premium_ui()
