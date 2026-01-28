
import os
from playwright.sync_api import sync_playwright, expect

def verify_ui():
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page()

        # Navigate to the app (Vite port 5176 as 5174 was busy)
        try:
            page.goto("http://localhost:5176?mock_user=true", timeout=15000)
        except Exception as e:
            print(f"Failed to load page: {e}")
            return

        print("Navigated to Home Panel with mock user.")

        # Take a screenshot of the dashboard
        os.makedirs("verification", exist_ok=True)
        page.screenshot(path="verification/dashboard_v12.png")
        print("Screenshot taken: verification/dashboard_v12.png")

        # Verify Quick Actions Grid
        try:
            expect(page.locator(".quick-actions-grid")).to_be_visible(timeout=5000)
            print("Verified: .quick-actions-grid is visible")
        except Exception as e:
            print(f"Failed to verify Quick Actions: {e}")

        # Verify Future Cards
        try:
            expect(page.locator(".future-card").first).to_be_visible(timeout=5000)
            print("Verified: .future-card is visible")
        except Exception as e:
            print(f"Failed to verify Future Cards: {e}")

        # Verify Pulse Guide Card
        if page.locator(".pulse-guide-card").count() > 0:
             expect(page.locator(".pulse-guide-card")).to_be_visible()
             print("Verified: .pulse-guide-card is visible")
        else:
             print("Note: .pulse-guide-card not found")

        browser.close()

if __name__ == "__main__":
    verify_ui()
