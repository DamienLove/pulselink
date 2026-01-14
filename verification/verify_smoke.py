from playwright.sync_api import sync_playwright

def verify_smoke():
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page()
        try:
            # Navigate to the app (using port 5174 as found in logs)
            page.goto("http://localhost:5174")

            # Wait for content to load
            page.wait_for_selector(".app-shell", timeout=5000)

            # Take a screenshot
            page.screenshot(path="verification/smoke_test.png")
            print("Screenshot taken: verification/smoke_test.png")

            # Check title or text
            content = page.content()
            if "PulseLink Web" in content:
                print("SUCCESS: PulseLink Web text found.")
            else:
                print("WARNING: PulseLink Web text NOT found.")

        except Exception as e:
            print(f"ERROR: {e}")
        finally:
            browser.close()

if __name__ == "__main__":
    verify_smoke()
