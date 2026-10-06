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

- [ ] The tile can be added from Android Quick Settings and has the required service declaration, icon, and strings.
- [ ] A normal tap atomically toggles the blocking-enabled value in the existing DataStore.
- [ ] Tile presentation deterministically maps the persisted flag to active/inactive state and remains synchronized with external app changes.
- [ ] A long press opens a compact app-owned blocking-level preferences activity.
- [ ] The compact activity changes the authoritative persisted blocking level through a selector reused with the existing preferences UI.
- [ ] No inline SystemUI submenu or parallel persistence path is introduced.
- [ ] The focused unit test, lint, and debug assembly checks pass.
- [ ] Physical-device smoke validation covers adding the tile, tapping it, state synchronization, long press, and mode changes; manual user confirmation may remain pending and must be recorded.

## Test-first policy

For deterministic presentation mapping, first add the smallest meaningful unit test and observe RED, then implement the mapping and observe GREEN. Android framework and UI portions use compile, lint, and physical-device smoke validation where no meaningful local RED exists. Do not claim RED or GREEN without observed evidence.

## Stable task checklist

- [ ] **QST-1 — Implement one coherent complete work unit.** Add the atomic persisted toggle, tested tile-presentation mapping, `TileService`, manifest/icon/strings, compact long-press preferences activity, reusable mode selector, and integration. Route: delegated direct. Writer/preparation evidence: the change spans 2+ non-trivial files and the preparation/mapping trigger fired. Close as a reviewable work-unit commit with implementation and tests together.
- [ ] **QST-2 — Verify behavior and device integration.** Run the focused automated checks and physical-device smoke scenarios. Route: delegated verification. Record exact results; manual user confirmation may remain pending.

## Checks

### Automated

- [ ] `.\gradlew.bat testDebugUnitTest`
- [ ] `.\gradlew.bat lintDebug`
- [ ] `.\gradlew.bat assembleDebug`

### Physical device

- [ ] Add the BlocFone tile from Quick Settings.
- [ ] Tap the tile and confirm persisted blocking toggles atomically.
- [ ] Change blocking in the app and confirm tile state synchronizes.
- [ ] Long-press the tile and confirm the compact selector opens.
- [ ] Change blocking level and confirm both compact and existing preferences surfaces reflect the persisted mode.

## Progress and evidence

| Task | Status | Evidence |
|---|---|---|
| QST-1 | Pending | RED/GREEN output, changed-file summary, authored-line count, commit hash, and review disposition: _pending_ |
| QST-2 | Pending | Exact automated command results and device smoke notes, including any pending user confirmation: _pending_ |

## Next step

Delegate QST-1 from `feat/quick-settings-tile`, beginning with the presentation-mapping RED test and preserving `825cf81` as the initial reviewed boundary.
