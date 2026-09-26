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
var selection by remember { mutableStateOf<BraceTableSelection?>(null) }
val viewport = rememberBraceTableViewport()
BraceDataTable(
    rows = rows,
    rowKey = { it.id },
    rowLabel = { it.name },
    columns = columns,
    selection = selection,
    onSelectionChange = { selection = it },
    viewport = viewport,
    height = 320.dp,
    label = "Jobs",
)
```

`BraceDataTable` composes only vertically visible rows and columns intersecting the horizontal viewport, with one adjacent column as a buffer. Rows use a `LazyColumn` keyed by caller-supplied stable row IDs. The fixed top header stays in place during vertical scroll; row number headers stay in place during horizontal scroll. Both offsets use Compose saveable states through `rememberBraceTableViewport`. Base column widths must meet the Brace minimum-column-width token. Optional controlled column widths and row heights are described in the [selection and resizing guide](table-selection-resize.md). Keep keys unique and stable across filtering and sorting. Brace validates nonblank, unique row keys and caches their positions once per immutable row-list version; replace the list when data changes. Column boundaries and key positions are indexed once per column-list or controlled-width change. Horizontal viewport lookup is logarithmic in the number of columns, then composes only the intersecting columns and one neighbor on each side. The 5,000-row × 400-column instrumented regression checks that distant two-axis scrolling and keyboard movement do not rescan row keys or compose offscreen cells. It does not establish a frame-time or memory benchmark for larger datasets.

Each visible cell, column header, and row header exposes its logical index, selected state, a localized spoken value, and a TalkBack selection action where applicable. Native accessibility checks confirm each cell label and click action share one Android node, column and row headers are visible, and keyboard focus stays on the table navigation stop. Partially scrolled row-header bounds are clipped to the body viewport. The table is one hardware-keyboard focus stop; arrows, Home, End, Page Up, and Page Down move controlled selection and reveal an offscreen target. Horizontal movement follows the visual direction in RTL, and Shift with a navigation key extends an inclusive rectangular range. Cell and row targets are at least 48 dp high and expand with large text. Long default text is ellipsized visually and spoken in full. Columns may supply `cellContent` for custom presentation while `cellText` remains the accessible value. Keep custom cell content noninteractive until dedicated cell-action APIs arrive. The table consumes Brace semantic colors, component table colors, typography, density, sizing, spacing, and metrics, so light, dark, high-contrast, compact, and comfortable theme changes apply at runtime.

This is an **in-progress** viewport slice, extended by [M18 selection and resizing](table-selection-resize.md) and [M21 copying](table-copying.md), [M22 cell editing](table-editing.md), and [M26 header-title editing](table-column-name.md) and [M46 controlled sorting](table-sorting.md). One cell, row, column, or inclusive rectangular range can be selected at a time. Column and row resizing is controlled by the caller. Disjoint multi-region selection, multi-cell and rich editing, row/column reordering, frozen data rows or columns, formatting helpers, loading states, and column-specific header menus remain planned. The caller owns row ordering and persistence of controlled selection; the table now reports one-column sorting intent and direction. The catalog shows a 120-row table with both-axis scrolling, selection, resizing, clipboard copying, single-cell and header-title editing, and theme variants.
