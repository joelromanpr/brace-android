# M32 delivery slice: PanelStack

**Status:** locally verified implementation in progress on `joelromanpr/m32-panel-stack`, based on main `47d2d38`. A draft pull request is pending. The pinned comparison is Blueprint `@blueprintjs/core@6.18.0` at `a60d4c92257612808fbfac81cfeee4fcba91a8b4`. The `core-panelstack` inventory row is **in progress**, with no first release and **0/121** applicable rows stable.

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
| Core/Foundation API, build, lint, catalog | PASS locally: exact API dumps and core/catalog compilation (138 tasks); full `build lint checkTokenGeneration checkInventory apiCheck :catalog:assembleDebug` (467 tasks). API baselines include the PanelStack API and generated foundation token types. |
| API 36 interaction and accessibility | PASS locally: focused `BracePanelStackTest` 6/6 on isolated `Brace_API36` emulator. It covers root/controlled/saveable state, Escape/system Back and host propagation, RTL/large text/high contrast/reduced motion, native same-node label/action/48 dp target, and automated accessibility checks. |
| Maven Local and independent consumer | PASS locally: foundation/core/icons/select published to Maven Local with source and Dokka documentation jars (156 tasks); inspected core POM foundation dependency and PanelStack source in source jar. Independent coordinate-only consumer `:app:assembleDebug` passed (37 tasks). |
| Visual, manual accessibility, hosted CI | PASS local visual smoke: 320 × 640 catalog detail, root/nested panel, and light/dark states show visible header, content, back action, and depth without overlap or clipping. Manual TalkBack/physical keyboard/tablet, hosted required checks, human review, and release remain pending. |

## Adaptations and known limits

Blueprint's React renderers and injected props become a typed panel ID and `BracePanelScope` actions. `initialPanel` maps to `rememberBracePanelStackState`; Blueprint's controlled `stack` maps to the controlled overload. Blueprint's `renderActivePanelOnly = false` retains arbitrary React trees in the DOM. Brace composes the active panel and preserves `rememberSaveable` state for covered panels by default; callers hoist ordinary `remember` state and ongoing work when needed. HTML titles and CSS classes have no Android component. The header follows Android logical navigation and uses a localized TalkBack label. Push/pop motion mirrors in RTL and follows reduced-motion settings. The content host should provide bounded height for scrolling panes.

The local Gradle checks used a temporary mirror of official Google Maven artifacts because direct dependency downloads stalled in this environment; no dependency or publication task was disabled. The API 36 emulator presented an Android compatibility warning that the native-input tests dismiss only when its exact platform text is present. API 34 hosted checks, manual TalkBack/physical keyboard/tablet review, maintainer review, and publication remain pending before any stable claim. The next concrete branch is `joelromanpr/m33-date-range` for the pinned DateRange row, followed by M34 TimezoneSelect. Full Blueprint parity is not claimed.
