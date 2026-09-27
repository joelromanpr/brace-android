# Simple table

[Blueprint 6.18.0 `HTMLTable`](https://github.com/palantir/blueprint/blob/a60d4c92257612808fbfac81cfeee4fcba91a8b4/packages/core/src/components/html-table/html-table.mdx) is a CSS skin for a small native HTML table. `BraceSimpleTable` is its Compose adaptation in `brace-core`. The larger Blueprint `@blueprintjs/table` maps to the separate `brace-table` artifact and carries viewport rendering, selection ranges, copying, editing, and resizing. The HTMLTable inventory row remains **in progress** until device and release acceptance are complete.

```kotlin
val columns = listOf(
    BraceSimpleTableColumn("name", "Name"),
    BraceSimpleTableColumn("status", "Status"),
)
val rows = listOf(
    BraceSimpleTableRow("a", mapOf("name" to "Alpha", "status" to "Ready")),
    BraceSimpleTableRow("b", mapOf("name" to "Beta", "status" to "Paused")),
)
var selectedKey by rememberSaveable { mutableStateOf<String?>(null) }
BraceSimpleTable(
    columns = columns,
    rows = rows,
    label = "Job status",
    bordered = true,
    striped = true,
    interactive = true,
    selectedRowKey = selectedKey,
    onRowClick = { selectedKey = it },
)
```

`columns` and `rows` require unique, stable keys. Each row maps column keys to text. Missing keys show empty cells. The optional `cellContent` slot can change visual content while each string remains the full spoken value. Keep controls out of that slot: use `brace-table` or a list for editable/action-rich data. `BraceSimpleTable` composes every row, so it is intended for small datasets only.

`bordered`, `striped`, `interactive` and `compact` mirror the four Blueprint flags. `interactive` adds hover feedback; `onRowClick` opts in to activation. Compact defaults to `BraceTheme.density` and can be overridden per table. Colors, borders, cell insets, typography and minimum widths come from Brace semantic and table tokens. Narrow layouts scroll horizontally. Selected rows use the selection color; disabled rows stay readable and cannot activate. Actionable cells retain 48 dp minimum height in either density. Large text grows row height; long values wrap and can widen a column through `minWidth`.

TalkBack sees a labeled collection with row and column coordinates, header headings and spoken cells containing each column name, row number and full value. Every cell can receive keyboard focus; arrow keys move through the grid, and left/right follow the visual direction in RTL. Enter and Space activate an enabled row when `onRowClick` is supplied. Touch and mouse activate the same callback. The host owns `selectedRowKey` and should save it across recreation. Visual focus uses the Brace focus-ring token. Localized English and Spanish cell announcements are included; translators can extend the Android resources.

The catalog exposes live bordered, striped, compact, interactive and selected states under **HTMLTable**, along with copyable usage. The independent Maven consumer includes a table. Device tests cover grid semantics, touch, mouse, keyboard, RTL, selection, disabled rows, large type and high contrast; automated Compose accessibility checks run where supported. Manual TalkBack review, device results and release evidence remain open.
