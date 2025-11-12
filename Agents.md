# Agents Guide for PulseLink

## 1. Project Structure & Architecture

### Monorepo layout
- `androidApp/`: Single Android application module (Compose UI, Hilt DI, Room, WorkManager). `build.gradle.kts` defines free (`com.free.pulselink`) and pro (`com.pulselink.pro`) flavors plus shared default config (`com.pulselink`).
- `docs/`: Release-supporting assets (e.g., `alpha-bugs.json`, PEM certificates, mockups) consumed by CI workflows.
- `PulseLink-ui-mockup/` and `branding/`: Static design references for gradients, logos, and conversation UI.
- `Free-Certs/` and `PRO-CERTS/`: Local-only upload keystores plus DER exports for the Play Console; select the right store file via `keystore.properties` or Gradle injected props before building.
- `artifacts/`: Cached sample outputs from experiments; never bake these paths into automation.
- `pulselink.txt`: Living build log outlining recent architectural decisions and release blockers.

### Android module organization
- `com.pulselink.assistant`: Assistant shortcut publisher and intent trampoline (`AssistantShortcuts`, `AssistantShortcutActivity`).
- `com.pulselink.data`: Data layer packages (`alert`, `link`, `location`, `sms`, etc.) that wrap platform services, repositories, Room DAOs, and notification helpers.
- `com.pulselink.domain`: Pure models and repository interfaces shared across UI and services (`EscalationTier`, `AlertProfile`, `PulseLinkSettings`).
- `com.pulselink.service`: Long-running coordinators such as `AlertRouter`, foreground services, and dispatchers; they orchestrate repositories plus the `AudioOverrideManager`.
- `com.pulselink.receiver`: SMS and broadcast receivers (`PulseLinkSmsReceiver`, `SmsSendReceiver`) that ingest modem events into the link manager.
- `com.pulselink.ui`: Compose entry points (`MainActivity`, navigation graph, screens) and state holders (`MainViewModel`).
- `com.pulselink.util`: Cross-cutting helpers (audio overrides, call monitoring, notification utilities).
- Resources follow the Android defaults under `src/main/res`, with per-flavor overrides in `src/free` and `src/pro` if needed.

### Data & alert flow
1. Triggers (Assistant shortcut, UI buttons, remote SMS) call `AlertRouter.dispatchManual` or `AlertRouter.onPhraseDetected`.
2. Router loads current `PulseLinkSettings`, resolves contacts via `ContactRepository`, and calls `AlertDispatcher`.
3. Dispatcher enforces `AudioOverrideManager` rules, builds the notification channel + tone, sends sequential SMS via `SmsSender`, and records results through `AlertRepository`.
4. `ContactLinkManager` manages remote handshakes, override permissions, and manual message state machines; its overrides are shared by UI conversations and remote call prep.

## 2. Coding Conventions & Style

- **Language & tooling**: Kotlin 1.9+, Compose UI, Hilt for DI, Room for persistence, Kotlin Serialization for wire formats. Target/compile SDK 35; min SDK 26.
- **Package-by-layer**: Keep platform-specific code under `data/` or `service/`. UI elements belong in `ui/`, and domain models/interfaces stay under `domain/`.
- **Compose patterns**: Prefer stateless composables with state hoisted into `MainViewModel` or screen-level `UiState`. Use `collectAsStateWithLifecycle` and remember `ScrollState` where scrolling is required (see `HomeScreen`).
- **Naming**: Classes in CamelCase, functions/methods in lowerCamelCase, constants in SCREAMING_SNAKE case. Resource IDs use snake_case (`assistant_setup_action`). Flavor-specific strings should live in their own resource files.
- **Dependency Injection**: Request dependencies via constructor injection (`@Inject constructor`) and annotate entry points with `@AndroidEntryPoint`. Avoid manual singletons.
- **Threading**: Use `viewModelScope` for UI interactions, `Dispatchers.IO` for heavy work (see `AlertDispatcher.dispatch`). Never block the main thread.
- **Logging & errors**: Prefer structured `Log` statements in data/service layers; bubble user-facing errors via Toast/snackbar copy stored in `strings.xml`.
- **Comments & docs**: Limited to non-obvious logic (e.g., tricky audio override timing). Use KDoc for public APIs that need context.
- **Git hygiene**: Do not commit populated `keystore.properties` or artifacts under `Free-Certs/` or `PRO-CERTS/`. Keep `pulselink.txt` updated whenever architectural choices change.

