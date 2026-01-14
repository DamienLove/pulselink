# PulseLink iOS

This folder contains the PulseLink and Beacon iOS applications.

## Apps & Variants

The project defines four build targets:
- **PulseLink Free** (`com.pulselink.ios.free`)
- **PulseLink Pro** (`com.pulselink.ios.pro`)
- **Beacon Free** (`com.pulselink.beacon.ios.free`)
- **Beacon Pro** (`com.pulselink.beacon.ios.pro`)

## Setup

1. Generate the CocoaPods spec and framework from the KMP module:
   ```bash
   ./gradlew :shared:podspec
   ```
2. Install pods and open the workspace:
   ```bash
   cd iosApp
   pod install
   open PulseLinkiOS.xcworkspace
   ```
3. In Xcode, ensure you select the appropriate scheme (e.g. `PulseLinkPro`).

## Functionality

- **Shared Logic**: Uses the `Shared` Kotlin Multiplatform module for core logic.
- **Communication**: Communicates with Android devices via Firebase Firestore (`outbox` and `lines` collections).
- **Pro Features**: The `PRO` compilation condition is used to enable features like Contacts (PulseLink) and Private Safe (Beacon).

## Notes
- Update `AlertRelayFactory` base URL if you point to a non-production functions instance.
