from playwright.sync_api import sync_playwright, expect
import time

def run():
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        # Taller viewport to see sidebar content
        page = browser.new_page(viewport={'width': 1280, 'height': 1200})

        try:
            page.goto("http://localhost:3000/?mock_user=true", timeout=10000)
        except:
            print("Retrying navigation...")
            time.sleep(2)
            page.goto("http://localhost:3000/?mock_user=true")

        page.wait_for_selector('.nav-item[title="Beacon"]', timeout=10000)
        page.locator('.nav-item[title="Beacon"]').click()

        expect(page.locator('.thread-item').first).to_be_visible()

        # Log the text
        print(page.locator('.thread-item').first.locator('.thread-date').text_content())

        page.screenshot(path="verification/thread_timestamp.png")
        print("Screenshot saved to verification/thread_timestamp.png")

        browser.close()

if __name__ == "__main__":
    run()
