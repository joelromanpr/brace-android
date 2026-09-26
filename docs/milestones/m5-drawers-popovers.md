# M5 delivery slice: drawers and anchored popovers

**Status:** implementation in progress on `joelromanpr/m5-drawers-popovers`; stacked source branch awaiting M4 review and hosted CI. These rows belong to the roadmap's **M2 overlays/navigation** phase. No Maven Central version has shipped. Released applicable coverage remains **0/121**; 43 rows are in progress and 104 planned in the full 147-row pinned ledger.

## Scope

- A controlled modal drawer on logical Start/End or Top/Bottom edges, with adaptive sizes, a scrollable body, a fixed footer, and overlay-stack dismissal ordering.
- A controlled anchored Compose popover with automatic and explicit logical placement, flip/clamp at window edges, keyboard dismissal, target focus return, and overlay-stack ordering.
- Versioned platform-neutral drawer/popover component tokens and regenerated type-safe Kotlin values.
- Explicit mapping from Blueprint's deprecated Popover to the same Android API as PopoverNext.
- Pinned inventory, API baseline, catalog examples, documentation, independent Maven consumer, and device tests in the same focused PR.

## Verification

| Gate | Result |
| --- | --- |
| Popover Kotlin and device interaction tests | Passed: 4/4 focused API 36 tests, including controlled state, Escape/focus return, overlay order, scoped RTL inside the popup window, and LTR/RTL geometry. |
| Drawer Kotlin and device interaction tests | Passed: 8/8 focused API 36 tests, including all four edges, scoped RTL, outside-dismiss flags, nested overlay order, large text with a fixed footer, tiny custom-extent target visibility, keyboard close, and automated accessibility. |
| Build, lint, token, inventory, and API checks | Passed locally: 287 Gradle tasks on the pinned toolchain using the temporary official-dependency mirror. |
| Documentation source and generated inventory | Passed: 147 rows and 15 guides built; token and coverage generation checks, JavaScript syntax, and diff checks passed. |
| Maven Local publication and independent consumer | Passed: foundation/core AAR, sources, and KDoc snapshot artifacts; a separate app assembled from Maven coordinates using M5 Drawer and Popover APIs. |
| Hosted CI and PR | Pending prior milestone merges. |

The final full API 36 core Android suite passed **73/74** tests with no failures. A focused drawer/popover/overlay rerun after the RTL fixes passed 18/19, with the same one skipped case. The one skipped case is the previously documented native Back-key input-injection harness gap; an installed-catalog Back action was verified manually in M4.

## Limits and next branch

These APIs remain in progress. Persistent split-pane drawers, Blueprint-specific web middleware and arrow geometry, nonmodal free-positioned overlays, custom web transitions, and hover-triggered tooltip behavior remain planned or require follow-up parity work. Manual TalkBack, pointer, representative visual, and form-factor review remain before stable status. The next focused branch should complete the remaining core overlay feedback surfaces, starting with `joelromanpr/m6-tooltip-toast`. The full pinned inventory remains the long-term scope.
