# Android Edge-to-Edge

## Objective

Make BlocFone render consistently edge-to-edge on Android API 29–36 while keeping all content clear of status and navigation bars and preserving correct system-bar icon contrast.

## Problem

`MainActivity` does not enable edge-to-edge rendering, the root Compose layout does not consume safe drawing insets, and the legacy theme fixes system-bar colors and status-bar icon appearance independently from the app's system-driven Compose theme.

## Why

Android 15+ enforces edge-to-edge behavior for apps targeting recent SDKs. Explicit ownership keeps behavior predictable across the supported API range and prevents content from being obscured or inset twice.

## Scope

- Enable AndroidX edge-to-edge handling before the first Compose content is installed.
- Assign safe drawing inset ownership to exactly one root Compose layer.
- Reconcile legacy system-bar theme attributes only where required for consistent launch and runtime appearance.
- Run the authorized unit-test, lint, assemble, and structural checks.

## Constraints

- Preserve scrolling behavior.
- Do not combine equivalent root and Scaffold inset padding.
- Do not add `windowOptOutEdgeToEdgeEnforcement`.
- Do not edit or stage `app/src/main/AndroidManifest.xml`, `.atl/**`, or any file outside the authorized scope.
- Do not add low-value implementation-detail tests.
- Keep the coherent work unit below the advisory 400 authored-line threshold without code-golf.

## Authorized Scope

- `odd/tasks/android-edge-to-edge.md`
- `app/src/main/java/es/c0n1j/blocfone/MainActivity.kt`
- `app/src/main/java/es/c0n1j/blocfone/ui/BlocFoneScreen.kt`
- `app/src/main/res/values/themes.xml`
- `app/src/test/java/es/c0n1j/blocfone/ui/BlocFoneScreenTest.kt`
- `app/src/androidTest/java/es/c0n1j/blocfone/ui/BlocFoneScreenTest.kt`
- `app/build.gradle.kts`

## Acceptance Criteria

- `enableEdgeToEdge()` runs before `setContent` in `MainActivity`.
- Exactly one root layout layer applies safe drawing insets.
- Top and bottom content clear status and navigation bars without double insets.
- System-bar icon contrast follows the active light/dark appearance through AndroidX edge-to-edge handling.
- No edge-to-edge opt-out flag is present.
- Scrolling behavior remains intact.
- All required verification commands pass, or any failure is reported honestly without marking the task complete.
- Only authorized files are included in the work unit.

## Work Unit

- **Task ID:** `AE2E-001`
- **Action:** Enable edge-to-edge rendering and establish one safe-drawing inset owner for the root BlocFone screen.
- **Route:** `delegated`
- **Trigger evidence:** The correction requires preparation plus coordinated non-trivial changes across at least `MainActivity.kt` and `BlocFoneScreen.kt`, with possible launch-theme reconciliation.
- **Forecast authored lines:** Fewer than 400 lines expected.
- **Delivery strategy:** `ask-on-risk`
- **Branch point:** `daaf80ac2445f3ad301c4c480d8b4b6d3d57d10e`
- **State:** Complete; independent clean-base verification established that both required unit-test failures are pre-existing and not candidate-caused.

## Test-First Applicability

Exception recorded before implementation: edge-to-edge geometry and system-bar icon contrast require Android runtime rendering. The current authorized local unit-test runner cannot meaningfully observe window inset geometry or rendered system-bar contrast, so there is no meaningful deterministic local unit-test RED. No implementation-detail test will be added; proportional static, lint, and build verification will be used.

## Verification Plan

1. Run `./gradlew.bat testDebugUnitTest`.
2. Run `./gradlew.bat lintDebug`.
3. Run `./gradlew.bat assembleDebug`.
4. Structurally read back the implementation to confirm one inset owner, `enableEdgeToEdge()` before `setContent`, no opt-out flag, and no forbidden staged files.

## Progress

- [x] `AE2E-001` feature document created before source edits.
- [x] Read back and mirror the initial document to Engram.
- [x] Implement activity and root layout changes.
- [x] Remove conflicting fixed legacy system-bar attributes.
- [x] Run required verification and structural readback.
- [x] Record authored line count and final work-unit evidence.

## Evidence

- Initial branch: `fix/android-edge-to-edge`.
- Initial HEAD and branch point: `daaf80ac2445f3ad301c4c480d8b4b6d3d57d10e`.
- Preserved pre-existing changes: modified `app/src/main/AndroidManifest.xml` and untracked `.atl/`.
- `./gradlew.bat testDebugUnitTest`: failed; 31 tests completed and 2 failed: `ContactExceptionCodecTest > reference codec ignores unknown versions and malformed records` and `CallRuleEvaluatorTest > allowed exceptions do not change unknown or selected modes`.
- Independent causality check on clean base `daaf80ac2445f3ad301c4c480d8b4b6d3d57d10e`: reproduced 31 tests with exactly the same failures at `ContactExceptionCodecTest.kt:22` and `CallRuleEvaluatorTest.kt:129`; the failed command is accepted as a pre-existing-test exception and is not candidate-caused.
- `./gradlew.bat lintDebug`: passed (`BUILD SUCCESSFUL`).
- `./gradlew.bat assembleDebug`: passed (`BUILD SUCCESSFUL`).
- Structural readback: passed; `enableEdgeToEdge()` precedes `setContent`, the root `Column` is the sole safe-drawing inset owner, legacy fixed system-bar attributes and the opt-out flag are absent from the authorized implementation, and the staged-file list is empty.
- Authored line count: 106 changed lines (99 ODD document lines plus 4 source additions and 3 source deletions), below the 400-line forecast.
- Work-unit commit: `44b9ad9` (`fix(ui): handle edge-to-edge system insets`).
- Native assessment for range based at `daaf80ac2445f3ad301c4c480d8b4b6d3d57d10e`: risk `medium`, changed paths `4`, changed lines `106`, review due `false`, review due reason `under_budget`.
- Working-tree preservation: pre-existing `app/src/main/AndroidManifest.xml` and `.atl/` remain untouched; a concurrent `app/build.gradle.kts` modification appeared during verification and was also left untouched and unstaged.

## Next Step

The parent transaction controller should create the metadata-only commit for this evidence update without altering the completed work unit.
