# Editable table column names

Brace adds `BraceEditableColumnName` and controlled column-header title editing to [BraceDataTable](table-viewport.md). The comparison is the pinned Blueprint [EditableName API](https://github.com/palantir/blueprint/blob/a60d4c92257612808fbfac81cfeee4fcba91a8b4/packages/table/src/docs/table-api.mdx) and [implementation](https://github.com/palantir/blueprint/blob/a60d4c92257612808fbfac81cfeee4fcba91a8b4/packages/table/src/headers/editableName.tsx). This inventory row is **in progress** in the `1.0.0` release.

```kotlin
private data class CaseRow(val id: String, val status: String)

var title by remember { mutableStateOf("Status") }
var selection by remember { mutableStateOf<BraceTableSelection?>(null) }
var editingName by remember { mutableStateOf<String?>(null) }
val columns = listOf(
    BraceTableColumn<CaseRow>("status", title, 160.dp, { it.status }, editableName = true),
)
BraceDataTable(
    rows = rows,
    rowKey = { it.id },
    columns = columns,
    selection = selection,
    onSelectionChange = { selection = it },
    editingColumnName = editingName,
    onEditingColumnNameChange = { editingName = it },
    onColumnNameCommit = { key, newTitle -> if (key == "status") title = newTitle },
    validateColumnName = { _, newTitle -> if (newTitle.length < 3) "Use at least 3 characters" else null },
)
```

The `BraceTableColumn.key` identifies the column and must remain stable as `title` changes. The caller owns the confirmed title and active editor key. Supply both edit callbacks. A blank or whitespace-only title is always rejected before the optional custom validator, because table columns require a nonblank title. Validation errors keep the draft open. For persistence across process recreation, save the caller's title and `editingColumnName`; the editor saves its unfinished text and selection while composed. If an editable column disappears or loses `editableName`, Brace closes its stale editor without committing.

One tap selects the column; this Android mapping opens editing when you select an opted-in header and press Enter or F2, double-tap or double-click it, or use its localized TalkBack **Edit column name** action. `BraceEditableColumnName` focuses the native text field and selects the entire title so typing replaces it. Enter or IME Done validates and commits; Escape or the TalkBack **Cancel editing** action discards the draft. Commit and cancel return hardware-keyboard focus to the table, while the selected column remains identified by its key. A caller may also set `editingColumnName` to an offscreen column key; Brace scrolls it into the horizontal viewport before composing its editor. The active column's resize grip is hidden while editing, and editor keys are kept out of table navigation and copy handling. While a title draft is open, other headers are read-only: tapping, double-tapping, or invoking Edit on another header does not change the selected column or replace the draft. Commit or cancel first. Cell and header editing sessions cannot be active together.

The active spoken label identifies the header row and numbered column. The editor uses Brace table component colors, focus/error borders, typography, spacing, and the 48 dp minimum touch target in light, dark, and high-contrast themes. The header grows to reserve space for validation text and large text. Android accessibility exposes one header context node with the column title, header-row/column position, and selected state when applicable, followed by a focused native editable text node with Save and Cancel actions. Compose semantics also holds the header collection coordinate; Android does not currently map that coordinate onto the native context node, so the spoken label supplies the column identity. The editor remains single-line and supports IME composition before handling Enter.

Blueprint's `onChange` callback fires for every draft change and its `index` callback argument follows React's column index. Brace sends the stable column key and confirmed title through `onColumnNameCommit`; the draft stays inside the editor until confirmation. Per-keystroke `onChange`, draft-valued `onCancel`, positional index callbacks, custom header renderers, menu interaction bars, column reordering, asynchronous validation, and header-specific multiline layouts are deferred. Apps should resolve remote validation and concurrent title changes in their own data layer. See [coverage](coverage.md) for the exact inventory status.
