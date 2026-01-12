import time
from playwright.sync_api import sync_playwright

def verify_map_reconciliation():
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page()

        try:
            print("Navigating to app...")
            page.goto("http://localhost:5173")

            # Wait for login screen to confirm app loaded
            print("Waiting for login card...")
            page.wait_for_selector(".login-card")

            # Take screenshot of login screen to confirm app entry point works
            # (Cannot access Map without login, but this confirms handleNewThread didn't break App mount)
            print("Taking screenshot...")
            page.screenshot(path="verification_login.png")
            print("Screenshot saved to verification_login.png")

        except Exception as e:
            print(f"Error: {e}")
            # Dump page content on error
            try:
                print("Page content:", page.content())
            except:
                pass
        finally:
            browser.close()

if __name__ == "__main__":
    # Give dev server a moment to start
    time.sleep(3)
    verify_map_reconciliation()
