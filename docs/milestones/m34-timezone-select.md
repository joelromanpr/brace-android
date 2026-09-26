# M34 · time-zone selection

**Status:** local implementation in progress on `joelromanpr/m34-timezone-select`, based on M13 `ac121b3`. One pinned Blueprint 6.18.0 TimezoneSelect row has source, tokens, tests, catalog, docs, and independent consumer usage. It is neither released nor stable; applicable stable coverage remains **0/121**.

## Scope

- Controlled `BraceTimeZoneSelect(ZoneId?)` and typed display modes in `brace-datetime`, using Android's IANA time-zone database.
- `Instant`-based UTC offset and daylight/standard label, localized names, literal multiword search, local zone priority, lazy 48 dp options, keyboard search navigation, TalkBack selection semantics, saveable query/open state.
- Versioned `timeZoneSelect` component tokens and English, Spanish, French labels; runtime catalog states and copyable example; coordinate-only consumer call site.

## Verification

Static token/coverage/docs/resource/JS/diff checks pass locally: 147 inventory rows, 35 generated guides, and 0/121 stable coverage. Focused compile/JVM, exact API dump/check, API 36 instrumented tests, broad build/lint/catalog, Maven Local publication, and independent consumer are pending the shared Gradle/ADB lane. The test files are planned evidence, not a passing result. GitHub Actions have recently stopped before running steps due to account billing; no hosted check result exists for this branch.

## Known limits and next branch

Android zone database contents and localized names vary by OS. Blueprint's preset list, custom React child, and pass-through button/input/popover props are replaced by typed parameters and a native popover; a customizable Compose target/filter slot is still open for stable acceptance. Manual TalkBack listening, large text, RTL, narrow phone, keyboard, and mouse QA remain. After verification, review this single inventory row in a focused draft PR; follow-on work should address the remaining pinned datetime behaviors and project-wide inventory.
