# Table regions

Brace adapts [Blueprint Table Region](https://blueprintjs.com/docs/#table/api.region) from inclusive zero-indexed intervals to stable row and column keys. The authority is Blueprint's [pinned `regions.ts`](https://github.com/palantir/blueprint/blob/a60d4c92257612808fbfac81cfeee4fcba91a8b4/packages/table/src/regions.ts). This API is **in progress** in the `0.1.0-alpha01` preview.

`BraceTableRegion.Cells`, `Rows`, `Columns`, and `Table` correspond to Blueprint's `CELLS`, `FULL_ROWS`, `FULL_COLUMNS`, and `FULL_TABLE` cardinalities. `BraceTableSelection.Regions` holds one or more disjoint regions. `BraceTableRegions.add` and `updateLast` create new controlled values. Keys remain attached to data during a host-owned sort; their current positions determine the inclusive interval. If any bound key disappears, the selection is stale: the table does not highlight or copy a partial result.

```kotlin
var selection by rememberBraceTableSelection()
val columns = listOf(
    BraceTableColumn<Case>("name", "Name", 160.dp, { it.name }),
    BraceTableColumn<Case>("status", "Status", 120.dp, { it.status }),
)
BraceDataTable(cases, { it.id }, columns, selection, { selection = it }, rowLabel = { it.name })

// Controlled additions from screen actions:
BraceButton("Add case", onClick = {
    selection = BraceTableRegions.add(selection, BraceTableRegion.Cells("case-1", "status"))
})
BraceButton("Add rows", onClick = {
    selection = BraceTableRegions.add(selection, BraceTableRegion.Rows("case-4", "case-8"))
})
BraceButton("Select all", onClick = {
    selection = BraceTableSelection.Regions(listOf(BraceTableRegion.Table))
})
```

Tap the fixed corner, press Ctrl/Cmd+A while the table has focus, or use the table's TalkBack **Select entire table** action to select all. Ctrl/Cmd+click adds a cell, row header, or column header as a disjoint region. A visible cell or header has a TalkBack **Add to selection** action. Existing tap, Shift+click, long-press range, Shift+arrow, and keyboard navigation remain. Shift+arrow extends the last selected cell region while preserving earlier regions. A plain arrow collapses to one cell. A row, column, or whole-table region has no finite cell anchor, so Shift+arrow starts a new rectangular range. Row and column headers announce selected state when their full axis is selected; each visible cell exposes its selected state. The fixed corner and table use token colors and focus rings and retain the table's touch-target and large-text sizing.

`BraceTableClipboard.formatSelection` and Ctrl/Cmd+C copy disjoint regions as **one sparse TSV rectangle** in current row and column order. Unselected gaps are blank and their cell values are never read. The formatter quotes selected values containing tabs, line breaks, or quotes. This differs from Blueprint's browser clipboard and preserves positions for spreadsheet paste. Whole-table copy includes all current rows and columns, even beyond the viewport. Large selections format synchronously; an app needing unbounded export should provide a separate background export. Column headers and row keys are not copied.

`rememberBraceTableSelection` uses `BraceTableSelectionSaver` to restore the controlled selection across configuration changes and process recreation. A restored key may become stale if the host's data changed; update or clear it when records are removed. The catalog contains an interactive selection and copying sample; the independent Maven consumer contains public API usage awaiting the shared build lane. Mouse drag selection, touch auto-scroll while extending, row/column interval drag, and frozen regions remain separate planned work. See the [inventory](coverage.md) for released coverage.
