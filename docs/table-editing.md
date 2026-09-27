# Table cell editing

The M22 source slice adds `BraceEditableCell` and controlled cell editing to [BraceDataTable](table-viewport.md). It maps the pinned Blueprint [EditableCell API](https://github.com/palantir/blueprint/blob/a60d4c92257612808fbfac81cfeee4fcba91a8b4/packages/table/src/docs/table-api.mdx) and [Editing feature](https://github.com/palantir/blueprint/blob/a60d4c92257612808fbfac81cfeee4fcba91a8b4/packages/table/src/docs/table-features.mdx) to Compose and Android focus, IME, pointer, and accessibility behavior. Both inventory rows remain **in progress** and unpublished.

```kotlin
private data class CaseRow(val id: String, val title: String)

var rows by remember { mutableStateOf(listOf(CaseRow("a", "Review invoice"))) }
var selection by remember { mutableStateOf<BraceTableSelection?>(null) }
var editing by remember { mutableStateOf<BraceTableSelection.Cell?>(null) }
val columns = remember {
    listOf(BraceTableColumn<CaseRow>("title", "Title", 180.dp, { it.title }, editable = true))
}

BraceDataTable(
    rows = rows,
    rowKey = { it.id },
    columns = columns,
    selection = selection,
    onSelectionChange = { selection = it },
    editingCell = editing,
    onEditingCellChange = { editing = it },
    onCellCommit = { cell, text ->
        rows = rows.map { if (it.id == cell.rowKey) it.copy(title = text) else it }
    },
    validateCell = { _, text -> if (text.isBlank()) "A title is required" else null },
)
```

`BraceEditableCell` is also public for a custom table layout. Give it explicit width and height; the input uses a 48 dp minimum target and needs extra height if validation text can appear:

```kotlin
BraceEditableCell(
    value = row.title,
    label = "Edit Title, row 1 (Review invoice)",
    onCommit = { updateTitle(row.id, it); closeEditor() },
    onCancel = { closeEditor() },
    modifier = Modifier.width(180.dp).height(80.dp),
    validate = { if (it.isBlank()) "A title is required" else null },
)
```

Mark each editable column explicitly. The caller owns the active `editingCell`, confirmed row values, and optional validation. Both edit callbacks must be supplied together. Save `editingCell` keys and row values in the owning screen if they need to survive process recreation; `BraceEditableCell` saves its unconfirmed text and selection while it stays composed. Keep row and column keys stable when committing: changing a visible value should not change its row key. If a row or editable column disappears, the table closes the stale editor without committing its draft.

A focused table opens the selected editable cell on Enter or F2. Double-tap or double-click a visible editable cell, or use its localized TalkBack **Edit cell** action, to open directly. TalkBack encounters a row/column context node before the focused native text field. Compose exposes these as two accessibility nodes: the context announces the column, row number, and row label; Compose semantics also holds collection coordinates, while Android does not currently expose those coordinates on the context node; the editable node retains Android text selection and IME behavior plus Save changes and Cancel editing actions. The context node contains no duplicate text value. Enter or IME Done validates and commits; Escape or the TalkBack **Cancel editing** action discards the draft. A validation message stays visible and is exposed as a field error. A successful commit or cancel returns keyboard focus to the table; arrows again navigate selected cells. While editing, table navigation and table-level Ctrl/Cmd+C are suspended so text input owns its keys and clipboard shortcuts. The editor has explicit TalkBack save and cancel actions. Tab can move focus away while leaving the draft open; press Enter or IME Done to commit, or Escape to cancel. Mouse hover and touch selection continue to use the existing table behavior outside the active cell.

The field and its error/focus colors come from versioned table component tokens and Brace typography and spacing. It uses the current light, dark, or high-contrast theme and preserves IME composition before handling Enter. The row stays at least a Brace touch target high. This slice covers single-line text in one cell at a time. Blueprint's live `onChange` callback is not separately exposed by `BraceDataTable`; the draft stays inside `BraceEditableCell` until confirmation, and callers receive the validated value through `onCellCommit`. [Editable column names](table-column-name.md) have their own controlled M26 slice. Multiline editors, formula evaluation, batched paste, fill handles, row edit transactions, multi-cell edit transactions remain planned. Validation runs synchronously; apps should handle server validation and conflicts in their own data layer. See [coverage](coverage.md) for exact row status.
