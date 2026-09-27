# Table selection and resizing

Brace extends [the table viewport](table-viewport.md) against the pinned [Blueprint Table features](https://github.com/palantir/blueprint/blob/a60d4c92257612808fbfac81cfeee4fcba91a8b4/packages/table/src/docs/table-features.mdx). It is **in progress** in the `1.0.0` release. Brace uses stable row and column keys, Compose gestures, and Android accessibility actions instead of Blueprint's DOM regions and pixel resize callbacks.

```kotlin
var selection by remember { mutableStateOf<BraceTableSelection?>(null) }
var columnWidths by remember { mutableStateOf<Map<String, Dp>>(emptyMap()) }
var rowHeights by remember { mutableStateOf<Map<String, Dp>>(emptyMap()) }
val viewport = rememberBraceTableViewport()

BraceDataTable(
    rows = jobs,
    rowKey = { it.id },
    rowLabel = { it.name },
    columns = columns,
    selection = selection,
    onSelectionChange = { selection = it },
    columnWidths = columnWidths,
    onColumnWidthChange = { key, width -> columnWidths = columnWidths + (key to width) },
    rowHeights = rowHeights,
    onRowHeightChange = { key, height -> rowHeights = rowHeights + (key to height) },
    maxColumnWidth = 480.dp,
    maxRowHeight = 144.dp,
    viewport = viewport,
)
```

`BraceTableSelection` has `Cell`, `Row`, `Column`, and inclusive rectangular `Range(anchorRowKey, anchorColumnKey, extentRowKey, extentColumnKey)` values. A cell tap or mouse click selects one cell; a row or column header selects that axis. Shift plus an arrow, Home, End, Page Up, or Page Down extends the current cell selection while keeping its anchor. Shift plus a mouse click selects a rectangle from the current cell or range anchor. On touch, long-press a cell to start or extend a range, then tap its endpoint. A TalkBack cell custom action starts or extends a range; activating an endpoint completes it. The focused table announces a selected cell value, row label, column, or range extent; visible cells expose their selected state. Page Up and Page Down use the controlled row heights to move roughly one viewport. If data changes and a selected range anchor disappears, the next Shift navigation starts from the current valid cell. RTL left/right movement follows visual direction.

Width and height maps are controlled overrides keyed by stable column or row IDs. Resize handles appear only for axes with a callback. Each grip uses Brace's table handle color and width token inside a 48 dp or larger pointer and focus target. Drag with touch or mouse, tap to increase by the Brace spacing step, or focus a handle and press left/right for a column or up/down for a row. In RTL, left increases column width. TalkBack announces the axis and current size plus its minimum and optional maximum; its click action increases size and a custom action decreases it. The minimum column width reserves a 48 dp header-selection target plus a separate 48 dp target for each enabled sort, resize, or reorder control. The header selection node occupies only its own target, so touching a grip never selects a column. Minimum row height follows density, touch target, and typography so large text remains usable. Requested overrides and optional maximums are clamped to the current theme minimum when density or font scale changes. Each callback receives a `Dp` size; update the matching map to render it. Omit a callback to remove that axis's grips.

The table continues to compose only visible rows and horizontal columns with one adjacent column of buffer. Resize keeps the fixed row and column headers, stable keys, scroll offsets, and logical collection indices. `rememberBraceTableViewport` saves both scroll axes. [Saveable, disjoint, and whole-table regions](table-regions.md) extend this single-range API.

Mouse drag selection, touch auto-scroll during range creation, and column header menus remain planned. [Frozen rows and columns](table-freezing.md) are available through controlled counts. Controlled [sorting](table-sorting.md) and [reordering](table-reordering.md) have separate APIs. Plain-text [clipboard copying](table-copying.md) and controlled [single-cell editing](table-editing.md) are available separately. Rows and columns are not automatically sorted or persisted by the table. See the [inventory](coverage.md) for exact status.
