# Data table viewport

Brace Table is the Android adaptation of the pinned Blueprint Table and Column APIs and its viewport, fixed-header, and keyboard-navigation behavior. The source is [Blueprint Table 6.2.4 at commit a60d4c9](https://github.com/palantir/blueprint/blob/a60d4c92257612808fbfac81cfeee4fcba91a8b4/packages/table/src/docs/table-features.mdx). Brace uses original Compose code and Brace tokens; no Blueprint table source or styles are copied.

The source-only Maven coordinate is `io.github.joelromanpr.brace:brace-table:0.1.0-SNAPSHOT`. It is unpublished. `brace-table` exposes `brace-foundation` transitively and has the same version as other Brace artifacts.

```kotlin
private data class Record(val id: String, val name: String, val status: String)

val rows = remember { listOf(Record("a", "Archive", "Ready"), Record("b", "Import", "Review")) }
val columns = remember {
    listOf(
        BraceTableColumn<Record>("name", "Name", 160.dp, { it.name }),
        BraceTableColumn<Record>("status", "Status", 120.dp, { it.status }),
    )
}
var selectedRowKey by rememberSaveable { mutableStateOf<String?>(null) }
var selectedColumnKey by rememberSaveable { mutableStateOf<String?>(null) }
val selection = when {
    selectedRowKey == null -> null
    selectedColumnKey == null -> BraceTableSelection.Row(selectedRowKey!!)
    else -> BraceTableSelection.Cell(selectedRowKey!!, selectedColumnKey!!)
}
val viewport = rememberBraceTableViewport()
BraceDataTable(
    rows = rows,
    rowKey = { it.id },
    rowLabel = { it.name },
    columns = columns,
    selection = selection,
    onSelectionChange = {
        when (it) {
            is BraceTableSelection.Cell -> { selectedRowKey = it.rowKey; selectedColumnKey = it.columnKey }
            is BraceTableSelection.Row -> { selectedRowKey = it.rowKey; selectedColumnKey = null }
        }
    },
    viewport = viewport,
    height = 320.dp,
    label = "Jobs",
)
```

`BraceDataTable` composes only vertically visible rows and columns intersecting the horizontal viewport, with one adjacent column as a buffer. Rows use a `LazyColumn` keyed by caller-supplied stable row IDs. The fixed top header stays in place during vertical scroll. Row number headers stay in place during horizontal scroll. The two scroll offsets use Compose saveable states through `rememberBraceTableViewport`. Column widths are fixed and must be at least the Brace minimum-column-width token. Keep keys unique and stable across filtering and sorting. Brace validates nonblank, unique row keys once per immutable row-list version; replace the list when data changes. This validation is linear in the row count.

Each visible cell and row header exposes its logical row and column index and the caller's human-readable row label, selected state, a localized spoken value, and a TalkBack selection action. The table is one hardware-keyboard focus stop; arrows, Home, End, Page Up, and Page Down move controlled selection and reveal an offscreen target. Horizontal movement follows the visual direction in RTL. Modified arrow keys are left available to the host until range selection is implemented. Cell and row targets are at least 48 dp high and expand with large text. Long default text is ellipsized visually and spoken in full. Columns may supply `cellContent` for custom presentation while `cellText` remains the accessible value. Keep custom cell content noninteractive until dedicated cell-action APIs arrive. The table consumes Brace semantic colors, component table colors, typography, density, sizing, spacing, and metrics, so light, dark, high-contrast, compact, and comfortable theme changes apply at runtime.

This is an **in-progress** viewport slice. It implements one selected cell or one whole row. The pinned Blueprint cell/row/column/range selection capability remains planned because multi-region selection and range extension are absent. Resizing, copying, editing, row/column reordering, frozen data rows or columns, formatting helpers, loading states, and column-specific header menus also remain planned. Data sorting and persistence of controlled selection belong to the caller. The catalog shows a 120-row table with horizontal scrolling, row/cell selection, and theme variants.
