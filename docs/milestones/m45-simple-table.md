# M45 — Simple table

## Shipped in the topic branch

- `BraceSimpleTable`, `BraceSimpleTableColumn` and `BraceSimpleTableRow` in `brace-core` adapt the pinned Blueprint 6.18.0 `HTMLTable` skin for small, fully composed tables.
- Semantic and table-token styling for light, dark, high contrast, brand overrides and compact or comfortable density; bordered, striped, hover, selected, disabled and keyboard-focus states.
- Labeled collection/cell semantics, full spoken values, logical RTL arrow navigation and optional row activation by touch, mouse, Enter and Space.
- Live catalog and copyable snippet, independent Maven consumer example, documentation and device tests. The pinned inventory row is **in progress** with source links and no first release.

## Verification

| Gate | Evidence |
| --- | --- |
| Pinned scope | Blueprint `@blueprintjs/core@6.18.0`, commit `a60d4c92257612808fbfac81cfeee4fcba91a8b4`; `HTMLTable` is a CSS-only skin distinct from `@blueprintjs/table`. |
| Inventory, token, Pages and JavaScript static checks | Passed: 147 inventory rows, 0/121 applicable stable, 0/94 component stable; tokens current; 33 Pages guides built; JavaScript syntax and diff whitespace checks passed. |
| Gradle build, lint and API check | Passed locally: `./gradlew build lint checkTokenGeneration checkInventory apiCheck`; the `brace-core` API snapshot is committed with this slice. |
| API 36 device interaction/accessibility checks | Passed locally: 4/4 focused `BraceSimpleTableTest` cases on `emulator-5556`, including touch, mouse, Enter/Space, logical RTL arrows, disabled and selected states, 48 dp cells, and native UIAutomation verification of one named `Open row` click action on the same cell node. |
| Maven-local publication and separate consumer build | Passed locally: foundation/core/icons/select published to Maven Local; `verification/consumer-smoke :app:assembleDebug` consumed the published artifacts. |
| Compact visual review | Inspected catalog at 320 × 640 in light, dark high contrast, and 2× font scale. Table rows grow and wrap at 2×; all three sample columns and action cells remained visible in the reviewed viewport. Emulator font scale restored to 1.0. Manual TalkBack review remains pending. |

## Remaining work and limits

The table composes every row and has no viewport virtualization, cell-range selection, copy/export, editing, column resizing or frozen headers. Those are tracked under `brace-table`. A `cellContent` slot changes the visual cell but uses the supplied row string for its spoken value; it does not support nested controls. This row stays in progress until manual TalkBack acceptance, hosted checks and a real release are recorded. The next concrete branch is `joelromanpr/m52-table-integration`, which validates the small-table and larger `brace-table` surfaces together.
