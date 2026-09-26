# M13: date picker and date input

**Status:** implementation in progress on PR #23 (`joelromanpr/m13-datetime-picker`), integrated locally with main `a88429c` (M19 radio and segmented choices) and the live Android showcase. Local gates pass; hosted checks on this integrated head are pending. This slice covers the pinned DatePicker and DateInput rows in roadmap phase M4. No Maven Central version has shipped; applicable stable coverage remains **0/121**.

## Scope

- New `brace-datetime` publication boundary with controlled `LocalDate` APIs, versioned component tokens, localized date parsing/formatting, bounds and disabled-day rules, keyboard and TalkBack semantics, calendar popover, saveable month and draft state.
- Interactive inventory-sourced catalog examples, a copyable usage example, documentation guide, independent Maven consumer usage, and CI/release wiring.

## Verification

| Gate | Result |
| --- | --- |
| Build, lint, unit tests, token, inventory, and API checks | On the integrated head, `:brace-foundation:testDebugUnitTest :brace-datetime:testDebugUnitTest :brace-datetime:lintDebug :brace-foundation:apiCheck :brace-core:apiCheck :brace-datetime:apiCheck :catalog:assembleDebug checkTokenGeneration checkInventory` passed (**246 actionable Gradle tasks**) on JDK 21 and SDK 36. `apiDump` regenerated the foundation baseline after composing date and radio/segmented token constructors. An earlier implementation head passed the full `build lint checkTokenGeneration checkInventory apiCheck :catalog:assembleDebug` gate (**563 tasks**). |
| Focused API 36 Compose interaction/accessibility tests | Passed **17/17** on the integrated head, zero failures/skips. These exercise bounds, changed controlled values, keyboard and mouse selection, RTL arrow movement, saveable state, and real Android accessibility nodes for days, navigation, and the date-field calendar button. The three JVM date-model tests passed in the focused Gradle gate. |
| Generated coverage and documentation site | Passed on the integrated head: 147 pinned rows, 9 real Android captures, **36** generated guides, and 0/121 released applicable rows. JavaScript syntax passed, and a local link check found zero missing targets across 37 HTML pages. |
| Maven Local artifact metadata and independent consumer | Passed on the integrated head: aligned foundation/core/icons/select/datetime Maven Local AARs, sources, KDoc JARs, POMs, and module metadata (**193 tasks**), followed by separate coordinate-only consumer assembly (**37 tasks**). |
| 320 dp catalog visual inspection | Passed on a 320 × 640 API 36 emulator: DatePicker and DateInput detail/sample screens had no overlapping controls. The calendar scrolls horizontally to retain 48 dp day targets; the seventh weekday may start offscreen and needs an intentional swipe. |
| Hosted CI and review | The prior showcase-based head `7e47c48` passed [hosted verify and API 34 instrumentation](https://github.com/joelromanpr/brace-android/actions/runs/36276380430) plus [CodeQL](https://github.com/joelromanpr/brace-android/actions/runs/36276380410). Fresh hosted checks on the M19-integrated head are pending. Review and release remain; no artifact has been published to Maven Central. |

## Known limits and next branch

These two rows remain in progress until review and release. `BraceDateField` handles calendar days and localized short text; it does not expose the Blueprint ISO timestamp/time precision/time-zone selector as a separate date-only Android control. Time pickers, date ranges, and a time-zone selector retain their own planned inventory rows. Custom DayPicker modifiers and the built-in English shortcut presets are not implemented in this slice. The calendar can scroll horizontally below seven 48 dp targets, which should be checked visually on narrow phones. The next queued branch is `joelromanpr/m14-table-viewport`; a later focused datetime slice will address TimePicker and TimeInput.
