# Table reordering

The M51 source slice adapts [Blueprint Table reordering](https://blueprintjs.com/docs/#table/features) from DOM drag callbacks to controlled Compose key orders. The pinned authority is [Blueprint 6.18.0 table features](https://github.com/palantir/blueprint/blob/a60d4c92257612808fbfac81cfeee4fcba91a8b4/packages/table/src/docs/table-features.mdx). This API is **in progress** and unpublished.

```kotlin
var rows by remember { mutableStateOf(cases) }
var columns by remember { mutableStateOf(caseColumns) }
var selection by remember { mutableStateOf<BraceTableSelection?>(null) }

BraceDataTable(
    rows = rows,
    rowKey = { it.id },
    columns = columns,
    selection = selection,
    onSelectionChange = { selection = it },
    onRowOrderChange = { keys -> rows = BraceTableReorder.applyOrder(rows, { it.id }, keys) },
    onColumnOrderChange = { keys -> columns = BraceTableReorder.applyOrder(columns, { it.key }, keys) },
)
```

Each optional callback receives the **complete requested key order**. Apply it to the corresponding immutable list and persist it in the owning screen. Brace does not mutate data or reorder an active server sort behind the caller's back. `BraceTableReorder.move` computes a new order for one key, and `applyOrder` validates that the requested keys are an exact permutation before reordering items. Stable keys keep the current cell or header selection attached to its data after a move. If a host sort is active, update or clear that sort when the user changes manual order, or the host's next sorted list will take precedence.

When a callback is present and at least two items exist, column headers and fixed row headers gain separate 48 dp reorder grips. Drag a grip with touch or mouse toward another **visible** header, then release. No callback fires when the target resolves to the current position. Dragging does not steal the header's select, edit, or resize gesture. With a grip focused, Up/Down moves a row; Left/Right moves a column in visual direction, including RTL; Home/End moves to the first or last position. A tap or TalkBack click moves one position, and the alternate direction is a TalkBack custom action. Each grip announces its row or column name and current position; that state is a polite live region after the caller updates the order. The grip returns keyboard focus after a controlled move.

The grip, selected header, focus ring, and touch target use Brace table and semantic tokens. Column minimum width reserves one touch target for selection and one each for enabled reorder and resize grips. The fixed row header allocates the same separate targets. Row height follows the existing density, large-text, and touch-target minimums. Viewport-only column composition and lazy rows continue; offscreen drag destinations are not offered. The caller may reorder offscreen items through its own controls using `BraceTableReorder.move` or an exact key order.

Blueprint can drag selected contiguous multi-column or multi-row blocks. This M51 slice moves one row or column at a time. Multi-block drag, drag auto-scroll at viewport edges, animated drop guides, and conflict handling with server-managed sort remain future work. M50 introduces disjoint region selection on a separate branch; integration can add an explicit block-move contract later. See the [inventory](coverage.md) for released coverage.
