from playwright.sync_api import sync_playwright

def run():
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page()

        # Capture console logs
        page.on("console", lambda msg: print(f"CONSOLE: {msg.text}"))
        page.on("pageerror", lambda exc: print(f"PAGE ERROR: {exc}"))

        try:
            page.goto("http://localhost:5173", timeout=10000)
            page.wait_for_timeout(3000)
            page.screenshot(path="verification/debug_screenshot.png")
            print("Screenshot saved to verification/debug_screenshot.png")
        except Exception as e:
            print(f"Script failed: {e}")
        finally:
            browser.close()

if __name__ == "__main__":
    run()
