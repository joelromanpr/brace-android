# M29 delivery slice: generic OverflowList

**Status:** in progress on draft PR #36 (`joelromanpr/m29-overflow-list`), rebased onto merged M11 main `e03bde9` and based on pinned Blueprint `@blueprintjs/core@6.18.0` (`a60d4c92257612808fbfac81cfeee4fcba91a8b4`). The OverflowList inventory row stays **in progress**. No Maven Central release or stable coverage is claimed.

## Scope

- Generic, key-stable Compose item and overflow slots with width-adaptive measurement, logical start/end collapse, `minVisibleItems`, and an always-rendered final overflow slot.
- Post-layout `onOverflow` callback deduped by actual hidden items, plus semantics-free item probes and a popup-safe trigger measurement slot.
- Interactive catalog variants, a Pages guide, an independent Maven consumer example, and Android interaction/accessibility tests.

## Verification

| Gate | Result |
| --- | --- |
| Pinned source and inventory | Blueprint 6.18.0 MDX and source inspected; one existing inventory row updated, with 0/121 applicable rows stable. |
| Static inventory, tokens, and documentation | `python3 scripts/generate_coverage.py --check`, `python3 scripts/generate_tokens.py --check`, `node scripts/build-docs.mjs`, JavaScript syntax checks, and `git diff --check` passed on the rebased head. Generated coverage is 0/121 applicable rows stable and 0/94 components stable. |
| API, build, lint, and tests | `:brace-core:apiDump`, `build lint checkTokenGeneration checkInventory apiCheck`, test compilation, and catalog assembly passed with the local offline toolchain before the M11 rebase. The post-rebase `build lint checkTokenGeneration checkInventory apiCheck :catalog:assembleDebug` gate passed (377 tasks). |
| Focused Android device tests | Post-rebase `BraceOverflowListTest` XML on the API 36 emulator: **7 tests, 0 failures, 0 errors, 0 skips**. The first run exposed duplicated accessibility nodes from unplaced measurement slots; measurement probes now clear semantics, and the final suite passed twice. |
| Hosted CI for PR #36 | The earlier [`verify` run passed](https://github.com/joelromanpr/brace-android/actions/runs/36241525929); `instrumented` was blocked before tests by the old hosted emulator disk setup. The rebased branch inherited the M11 AOSP automated-test-device workflow and runner cleanup. Hosted [`verify` and API 34 `instrumented` both passed](https://github.com/joelromanpr/brace-android/actions/runs/36244980139) on code/report head `aa65f42`; a final report-only head rerun is pending. |
| Catalog visual/interaction review | Installed at 320×640px, density 160. Narrow start collapse showed `More 3` and `Exports`; opening the menu exposed three ordered hidden actions, and selecting `Analysis` updated the visible state. Widening showed all four sections with hidden count 0; narrowing after end collapse showed `Overview` and `More 3`. Screenshot inspected for overlap and clipping. |
| Maven Local and independent consumer | On the rebased head, published Foundation, Core, and Icons `0.1.0-SNAPSHOT` locally (115 tasks) with AAR, sources JAR, documentation JAR, POM, and Gradle metadata. `verification/consumer-smoke :app:assembleDebug` passed against Maven coordinates only. This was local publication, not Maven Central deployment; the post-rebase coordinate-only consumer `:app:assembleDebug` passed (37 tasks). |
| Manual TalkBack and tablet | Not yet performed. |

## Adaptations and known limits

Compose parent constraints replace Blueprint's DOM resize observer and parent-watching prop. Callers provide accessible visible controls and a labeled overflow trigger/menu because generic items have no native navigation action. The final overflow slot is stable when `alwaysRenderOverflow` is set; callers using popups should provide `overflowMeasureContent` so measurement candidates do not create extra popup windows. Stateful visible slots should use the width-matched `visibleMeasureContent` option. Very large generic item lists have not been benchmarked, and callers should virtualize a large hidden-item menu. A forced `minVisibleItems` count can exceed an extremely narrow width, as it can in the Blueprint contract; callers should choose a minimum for their supported viewport. Manual TalkBack and tablet/physical-device review, final report-head CI, human review, and a public release are outstanding. The inventory stays in progress until review, accessibility, and release gates are met.

Remaining Blueprint core navigation, forms, selects, date/time, icons, and table inventory rows continue in their tracked slices. The next proposed focused branch is `joelromanpr/m30-panel-stack` for the pinned PanelStack row. Full Blueprint parity is not claimed.
