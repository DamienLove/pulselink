# Palette's Journal

## 2026-01-20 - Visual Verification in Headless Environment
**Learning:** Visual verification scripts (Playwright) require `mock_user=true` in the URL to ensure data is populated when running against a local dev server without a live Firestore connection. Without this, the UI remains in a loading or empty state, making screenshots useless for verifying component rendering.
**Action:** Always include `?mock_user=true` when designing visual verification tests for this web frontend.
