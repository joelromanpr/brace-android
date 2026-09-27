# M33 · date ranges

**Status:** local repair in progress on `joelromanpr/m33-date-range-m55`, rebuilt from two M33-only commits atop M55 main `1891213`; PR #43 still points to its older draft head. The pinned Blueprint 6.18.0 DateRangePicker and DateRangeInput rows are covered by source, samples, documentation, and tests. They are not marked stable, released, or merged. Applicable stable coverage remains **0/122**.

## Scope

- Controlled `BraceDateRange`, `BraceDateRangePicker`, and `BraceDateRangeField` APIs in `brace-datetime`.
- Localized partial endpoints, chronological selection, one-day policy, bounds, continuous disabled-interior validation, shortcut validation, two-month responsive calendar, keyboard and TalkBack semantics, 48 dp targets, saved visible month and field drafts.
- Versioned `dateRangePicker` and `dateRangeInput` tokens; English, Spanish, and French action/error strings; catalog demos and copyable examples; Maven consumer compile use.

## Verification

| Gate | Current-head result |
| --- | --- |
| Static generation and site | Passed token, coverage, legacy-icon, and next-icon generator checks, JavaScript syntax, and documentation build: **148 pinned rows, 44 guides, 11 real captures, 0/122 stable**. Checked **2,314 local links across 45 HTML pages**, zero missing. |
| Focused Kotlin, JVM, AndroidTest, and catalog compile | Passed (**192 actionable tasks**). `DateModelTest` **3/3** and `DateRangeModelTest` **8/8** passed; the new model tests reject disabled interior days and shortcut spans. |
| Public API | Regenerated the `brace-foundation` baseline in a separate `apiDump` invocation; `brace-datetime` baseline was already current. Both `apiCheck` tasks passed in the full gate. |
| API 36 instrumented interactions and accessibility | Full `brace-datetime` suite passed **30/30**, zero failed/skipped (**91 tasks**) on the 320×640/160 dpi API 36 emulator. Added disabled-interior calendar, shortcut, and field entry cases plus RTL keyboard and 2× font-scale target checks. Existing M13 picker/field regressions also passed. |
| Full build, lint, token and icon generation, inventory, API, catalog | Passed `build lint checkTokenGeneration checkBlueprintIconGeneration checkBlueprintNextIconGeneration checkInventory apiCheck :catalog:assembleDebug` (**745 actionable tasks**). |
| Maven Local and independent consumer | Published seven aligned foundation/core/icons/optional Blueprint legacy and next packs/select/datetime AARs with sources, KDoc, POMs, and module metadata (**271 tasks**). The separate coordinate-only consumer assembled using the range APIs (**37 tasks**). No Maven Central upload occurred. |
| 320 × 640 catalog visual/interaction smoke | The earlier draft head had light picker/field, shortcut, popover, and dark field inspected. The repaired head has automated 320 dp, RTL keyboard, and large-text target tests; a fresh visual capture and manual TalkBack listening remain. |
| Hosted CI and review | The repaired work is local and unpushed. PR #43 is a stale draft; hosted checks on this repaired head have not run. Restack on the next protected main after M30 and table integration remains before push. |

## Known limits and next branch

Every day of a complete selected or typed range is filtered by `isDateEnabled`; crossing a disabled interior day blocks the candidate or reports an error. Blueprint hover range preview, independent noncontiguous month navigation, built-in shortcut presets, controlled shortcut index, shortcut-change callbacks, and range time-of-day controls remain open for this row before stable acceptance. The field uses localized short date input rather than Blueprint formatter callbacks, natural-language parsing, or a web time-zone object. Two months appear at a wide viewport; narrow windows use one scrollable month. Manual TalkBack, large-text and RTL visual review, and physical input QA remain. After source and local gates pass, the next datetime branch should address the pinned timezone-select row; the project-wide inventory remains the authority for all other pending components.
