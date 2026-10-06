# Quick Settings tile

## Objective

Add an Android Quick Settings tile that controls BlocFone's persisted call-blocking state and provides a compact app-owned route for selecting the blocking level.

## Problem and why

Users need a fast way to enable or disable blocking without opening the full app. Android SystemUI does not support an app-defined inline submenu for a tile, so blocking-level changes must open an app-owned activity.

## Scope

- Add one Quick Settings tile whose normal tap atomically toggles the persisted blocking-enabled flag.
- Map active/inactive tile presentation to that persisted flag, matching current app semantics.
- Keep the existing DataStore as the authoritative state source.
- Open a compact app-owned blocking-level selector on long press.
- Reuse the mode selector across the compact activity and existing preferences UI.
- Add the service, manifest entry, icon, strings, presentation mapping and test, and integration needed for a complete feature.
- Follow up with a visible, accessible X close control in the compact activity and release artifacts carrying app `versionName` `1.2.0`.

## Out of scope

- An inline SystemUI blocking-level submenu.
- A new persistence mechanism or changed blocking semantics.
- Unrelated settings, UI, or call-blocking refactors.
- A `versionCode` change, a new close string, or any broader compact-activity redesign.

## Constraints and delivery

| Item | Decision |
|---|---|
| Authorized scope | Android Quick Settings tile and the app-owned compact blocking-level selector described here |
| State authority | Existing DataStore |
| Tile state meaning | Active/inactive equals the persisted blocking-enabled flag |
| Long press | Opens an app-owned activity; no inline SystemUI submenu is possible |
| Forecast | 260–340 authored lines |
| Delivery strategy | `ask-on-risk` |
| Chain strategy | None planned; forecast is below the approximately 400-line delivery threshold |
| Reviewed boundary | Start at branch point `825cf81` on `main` |

### Authorized follow-up

| Item | Decision |
|---|---|
| Source/config surfaces | `app/src/main/java/es/c0n1j/blocfone/quicksettings/BlockingLevelActivity.kt`, `app/build.gradle.kts` |
| Close behavior | A visible, accessible X closes the compact activity and reuses `R.string.close` |
| Compose dependency | Add only the minimum icon dependency if the implementation requires it |
| Release version | Set `versionName` to `1.2.0`; leave `versionCode` unchanged |
| Forecast | Approximately 15–35 authored source/config lines across the two authorized surfaces |
| Route | QST-3 delegated direct; QST-4 delegated verification/build |
| Delivery strategy | Verified local integration into `main`, explicitly requested by the user; no push or remote operation is authorized |

## Acceptance criteria

- [x] The tile can be added from Android Quick Settings and has the required service declaration, icon, and strings.
- [x] A normal tap atomically toggles the blocking-enabled value in the existing DataStore.
- [x] Tile presentation deterministically maps the persisted flag to active/inactive state and remains synchronized with external app changes.
- [x] A long press opens a compact app-owned blocking-level preferences activity.
- [x] The compact activity changes the authoritative persisted blocking level through a selector reused with the existing preferences UI.
- [x] No inline SystemUI submenu or parallel persistence path is introduced.
- [x] The focused unit test, lint, and debug assembly checks pass.
- [x] Physical-device smoke validation covers adding the tile, tapping it, state synchronization, long press, and mode changes; the user explicitly confirmed the requested visual/manual smoke test with “Ok, funciona”.
- [x] The compact `BlockingLevelActivity` presents a visible X with an accessible `R.string.close` description, and activating it closes the activity.
- [x] App `versionName` is `1.2.0`, while `versionCode` remains unchanged.
- [x] Debug lint and debug/release APK plus release AAB builds complete; exact artifact paths and observed signing status are recorded rather than assumed.
- [ ] Physical-device smoke confirms that the X closes the compact window.

## Test-first policy

For deterministic presentation mapping, first add the smallest meaningful unit test and observe RED, then implement the mapping and observe GREEN. Android framework and UI portions use compile, lint, and physical-device smoke validation where no meaningful local RED exists. Do not claim RED or GREEN without observed evidence.

QST-3 has no proportionate runnable local RED for `Activity.finish()`/Compose UI behavior because the repository has no `androidTest` or Compose UI-test harness. Use structural review, compile/lint, and physical-device smoke instead; this exception does not waive verification.

## Stable task checklist

