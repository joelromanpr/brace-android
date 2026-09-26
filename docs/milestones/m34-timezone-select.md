# M34 · time-zone selection

**Status:** local implementation in progress on `joelromanpr/m34-timezone-select`, based on M13 `ac121b3`. One pinned Blueprint 6.18.0 TimezoneSelect row has source, tokens, tests, catalog, docs, and independent consumer usage. It is neither released nor stable; applicable stable coverage remains **0/121**.

## Scope

- Controlled `BraceTimeZoneSelect(ZoneId?)` and typed display modes in `brace-datetime`, using Android's IANA time-zone database.
- `Instant`-based UTC offset and daylight/standard label, localized names, literal multiword search, local zone priority, lazy 48 dp options, keyboard search navigation, TalkBack selection semantics, saveable query/open state.
- Versioned `timeZoneSelect` component tokens and English, Spanish, French labels; runtime catalog states and copyable example; coordinate-only consumer call site.

## Verification

| Gate | Current-head result |
| --- | --- |
| Static generation and site | Token and coverage checks, JavaScript syntax, datetime resource XML parsing, documentation build, and `git diff --check` passed: **147 pinned rows, 35 guides, 0/121 stable**. |
| Focused Kotlin, JVM, AndroidTest, and catalog compile | Passed after correcting an invalid Android test import and splitting a chained test input/key action. `DateModelTest` **3/3** and `TimeZoneModelTest` **4/4** passed; focused Gradle run **102 actionable tasks**. |
| Public API | Regenerated exact `brace-foundation` and `brace-datetime` baselines in a separate `apiDump` invocation; both `apiCheck` tasks passed in the broad gate. |
| API 36 instrumented interactions and accessibility | Full `brace-datetime` suite passed **23/23**, 0 failed/skipped on `emulator-5556`: M13 picker/field regressions plus M34 touch selection, keyboard Enter, daylight offset changes, state restoration, disabled trigger, high-contrast 48 dp target, and supported automated accessibility checks. |
| Full build, lint, token, inventory, API, catalog | Passed `build lint checkTokenGeneration checkInventory apiCheck :catalog:assembleDebug --no-parallel` (**563 actionable tasks**). |
| Maven Local and independent consumer | Published aligned foundation/core/icons/select/datetime AARs, sources, KDoc JARs, POMs, and module metadata locally (**193 tasks**). The separate coordinate-only consumer assembled (**37 tasks**). No Maven Central upload occurred. |
| 320 × 640 catalog visual/interaction smoke | Inspected light and dark high-contrast states, opened the popover with the IME, filtered `Honolulu`, and touched the `Pacific/Honolulu` row. The catalog updated its controlled IANA ID; rows/search stayed visible above the keyboard without overlap. Long selected trigger text ellipsizes at this width while its full ID/offset remains in semantics and the catalog's value readout. |
| Hosted CI and review | Pending draft PR. Recent GitHub Actions jobs in this account have stopped before executing steps because of a billing gate; no hosted check result exists for this branch yet. |

## Known limits and next branch

Android zone database contents and localized names vary by OS. Blueprint's preset list, custom React child, and pass-through button/input/popover props are replaced by typed parameters and a native popover; a customizable Compose target/filter slot is still open for stable acceptance. Manual TalkBack listening, large text, RTL, physical keyboard, and mouse QA remain; the 320 × 640 emulator narrow-phone visual smoke has passed. After verification, review this single inventory row in a focused draft PR; follow-on work should address the remaining pinned datetime behaviors and project-wide inventory.
