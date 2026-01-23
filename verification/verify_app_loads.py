from playwright.sync_api import sync_playwright

def verify_app_loads():
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page()
        try:
            print("Navigating to app...")
            page.goto("http://localhost:5173/")

            # Wait for login screen elements
            print("Waiting for login screen...")
            page.wait_for_selector("text=PulseLink Web", timeout=10000)
            page.wait_for_selector("text=Login to access your messages")

            print("Taking screenshot...")
            page.screenshot(path="verification/app_loaded.png")
            print("Screenshot saved to verification/app_loaded.png")

        except Exception as e:
            print(f"Verification failed: {e}")
            page.screenshot(path="verification/error.png")
            raise e
        finally:
            browser.close()

if __name__ == "__main__":
    verify_app_loads()
