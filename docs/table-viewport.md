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

`BraceDataTable` composes only vertically visible rows and columns intersecting the horizontal viewport, with one adjacent column as a buffer. Rows use a `LazyColumn` keyed by caller-supplied stable row IDs. The fixed top header stays in place during vertical scroll. Row number headers stay in place during horizontal scroll. The two scroll offsets use Compose saveable states through `rememberBraceTableViewport`. Base column widths must be finite and at least the Brace minimum-column-width token. Optional controlled column widths and row heights are described in the [selection and resizing guide](table-selection-resize.md). The table requires a bounded parent width. [Compose encodes measured width and height in a packed constraint](https://developer.android.com/reference/kotlin/androidx/compose/ui/unit/Constraints), so this first slice rejects schemas whose combined column widths exceed that layout limit with an actionable exception; pass a narrower column subset for such schemas. Keep keys unique and stable across filtering and sorting. Brace validates nonblank, unique row keys once per immutable row-list version and retains their positions for keyboard navigation; replace the list when data changes. This validation is linear in the row count. Column boundaries and key positions are indexed once per immutable column-list or controlled-width change. Horizontal viewport lookup is logarithmic in the number of columns, then composes only the intersecting columns and one neighbor on each side. The 5,000-row × 400-column instrumented regression checks that distant two-axis scrolling and keyboard movement do not rescan row keys or compose offscreen cells. It does not establish a frame-time or memory benchmark for larger datasets.

Each visible cell and row header exposes its logical row and column index and the caller's human-readable row label, selected state, a localized spoken value, and a TalkBack selection action. Native accessibility checks confirm the cell label and click action share one Android node, column and row headers are visible, and keyboard focus stays on the table navigation stop. Partially scrolled row-header bounds are clipped to the body viewport. Row headers measure the localized corner label and the longest row number so their width grows with 3× text. The Compose semantics tree supplies explicit row-major traversal indices across the fixed overlay and viewport; on-device tests verify those hints and native node actions, while manual TalkBack reading-order QA remains open. The table is one hardware-keyboard focus stop; arrows, Home, End, Page Up, and Page Down move controlled selection and reveal an offscreen target. Horizontal movement follows the visual direction in RTL. Shift plus navigation extends a rectangular selection; see the [selection and resizing guide](table-selection-resize.md). Cell and row targets are at least 48 dp high and expand with large text. Long default text is ellipsized visually and spoken in full. Columns may supply `cellContent` for custom presentation while `cellText` remains the accessible value. Keep custom cell content noninteractive until dedicated cell-action APIs arrive. The table consumes Brace semantic colors, component table colors, typography, density, sizing, spacing, and metrics, so light, dark, high-contrast, compact, and comfortable theme changes apply at runtime.

This **in-progress** viewport supports [selection and resizing](table-selection-resize.md), [copying](table-copying.md), [cell editing](table-editing.md), [header-title editing](table-column-name.md), [controlled sorting](table-sorting.md), [formatters](table-formats.md), [public cell and header primitives](table-cells.md), [loading and status states](table-loading.md), and [controlled regions](table-regions.md), [row and column reordering](table-reordering.md), [frozen rows and columns](table-freezing.md), and [native grid accessibility](table-accessibility.md). Cells, rows, columns, rectangular ranges, and disjoint regions may be selected. The caller controls resizing, sorting, and selection. Formatters provide visual JSON and truncated text with full spoken values; table-level reveal remains open. Row and column reordering and frozen rows and columns remain planned. The catalog shows a 120-row table with scrolling, selection, resizing, copying, editing, and theme variants.
