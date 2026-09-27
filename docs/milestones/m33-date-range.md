# M33 · date ranges

**Status:** draft source on `joelromanpr/m33-date-range-m55`, integrated locally with protected main `217b1b9f253bca79dcd57ea5d9071fbc9724e2a6` (M57 icon large-text, following M18 table selection) at merge `6a7acbf`. PR #43 still points to the fully hosted-green pre-M18 head until this exact-main repair is pushed. The pinned Blueprint 6.18.0 DateRangePicker and DateRangeInput rows have source, samples, documentation, and tests; both remain **in progress**. Released applicable coverage is **0/122**.

## Scope

- Controlled `BraceDateRange`, `BraceDateRangePicker`, and `BraceDateRangeField` APIs in `brace-datetime`.
- Localized partial endpoints, chronological selection, one-day policy, bounds, continuous disabled-interior validation, shortcut validation, responsive one/two-month calendar, keyboard and TalkBack semantics, 48 dp targets, saved visible month and field drafts.
- Versioned `dateRangePicker` and `dateRangeInput` tokens; English, Spanish, and French action/error strings; catalog demos and copyable examples; Maven consumer compile use.
- A real 400 × 800 API 36 light-mode catalog capture with a selected September 14–18 range, supplied shortcut, and clear action in the public gallery manifest.

## Verification

| Gate | Evidence |
| --- | --- |
| Current M18/M57-integrated source and Pages | Token/coverage generators, JavaScript syntax, and documentation build passed: **148 inventory rows, 15 real captures, 53 guides, 0/122 stable**. Checked **3,269 local paths across 54 HTML pages**, zero missing. M20 Links, M14/M18 table, M30 time picker, M57 icon large-text catalog logic, both range sample-code entries, and the recovered TimePicker sample-code entry are present in the combined catalog. A focused current-head catalog APK, catalog AndroidTest compile, foundation/datetime/table API, and datetime JVM check passed **253 Gradle tasks** (43 executed). |
| M14/M30-integrated source before M20 | Focused Kotlin/JVM/AndroidTest/catalog compile passed (**212 tasks**). `DateModelTest` **3/3**, `DateRangeModelTest` **8/8**, and `TimeModelTest` **4/4** passed. Foundation and datetime API baselines regenerated in a separate **30-task** `apiDump` gate. Full build, lint, token/icon generation, inventory, API, and catalog passed **835 tasks**. |
| API 36 instrumented interactions and accessibility before M20 | Full `brace-datetime` suite passed **43/43**, zero failed/skipped (**91 tasks**) on the 320 × 640/160 dpi API 36 emulator. It covers the disabled-interior calendar, shortcut, and field cases, plus RTL keyboard and 2× font-scale target checks, alongside the existing date/time regressions. |
| Maven Local and independent consumer before M20 | Published eight aligned foundation/core/icons/optional Blueprint legacy and next packs/select/datetime/table AARs with sources, KDoc, POMs, and module metadata (**308 tasks**). The separate coordinate-only consumer assembled with range APIs (**37 tasks**). No Maven Central upload occurred. |
| Visual review | A selected DateRangePicker gallery capture was inspected at its full 400 × 800 resolution. The catalog was also used to select a complete range through the DateRangeField popover. The shared emulator was restored to 320 × 640/160 dpi/font scale 1.0 after capture. Manual TalkBack listening remains. |
| Hosted CI and review | The pre-M18 PR head `491ec79` passed hosted verify (4m00), CodeQL (3m35), and API 34 instrumentation (7m18). The M18/M57-integrated source is local and unpushed at this report revision. The final squash-main merge changed no file content from the locally verified M57-branch integration; final hosted checks on the pushed exact-main head have not run. |

## Known limits and next branch

Every day of a complete selected or typed range is filtered by `isDateEnabled`; crossing a disabled interior day blocks the candidate or reports an error. Blueprint hover range preview, independent noncontiguous month navigation, built-in shortcut presets, controlled shortcut index, shortcut-change callbacks, and range time-of-day controls remain open for these rows before stable acceptance. The field uses localized short date input rather than Blueprint formatter callbacks, natural-language parsing, or a web time-zone object. Two months appear at a wide viewport; narrow windows use one scrollable month. Manual TalkBack, large-text and RTL visual review, and physical input QA remain. After this slice, the next datetime branch should address the pinned time-zone-select row; the project-wide inventory remains the authority for all other pending components.
