# M30: time picker and field

**Status:** implementation in progress on draft `joelromanpr/m30-time-picker`, stacked on M13 commit `ac121b3` (`joelromanpr/m13-datetime-picker`). The pinned Blueprint 6.18.0 documentation has one public TimePicker row and no separate TimeInput row. BraceTimeField is the native companion API mapped within TimePicker. No Maven Central version has shipped; stable applicable coverage remains **0/121**.

## Scope

- Controlled `LocalTime` picker with locale-derived 12/24 hour mode, optional second/millisecond precision, inclusive overnight bounds, localized numeric units, 48 dp steppers, keyboard/IME and mouse/touch interactions, and TalkBack semantics.
- Validated `BraceTimeField` with localized text, anchored picker, saveable draft and popup state, clear and Done actions, external error text, and runtime Brace theme inheritance.
- Versioned platform-neutral time component tokens and generated Kotlin API, catalog states and copyable usage, Pages guide, pinned inventory evidence, an independent Maven consumer, and tests.

## Verification

| Gate | Result |
| --- | --- |
| Build, lint, unit tests, token/inventory generation, API checks, catalog APK | **Passed** on the current M30 head: `build lint checkTokenGeneration checkInventory apiCheck :catalog:assembleDebug` (**563 actionable Gradle tasks**) on JDK 21 and SDK 36. Foundation and datetime API baselines were generated with `apiDump` in separate invocations and checked. |
| API 36 Compose device suite | **Passed 30/30**, with zero skipped or failed: 17 existing M13 datetime tests and 13 new M30 tests. Coverage includes overnight bounds, direct number entry, keyboard, mouse and touch, AM/PM, precision, high-contrast compact 48 dp targets, native Android accessibility nodes, popup/selection/draft restoration, validation, and disabled states. Four new JVM time-model tests passed in the broad gate. |
| Generated coverage and GitHub Pages source | **Passed**: 147 pinned inventory rows, 35 generated guides, and 0/121 released applicable rows. Pages JavaScript syntax, XML resources, token generation, inventory links, and whitespace checks passed. |
| Maven Local artifact metadata and independent consumer | **Passed**: aligned foundation/core/icons/select/datetime `0.1.0-SNAPSHOT` AARs, sources JARs, KDoc JARs, POMs, and module metadata (**193 tasks**); the separate coordinate-only consumer assembled (**37 tasks**) using both public time APIs. |
| 320 dp visual and manual assistive technology review | **Pending** on this slice. Automated Compose accessibility checks and native UIAutomation nodes passed, but TalkBack listening, RTL/large-text visual inspection, and multiple physical devices remain acceptance work. |
| Hosted CI and review | **Pending** for this head. New GitHub Actions jobs on this private repository have been blocked before any steps by the owner's account billing state; [run 36246400402](https://github.com/joelromanpr/brace-android/actions/runs/36246400402) records the message. The branch stays draft and unmerged while required hosted checks are unavailable. |

## Known limits and next branch

`LocalTime` is a wall-clock time without a date, zone, or DST transition; apps must associate those values before creating an instant. Android's localized complete time pattern is required for manual entry, and the picker accepts only individual unit changes rather than a free-form natural-language phrase. The min/max range checks the full time even if seconds or milliseconds are hidden. Time-zone selection and date ranges remain separate planned pinned rows. The next datetime branch is `joelromanpr/m33-date-range`, covering DateRangePicker and DateRangeInput as a focused slice. No release or parity is claimed.
