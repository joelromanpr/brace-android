# Table accessibility

The M56 source slice adapts [Blueprint Table features at the pinned commit](https://github.com/palantir/blueprint/blob/a60d4c92257612808fbfac81cfeee4fcba91a8b4/packages/table/src/docs/table-features.mdx) to Android's accessibility model. The public surface is the existing `BraceDataTable` and its cell and header composables; there is no separate `BraceTableSemantics` object. This slice is **in progress** and unpublished.

```kotlin
var selection by rememberBraceTableSelection()
val viewport = rememberBraceTableViewport()
BraceDataTable(
    rows = records,
    rowKey = { it.id },
    rowLabel = { it.name },
    columns = columns,
    selection = selection,
    onSelectionChange = { selection = it },
    viewport = viewport,
    frozenRows = 1,
    frozenColumns = 1,
    label = "Cases",
)
```

The table exposes its full row and column counts as one Compose collection. The corner, column headers, row headers, and composed cells expose zero-based grid coordinates; spoken row and column numbers are one-based. Column and row headers are native accessibility headings. Each cell speaks its column title, row number and label, and full `cellText` value, even when a custom visual cell is ellipsized. The current keyboard cell announces **Active cell**. A pinned active cell combines that state with **Frozen row**, **Frozen column**, or **Frozen row and column**. The selected flag remains independent of focus, so a selected range does not imply every cell owns keyboard focus.

Frozen rows and columns are drawn in separate clipped panes. Brace gives their visible nodes a logical row-major traversal order, including the corner, headers, separate sort and resize controls, and cells. The order follows logical column positions in RTL, with the first column at the right edge. Scroll containers relinquish their automatic traversal grouping to the table's single group, following Android's [Compose traversal guidance](https://developer.android.com/develop/ui/compose/accessibility/traversal). Virtualized offscreen rows and columns are not separate accessibility nodes until composed. Touch, mouse, keyboard, and TalkBack actions use the same controlled selection and callback state; the table does not change caller-owned row order or values.

A disabled sort control announces disabled and offers no click. A resize handle offers Increase only below its maximum and Decrease only above its minimum. The visible grip keeps its 48 dp or larger target; pointer drag and keyboard arrows still clamp at the configured bounds. Row and column headers keep selection distinct from sort, edit, reorder, and resize controls.

The catalog entry **Table accessibility** demonstrates an interactive table, frozen-pane toggles, selection, headers, and theme variants. On a device, inspect headings, grid coordinates, row-major traversal through frozen and scrolling panes, focused-cell announcements, resize bounds, 200% and 300% text, RTL, and light, dark, and high-contrast modes. Instrumented Compose semantics tests cover pane ordering, and Android accessibility-node tests cover grid metadata, headings, active state, and actions. A human TalkBack traversal audit remains required before this inventory row can be stable. See [table viewport](table-viewport.md), [freezing](table-freezing.md), and the [coverage inventory](coverage.md).
