# AGENTS.md - Hermes-Android Coordination

Last updated: 2026-06-20
Scope: `android-wrapper/` only

## 1) Mission

Build and maintain a production-minded Android wrapper for Hermes WebUI that feels native while remaining a thin, secure shell around the web app.

## 2) Scope Guardrails

- ONLY modify files under `android-wrapper/`.
- DO NOT modify Hermes main web project files outside this folder.
- Keep architecture modular and easy to hand off.
- Prefer incremental changes over large rewrites.

## 3) Product Goals (MVP + Phase 2)

### MVP (target: stable and release-ready)
- [x] Secure WebView shell opening Hermes URL
- [x] Back handling + in-app navigation policy
- [x] Pull-to-refresh + loading/error/offline states
- [x] File upload/download support
- [x] Share-to-app intake (text/files)
- [x] Session persistence + secure app settings storage
- [x] Native identity basics (icon/splash/settings)
- [x] Basic unit tests for key URL/security logic

### Phase 2 (planned)
- [ ] Push notifications (FCM + channels + routing)
- [ ] Deep links / app links to Hermes routes
- [ ] Optional biometric app lock
- [ ] Expanded native settings (theme, notifications, host profiles)
- [ ] Better attachment/camera integration
- [ ] Instrumentation tests for WebView + intent flows

## 4) Current Technical Baseline

- Language/UI: Kotlin + Jetpack Compose
- App type: native shell over Hermes WebUI
- Min/Target SDK: 26 / 35
- Java toolchain: JDK 17 language level (builds verified on JDK 21 runtime)
- Security posture:
  - HTTPS-only navigation policy
  - Host allowlist enforcement
  - Externalize non-allowlisted HTTPS navigation
  - Cleartext traffic disabled
  - Hardened WebView defaults
  - Encrypted local settings storage

## 5) Architecture Map (quick)

- `app/src/main/java/com/hermes/wrapper/MainActivity.kt`
  - Platform boundary: WebView, intents, chooser/downloads, nav policy hookup
- `app/src/main/java/com/hermes/wrapper/core/security/UrlPolicy.kt`
  - URL and navigation allow/block/external decisions
- `app/src/main/java/com/hermes/wrapper/data/SettingsRepository.kt`
  - Encrypted settings persistence
- `app/src/main/java/com/hermes/wrapper/domain/ServerUrlValidator.kt`
  - Server URL validation rules
- `app/src/main/java/com/hermes/wrapper/domain/ShareIntentParser.kt`
  - Android share-sheet parsing
- `app/src/main/java/com/hermes/wrapper/ui/`
  - `MainViewModel`, `MainUiState`, Compose screens

## 6) Active Work Board

Status key: `todo` | `in_progress` | `blocked` | `done`
Priority key: `P0` critical, `P1` high, `P2` medium, `P3` low

| ID | Priority | Status | Area | Task | Notes |
|---|---|---|---|---|---|
| A-001 | P0 | done | Build | Fix Java/Gradle setup and verify build/test | `test` + `assembleDebug` successful |
| A-002 | P0 | done | Security | Enforce HTTPS-only URL policy | Includes validation + docs/tests alignment |
| A-003 | P1 | done | Tooling | Align AGP/Gradle to avoid Gradle 10 deprecation pressure | AGP `8.7.3`, Gradle `8.10.2` |
| A-004 | P1 | done | UI | Replace deprecated accompanist swipe refresh | Migrated to Compose pull refresh |
| A-005 | P1 | todo | Deep Links | Add deep-link intent filter + route mapping | Include allowlist/safety checks |
| A-006 | P1 | todo | Notifications | Add FCM plumbing + channel strategy + click routing | Phase 2 |
| A-007 | P1 | todo | Security UX | Add optional biometric app lock gate | Feature-flagged in settings |
| A-008 | P2 | todo | Attachments | Add camera capture in file chooser flow | UX + permissions |
| A-009 | P2 | todo | Settings | Expand native settings screen and persistence | Theme/notif/profile toggles |
| A-010 | P2 | todo | Tests | Add instrumentation tests for navigation/share/deep-links | Emulator CI-ready |
| A-011 | P3 | todo | DX | Add release signing automation docs/snippets | Keep secrets out of repo |
| A-012 | P1 | done | Navigation | Add native drawer and Dashboard Terminal route | WebUI + `/chat` terminal URLs stored in encrypted settings; `test` + `assembleDebug` successful |

## 7) Next Recommended Execution Order

1. A-005 Deep links (high impact, platform-native)
2. A-009 Server profile list (needed before many-user use)
3. A-007 Biometric lock (security/privacy)
4. A-006 Push notifications (infrastructure)
5. A-008 Attachment/camera enhancements

## 8) Definition of Done (DoD)

A task is `done` only if all are true:
- Code stays within `android-wrapper/`
- Security constraints still hold (HTTPS-only + allowlist)
- Unit tests pass: `./gradlew.bat test`
- Debug build passes: `./gradlew.bat assembleDebug`
- Relevant docs updated (`README.md`, `ARCHITECTURE.md`, `TODO-PHASE2.md`, this file)
- No new critical warnings/errors introduced

## 9) Contributor Workflow

