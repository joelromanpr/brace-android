# M31 delivery slice: Tree and TreeNode

**Status:** locally verified source in draft [PR #41](https://github.com/joelromanpr/brace-android/pull/41) on `joelromanpr/m31-tree`, based on main `47d2d38`; hosted checks remain pending. The two pinned Blueprint rows are **in progress**. Released coverage remains **0/121** applicable rows and no release version is assigned.

## Scope

- Controlled multi-root `BraceTree` and stable-key `BraceTreeNode` in `brace-core`, with `rememberBraceTreeState` for restoration.
- Lazy viewport, touch and mouse selection/caret expansion, keyboard roving navigation with RTL arrows, and one hierarchical TalkBack node per visible item.
- Tokenized tree states, compact/comfortable 48 dp targets, catalog sample and copyable usage, Pages guide, Maven consumer source, and focused device tests.

## Verification

| Gate | Result |
| --- | --- |
| Pinned Blueprint comparison | Inspected local Blueprint 6.18.0 commit `a60d4c92257612808fbfac81cfeee4fcba91a8b4` Tree docs and source. |
| Token and inventory generation | Passed `python3 scripts/generate_tokens.py --check`, `python3 scripts/generate_coverage.py --check`, `node --check docs/site/app.js`, and `node scripts/build-docs.mjs` (147 rows, 33 guides, zero stable). |
| Kotlin compile, lint, API baseline and catalog | Passed exact Gradle `:brace-foundation:apiDump :brace-core:apiDump`, focused core AndroidTest compilation, and `:catalog:assembleDebug`. Passed broad `build lint checkTokenGeneration checkInventory apiCheck :catalog:assembleDebug` (task count not emitted by `--quiet`). Both generated API baselines are committed with this slice. |
| API 36 interaction and accessibility | Passed focused `BraceTreeTest` **8/8**, zero failures on `Brace_API36`, including controlled state, touch/mouse, keyboard/RTL, lazy viewport, save restoration, large text/high contrast, native accessibility node and API 34+ automated checks. The initial run exposed missing row test tags inside `clearAndSetSemantics`; after correction, native UIAutomation also needed interactive-window retrieval and dismissal of the emulator's older-target compatibility dialog. The final suite passed. |
| Maven Local and separate consumer | Passed publication of aligned `brace-foundation`, `brace-core`, `brace-icons`, and `brace-select` `0.1.0-SNAPSHOT` artifacts to Maven Local, then passed coordinate-only `verification/consumer-smoke :app:assembleDebug`. Task counts were not emitted by `--quiet`. |
| 320×640 catalog review | Passed light and dark high-contrast inspection on API 36: inventory search lists Tree and TreeNode as in progress, detail/usage and live sample fit, touch-selected Alpha changes label and highlight, and caret-expanded Gamma reveals Notes. Screenshots were inspected from the local emulator; no artifact is published. |
| Hosted CI, review and release | Draft [PR #41](https://github.com/joelromanpr/brace-android/pull/41) is open. Hosted Actions checks are blocked by repository billing and remain required before ready-for-review or merge. No publication or stable claim. |

## Limits and next branch

The current model intentionally uses stable keys and text labels rather than Blueprint's DOM node paths, CSS classes, arbitrary React labels, and DOM element lookup. Secondary labels stack beneath primary labels for large text rather than using Blueprint's right-aligned layout. Generic context-menu, double-click, and mouse enter/leave node callbacks are not implemented in this slice. Manual TalkBack remains required before a stable release. Hosted CI is currently blocked by repository Actions billing, so local verification does not imply a green PR check. The next concrete source branch is `joelromanpr/m35-blueprint-icon-pack` for an optional, licensed pinned icon glyph pack; the remaining pinned rows keep their individual inventory statuses.
