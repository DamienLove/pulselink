# PulseLink Beacon (default SMS/Call fork)

Objective: spin a forked SKU that acts as a full default SMS/Phone handler while keeping PulseLink alert capabilities. This branch holds the scaffolding and plan; push to a new repo named `PulseLink Beacon` once credentials are available.

## Target app IDs / names
- Application ID: `com.pulselink.beacon`
- App name: `PulseLink Beacon`
- Icon: reuse for now; replace after branding pass.

## Must-have for Play approval as default SMS
- Manifest intent filters:
  - `android.provider.Telephony.SMS_DELIVER`
  - `android.provider.Telephony.WAP_PUSH_DELIVER` (MMS)
  - `RECEIVE_MMS`, `RECEIVE_SMS`, `SEND_SMS`, `READ_SMS`
  - Compose handlers for `sms:`/`smsto:`/`mms:` URIs
- Role prompt: `RoleManager.ROLE_SMS` request on first run, graceful fallback.
- SMS storage:
  - Provide `content://` `Telephony`-compatible provider or thin inbox wrapper using platform APIs.
  - Minimal threads list + conversation screen (read/send).
- MMS handling: at least accept/ignore gracefully; can defer full media rendering.
- Keep call features limited to `CALL_PHONE` + optional `READ_PHONE_STATE`; avoid `READ_CALL_LOG` unless you surface call history.

## Migration plan from current code
1) Copy package to `com.pulselink.beacon` (Gradle `applicationId`, namespace; adjust `BuildConfig`, `google-services.json`).
2) Rename app strings to “PulseLink Beacon”.
3) Add default-SMS role request flow and store listing copy aligned with “default SMS handler”.
4) Implement inbox UI:
   - List threads (address, snippet, timestamp, unread).
   - Conversation view with send box; use existing `SmsSender` for outbound.
   - Minimal MMS: show placeholder for media; store raw PDU for now.
5) Alerts integration:
   - Keep alert send/receive; allow SMS fallback automatically (since default handler).
   - Preserve existing alert codecs (`SmsCodec`) and link handling.
6) Data safety + Play Console text:
   - Declare as default SMS app; state it sends/receives user messages and alert codes.
   - Call permissions only if used; avoid `READ_CALL_LOG` unless shown.

## What’s next to start coding
- Add new flavor/dimension or separate module for Beacon; or keep a branch-only SKU with updated `applicationId`.
- Wire RoleManager prompt and inbox stubs.
- Add QA checklist for Play default-SMS compliance (in/outbound, notifications, deletion).

## Limitations here
We can’t create the GitHub fork without your remote credentials. Once you create the `PulseLink Beacon` repo, push this branch there and continue implementation.