## 3. Testing Protocols

- **Unit tests**: Run `./gradlew testFreeDebugUnitTest testProDebugUnitTest` before opening a PR. These cover repository logic, link manager flows, and utility classes.
- **Instrumentation / integration**: Use `./gradlew connectedFreeDebugAndroidTest` when touching Compose screens, and `./gradlew androidApp:assemble{Free,Pro}Debug` for smoke tests on devices.
- **Release verification**: For signing-sensitive builds, execute `./gradlew clean bundleFreeRelease bundleProRelease assembleFreeRelease assembleProRelease` with the correct keystore configured, mirroring the CI `release-aab.yml` workflow.
- **CI workflows**: `verify-main.yml` runs assemble + unit tests on each push; `publish-apk.yml`/`release-aab.yml` handle signed artifacts. Match local Gradle invocations to those workflows when debugging failures.
- **Manual QA**: Validate Assistant shortcuts (“Hey Google, PulseLink emergency/check-in”), SMS escalation, contact linking, and audio override restore loops on physical hardware before tagging a release.

## 4. Pull Request Guidelines

- **Branching**: Use `feature/<summary>` or `fix/<issue-id>` prefixes. Rebase on `main` before submitting.
- **Description template**:
  1. Summary of user-facing change.
  2. Implementation notes (highlight risky areas such as AlertRouter, ContactLinkManager, or signing config changes).
  3. Testing evidence (`./gradlew …` output, device smoke steps, screenshots for UI changes).
- **Checklist**:
  - [ ] Updated `pulselink.txt` if architecture, release process, or signing details changed.
  - [ ] Added/updated strings, resources, or migrations where applicable.
  - [ ] Included screenshots or screen recordings for Compose/UI modifications.
  - [ ] Ensured keystore secrets stay out of the diff.
- **Review expectations**: Call out breaking changes, migrations, or ops playbook updates. Request reviews from owners of affected areas (UI/alerts/data) and mention any follow-up tasks.
- **Merge policy**: Squash commits with a clear title (`feature: assistant shortcut polish`) unless a multi-commit history is required for bisecting. CI must be green before merge.

-## 5. Automation & Release Workflow

- `release-aab.yml` builds both flavors (free/pro) and uses `r0adkll/upload-google-play` to push `.aab` files to Google Play. Provide the Free secrets (`UPLOAD_KEYSTORE_*`) plus the Pro-specific ones (`PRO_UPLOAD_KEYSTORE_BASE64`, `PRO_KEYSTORE_PASSWORD`, `PRO_KEY_ALIAS`, `PRO_KEY_PASSWORD`) alongside `PLAY_SERVICE_ACCOUNT_JSON`. Trigger with `workflow_dispatch`; you can now independently toggle each upload (`upload_free`, `upload_pro`) and select different tracks (`track_free`, `track_pro`) for the variants.
- Free flavor package: `com.free.pulselink` → bundle path `androidApp/build/outputs/bundle/freeRelease/androidApp-free-release.aab`.
- Pro flavor package: `com.pulselink.pro` → bundle path `androidApp/build/outputs/bundle/proRelease/androidApp-pro-release.aab`.
- The workflow decodes both keystores: Free from `${{ secrets.UPLOAD_KEYSTORE_BASE64 }}` (with `UPLOAD_KEYSTORE_PASSWORD`, `UPLOAD_KEY_ALIAS`, `UPLOAD_KEY_PASSWORD`) and Pro from the `PRO_*` counterparts listed above. Rotate the appropriate set when Play revokes a certificate. If only one flavor needs shipping, leave the other toggle off so the Gradle step skips that bundle.
- Always run `bundleFreeRelease bundleProRelease` locally when touching release signing logic to catch issues outside CI.
- For staged rollouts, adjust the `track` input or extend the upload step with `inAppUpdatePriority`, `releaseStatus`, etc.
- `deploy-pages.yml` publishes everything under `docs/` to GitHub Pages. The Android bug reporter opens `https://DamienLove.github.io/PulseLink/bug-report/`, which prefills a GitHub Issue with the captured form data (labels `bug`, `triage`).
