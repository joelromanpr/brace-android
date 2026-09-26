# M19 delivery slice: radio and segmented choices

**Status:** local source slice rebased onto main `b8dc999` in `joelromanpr/m19-radio-segmented`; no pull request or Maven Central release yet. Released applicable coverage remains **0/121**.

## Scope

The pinned Blueprint `Radio`, `RadioGroup`, and `SegmentedControl` rows are present and applicable. M19 adds their controlled Compose APIs, semantic/component tokens, keyboard and TalkBack behavior, catalog samples, documentation, Android interaction tests, and an independent Maven consumer example. The separate `RadioCard` inventory row remains planned.

## Verification

| Gate | Result |
| --- | --- |
| Post-rebase static checks | Passed on the rebased tree: token generation and coverage `--check`, docs site build (147 rows, 27 guides), JavaScript syntax, and Git whitespace/conflict checks. |
| Post-rebase build, lint, API, device tests, and Maven consumer | Pending. The shared Gradle/device lane is occupied; the prior results below do not validate the rebased tree. |
| Prior token contrast checks | Passed on the original `fba7156` branch before the rebase: every segmented text/state pair above 4.5:1, each neutral selected border above 3:1, and every radio indicator pair above 3:1. Foundation unit tests encode these checks. |
| Prior build, lint, tests, and API baseline/check | Passed on the original `fba7156` branch before the rebase with the pinned toolchain: foundation/core `apiDump`, then the 287-task `build lint checkTokenGeneration checkInventory apiCheck` graph. A temporary init script resolved official dependencies from a local mirror. |
| Prior focused API 36 interaction tests | Passed on the original `fba7156` branch before the rebase: 15/15, 0 failures, 0 skips. Tests cover controlled/disabled selection, touch, mouse, Enter/Space, roving Tab and arrows, RTL, restoration, large text, theme changes, and automated API 34+ accessibility checks. |
| Prior Maven Local publication and independent consumer | Passed on the original `fba7156` branch before the rebase: foundation/core AAR, sources JAR, KDoc JAR, POM, and module metadata published locally; independent `verification/consumer-smoke` app assembled from Maven coordinates only. |
| Prior installed 320×640 catalog visual/interaction QA | Passed before the rebase in light and dark high contrast: RadioGroup and SegmentedControl rendered within the narrow viewport and responded to real touch selection. Android accessibility-node inspection found each segment as one labeled, checkable `RadioButton` with selected/disabled state; the disabled segment was not keyboard focusable. |
| Hosted CI, review, release | Pending. |

## Known limits and next branch

The Android API uses hoisted values instead of Blueprint's uncontrolled `defaultValue`; browser-only DOM refs, CSS inline layout, and alternate ARIA group, toolbar, and menu roles map to native Compose modifiers and selection semantics. Segment option icons are decorative; a spoken text label is required. Manual TalkBack speech, external hardware keyboard, broader pointer-device and form-factor checks remain for release acceptance. UIAutomator node inspection and Compose interaction tests cover the machine-verifiable semantics and controls. All three rows stay **in progress** until review and publication. The next branch should be chosen from the remaining pinned P1 inventory after M19 is merged; `RadioCard` is a separate row.
