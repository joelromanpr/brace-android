# M33 · date ranges

**Status:** local implementation in progress on `joelromanpr/m33-datetime-range`, based on M13 `ac121b3`. The pinned Blueprint 6.18.0 DateRangePicker and DateRangeInput rows are covered by source, samples, documentation, and tests. They are not marked stable, released, or merged. Applicable stable coverage remains **0/121**.

## Scope

- Controlled `BraceDateRange`, `BraceDateRangePicker`, and `BraceDateRangeField` APIs in `brace-datetime`.
- Localized partial endpoints, chronological selection, one-day policy, bounds, disabled endpoints, shortcut validation, two-month responsive calendar, keyboard and TalkBack semantics, 48 dp targets, saved visible month and field drafts.
- Versioned `dateRangePicker` and `dateRangeInput` tokens; English, Spanish, and French action/error strings; catalog demos and copyable examples; Maven consumer compile use.

## Verification

| Gate | Current-head result |
| --- | --- |
| Static generation and site | Passed token and coverage checks, JavaScript syntax, resource XML parsing, and documentation build: **147 pinned rows, 35 guides, 0/121 stable**. |
| Focused Kotlin, JVM, AndroidTest, and catalog compile | Passed after correcting the IME Done callback type. `DateModelTest` **3/3** and `DateRangeModelTest` **6/6** passed; focused Gradle run **102 actionable tasks**. |
| Public API | Regenerated exact `brace-foundation` and `brace-datetime` baselines in a separate `apiDump` invocation, then both `apiCheck` tasks passed. |
| API 36 instrumented interactions and accessibility | Full `brace-datetime` suite passed **27/27**, 0 failed/skipped on `emulator-5556`: M13 picker/field regressions plus M33 range touch, mouse, keyboard, bounds, shortcut, restoration, high-contrast target, and popover flows. |
| Full build, lint, token, inventory, API, catalog | Passed `build lint checkTokenGeneration checkInventory apiCheck :catalog:assembleDebug --no-parallel` (**563 actionable tasks**). |
| Maven Local and independent consumer | Published aligned foundation/core/icons/select/datetime AARs, sources, KDoc JARs, POMs, and module metadata locally (**193 tasks**). The separate coordinate-only consumer assembled (**37 tasks**). No Maven Central upload occurred. |
| 320 × 640 catalog visual/interaction smoke | Inspected light DateRangePicker and DateRangeInput states, selected the range shortcut, opened the shared range popover, and inspected the dark field state. No overlap or clipping of actionable controls observed. The seven 48 dp day targets scroll horizontally, with the last weekday initially offscreen. |
| Hosted CI and review | Pending. Recent GitHub Actions jobs in this account have stopped before executing steps because of a billing gate; this branch has not been pushed or opened as a PR. |

## Known limits and next branch

Only range endpoints are filtered by `isDateEnabled`; a range can span interior disabled dates. Blueprint hover range preview, independent noncontiguous month navigation, built-in shortcut presets, controlled shortcut index, shortcut-change callbacks, and range time-of-day controls remain open for this row before stable acceptance. The field uses localized short date input rather than Blueprint formatter callbacks, natural-language parsing, or a web time-zone object. Two months appear at a wide viewport; narrow windows use one scrollable month. Manual TalkBack, large text, RTL, narrow phone, and physical input QA remain. After source and local gates pass, the next datetime branch should address the pinned timezone-select row; the project-wide inventory remains the authority for all other pending components.