- [x] **QST-1 — Implement one coherent complete work unit.** Add the atomic persisted toggle, tested tile-presentation mapping, `TileService`, manifest/icon/strings, compact long-press preferences activity, reusable mode selector, and integration. Route: delegated direct. Writer/preparation evidence: the change spans 2+ non-trivial files and the preparation/mapping trigger fired. Close as a reviewable work-unit commit with implementation and tests together.
- [x] **QST-2 — Verify behavior and device integration.** Run the focused automated checks and physical-device smoke scenarios. Route: delegated verification. Exact automated failures and manual user confirmation are recorded below.
- [x] **QST-3 — Add close control and update release version.** In `BlockingLevelActivity`, add a visible accessible X that reuses `R.string.close` and closes the compact activity. Add the minimum Compose icon dependency only if needed. Set `versionName` to `1.2.0` without changing `versionCode`. Route: delegated direct because reading prepares a write and two non-trivial files are involved.
- [ ] **QST-4 — Verify and package the follow-up.** Run the exact compile/lint/build checks, establish signing status when needed, record exact APK/AAB paths, and obtain physical confirmation that the X closes the compact window. Route: delegated verification/build. Do not mark the full unit suite passing: its two known existing failures remain documented below.

## Checks

### Automated

- [x] `.\gradlew.bat testDebugUnitTest` ran exactly 34 tests with 2 failures, both matching previously documented base failures:
  - `ContactExceptionCodecTest > reference codec ignores unknown versions and malformed records` at `ContactExceptionCodecTest.kt:22`.
  - `CallRuleEvaluatorTest > allowed exceptions do not change unknown or selected modes` at `CallRuleEvaluatorTest.kt:129`.
- [x] `.\gradlew.bat lintDebug`
- [x] `.\gradlew.bat assembleDebug`

### Physical device

- [x] Add the BlocFone tile from Quick Settings.
- [x] Tap the tile and confirm persisted blocking toggles atomically.
- [x] Change blocking in the app and confirm tile state synchronizes.
- [x] Long-press the tile and confirm the compact selector opens.
- [x] Change blocking level and confirm both compact and existing preferences surfaces reflect the persisted mode.

Device evidence: `.\gradlew.bat installDebug` succeeded on one A142 running Android API 36. Explicit launcher `es.c0n1j.blocfone/.MainActivity` succeeded. The registered tile component is `es.c0n1j.blocfone/.quicksettings.BlocFoneTileService` with `android.permission.BIND_QUICK_SETTINGS_TILE`; the registered long-press activity is `es.c0n1j.blocfone/.quicksettings.BlockingLevelActivity`. `cmd statusbar check-support` returned `true`, and the add-tile and click-tile commands completed successfully. The user then explicitly confirmed visibility, tap/state synchronization, long-press opening, and blocking-level behavior with “Ok, funciona”. Repository status before and after verification remained `## feat/quick-settings-tile` plus only `?? .atl/`.

### Planned follow-up checks

- [x] `.\gradlew.bat :app:lintDebug :app:assembleDebug :app:assembleRelease :app:bundleRelease` completed successfully in 1m16s with 98 actionable tasks (33 executed, 65 up-to-date).
- [x] `.\gradlew.bat :app:signingReport` passed: debug signing is configured and the release signing configuration is null.
- [x] Verified and recorded the exact produced debug APK, release APK, and release AAB paths below. No debug AAB was requested or produced.
- [x] Verified signing rather than assuming it: the debug APK is signed with the debug certificate and verifies with APK Signature Scheme v2; the release APK and AAB are unsigned and are not distribution-ready.
- [ ] On a physical device, open the compact blocking-level activity and confirm that activating the X closes its window.
- [ ] Preserve the existing full-unit-suite result: 34 tests with the two known failures above; never record that suite as passing unless a later observed run actually passes.

## Progress and evidence

