# Table cells and headers

Brace Table's `BraceTableCell`, `BraceColumnHeader`, and `BraceRowHeader` are the real visible nodes in `BraceDataTable`. They adapt the pinned [Blueprint Table 6.2.4 Cell, ColumnHeaderCell, and RowHeaderCell APIs](https://github.com/palantir/blueprint/blob/a60d4c92257612808fbfac81cfeee4fcba91a8b4/packages/table/src/docs/table-api.mdx) to Compose. Source is original Apache-2.0 Brace code. The Maven Central coordinate is `io.github.joelromanpr.brace:brace-table:1.0.0`; these APIs remain in progress.

A typed `BraceTableColumn` always provides `cellText`, which supplies copying and the full spoken value. Its optional `cellContent` and `headerContent` replace visual content. `BraceDataTable.rowHeaderContent` replaces visible row numbers. Keep custom content noninteractive: the outer cell or header owns touch, mouse, and TalkBack selection, and the table owns keyboard focus and navigation. Custom Material 3 `Text` inherits the correct selected or normal content color from the primitive. The visible presentation can ellipsize; the full `cellText`, column `title`, and `rowLabel` remain in their accessible names.

```kotlin
data class Job(val id: String, val name: String, val status: String)

val columns = listOf(
    BraceTableColumn<Job>(
        key = "status", title = "Status", width = 140.dp,
        cellText = { it.status },
        cellContent = { row -> Text("● " + row.status) },
        headerContent = { Text("◆ Status") },
    ),
)
var selection by remember { mutableStateOf<BraceTableSelection?>(null) }
BraceDataTable(
    rows = jobs,
    rowKey = { it.id },
    rowLabel = { it.name },
    columns = columns,
    selection = selection,
    onSelectionChange = { selection = it },
    rowHeaderContent = { _, index -> Text("R" + (index + 1)) },
)
```

The three composables are also public for table-like surfaces with caller-owned selection. Each takes stable keys, zero-based indices, a full accessible label, and an `onSelect` callback. `BraceTableCell` has optional range and edit actions; `BraceColumnHeader` has an optional edit action. For an actual data table, use `BraceDataTable` so the shared grid semantics, viewport rendering, fixed headers, keyboard navigation, and resize handles remain coordinated. Body cells announce column title, one-based row number, row label, and full value. Headers announce their axis and one-based index. TalkBack sees one selectable node per primitive; decorative slot content does not create an extra accessibility stop. The table has one hardware-keyboard focus stop, with logical movement in RTL. It expands rows for large text and retains 48 dp targets in compact density. All backgrounds, text, borders, hover, and focus colors come from Brace table and semantic tokens.

These primitives remain **in progress**. Table-level loading and key-scoped skeletons are covered in [loading states](table-loading.md); row and column grips are covered in [reordering](table-reordering.md). Native tooltip, wrapped content, interactive descendants, header menus, and arbitrary child layouts remain gaps. The combined device tests cover these public primitives; a human TalkBack audit and a release remain open.
