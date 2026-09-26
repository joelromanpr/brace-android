# M4 delivery slice: menus and modal overlays

**Status:** implementation in progress on `joelromanpr/m4-core-overlays`; review and hosted CI pending. These rows belong to the roadmap's **M2 overlays/navigation** phase. No Maven Central version has shipped, so released applicable coverage remains **0/121**. Across the full pinned ledger, 40 rows are in progress and 107 remain planned; none is stable.

## Prior milestone state

M1 was squash merged into `main` as `eed0a6644b417a4e46ac1845fa55c777828661df` after the required `verify` and API 34 `instrumented` checks passed on its final head. M2 was squash merged into `main` as `e74485eeac83d6d2c805eb5a7df4af2e55c53c57` after both required hosted checks passed. M3 was squash merged into `main` as `26d766bf75f77e491909a1021ed30e27bb3ef329` after both required hosted checks passed; its emulator job required a documented 4096M userdata setting to fit the runner disk. The first Pages workflow built the source documentation but `configure-pages` failed because this private repository has no accessible Pages site yet. This slice makes the build run while private and defers Pages setup/deployment until public visibility and site enablement. The repository stays private while owner visibility guidance is pending.

## Scope

- Static and anchored menus, items, and titled dividers with token-driven visual states and native focus behavior.
- Controlled modal overlays with a scoped stack host; dialogs, scrollable bodies, fixed action areas, and confirmation alerts.
- Explicit Android mappings for deprecated Overlay, DOM Portal/PortalProvider, OverlaysProvider, and useOverlayStack.
- KDoc, API baseline, interactive inventory-driven catalog, Pages guide, external Maven consumer, and pinned inventory evidence in the same PR.

## Verification

| Gate | Result |
| --- | --- |
| `:brace-core:apiDump` followed by `build lint checkTokenGeneration checkInventory apiCheck` | Passed locally: 287 tasks on the pinned toolchain using the temporary official-dependency mirror. |
| Full API 36 core Android suite | Passed: 60 tests, 0 failures, 1 skipped Back-key injection test. The focused Menu (7/7, including 180 dp RTL at 2× font scale), Dialog (7/7), and Overlay (6 passed, 1 skipped) runs also passed. A real `adb shell input keyevent 4` through the installed catalog opened and dismissed an Overlay2 dialog while the catalog detail remained. |
| Documentation source and inventory | `node scripts/build-docs.mjs` built 147 rows and 13 guides; JavaScript syntax, inventory generation, and diff checks passed. |
| Maven Local publication and independent consumer | Passed: foundation/core AAR, sources, and KDoc snapshot publication (core sources jar: 18 entries; KDoc jar: 248 entries); a separate app assembled against `io.github.joelromanpr.brace:brace-core:0.1.0-SNAPSHOT` using M4 menu, dialog, alert, and overlay APIs. |
| Hosted CI and PR | Pending focused PR creation. |

The skipped automated Back test is an input-injection harness gap; manual emulator behavior is recorded above. Manual TalkBack, mouse, and representative visual QA remain separate acceptance work.

## Limits and next branch

This slice remains in progress. Nested Blueprint submenus, nonmodal free-positioned overlays, and custom web transition/renderer hooks require follow-up parity work. Manual TalkBack, mouse, and representative light/dark/high-contrast review remain before stable status. The next focused branch is `joelromanpr/m5-drawers-popovers`, building on the shared overlay state and menu primitives. The long-term pinned inventory remains the full project scope.