| Task | Status | Evidence |
|---|---|---|
| QST-1 | Complete | RED: focused test failed to compile because the presentation mapping and tile strings did not exist. GREEN/refactor: all 3 mapping tests pass. Surfaces: repository toggle; quick-settings mapping, service, and activity; shared selector/main-screen integration; manifest, strings, icon, and test. Authored lines: 376 additions. Route: delegated direct; preparation trigger confirmed by 2+ non-trivial files. Verification: focused test, lint, and assembly pass; full unit suite reproduces only the documented `ContactExceptionCodecTest.reference codec ignores unknown versions and malformed records` and `CallRuleEvaluatorTest.allowed exceptions do not change unknown or selected modes` base failures. Implementation commit: `ee622cd feat(android): add blocking quick settings tile`. |
| QST-2 | Complete | `.\gradlew.bat testDebugUnitTest` ran 34 tests with 2 failures: `ContactExceptionCodecTest > reference codec ignores unknown versions and malformed records` at `ContactExceptionCodecTest.kt:22`, and `CallRuleEvaluatorTest > allowed exceptions do not change unknown or selected modes` at `CallRuleEvaluatorTest.kt:129`; both are the same previously documented base failures, so the full unit suite is not recorded as passing. `.\gradlew.bat installDebug` succeeded on one A142 running Android API 36; explicit launcher, tile support check, add-tile, and click-tile commands succeeded; component registrations were confirmed. The user explicitly confirmed the complete requested visual/manual smoke test with “Ok, funciona”. Repository status remained clean except for untouched untracked `.atl/`. |
| QST-3 | Complete | Added a Material 3 `IconButton` with `Icons.Default.Close` beside the compact activity title. Its `contentDescription` reuses `R.string.close`, and its click callback invokes `finish()` on `BlockingLevelActivity`. Added the BOM-managed `androidx.compose.material:material-icons-core` dependency. Set app `versionName` to `1.2.0`; `versionCode` remains `3`. `.\gradlew.bat :app:lintDebug :app:assembleDebug` completed successfully in 37 seconds with 46 actionable tasks (19 executed, 27 up-to-date). `git diff --check` exited successfully with only Git's LF-to-CRLF working-copy warnings. Structural readback confirmed the X, accessibility string, `finish()`, version values, and that tracked edits were confined to the three authorized surfaces; untracked `.atl/` remained untouched. No local RED applies because the repository has no `androidTest` or Compose UI-test harness. |
| QST-4 | Partial | Local build, artifact, and signing checks are complete. `.\gradlew.bat :app:lintDebug :app:assembleDebug :app:assembleRelease :app:bundleRelease` was BUILD SUCCESSFUL in 1m16s with 98 actionable tasks (33 executed, 65 up-to-date). `.\gradlew.bat :app:signingReport` passed with debug signing configured and release config null. The debug APK is signed and verified with APK Signature Scheme v2 and a debug certificate; release outputs are unsigned and not distribution-ready. Physical X-close behavior remains pending because no device deployment or ADB was authorized. Repository status immediately before this tracker edit was `## feat/quick-settings-close-release` plus only untouched untracked `.atl/`. |

### QST-4 local artifact evidence

| Artifact | Size | UTC modified | SHA-256 | Version | Signing |
|---|---:|---|---|---|---|
| `app/build/outputs/apk/debug/app-debug.apk` | 33,687,359 bytes | `2026-10-06T18:08:13.8933863Z` | `9528DE52A6A0C42D667395E5DE3748867633111506E85EE9642C9A5CFD800D3F` | `versionName 1.2.0`, `versionCode 3` | Signed and verified with APK Signature Scheme v2 using the debug certificate. |
| `app/build/outputs/apk/release/app-release-unsigned.apk` | 5,770,313 bytes | `2026-10-06T18:13:00.8344597Z` | `4A98AFCCA5A35D741084D13DFC6D4E6E3030C140D8C61CF7C6E51B7A5CE12408` | `versionName 1.2.0`, `versionCode 3` | Unsigned and does not verify; not distribution-ready. |
| `app/build/outputs/bundle/release/app-release.aab` | 6,479,799 bytes | `2026-10-06T18:13:02.1028608Z` | `50AF00F498A78252CA824F547291D6C5170062CB4529E6EE3632F6BEED066B37` | `versionName 1.2.0`, `versionCode 3` | Unsigned per `jarsigner`; not distribution-ready. |

No debug AAB was requested or produced. Non-blocking observed build warnings were a missing `ExperimentalCoroutinesApi` opt-in at `SettingsRepository.kt:71` and Gradle deprecations affecting Gradle 9.0 compatibility.

## Next step

Complete QST-4 by obtaining authorized physical-device confirmation that activating the X closes the compact window. Local build, artifact, and signing evidence is complete; release outputs remain unsigned and are not distribution-ready. After physical confirmation is recorded, use the user-authorized local integration into `main`; do not push or perform any remote operation. Preserve `825cf81` as the initial reviewed boundary and all completed QST-1/QST-2 evidence.
