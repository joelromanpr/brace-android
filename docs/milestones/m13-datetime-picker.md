# M13: date picker and date input

**Status:** implementation in progress on draft PR #23 (`joelromanpr/m13-datetime-picker`), based on M12 squash merge `f457366` after the M11 icon merge. This delivery slice covers the pinned DatePicker and DateInput rows in roadmap phase M4. No Maven Central version has shipped; applicable stable coverage remains **0/121**.

## Scope

- New `brace-datetime` publication boundary with controlled `LocalDate` APIs, versioned component tokens, localized date parsing/formatting, bounds and disabled-day rules, keyboard and TalkBack semantics, calendar popover, saveable month and draft state.
- Interactive inventory-sourced catalog examples, a copyable usage example, documentation guide, independent Maven consumer usage, and CI/release wiring.

## Verification

| Gate | Result |
| --- | --- |
| Build, lint, unit tests, token, inventory, and API checks | Passed on the pre-M11 restack: `build lint checkTokenGeneration checkInventory apiCheck` (563 actionable Gradle tasks), including generated-token/inventory checks and regenerated foundation/datetime API baselines, on JDK 21 and SDK 36. |
| Focused API 36 Compose interaction/accessibility tests | Passed 14/14 on the earlier diff, with 0 failed/skipped, after narrow-viewport and keyboard-mode fixes. The pre-M11-restack diff passed **17/17** API 36 device tests with zero failures or skips, including a far-out-of-bounds initial month and real Android accessibility nodes for days, navigation, and the date-field calendar button. Three JVM date-model tests also passed in the broad gate. |
| Generated coverage and documentation site | Passed on the current stacked head: 147 pinned rows, 31 generated guides, and 0/121 released applicable rows. Pages JavaScript syntax and inventory guide links were checked. |
| Maven Local artifact metadata and independent consumer | Passed before the M11 restack: aligned foundation/core/icons/select/datetime Maven Local AARs, sources, KDoc JARs, POMs, and module metadata; the separate consumer compiled by coordinates only. Current stacked-head Gradle and consumer reruns are pending. |
| 320 dp catalog visual inspection | Passed on a 320 × 640 API 36 emulator: DatePicker and DateInput detail/sample screens had no overlapping controls. The calendar scrolls horizontally to retain 48 dp day targets; the seventh weekday may start offscreen and needs an intentional swipe. |
| Hosted CI and review | The M13 PR is retargeted to main after M12 merged. The current head includes precompiled datetime instrumentation APKs and the M11 AOSP emulator safeguards; hosted checks on the main-based head are pending. |

## Known limits and next branch

These two rows remain in progress until review and release. `BraceDateField` handles calendar days and localized short text; it does not expose the Blueprint ISO timestamp/time precision/time-zone selector as a separate date-only Android control. Time pickers, date ranges, and a time-zone selector retain their own planned inventory rows. Custom DayPicker modifiers and the built-in English shortcut presets are not implemented in this slice. The calendar can scroll horizontally below seven 48 dp targets, which should be checked visually on narrow phones. The next queued branch is `joelromanpr/m14-table-viewport`; a later focused datetime slice will address TimePicker and TimeInput.
