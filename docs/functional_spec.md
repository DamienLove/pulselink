# PulseLink Functional Spec

## Overview
PulseLink passively listens for user-defined trigger phrases ("PulseLink phrases") and escalates alerts to trusted contacts across SMS, high-priority notifications, and optional automated calls. The system must never block outgoing or incoming messaging; it augments communication pathways instead of restricting them.

## Core Capabilities
- Continuous phrase detection via foreground service + on-device speech recognition
- Alert orchestration that can:
  - Send SMS messages with high-visibility language
  - Dispatch critical notifications that override Do Not Disturb by maxing STREAM_RING/STREAM_NOTIFICATION/STREAM_ALARM, requesting audio focus, and posting bypass-enabled channels
  - Share last-known and live location updates when granted
  - Initiate optional follow-up phone call prompts
- Multi-contact management with separate escalation profiles per tier (Emergency vs. Check-in)
- Silent SOS mode that suppresses local UI feedback while still signaling contacts

## Do Not Disturb Override
PulseLink guarantees that critical alerts are audible even when the device is in Do Not Disturb (DND) mode.

1. **Audio gain staging** – `AudioOverrideManager` captures the existing STREAM_RING, STREAM_NOTIFICATION, and STREAM_ALARM values, forces them to their maxima, and normalizes the ringer mode before playback. A short propagation delay (≈75 ms) ensures the new levels apply before the siren starts.
2. **Audio focus** – The app requests transient audio focus (`AUDIOFOCUS_GAIN_TRANSIENT` for emergencies, `AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK` for check-ins, and `AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE` for incoming-call sirens) so other media is momentarily ducked or paused.
3. **Notification channels** – Emergency channels are created with `IMPORTANCE_MAX`, `USAGE_ALARM`, and `setBypassDnd(true)`. On Android 15 (API 35) and newer, this channel-level bypass is the only supported way to break through global DND because `NotificationManager.setInterruptionFilter(INTERRUPTION_FILTER_ALL)` now applies only to the app's AutomaticZenRule.
4. **Interruption filter fallback** – On Android 14 and below, PulseLink still attempts to flip the global interruption filter back to `ALL` when it has `ACCESS_NOTIFICATION_POLICY`. Failures are logged and surfaced to the user.
5. **Restoration** – After playback (default 2 minutes, configurable per use case), the manager restores the original volumes, ringer mode, audio focus, and interruption filter to avoid side effects.

### Limitations
- If the user revokes notification-policy access, PulseLink can still raise STREAM_ALARM but cannot change the global DND filter. The UI surfaces this state and points users to Settings.
- Users can manually downgrade channel importance or disable bypass at the system level. PulseLink validates channels on launch and logs when properties drift.
- OEM-specific DND behaviors (e.g., manufacturer skins that remap usage streams) are outside the app's control; troubleshooting guidance instructs users to re-enable bypass per channel.

## Platform Notes
- Android: Compose UI, WorkManager/ForegroundService for long-running tasks, Room for persistence, Hilt for DI. Audio alerts rely on `STREAM_ALARM` for sirens, `AudioAttributes.USAGE_ALARM`, and `MediaPlayer` instances that loop during emergencies.
- iOS: Planned via SwiftUI, CallKit, PushKit (not yet implemented in this rebuild).

## Security & Privacy
- Data minimization: store only hashed phrases + encrypted contact details
- Require biometric or PIN to alter critical settings
- Provide transparent audit log users can clear

## Troubleshooting DND Override
- **No audible alert while in DND** – Confirm PulseLink has Do Not Disturb access, then verify the Emergency Alerts notification channel is set to "Allow" and can bypass DND.
- **Android 15+ device still silent** – Channel bypass is mandatory on API 35+. Re-enable bypass inside Settings → Apps → PulseLink → Notifications.
- **Partial override** – If PulseLink reports a partial override, volumes were raised but global DND could not be disabled (policy missing or Android 15 restriction). Check notification-policy permissions and channel settings.
- **Remote call siren too quiet** – Ensure linked contacts are allowed to override audio (`allowRemoteOverride=true`) and that STREAM_ALARM wasn't manually muted immediately beforehand. The app restores user volume after the override window.
