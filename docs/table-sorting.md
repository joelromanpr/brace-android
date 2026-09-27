# Table sorting

Blueprint Table 6.2.4 is data-agnostic: its [pinned sorting guide](https://github.com/palantir/blueprint/blob/a60d4c92257612808fbfac81cfeee4fcba91a8b4/packages/table/src/docs/table-features.mdx) uses column-header menu actions and lets the application update the row mapping. Brace likewise reports sorting intent through a controlled Compose API. The host sorts its immutable row list and passes it back with stable row IDs.

```kotlin
private data class Record(val id: String, val name: String)

val source = remember { listOf(Record("b", "Beta"), Record("a", "Alpha")) }
val sort = rememberBraceTableSortState()
val visible = remember(source, sort.value) {
    val current = sort.value
    if (current == null) source else source.withIndex().sortedWith { a, b ->
        val value = a.value.name.compareTo(b.value.name)
        val directed = if (current.direction == BraceTableSortDirection.Ascending) value else -value
        if (directed == 0) a.index.compareTo(b.index) else directed
    }.map { it.value }
}
var selection by remember { mutableStateOf<BraceTableSelection?>(null) }
val columns = remember {
    listOf(BraceTableColumn<Record>("name", "Name", 160.dp, { it.name }, sortable = true))
}
BraceDataTable(
    rows = visible,
    rowKey = { it.id },
    columns = columns,
    selection = selection,
    onSelectionChange = { selection = it },
    sort = sort.value,
    onSortChange = { sort.value = it },
    label = "Cases",
)
```

`BraceTableSort` holds one stable column key and `Ascending` or `Descending`. `null` means original order. The header cycles unsorted → ascending → descending → unsorted; choosing another column starts ascending. `BraceTableSort.next` implements that transition for alternate UIs. `rememberBraceTableSortState` saves the descriptor through configuration and process recreation. The table is controlled: if `onSortChange` does not update `sort`, the visual state stays unchanged. The table never assumes a locale comparator, number parser, server query, or tie-breaking policy. The example uses original index as the tie break so equal values keep their source order. A remote data source can handle `onSortChange` and later replace `rows` when its response arrives.

Set `sortable = true` only on columns with a meaningful ordering. A sortable header retains its selection action, and an editable name retains its separate double-click, Enter/F2, and TalkBack Edit action. The 48 dp sort control is beside the title; when a resize grip exists, it occupies a separate 48 dp area. Only opted-in sortable columns reserve extra width for that control; unsortable columns retain their compact width. During cell editing, sort actions are disabled. A header editor replaces its own sort control and disables the other sort actions so an active draft cannot be displaced. The fixed header stays present during vertical scrolling; the control moves with its column during horizontal scrolling. Virtualized columns remain clickable when revealed. Narrow or large-text headers ellipsize the visible title but continue to speak its full name.

The sort control has a keyboard focus ring, touch and mouse click behavior, and a TalkBack action label describing its *next* effect. The header also announces its current direction and offers a custom sort action. These strings are localized in English and Spanish resources. Arrow indicators use Brace table and semantic color tokens and keep ascending/descending meaning in RTL. Light, dark, high-contrast, compact, and comfortable themes resolve through `BraceTheme`. The caller should keep row and column keys stable so selection, active editing, and clipboard references continue to identify the same record as rows move.

This row is **in progress**. The focused API 36 suite passed 7/7, and the complete table suite passed 70/70 on the M26/M41 integration base. The current-main API, catalog build, lint, local Maven publication and independent consumer passed. Manual TalkBack, mouse and tablet review and hosted CI remain pending. Multi-column sorting, custom sort menus, and a built-in comparator are outside this slice. The [coverage ledger](coverage.md) remains authoritative for release status.
