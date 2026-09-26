# M48: table cell and header primitives

Status: **in progress**, unreleased. Branch: `joelromanpr/m48-table-cells`, stacked on M26 `ceba218`. Blueprint authority: Table 6.2.4 at `a60d4c92257612808fbfac81cfeee4fcba91a8b4`.

## Delivered in this branch

- Public `BraceTableCell`, `BraceColumnHeader`, and `BraceRowHeader` now render the actual table nodes, using Brace component/semantic tokens and one TalkBack node per cell or header.
- `BraceTableColumn.headerContent` and `BraceDataTable.rowHeaderContent` add visual header customization alongside existing `cellContent`; the complete accessible value remains `cellText`, `title`, or `rowLabel`.
- Touch, mouse, range/edit actions, table keyboard focus, fixed headers, RTL movement, large text sizing, and selection are retained by the existing table behavior.
- Catalog live examples and copyable snippets, documentation/Pages route, independent Maven consumer example, and focused Android tests are updated with the inventory rows.

## Validation

Static token, coverage, Pages, JavaScript syntax, and diff checks are run before handoff. Gradle compilation, API dump/check, instrumented accessibility and interaction tests, and separate Maven consumer compilation are pending the shared device/Gradle lane. No artifact is published.

## Remaining

Cell loading, native tooltip, wrapped/multiline content, interactive descendants, rich formats, header menus, reordering, custom child layouts, and header loading states remain. They must be implemented or explicitly resolved before these inventory rows can be marked stable. The next table branch should target one of those behaviors after M48 device validation; the parallel M46 branch owns sorting.
