# M30: time picker and field

**Status:** implementation in progress on draft `joelromanpr/m30-time-picker`, based on main `286d04e` after the M54 documentation merge. The pinned Blueprint 6.18.0 documentation has one public TimePicker row and no separate TimeInput row. `BraceTimeField` is the native companion API mapped within TimePicker. No Maven Central version has shipped; stable applicable coverage remains **0/121**.

## Scope

- Controlled `LocalTime` picker with locale-derived 12/24 hour mode, optional second/millisecond precision, inclusive overnight bounds, localized numeric units, 48 dp steppers, keyboard/IME and mouse/touch interactions, and TalkBack semantics.
- Validated `BraceTimeField` with localized text, anchored picker, saveable draft and popup state, clear and Done actions, external error text, and runtime Brace theme inheritance.
- Versioned platform-neutral time component tokens and generated Kotlin API, catalog states and copyable usage, Pages guide, pinned inventory evidence, an independent Maven consumer, and tests.

## Verification

| Gate | Result |
| --- | --- |
| Build, lint, unit tests, token and inventory generation, API checks, catalog APK, Maven Local metadata | **Passed** on M30 merged with main `286d04e`: `build lint checkTokenGeneration checkBlueprintIconGeneration checkInventory apiCheck :catalog:assembleDebug` and six aligned `publishToMavenLocal` tasks (**690 actionable Gradle tasks**) on JDK 21 and SDK 36. Foundation and datetime API baselines were regenerated and checked. |
| API 36 Compose device suite | **Passed 30/30**, zero skipped or failed (**91 Gradle tasks**): 17 M13 datetime tests and 13 M30 tests. The suite covers overnight bounds, direct number entry, keyboard, mouse and touch, AM/PM, precision, high-contrast compact 48 dp targets, native Android accessibility nodes, popup/selection/draft restoration, validation, and disabled states. M54 changed documentation and inventory after this device run; it did not change the datetime production code or these tests. |
| Generated coverage and GitHub Pages source | **Passed**: 147 pinned inventory rows, 43 generated guides, 12 real catalog captures, 12/24 documented web-specific mappings, and 0/121 released applicable rows. Pages JavaScript syntax, token generation, inventory links, and whitespace checks passed. |
| Maven Local artifact metadata and independent consumer | **Passed**: aligned foundation/core/icons/optional Blueprint icons/select/datetime `0.1.0-SNAPSHOT` local publications with AARs, sources, KDoc, POMs, and module metadata; the separate coordinate-only consumer assembled using both time APIs (**37 tasks**). The consumer gate found missing time imports after restack; they were restored and the gate reran successfully. |
| Visual and manual assistive technology review | A real 400×800 API 36 catalog image of the picker, field, and disabled state was captured and visually inspected. Automated Compose accessibility checks and native UIAutomation nodes passed. 320 dp, RTL/large-text visual inspection, TalkBack listening, and multiple physical devices remain acceptance work. |
| Hosted CI and review | **Pending** for the updated draft head. This report does not treat local gates as hosted CI evidence or mark the API stable. |

## Known limits and next branch

`LocalTime` is a wall-clock time without a date, zone, or DST transition; apps must associate those values before creating an instant. Android's localized complete time pattern is required for manual entry, and the picker accepts only individual unit changes rather than a free-form natural-language phrase. The min/max range checks the full time even if seconds or milliseconds are hidden. Time-zone selection and date ranges remain separate planned pinned rows. The next datetime branch is `joelromanpr/m33-date-range`, covering DateRangePicker and DateRangeInput as a focused slice. No release or parity is claimed.
