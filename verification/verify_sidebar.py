from playwright.sync_api import sync_playwright, expect

def run():
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page()
        try:
            # Navigate to the app
            page.goto("http://localhost:5173")

            # Wait for the sidebar to be visible
            sidebar = page.locator(".sidebar")
            expect(sidebar).to_be_visible()

            # Check for duplicate sidebars
            count = sidebar.count()
            print(f"Sidebar count: {count}")

            if count != 1:
                print("Error: Expected 1 sidebar, found", count)
            else:
                print("Success: Found exactly 1 sidebar.")

            # Take a screenshot
            page.screenshot(path="verification/sidebar_verification.png")

        except Exception as e:
            print(f"Error: {e}")
        finally:
            browser.close()

if __name__ == "__main__":
    run()
