# M32 delivery slice: PanelStack

**Status:** source implementation in progress on `joelromanpr/m32-panel-stack`, based on main `47d2d38`. This branch has not been pushed or opened as a pull request. The pinned comparison is Blueprint `@blueprintjs/core@6.18.0` at `a60d4c92257612808fbfac81cfeee4fcba91a8b4`. The `core-panelstack` inventory row is **in progress**, with no first release and **0/121** applicable rows stable.

## Scope

- Root-first saveable state and a controlled stack, with unique destination IDs, panel titles, open/close actions, and an unclosable root.
- Native Android Back and keyboard Escape, a labeled previous-panel action, heading and pane semantics, focus movement after push/pop, a 48 dp back target, and an explicit host Back path at the root.
- Logical RTL push/pop transitions driven by Brace motion tokens, with no animated transition under reduced motion.
- Panel-specific platform-neutral color/metric tokens, generated Kotlin tokens, catalog states, an independent Maven-coordinate consumer example, Pages guide, and interaction/accessibility tests.

## Verification

| Gate | Result |
| --- | --- |
| Pinned source | Blueprint 6.18.0 PanelStack MDX, `panelStack.tsx`, `panelTypes.ts`, `panelView.tsx`, and SCSS inspected at the pinned commit. The public controlled/uncontrolled, root, header, callbacks, active-only, and transition behaviors informed the Compose API. |
| Inventory, tokens, and Pages | PASS locally: `generate_tokens.py --check`, `generate_coverage.py --check`, `node scripts/build-docs.mjs` (147 rows, 33 guides), JavaScript syntax, XML parse, conflict scan, and `git diff --check`. Static token color calculation found panel title/back text at least 9.06:1 across the four default palettes. Coverage remains generated from the pinned inventory; no stable row is claimed. |
| Core/Foundation API, build, lint, catalog | Pending shared Gradle lane. Kotlin API baseline and catalog compilation are not yet validated. |
| API 36 interaction and accessibility | Six Android tests authored for root/controlled/saveable state, Escape/system Back and host propagation, RTL/large text/high contrast/reduced motion, labeled native node bounds/action, and automated accessibility checks. Device execution pending. |
| Maven Local and independent consumer | Coordinate-only consumer example authored; publication and consumer compilation pending. |
| Visual, manual accessibility, hosted CI | 320 dp/light/dark visual review, manual TalkBack/physical keyboard/tablet, hosted required checks, human review, and release remain pending. |

## Adaptations and known limits

Blueprint's React renderers and injected props become a typed panel ID and `BracePanelScope` actions. `initialPanel` maps to `rememberBracePanelStackState`; Blueprint's controlled `stack` maps to the controlled overload. Blueprint's `renderActivePanelOnly = false` retains arbitrary React trees in the DOM. Brace composes the active panel and preserves `rememberSaveable` state for covered panels by default; callers hoist ordinary `remember` state and ongoing work when needed. HTML titles and CSS classes have no Android component. The header follows Android logical navigation and uses a localized TalkBack label. Push/pop motion mirrors in RTL and follows reduced-motion settings. The content host should provide bounded height for scrolling panes.

Current-head build/API/device/Maven/consumer and hosted CI results, visual review, manual accessibility, and release evidence are still required before any stable claim. The next proposed focused branch is `joelromanpr/m33-tree` for the pinned Tree and TreeNode rows, subject to maintainer allocation. Full Blueprint parity is not claimed.
