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

## Out of scope

- An inline SystemUI blocking-level submenu.
- A new persistence mechanism or changed blocking semantics.
- Unrelated settings, UI, or call-blocking refactors.

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

## Acceptance criteria

- [x] The tile can be added from Android Quick Settings and has the required service declaration, icon, and strings.
- [x] A normal tap atomically toggles the blocking-enabled value in the existing DataStore.
- [x] Tile presentation deterministically maps the persisted flag to active/inactive state and remains synchronized with external app changes.
- [x] A long press opens a compact app-owned blocking-level preferences activity.
- [x] The compact activity changes the authoritative persisted blocking level through a selector reused with the existing preferences UI.
- [x] No inline SystemUI submenu or parallel persistence path is introduced.
- [x] The focused unit test, lint, and debug assembly checks pass.
- [ ] Physical-device smoke validation covers adding the tile, tapping it, state synchronization, long press, and mode changes; manual user confirmation may remain pending and must be recorded.

## Test-first policy

For deterministic presentation mapping, first add the smallest meaningful unit test and observe RED, then implement the mapping and observe GREEN. Android framework and UI portions use compile, lint, and physical-device smoke validation where no meaningful local RED exists. Do not claim RED or GREEN without observed evidence.

## Stable task checklist

- [x] **QST-1 — Implement one coherent complete work unit.** Add the atomic persisted toggle, tested tile-presentation mapping, `TileService`, manifest/icon/strings, compact long-press preferences activity, reusable mode selector, and integration. Route: delegated direct. Writer/preparation evidence: the change spans 2+ non-trivial files and the preparation/mapping trigger fired. Close as a reviewable work-unit commit with implementation and tests together.
- [ ] **QST-2 — Verify behavior and device integration.** Run the focused automated checks and physical-device smoke scenarios. Route: delegated verification. Record exact results; manual user confirmation may remain pending.

## Checks

### Automated

- [ ] `.\gradlew.bat testDebugUnitTest`
- [x] `.\gradlew.bat lintDebug`
- [x] `.\gradlew.bat assembleDebug`

### Physical device

- [ ] Add the BlocFone tile from Quick Settings.
- [ ] Tap the tile and confirm persisted blocking toggles atomically.
- [ ] Change blocking in the app and confirm tile state synchronizes.
- [ ] Long-press the tile and confirm the compact selector opens.
- [ ] Change blocking level and confirm both compact and existing preferences surfaces reflect the persisted mode.

## Progress and evidence

| Task | Status | Evidence |
|---|---|---|
| QST-1 | Complete | RED: focused test failed to compile because the presentation mapping and tile strings did not exist. GREEN/refactor: all 3 mapping tests pass. Surfaces: repository toggle; quick-settings mapping, service, and activity; shared selector/main-screen integration; manifest, strings, icon, and test. Authored lines: 376 additions. Route: delegated direct; preparation trigger confirmed by 2+ non-trivial files. Verification: focused test, lint, and assembly pass; full unit suite reproduces only the documented `ContactExceptionCodecTest.reference codec ignores unknown versions and malformed records` and `CallRuleEvaluatorTest.allowed exceptions do not change unknown or selected modes` base failures. Commit identity and review disposition: _pending parent commit/review_. |
| QST-2 | Pending | Exact automated command results and device smoke notes, including any pending user confirmation: _pending_ |

## Next step

Run QST-2 automated re-verification and physical-device smoke from `feat/quick-settings-tile`; preserve `825cf81` as the initial reviewed boundary.
