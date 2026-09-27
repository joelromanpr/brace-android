# M31 delivery slice: Tree and TreeNode

**Status:** local preflight with protected M16 main `dd8ab119805a8676fa72a055d0803f4923cf24a3` statically integrated. The focused compile, API, device, and Maven evidence below was collected on the preceding M22 main. [PR #41](https://github.com/joelromanpr/brace-android/pull/41) still points to the earlier draft branch; this replay has not been pushed or reviewed by hosted CI. The pinned Tree and TreeNode inventory rows remain **in progress**, with no first release version. Generated coverage is **0/122 applicable rows** and **0/94 components** stable.

## Scope

- Controlled multi-root `BraceTree` and stable-key `BraceTreeNode` in `brace-core`, with saveable `rememberBraceTreeState`.
- Lazy viewport, touch and mouse selection/caret expansion, keyboard navigation with RTL arrows, and one hierarchical accessibility node per visible row.
- Versioned Tree color and metric tokens, compact/comfortable 48 dp targets, interactive catalog sample with copyable usage, Pages guide, separate-consumer source, and eight focused device tests.
- Android adaptation of pinned Blueprint 6.18.0 commit `a60d4c92257612808fbfac81cfeee4fcba91a8b4` [Tree documentation](https://github.com/palantir/blueprint/blob/a60d4c92257612808fbfac81cfeee4fcba91a8b4/packages/core/src/components/tree/tree.mdx).

## Current local verification

| Gate | Result |
| --- | --- |
| Inventory, tokens, and Pages | On the M16 integration, generated inventory has 148 rows, 24 authentic Android captures, and 66 guides. Local Pages link check found 67 HTML pages and 9,204 references with none missing. Token and coverage generation, JavaScript syntax, and Pages build passed. The new [API 36 Tree capture](../site/showcase/tree-400.png) shows selected, disabled, and expandable nodes at 400 × 960; its manifest records source commit `7d7390e606c2c8703724e3b1d426bb47d037b1d6`, device settings, and copyable usage. |
| API baselines | `:brace-foundation:apiDump :brace-core:apiDump` passed, 21 actionable Gradle tasks. The generated foundation baseline includes the Tree tokens alongside current main. |
| Focused build and lint | On the M22 integration, core test/AndroidTest compile, core and catalog lint, catalog debug assembly, foundation/core API checks, token check, and inventory check passed, 479 actionable Gradle tasks. The integrated catalog initially exceeded Kotlin's JVM method size; extracting `TreeSample` to its own composable resolved it. `:brace-core:testDebugUnitTest` had no source tests to run. |
| API 36 device | On the M22 integration, focused `BraceTreeTest` passed **8/8**, zero skipped or failed, 71 actionable Gradle tasks on `Brace_API36(AVD)`. Cases exercise controlled state, touch/mouse, keyboard/RTL, disabled nodes, lazy viewport, saved state, large text/high contrast, accessibility hierarchy, and automated checks where supported. |
| Maven Local and independent consumer | All eight aligned artifacts published to Maven Local in 308 actionable tasks, each with AAR, sources jar, documentation jar, POM, and Gradle Module Metadata. The separate coordinate-only consumer compiled Kotlin offline with all 16 tasks executed, including Tree usage. |
| Hosted CI and review | Pending the final main integration and push of this replay to draft PR #41. Current local results are not hosted check results. |

## Limits and next step

The API uses stable keys and text labels instead of Blueprint DOM node paths, CSS classes, arbitrary React elements, and DOM element lookup. Secondary labels stack below the primary label for large text. Separate context-menu, double-click, and mouse enter/leave callbacks are not implemented. Manual TalkBack review and final hosted CI remain before considering stable status. Push the focused Tree branch for hosted CI and review; keep both rows in progress until the acceptance criteria are met.
