# M30: time picker and field

**Status:** implementation in progress on PR #39 (`joelromanpr/m30-time-picker`), restacked on main `1891213e3eafac34a4e9bb32d372e6018a366c6c` after the M55 next-icon merge. The pinned Blueprint 6.18.0 documentation has one public TimePicker row and no separate TimeInput row. `BraceTimeField` is the native companion API mapped within TimePicker. No Maven Central version has shipped; stable applicable coverage remains **0/122**.

## Scope

- Controlled `LocalTime` picker with locale-derived 12/24 hour mode, optional second/millisecond precision, inclusive overnight bounds, localized numeric units, 48 dp steppers, keyboard/IME and mouse/touch interactions, and TalkBack semantics.
- Validated `BraceTimeField` with localized text, anchored picker, saveable draft and popup state, clear and Done actions, external error text, and runtime Brace theme inheritance.
- Versioned platform-neutral time component tokens and generated Kotlin API, catalog states and copyable usage, Pages guide, pinned inventory evidence, an independent Maven consumer, and tests.

## Verification

| Gate | Result |
| --- | --- |
| Build, lint, unit tests, token and icon generation, API checks, catalog APK | **Passed** on the M30 + M55 tree at main `1891213e`: `build lint checkTokenGeneration checkBlueprintIconGeneration checkBlueprintNextIconGeneration checkInventory apiCheck :catalog:assembleDebug` (**745 actionable Gradle tasks**) on JDK 21 and SDK 36. Foundation, datetime, and next-icon API baselines passed. |
| API 36 Compose device suite | **Passed 30/30**, zero skipped or failed (**91 Gradle tasks**) on the combined M55 tree at 320×640/160 dpi: 17 M13 datetime tests and 13 M30 tests. The suite covers overnight bounds, direct number entry, keyboard, mouse and touch, AM/PM, precision, high-contrast compact 48 dp targets, native Android accessibility nodes, popup/selection/draft restoration, validation, and disabled states. |
| Generated coverage and GitHub Pages source | **Passed**: 148 pinned inventory rows, 44 generated guides, 12 real catalog captures, 12/24 documented web-specific mappings, and 0/122 released applicable rows. Token, coverage, legacy and next-icon generators, JavaScript syntax, and 2,313 local links across 45 Pages HTML files passed with zero missing paths. |
| Maven Local artifact metadata and independent consumer | **Passed** on the combined M55 tree: seven aligned foundation/core/icons/optional Blueprint legacy and next packs/select/datetime `0.1.0-SNAPSHOT` local publications (**271 tasks**), each with AAR, sources, KDoc, POM, and Gradle module metadata. The separate coordinate-only consumer assembled using both time APIs and the next-icon pack (**37 tasks**). |
| Visual and manual assistive technology review | A real 400×800 API 36 catalog image of the picker, field, and disabled state was captured and visually inspected. Automated Compose accessibility checks and native UIAutomation nodes passed. 320 dp, RTL/large-text visual inspection, TalkBack listening, and multiple physical devices remain acceptance work. |
| Hosted CI and review | The prior M54-based PR head `a290c82` passed [verify and API 34 instrumentation](https://github.com/joelromanpr/brace-android/actions/runs/36281541473) and [CodeQL Java/Kotlin](https://github.com/joelromanpr/brace-android/actions/runs/36281541494), including datetime **30/30** with zero skipped or failed. Hosted checks on the final M55-based head are pending; review and stable acceptance remain pending. |

## Known limits and next branch

`LocalTime` is a wall-clock time without a date, zone, or DST transition; apps must associate those values before creating an instant. Android's localized complete time pattern is required for manual entry, and the picker accepts only individual unit changes rather than a free-form natural-language phrase. The min/max range checks the full time even if seconds or milliseconds are hidden. Time-zone selection and date ranges remain separate planned pinned rows. The next datetime branch is `joelromanpr/m33-date-range`, covering DateRangePicker and DateRangeInput as a focused slice. No release or parity is claimed.
