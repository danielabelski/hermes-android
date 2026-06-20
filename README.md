# Hermes-Android

Production-minded Android shell app for Hermes WebUI, built with Kotlin + Jetpack Compose.

## What this delivers (MVP)

- Secure WebView shell that opens directly to your Hermes URL
- First-launch URL prompt so each deployment can point to its own Hermes host
- Native slide-out drawer for switching between Hermes WebUI and Dashboard Terminal
- HTTPS-only transport + domain allowlist
- In-app back handling with proper WebView history behavior
- Pull-to-refresh + loading/error/offline states
- File upload and download support
- Android share-sheet support (text/files)
- Session persistence via WebView cookies and encrypted app settings storage
- Native app identity (icon, splash, settings surface)

## Recommended SDK policy

- `minSdk = 26` (Android 8.0): good security baseline and modern WebView behavior
- `targetSdk = 35`: aligns with current Play requirements and platform hardening

## Project structure

- `app/src/main/java/com/hermes/wrapper/MainActivity.kt`: app entrypoint, native drawer, WebView security config, navigation policy, file chooser/download integration, share intent handling
- `app/src/main/java/com/hermes/wrapper/core/security/UrlPolicy.kt`: URL/domain/HTTPS enforcement logic
- `app/src/main/java/com/hermes/wrapper/data/SettingsRepository.kt`: encrypted storage for WebUI URL, Dashboard Terminal URL, and local app settings
- `app/src/main/java/com/hermes/wrapper/domain/ShareIntentParser.kt`: share-sheet payload parser (`ACTION_SEND`, `ACTION_SEND_MULTIPLE`)
- `app/src/main/java/com/hermes/wrapper/domain/ServerUrlValidator.kt`: safe URL validation for settings input
- `app/src/main/java/com/hermes/wrapper/ui/MainViewModel.kt`: app state orchestration
- `app/src/main/java/com/hermes/wrapper/ui/web/WebShell.kt`: Compose WebView host + refresh/loading/error UX
- `app/src/main/java/com/hermes/wrapper/ui/settings/SettingsBottomSheet.kt`: native settings panel
- `app/src/main/AndroidManifest.xml`: app identity, permissions, share intent filters, deep-link placeholder
- `app/src/main/res/xml/network_security_config.xml`: cleartext disabled globally
- `app/src/test/java/com/hermes/wrapper/UrlPolicyTest.kt`: navigation policy tests
- `app/src/test/java/com/hermes/wrapper/ServerUrlValidatorTest.kt`: server URL validation tests

## Security controls implemented

- Enforces HTTPS-only navigation in-app
- Restricts in-WebView browsing to allowlisted host(s)
- Blocks non-web and malformed URLs
- Opens non-allowlisted HTTPS links in external browser (never silently inside app)
- Cancels SSL-error pages
- Hardens WebView defaults:
  - `allowFileAccess = false`
  - `allowContentAccess = false`
  - `allowUniversalAccessFromFileURLs = false`
  - `mixedContentMode = NEVER_ALLOW`
  - third-party cookies disabled
- Encrypted app preference storage via Android Keystore-backed `EncryptedSharedPreferences`

## How this tracks Hermes updates

- The app is intentionally a thin wrapper around the existing Hermes WebUI.
- Most product behavior, UX, and feature updates come from the server-delivered web app.
- The Android layer only owns platform concerns (navigation policy, intents, file integration, secure local settings).
- Result: normal Hermes frontend updates do not require an Android app release unless a native integration surface changes.

## First-run URL behavior

- On first launch, the app opens native settings and asks for your Hermes WebUI URL.
- The app can also store an optional Hermes Dashboard Terminal URL, such as `https://host:8455/chat`.
- This mirrors how Hermes deployment works: onboarding in WebUI configures provider/workspace/password, while the server URL depends on where you host Hermes.
- Saved URLs drive the allowlist policy automatically.
- WebUI and Dashboard Terminal URLs must use `https://`.

## Important configuration before production

1. Set your real Hermes URL default:
   - Update `default_server_url` in `app/src/main/res/values/strings.xml`
2. Set your real Dashboard Terminal URL default:
   - Update `default_dashboard_terminal_url` in `app/src/main/res/values/strings.xml`
3. Set your real deep-link host:
   - Update `android:host` in `app/src/main/AndroidManifest.xml`
4. Rename package/application IDs if needed:
   - `namespace` and `applicationId` in `app/build.gradle.kts`

## Build and run

### Prerequisites

- Android Studio (latest stable) with Android SDK 35
- JDK 17

### Step 1: Generate Gradle wrapper (once)

This scaffold intentionally does not include wrapper binaries.

Option A (recommended): open `android-wrapper/` in Android Studio and run the Gradle sync, then run:

```powershell
gradle wrapper --gradle-version 8.7
```

Option B: if local `gradle` is not installed, install Gradle or use Android Studio's embedded Gradle to generate wrapper files.

### Step 2: Build debug APK

```powershell
cd android-wrapper
./gradlew.bat assembleDebug
```

### Step 3: Run unit tests

```powershell
cd android-wrapper
./gradlew.bat test
```

## Release signing steps

1. Create upload keystore:

```powershell
keytool -genkeypair -v -keystore hermes-upload.jks -alias hermes-upload -keyalg RSA -keysize 2048 -validity 10000
```

2. Add signing properties to `~/.gradle/gradle.properties` (do not commit):

```properties
HERMES_UPLOAD_STORE_FILE=C:/secure/hermes-upload.jks
HERMES_UPLOAD_STORE_PASSWORD=...
HERMES_UPLOAD_KEY_ALIAS=hermes-upload
HERMES_UPLOAD_KEY_PASSWORD=...
```

3. Add a `signingConfigs` block in `app/build.gradle.kts` and wire it to `release`.

4. Build release artifacts:

```powershell
cd android-wrapper
./gradlew.bat bundleRelease
./gradlew.bat assembleRelease
```

5. Verify app bundle locally, then upload AAB to Play Console.

## Incremental implementation status

- [x] Secure WebView shell + navigation policy
- [x] Pull-to-refresh + loading/error/offline UI
- [x] Upload/download support
- [x] Share-to-app ingestion (text/files)
- [x] Session persistence and secure app settings storage
- [x] Native identity shell (icon/splash/settings)
- [x] Native drawer switching between WebUI and Dashboard Terminal
- [x] Basic tests for key security logic

## Phase-2 TODOs

- Push notifications for important run/session events
- Fully wired deep links to session routes
- Optional biometric app lock before revealing WebView
- Expanded native settings (server profiles, theme, notifications)
- Better attachment and camera capture UX

## Decision points with options

### 1. How strict should external navigation be?

Option A: block everything except allowlist (highest security, can break auth/provider redirects).
Option B: open non-allowlisted HTTPS links externally (recommended).
Option C: allow a secondary allowlist for known provider domains.

Recommendation: Option B now; add Option C as a controlled enterprise setting if needed.

### 2. Session/auth persistence strategy

Option A: default WebView cookie persistence only (simple, robust).
Option B: custom token bridge from JS/native (more control, larger attack surface).
Option C: AccountManager/OAuth native stack (best if moving away from pure WebView auth).

Recommendation: Option A for MVP with strict host policy and no JS bridge for auth secrets.

### 3. Native feature growth path

Option A: keep app as secure shell with selective native additions (recommended MVP+).
Option B: migrate more workflows to native screens over time.
Option C: replace WebView entirely with full native client.

Recommendation: Option A in near term to preserve Hermes parity and reduce maintenance risk.
