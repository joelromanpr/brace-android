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
| Gradle build, lint and API check | Pending shared validation lane. |
| API 36 device interaction/accessibility checks | Pending shared validation lane. |
| Maven-local publication and separate consumer build | Pending shared validation lane. |
| Visual and manual TalkBack review | Pending. |

## Remaining work and limits

The table composes every row and has no viewport virtualization, cell-range selection, copy/export, editing, column resizing or frozen headers. Those are tracked under `brace-table`. A `cellContent` slot changes the visual cell but uses the supplied row string for its spoken value; it does not support nested controls. This row stays in progress until automated and manual acceptance passes and a real release is recorded. The next branch in this family should address remaining `brace-table` capabilities and verified simple-table interoperability.
