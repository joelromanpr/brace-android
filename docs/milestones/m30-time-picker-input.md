# M30: time picker and field

**Status:** implementation in progress on PR #39 (`joelromanpr/m30-time-picker`), restacked on main `eda2cf60382992fbf649bfaa91410712ad8ffeb1` after the M14 table viewport merge. The pinned Blueprint 6.18.0 documentation has one public TimePicker row and no separate TimeInput row. `BraceTimeField` is the native companion API mapped within TimePicker. No Maven Central version has shipped; stable applicable coverage remains **0/122**.

## Scope

- Controlled `LocalTime` picker with locale-derived 12/24 hour mode, optional second/millisecond precision, inclusive overnight bounds, localized numeric units, 48 dp steppers, keyboard/IME and mouse/touch interactions, and TalkBack semantics.
- Validated `BraceTimeField` with localized text, anchored picker, saveable draft and popup state, clear and Done actions, external error text, and runtime Brace theme inheritance.
- Versioned platform-neutral time component tokens and generated Kotlin API, catalog states and copyable usage, Pages guide, pinned inventory evidence, an independent Maven consumer, and tests.

## Verification

| Gate | Result |
| --- | --- |
| Build, lint, unit tests, token and icon generation, API checks, catalog APK | **Passed** on the M30 + M14 table tree at main `eda2cf6`: `build lint checkTokenGeneration checkBlueprintIconGeneration checkBlueprintNextIconGeneration checkInventory apiCheck :catalog:assembleDebug` (**835 actionable Gradle tasks**) on JDK 21 and SDK 36. All eight artifact API baselines passed. The catalog merge initially exceeded the JVM method-size limit; extracting time and table samples into separate Compose functions fixed debug and release compilation. |
| API 36 Compose device suite | **Passed 46/46**, zero skipped or failed (**142 Gradle tasks**) on the combined M14 tree at 320×640/160 dpi: datetime **30/30** (17 M13 and 13 M30 tests) and table **16/16**. Datetime cases cover overnight bounds, direct number entry, keyboard, mouse and touch, AM/PM, precision, high-contrast compact 48 dp targets, native Android accessibility nodes, popup/selection/draft restoration, validation, and disabled states. |
| Generated coverage and GitHub Pages source | **Passed**: 148 pinned inventory rows, 46 generated guides, 13 real catalog captures, 12/24 documented web-specific mappings, and 0/122 released applicable rows. Token, coverage, legacy and next-icon generators, JavaScript syntax, and 2,509 local links across 47 Pages HTML files passed with zero missing paths. Both the time and table catalog captures remain in the merged manifest. |
| Maven Local artifact metadata and independent consumer | **Passed** on the combined M14 tree: eight aligned foundation/core/icons/optional Blueprint legacy and next packs/select/datetime/table `0.1.0-SNAPSHOT` local publications (**308 tasks**), each with AAR, sources, KDoc, POM, and Gradle module metadata. The separate coordinate-only consumer assembled using both time APIs and the table (**37 tasks**). |
| Visual and manual assistive technology review | A real 400×800 API 36 catalog image of the picker, field, and disabled state was captured and visually inspected. Automated Compose accessibility checks and native UIAutomation nodes passed. 320 dp, RTL/large-text visual inspection, TalkBack listening, and multiple physical devices remain acceptance work. |
| Hosted CI and review | Previous M55-based report head `7d85015` passed [verify](https://github.com/joelromanpr/brace-android/actions/runs/36282967147/job/108518226025) (3m51s), [API 34 instrumentation](https://github.com/joelromanpr/brace-android/actions/runs/36282967147/job/108518226101) (7m53s), and [CodeQL Java/Kotlin](https://github.com/joelromanpr/brace-android/actions/runs/36282967154/job/108518226026) (2m59s). Earlier source-head datetime completed **30/30**, zero skipped or failed. Hosted checks on the M14 table-restacked head are pending; review and stable acceptance remain pending. |

## Known limits and next branch

`LocalTime` is a wall-clock time without a date, zone, or DST transition; apps must associate those values before creating an instant. Android's localized complete time pattern is required for manual entry, and the picker accepts only individual unit changes rather than a free-form natural-language phrase. The min/max range checks the full time even if seconds or milliseconds are hidden. Time-zone selection and date ranges remain separate planned pinned rows. The next datetime branch is `joelromanpr/m33-date-range`, covering DateRangePicker and DateRangeInput as a focused slice. No release or parity is claimed.
