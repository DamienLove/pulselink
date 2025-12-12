# PulseLink Suite

PulseLink is now a 3‑part suite:

- Core: shared engine, data models, and UI foundations.
- PulseLink Safety: the personal safety app (formerly PulseLink / PulseLink Pro) that listens for discreet trigger phrases and escalates alerts with location context to trusted contacts. Focused on emergencies.
- PulseLink Beacon: a default SMS/MMS experience with safety‑aware features. Favorites can break through Do Not Disturb with a specific tone, and a sender can use a keyphrase to trigger an audible alarm on the receiver device (even if the sender doesn’t have the app). Beacon interops with Safety: if a linked contact triggers an emergency in Safety, Beacon can sound the emergency siren too. A future Beacon Pro adds AI summarization and contextual DND bypass for emergent language.

[![CI](https://github.com/DamienLove/pulselink/actions/workflows/verify-main.yml/badge.svg)](https://github.com/DamienLove/pulselink/actions/workflows/verify-main.yml)

<!-- Donation badges — replace placeholders after you enable them (see instructions below) -->
[![GitHub Sponsors](https://img.shields.io/badge/Sponsor-❤_GitHub_Sponsors-ea4aaa?logo=github)](https://github.com/sponsors/DamienLove)
[![Ko‑fi](https://img.shields.io/badge/Buy_me_a_coffee-Ko%E2%80%91fi-29abe0?logo=kofi)](https://ko-fi.com/YOUR_USERNAME)
[![PayPal](https://img.shields.io/badge/Donate-PayPal-00457C?logo=paypal)](https://www.paypal.com/donate?hosted_button_id=YOUR_BUTTON_ID)

## Downloads

- Google Play: listings coming soon
- Android pre-release APKs:
  - Safety (main app): see Releases for signed builds
  - Beacon (SMS app): see Releases for signed builds
  - Note: module debug APKs are available under each module’s `build/outputs/apk/**` after a local build

## iOS Roadmap

PulseLink for iOS is in active planning. See the detailed milestones, dependencies, and how to contribute:

- docs: [iOS Roadmap](docs/ios-roadmap.md)
- GitHub Pages: https://damienlove.github.io/pulselink/ (auto-published from the `docs/` folder)

## Documentation

- Suite landing: docs/index.md
- PulseLink Safety: docs/safety/overview.md
- PulseLink Beacon: docs/beacon/overview.md
- Store listing copy (Beacon): docs/store/beacon/
- Static website bundle to upload to pulselink.damiennichols.com: see `website/` (open `website/index.htm`).

This repository publishes documentation via GitHub Pages from the `docs/` directory. Any changes pushed under `docs/**` will automatically re‑deploy the site via GitHub Actions.

- Source docs: [`/docs`](docs)
- Published site: https://damienlove.github.io/pulselink/
- If you also use the GitHub Wiki feature, mirror or link the same content there for consistency. See `docs/docs-and-wiki.md` for tips.

For a simple static site you can host anywhere (no build), upload the contents of the `website/` folder to your host so that `index.htm` is at the site root.

## Support the Project

If PulseLink helps you or someone you care about, please consider supporting development. Your contributions fund:

- App Store fees and infrastructure (build minutes, test devices)
- Accessibility and safety research
- iOS development to reach more users

How to donate:

- GitHub Sponsors: https://github.com/sponsors/DamienLove
- Ko‑fi: https://ko-fi.com/YOUR_USERNAME
- PayPal: https://www.paypal.com/donate?hosted_button_id=YOUR_BUTTON_ID

Prefer to contribute code, docs, or testing? See Issues and the iOS Roadmap above—PRs are welcome.

## Status

Fresh build created on December 11, 2025.

Recent highlights:

- FCM relay via Cloud Functions for link requests, approvals, and user messages (Wi‑Fi/no‑SIM friendly)
- WebRTC signaling (beta) for low‑latency P2P sessions; off by default
- Diagnostics Logger for easy log export to aid support
- Home screen widget with Emergency / Cancel / Check‑in quick actions
