# M34 · time-zone selection

**Status:** draft source for [PR #44](https://github.com/joelromanpr/brace-android/pull/44), integrated with protected main `47989fc0f56652bee74223e08760907eeb6d9107` after the M33 date-range and M59 visual-catalog merges. The pinned Blueprint 6.18.0 TimezoneSelect row has a native API, tokens, tests, catalog states, documentation, and an independent consumer call site. It remains **in progress**, unreleased, and outside stable coverage (**0/122** applicable rows).

## Scope

- Controlled `BraceTimeZoneSelect(ZoneId?)` and typed trigger display modes in `brace-datetime`, using Android's IANA time-zone database.
- `Instant`-based UTC offsets and daylight/standard names, localized literal-word search, optional local-zone priority, lazy 48 dp rows, keyboard and TalkBack semantics, and saveable open/query state.
- Versioned `timeZoneSelect` tokens; English, Spanish, and French labels; runtime catalog controls; copyable usage; Maven consumer compilation.
- Two real 400 × 800 API 36 catalog captures: the filtered `Asia/Tokyo` popup and selected trigger/disabled state, recorded in the Pages gallery manifest.
- Corrected the M33 report to record its final hosted-green squash merge.

## Verification

| Gate | Result and scope |
| --- | --- |
| Source, tokens, inventory, and Pages | Token and coverage generators, datetime resource XML, JavaScript syntax, and documentation build passed: **148 inventory rows, 23 real captures, 56 guides, 0/122 stable**. Checked **6,802 local references across 57 HTML pages**, zero missing. |
| Public API | Regenerated `brace-foundation` and `brace-datetime` baselines in a separate **30-task** `apiDump` run; both `apiCheck` tasks passed in the focused gate. |
| Focused build, lint, catalog, and JVM tests before the M59 site merge | **426 Gradle tasks passed**, including datetime and catalog lint, catalog APK, Android test compilation, token/inventory checks, and datetime unit tests: DateModel **3/3**, DateRangeModel **8/8**, TimeModel **4/4**, TimeZoneModel **4/4**. |
| API 36 device interaction and accessibility before the M59 site merge | Focused TimeZoneSelect suite **7/7**, zero failures/skips (**91 tasks**): touch search, Enter, DST offset, saved-state restoration, disabled trigger, high-contrast targets, and RTL/2× text/mouse interaction with supported accessibility checks. |
| Maven Local and external consumer before the M59 site merge | Eight aligned foundation/core/icons/optional Blueprint legacy and next packs/select/datetime/table AARs, sources, KDoc, POMs, and module metadata published locally (**308 tasks**). The separate offline, coordinate-only consumer assembled (**37 tasks**, Kotlin compilation executed). No Maven Central upload occurred. |
| Real-device-size visual inspection | Inspected the 400 × 800 light-mode catalog's filtered popup and selected state at full resolution. The popup exposes `Asia/Tokyo`, UTC+09:00, and the localized name above the IME; the selected trigger and disabled field remain visible. Restored shared emulator to 320 × 640/160 dpi/font scale 1.0. |
| Hosted CI and review | Pending final draft PR checks on the M59-integrated source. The current source, inventory, captures, and Pages build passed static validation; the focused Gradle/device/consumer gates above ran before M59 changed catalog and site files. |

## Known limits and next work

Android zone database contents and localized names vary by OS. Blueprint's preset list, custom React child, and pass-through button/input/popover props are adapted to typed parameters and a native popover. Custom Compose target and filter slots remain open before stable acceptance. Automated RTL, large-text, mouse, and accessibility checks pass; manual TalkBack listening, physical keyboard, and device QA remain. Continue with the pinned datetime inventory and keep this row in progress until its remaining acceptance work and a release are verified.
