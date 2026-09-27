# Table row and column freezing

Brace adapts [Blueprint Table freezing](https://blueprintjs.com/docs/#table/features) from `numFrozenRows` and `numFrozenColumns` to caller-controlled Compose counts. The pinned comparison is [Blueprint 6.18.0 table features](https://github.com/palantir/blueprint/blob/a60d4c92257612808fbfac81cfeee4fcba91a8b4/packages/table/src/docs/table-features.mdx). This API is **in progress** in the `1.0.0` release.

```kotlin
val viewport = rememberBraceTableViewport()
var selection by remember { mutableStateOf<BraceTableSelection?>(null) }
var frozenRows by rememberSaveable { mutableIntStateOf(1) }
var frozenColumns by rememberSaveable { mutableIntStateOf(1) }

BraceDataTable(
    rows = records,
    rowKey = { it.id },
    columns = columns,
    selection = selection,
    onSelectionChange = { selection = it },
    viewport = viewport,
    frozenRows = frozenRows,
    frozenColumns = frozenColumns,
)
```

Counts pin the leading positions in the current caller-supplied row and column order. The table never changes that order. When a sort or reorder changes the first item, the new first item becomes pinned. Keep keys stable to preserve selection and editing identity. Counts must be between zero and the number of items on their axis. Set counts that leave room for at least one scrolling row and column on the narrowest supported screen and at large text sizes; oversized pinned panes are clipped.

Fixed row and column headers remain in their own pane. Frozen body rows remain at the top while the rest of the rows use a lazy vertical list. Frozen body columns remain at the logical start edge while the rest use a horizontal scroll state. The intersection is drawn once, so TalkBack receives one cell node with its absolute row and column index. Frozen cells announce their pinned state. Keyboard navigation crosses pane boundaries and reveals scrollable targets. Selection, copying, editing, resizing, sorting, loading, and reordering continue to use the same stable keys and controlled values.

Instrumented tests cover two-axis pinning, distinct accessibility nodes, keyboard reveal, selection, and RTL. The [table integration PR](https://github.com/joelromanpr/brace-android/pull/67) records the device, API, and consumer checks. A human TalkBack review remains open. Multi-item drag, drag auto-scroll, and column header menus remain open table work. See the [coverage inventory](coverage.md) for release status.
