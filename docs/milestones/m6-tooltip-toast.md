# M6 delivery slice: tooltips and toast notifications

**Status:** implementation in progress on `joelromanpr/m6-tooltip-toast`; review and hosted CI pending. These rows belong to roadmap phase **M2 overlays/navigation**. No Maven Central version has shipped. Released applicable coverage remains **0/121**; the full pinned ledger now has **46 in progress** and **101 planned** rows out of 147.

## Scope

- Brief noninteractive tooltips triggered by Android hover, long press, keyboard focus, and TalkBack action.
- Token-styled toasts with intent states, optional action and close control, live-region announcements, timed or manual dismissal, and accessible timeout extension.
- A bounded screen-scoped toast host and state holder with keyed replacement, eviction, logical placement, and focused Escape behavior; explicit mapping from Blueprint `OverlayToaster`.
- KDoc, pinned inventory evidence, an interactive catalog, documentation site source, a separate Maven consumer, API baseline, and interaction tests in the same focused slice.

## Verification

| Gate | Result |
| --- | --- |
| Build, lint, token, inventory, and API checks | Passed locally: 287 Gradle tasks on the pinned toolchain with the temporary official-dependency mirror. |
| API 36 device interaction tests | Passed: 94/95 full core tests, 0 failures, 1 previously documented native Back injection skip. Focused Toast 13/13 and Tooltip 8/8 passed. |
| Documentation build and generated inventory | Passed: 147 rows and 17 guides; generated coverage, JavaScript syntax, and diff checks passed. |
| Maven Local publication and independent consumer | Passed: foundation/core AAR, source and KDoc artifacts; separate consumer app assembled with Tooltip and Toast APIs. |
| Installed catalog interaction | Passed on API 36 phone emulator: Toast search/detail opened; persistent actionable danger toast displayed without clipping and action dismissed it. |
| Hosted CI and PR | Pending focused PR creation. |

## Limits and next branch

All three rows remain in progress pending review and release evidence. The toast host defaults to BottomEnd and three visible messages as deliberate Android adaptations; rapid messages can evict earlier ones before they are announced. Material 3 controls touch and pointer timing and tooltip animation; Brace handles keyboard focus and viewport placement, and manual TalkBack, pointer, theme, and form-factor review remains. The next focused branch will address the remaining core interaction rows, beginning with context menus and keyboard shortcuts. The pinned inventory remains the long-term scope.
