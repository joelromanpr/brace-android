# Table copying

Brace adds plain-text copying to the [Brace data table](table-viewport.md). It covers the pinned Blueprint [Copying capability](https://github.com/palantir/blueprint/blob/a60d4c92257612808fbfac81cfeee4fcba91a8b4/packages/table/src/docs/table-features.mdx) as an Android adaptation. The inventory row remains **in progress** in the `1.0.0` release.

`BraceDataTable` copies the current controlled `BraceTableSelection` with Ctrl+C or Meta+C when the table has keyboard focus. Its TalkBack node exposes a localized **Copy selected cells** action. The text goes to the Android system clipboard. The clipboard can surface a system confirmation or preview according to the OS version. Touch users can use the accessibility action or call the formatter from their own toolbar:

```kotlin
val clipboard = LocalContext.current.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
val text = BraceTableClipboard.formatSelection(rows, { it.id }, columns, selection)
if (text != null) clipboard.setPrimaryClip(ClipData.newPlainText("Cases", text))
```

Cell, row, column, and rectangular range selections all copy in the table's current logical row and column order, including cells outside the viewport. Rows are separated by line breaks and columns by tabs. Embedded tabs, line breaks, and quotes are quoted and doubled. Headers, row keys, and hidden formatting are not included. The formatter returns `null` for an empty or stale selection. Keyboard copying then leaves the clipboard unchanged, and the TalkBack copy action is hidden for that selection. Apps should avoid rendering or logging copied text if it may contain sensitive data.

The table reuses its remembered row-key index for built-in copying; a standalone formatter call validates the supplied row keys on each invocation. This API formats the full selected region synchronously. Very large whole-column selections can take time and allocate a large string; keep data sets bounded or provide an app-specific background export for such cases. [Selection regions](table-regions.md) add whole-table and disjoint sparse TSV copying. HTML clipboard formats and copy shortcuts from outside table focus remain planned. Controlled single-cell editing is available in [controlled cell editing](table-editing.md); while its field has focus, text-input clipboard shortcuts take precedence. See the [coverage ledger](coverage.md) for exact status.
