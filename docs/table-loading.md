# Table loading, empty, and error states

Brace Table adapts the pinned [Blueprint Table 6.2.4 loading API](https://github.com/palantir/blueprint/blob/a60d4c92257612808fbfac81cfeee4fcba91a8b4/packages/table/src/docs/table-features.mdx). Blueprint offers table, column, and individual cell/header loading flags, with the closest override taking priority. Brace adds row body overrides and explicit empty/error states for Android. This is original Compose code; no Blueprint styles or components are copied. The Maven Central coordinate is `io.github.joelromanpr.brace:brace-table:1.0.0`; this behavior remains in progress.

`BraceDataTable(state = ...)` accepts caller-owned `BraceTableState.Ready`, `Loading`, `Empty`, or `Error`. Loading defaults to all body cells and headers. Use `BraceTableLoading` to mask only a scope:

```kotlin
var tableState by remember { mutableStateOf<BraceTableState>(BraceTableState.Ready) }

tableState = BraceTableState.Loading(
    BraceTableLoading(
        columnCells = mapOf("status" to true),
        columnHeaderOverrides = mapOf("status" to true),
        cellOverrides = mapOf(BraceTableSelection.Cell("job-7", "status") to false),
    ),
)
BraceDataTable(
    rows = jobs,
    rowKey = { it.id },
    columns = columns,
    selection = selection,
    onSelectionChange = { selection = it },
    state = tableState,
)
```

For body cells, an explicit cell override wins over a row override, then a column override, then the table-wide `cells` flag. A `false` value opts out of a broader loading mask. Column and row header overrides win over `columnHeaders` and `rowHeaders`. Use stable keys and immutable maps. Static skeletons reuse the table's visible viewport and fixed header geometry, so offscreen rows and columns are not composed. They use Brace table grid tokens and semantic disabled colors without animation; reduced-motion users receive the same display.

Loading is intentionally read-only across the table, including visible cells outside the mask. It pauses touch, mouse, keyboard, TalkBack selection, copy, edit, and resize while preserving caller-owned selection and saveable viewport state. The table announces a single polite loading state; skeletons do not expose stale values or actions. Ready restores the same data and controlled selection. With no rows, Loading shows a status panel instead of an empty grid.

`BraceTableState.Empty("No matching jobs")` and `BraceTableState.Error("Could not load jobs", onRetry = ::refresh)` replace the grid with one status panel. The panel announces its message, scrolls at large text sizes, and gives Retry a 48 dp target with touch, mouse, Enter, and TalkBack click behavior. Error announcements are assertive. `Ready` with an empty row list retains the table's existing default “No rows” body. Keep error text actionable and avoid showing stale data as live results.

This API is **in progress**. Blueprint's per-component loading props are represented by an explicit key-based mask rather than component props; Brace does not yet animate skeletons or allow interaction with unaffected cells during refresh. Loading does not cancel an active controlled edit session; the caller should close its editor before starting a refresh if it does not want that draft to reappear when Ready returns. The [table integration PR](https://github.com/joelromanpr/brace-android/pull/67) records combined device and Maven consumer checks.
