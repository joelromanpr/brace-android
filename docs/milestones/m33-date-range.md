# M33 · date ranges

**Status:** local implementation in progress on `joelromanpr/m33-date-range`, based on M13 `ac121b3`. The pinned Blueprint 6.18.0 DateRangePicker and DateRangeInput rows are covered by source, samples, documentation, and tests. They are not marked stable, released, or merged. Applicable stable coverage remains **0/121**.

## Scope

- Controlled `BraceDateRange`, `BraceDateRangePicker`, and `BraceDateRangeField` APIs in `brace-datetime`.
- Localized partial endpoints, chronological selection, one-day policy, bounds, disabled endpoints, shortcut validation, two-month responsive calendar, keyboard and TalkBack semantics, 48 dp targets, saved visible month and field drafts.
- Versioned `dateRangePicker` and `dateRangeInput` tokens; English, Spanish, and French action/error strings; catalog demos and copyable examples; Maven consumer compile use.

## Verification

Static source checks pass: token generation, coverage generation (147 rows, 0/121 stable), documentation site build (35 guides), JavaScript syntax, resource XML parsing, and `git diff --check`. The source has not yet entered the shared Gradle/ADB lane. Focused unit, API dump/check, Android instrumentation, broad build/lint/catalog, Maven Local publication, and independent consumer gates are pending. Do not infer a passing gate from the existence of test files. The draft PR and hosted checks are pending; GitHub Actions in this account have recently failed before starting jobs due to a billing gate.

## Known limits and next branch

Only range endpoints are filtered by `isDateEnabled`; a range can span interior disabled dates. Blueprint hover range preview, independent noncontiguous month navigation, built-in shortcut presets, controlled shortcut index, shortcut-change callbacks, and range time-of-day controls remain open for this row before stable acceptance. The field uses localized short date input rather than Blueprint formatter callbacks, natural-language parsing, or a web time-zone object. Two months appear at a wide viewport; narrow windows use one scrollable month. Manual TalkBack, large text, RTL, narrow phone, and physical input QA remain. After source and local gates pass, the next datetime branch should address the pinned timezone-select row; the project-wide inventory remains the authority for all other pending components.