1. Pick one task ID from the board.
2. Set status to `in_progress` and add initials/date in notes.
3. Implement minimal incremental changes.
4. Run verification commands.
5. Update board row status + notes.
6. Add short handoff summary under section 11.

## 10) Quick Commands

Use from `android-wrapper/`.

```powershell
.\gradlew.bat test --no-daemon
.\gradlew.bat assembleDebug --no-daemon
.\gradlew.bat help --warning-mode all --no-daemon
```

Optional:

```powershell
.\gradlew.bat lint --no-daemon
.\gradlew.bat connectedDebugAndroidTest --no-daemon
```

## 11) Handoff Log (append only)

Template:

```
Date:
Contributor:
Task IDs:
Summary:
Files touched:
Verification run:
Risks/Follow-ups:
```

Latest:

```
Date: 2026-06-20
Contributor: AI assistant
Task IDs: Initial branding
Summary: Renamed the user-facing app/build identity from HermesWrapper to Hermes-Android before the initial repository commit.
Files touched: settings.gradle.kts, app/src/main/res/values/strings.xml, app/src/main/res/values/themes.xml, app/src/main/AndroidManifest.xml, app/src/main/java/com/hermes/wrapper/MainActivity.kt, README.md, AGENTS.md.
Verification run: test, assembleDebug
Risks/Follow-ups: Package namespace and application ID remain `com.hermes.wrapper`; changing those later would affect installed-app identity.
```

```
Date: 2026-06-20
Contributor: AI assistant
Task IDs: A-012
Summary: Added a native slide-out drawer that switches the hardened WebView between Hermes WebUI and the Dashboard Terminal `/chat` route.
Files touched: app/src/main/java/com/hermes/wrapper/MainActivity.kt, app/src/main/java/com/hermes/wrapper/data/AppSettings.kt, app/src/main/java/com/hermes/wrapper/data/SettingsRepository.kt, app/src/main/java/com/hermes/wrapper/ui/MainUiState.kt, app/src/main/java/com/hermes/wrapper/ui/MainViewModel.kt, app/src/main/java/com/hermes/wrapper/ui/MainViewModelFactory.kt, app/src/main/java/com/hermes/wrapper/ui/settings/SettingsBottomSheet.kt, app/src/main/res/values/strings.xml, README.md, ARCHITECTURE.md, TODO-PHASE2.md, AGENTS.md.
Verification run: test, assembleDebug
Risks/Follow-ups: Drawer has WebUI and Terminal only; files/kanban/sessions need stable route mapping before adding destinations.
```

```
Date: 2026-06-19
Contributor: AI assistant
Task IDs: A-001, A-002, A-003, A-004
Summary: Stabilized build toolchain, enforced HTTPS-only policy, removed Gradle deprecation pressure path, migrated deprecated swipe refresh API.
Files touched: app/build.gradle.kts, gradle/libs.versions.toml, gradle/wrapper/gradle-wrapper.properties, app/src/main/java/com/hermes/wrapper/ui/web/WebShell.kt, app/src/main/java/com/hermes/wrapper/MainActivity.kt, app/src/main/java/com/hermes/wrapper/core/security/UrlPolicy.kt, app/src/main/java/com/hermes/wrapper/domain/ServerUrlValidator.kt, tests and docs.
Verification run: test, assembleDebug, help --warning-mode all
Risks/Follow-ups: phase-2 features not started yet.
```

## 12) Fast Context for Any AI Agent

When starting work, read these first:
- `AGENTS.md` (this file)
- `README.md`
- `ARCHITECTURE.md`
- `TODO-PHASE2.md`

Then inspect:
- `app/src/main/java/com/hermes/wrapper/MainActivity.kt`
- `app/src/main/java/com/hermes/wrapper/ui/MainViewModel.kt`
- `app/src/main/java/com/hermes/wrapper/ui/web/WebShell.kt`

Non-negotiables:
- Stay inside `android-wrapper/`
- Preserve HTTPS-only + allowlist behavior
- Run build/tests before claiming completion

## 13) Update Process (Board + Commits + Handoffs)

Use this process for every change so work remains easy to coordinate.

### Board update rules

- Before coding: set one task to `in_progress` and add initials/date in Notes.
- During work: keep scope focused on that single task ID unless explicitly expanding.
- On blocker: set status to `blocked` and write a one-line unblock condition.
- On completion: set status to `done` only after verification commands pass.

### Commit and branch conventions

- Branch name (recommended): `android-wrapper/<task-id>-<short-topic>`
  - Example: `android-wrapper/A-005-deep-links`
- Commit subject format:
  - `<task-id>: <imperative summary>`
  - Example: `A-005: add deep-link intent routing for session URLs`
- Keep commits incremental and reversible; avoid mixing unrelated tasks.

### Required handoff cadence

- Every completed task must append one entry to section 11 (Handoff Log).
- If work spans sessions, append interim handoff notes at the end of each session.
- Handoff must include: task ID(s), files touched, commands run, and remaining risks.

### Verification minimum before status `done`

Run from `android-wrapper/`:

```powershell
.\gradlew.bat test --no-daemon
.\gradlew.bat assembleDebug --no-daemon
```

If verification is skipped (exception case), explicitly document why in Notes and Handoff.

