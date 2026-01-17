from playwright.sync_api import Page, expect, sync_playwright
import time

def verify_ui():
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page()
        try:
            # 1. Navigate to the app
            page.goto("http://localhost:5173")

            # 2. Wait for the login card to appear
            expect(page.locator(".login-card")).to_be_visible()

            # 3. Take a screenshot of the login page to verify glassmorphism and inputs
            page.screenshot(path="verification/login_page.png")
            print("Login page screenshot taken.")

            # 4. Check for specific new styles if possible (e.g. check background color of body)
            # This is hard to assert visually without screenshot analysis, but we can check computed styles.
            bg_color = page.evaluate("getComputedStyle(document.body).backgroundColor")
            print(f"Body background color: {bg_color}")

            # Verify it matches our deep black (approximate or exact string)
            # #020305 is rgb(2, 3, 5)
            if "rgb(2, 3, 5)" in bg_color or "rgba(2, 3, 5" in bg_color:
                print("Background color verification passed.")
            else:
                print(f"Warning: Background color {bg_color} might not match expected rgb(2, 3, 5).")

        except Exception as e:
            print(f"Verification failed: {e}")
        finally:
            browser.close()

if __name__ == "__main__":
    verify_ui()
