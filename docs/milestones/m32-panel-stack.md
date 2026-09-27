# M32 delivery slice: PanelStack

**Status:** local preflight with protected M31 Tree main `fee948c5823a28b9e23325345181aff19ca56ef7` integrated. The combined API dump, focused build, and six device tests below ran on this main. [PR #42](https://github.com/joelromanpr/brace-android/pull/42) still points to its earlier draft branch; this replay has not been pushed or reviewed by hosted CI. Later main integration and hosted review remain. The pinned `core-panelstack` row is **in progress**, with no first release version. Generated coverage is **0/122 applicable rows** and **0/94 components** stable.

## Scope

- Root-first saveable state and a controlled stack, with unique destination IDs, panel titles, open/close actions, and an unclosable root.
- Native Android Back and keyboard Escape, a labeled previous-panel action, heading and pane semantics, focus movement after push/pop, and a 48 dp back target.
- Logical RTL push/pop transitions driven by Brace motion tokens, with an instantaneous transition under reduced motion.
- Versioned PanelStack color and metric tokens, interactive catalog sample, copyable usage, Pages guide, independent-consumer source, and six focused device tests.
- Android adaptation of pinned Blueprint 6.18.0 commit `a60d4c92257612808fbfac81cfeee4fcba91a8b4` [PanelStack documentation](https://github.com/palantir/blueprint/blob/a60d4c92257612808fbfac81cfeee4fcba91a8b4/packages/core/src/components/panel-stack/panel-stack.mdx).

## Current local verification

| Gate | Result |
| --- | --- |
| Inventory, tokens, and Pages | On the Tree-main integration, generated inventory has 148 rows, 25 authentic Android captures including the [real PanelStack capture](../site/showcase/panel-stack-400.png), and 68 guides. Token and coverage generation, JavaScript syntax, XML parse, and Pages build passed; 69 HTML pages and local references have no missing targets. PanelStack remains in progress. |
| API baselines | Combined `:brace-foundation:apiDump :brace-core:apiDump :catalog:compileDebugKotlin` passed, 141 actionable Gradle tasks. The regenerated foundation baseline includes both PanelStack and Tree tokens alongside current main. |
| Focused build and lint | Foundation unit tests, core AndroidTest compile and lint, catalog debug assembly and lint, foundation/core API checks, token check, and inventory check passed, 479 actionable Gradle tasks on Tree main. The catalog PanelStack sample was extracted to a separate composable to stay under Kotlin's JVM method limit. |
| API 36 device | Focused `BracePanelStackTest` passed **6/6**, zero skipped or failed, 71 actionable Gradle tasks on `Brace_API36(AVD)` with Tree main integrated. Cases exercise root and controlled state, restoration, Back and Escape, RTL, large text, high contrast, reduced motion, native accessibility bounds and actions, and automated checks where supported. |
| Maven Local and independent consumer | All eight aligned `0.1.0-SNAPSHOT` artifacts published to Maven Local (**308 tasks**); each has an AAR, sources JAR, KDoc JAR, POM, and Gradle Module Metadata. The separate offline coordinate-only consumer assembled with **37/37 executed tasks**, including Kotlin compilation of PanelStack usage. No Maven Central upload occurred. |
| Hosted CI and review | Pending the final protected-main integration and push of this replay to draft PR #42. The old branch's billing-blocked run is historical and is not current code evidence. |

## Adaptations and limits

Blueprint React renderers and injected props become typed destination IDs and `BracePanelScope` actions. Brace composes the active panel and retains its `rememberSaveable` state while covered by default; callers hoist ordinary `remember` state and long-running work when it must continue. HTML titles and CSS classes have no Android component. The content host should provide bounded height for scrolling panes. Manual TalkBack, physical keyboard, and tablet review remain before stable status. PanelStack has no Tree source dependency; the combined tokens, catalog, documentation, API dump, and device suite are verified on Tree main. Next, integrate any subsequent protected-main merges, run scoped checks, and obtain hosted CI and review. Keep the row in progress until all acceptance criteria are met.
