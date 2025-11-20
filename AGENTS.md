# Repository Guidelines

## Project Structure & Module Organization
- `androidApp/` (Compose + Hilt) holds the client: shared code in `src/main/java/com/pulselink`, UI assets in `src/main/res`, instrumentation scaffolding in `src/androidTest/java`, and tier overrides in `src/free` + `src/pro`.
- `functions/` hosts the Node 20 Firebase/Genkit automations (`src/` TypeScript → `lib/` JS); keep emulator credentials and service keys outside git.
- `docs/`, `branding/`, and `PulseLink-ui-mockup/` provide copy and visual references, while binary artifacts under `androidApp/build/outputs/` or `temp_aab_free/` must be regenerated via Gradle (never edited manually).

## Build, Test, and Development Commands
- `./gradlew assembleFreeDebug` (Windows: `.\gradlew`) builds the beta APK in `androidApp/build/outputs/apk/free/debug/`; `./gradlew bundleProRelease` generates the Play AAB once keystore inputs are set.
- `./gradlew lintFreeDebug testFreeDebugUnitTest` keeps code linted and unit-tested, and `./gradlew connectedFreeDebugAndroidTest` runs Compose/UI suites on an emulator or device.
- Inside `functions/`, run `npm install`, `npm run lint`, `npm run build`, and `npm run serve`; `npm run deploy` ships the Cloud Functions target.

## Coding Style & Naming Conventions
- Kotlin follows the official style (4-space indents). Name Composables and Hilt entry points with `UpperCamelCase`, keep state holders `lowerCamelCase`, and constants `ALL_CAPS`.
- Resource files stay lowercase with underscores (`ic_help_button.xml`, `color_alert_primary`), with flavor-specific overrides in `src/free/res` or `src/pro/res`.
- TypeScript is linted with ESLint’s Google config; favor named exports, async/await, and keep shared config in `genkit.config.ts`.

## Testing Guidelines
- Put JVM tests in `androidApp/src/test/java` (create if missing) and instrumentation specs in `androidApp/src/androidTest/java`, mirroring package names. Compose tests end with `Test`.
- Run `./gradlew testFreeDebugUnitTest` before every commit and `./gradlew connectedFreeDebugAndroidTest` whenever navigation, permissions, or Hilt wiring change.
- Functions updates must pass `npm run lint` + `npm run build`; never encode secrets in `google-services.json`—inject them via Firebase config or environment variables.

## Commit & Pull Request Guidelines
- Use Conventional Commits (`feat`, `fix`, `chore`, etc.) as in history (`feat: polish onboarding + settings`).
- PRs must describe user impact, list Gradle/Firebase commands executed, and attach screenshots or recordings for UI changes (cover free and pro). Link relevant issues or docs.
- Rebuild `androidApp/build/outputs/apk/free/debug/androidApp-free-debug.apk`; `verify-main.yml` fails if the checked-in beta APK is missing. Highlight signing or Firebase config adjustments, especially around `keystore.properties*`.

## Security & Configuration Tips
- Keep signing secrets in `keystore.properties` or `UPLOAD_KEYSTORE_*`; only the template belongs in git.
- Store Firebase Admin keys and Genkit credentials outside the repo (use `firebase login:ci` or Application Default Credentials) and scrub logs before attaching them to tickets.
