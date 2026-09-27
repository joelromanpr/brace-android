# Table value formatting and reveal

Pinned Blueprint [TruncatedFormat and JSONFormat](https://github.com/palantir/blueprint/blob/a60d4c92257612808fbfac81cfeee4fcba91a8b4/packages/table/src/docs/table-api.mdx) display compact cell values and offer a full-value popover. Brace exposes `BraceTruncatedCell`, `BraceJsonCell`, `BraceTruncatedFormatter`, and `BraceJsonFormatter` in `brace-table`. These inventory rows remain **in progress** in the `0.1.0-alpha01` preview.

```kotlin
val details = "A long log line that needs room to read..."
BraceTruncatedCell(details, Modifier.width(220.dp), maxCharacters = 24)

val payload = linkedMapOf<String, Any?>("status" to "ready", "items" to listOf(1, null, true))
BraceJsonCell(payload, Modifier.width(220.dp), maxCharacters = 30)
```

`BraceTruncatedCell` counts Unicode code points. `maxCharacters = 0` disables character clipping. It applies `suffix` only when clipping, then uses Compose text layout to detect line or width overflow. `BraceRevealMode.WhenTruncated` offers a 48 dp **More** action only when needed; `Always` and `Never` override that. Compose measures text at the actual Android width, font scale, and style. The preview keeps the full value in TalkBack semantics even while visually clipped. **More** opens a Brace-token-styled dialog with selectable full text. Touch, mouse, Enter, or Space open it; Close, Back, outside touch, or Escape dismiss it. The standalone trigger regains keyboard focus. Uncontrolled open state is saveable; controlled `expanded` requires `onExpandedChange` and caller restoration.

`BraceJsonFormatter.format` accepts null, strings, finite numbers, booleans, `Map<String, *>`, lists, arrays, `JSONObject`, and `JSONArray`. It rejects cycles, non-string map keys, unsupported objects, non-finite numbers, and nesting beyond 128 levels. Maps keep iteration order. Replace mutable maps or lists with a new instance when data changes so Compose can refresh the remembered formatted value. The default two-space indentation and unquoted top-level strings follow Blueprint; `omitQuotesOnStrings = false` adds JSON quotes. `indent = 0` produces compact JSON. `BraceJsonCell` uses code typography and left-to-right JSON text. Null displays `null` and has no standalone reveal action.

## Revealing a value inside `BraceDataTable`

The table owns cell selection gestures. Pass the **complete** string to `cellText` for TalkBack, clipboard copying, and the full-value dialog. Use noninteractive `cellContent` for the compact display. The row-dependent `revealFullValue` predicate adds a separate 48 dp **More** target and a custom TalkBack action only where a full-value view is useful:

```kotlin
val columns = listOf(
    BraceTableColumn<Record>("payload", "Payload", 220.dp,
        cellText = { BraceJsonFormatter.format(it.payload) },
        cellContent = { row -> BraceJsonCell(row.payload, maxCharacters = 24,
            revealMode = BraceRevealMode.Never) },
        revealFullValue = { row -> row.payload != null &&
            BraceJsonFormatter.format(row.payload).length > 24 },
        fullValuePreformatted = true),
)
BraceDataTable(rows, { it.id }, columns, selection, { selection = it },
    frozenRows = 1, frozenColumns = 1)
```

The predicate decides which values need reveal; the table cannot infer whether arbitrary custom `cellContent` is clipped. A nullable predicate means no reveal action. Columns with a predicate have at least two 48 dp touch targets even if a smaller width or maximum is requested. The button does not alter selection, while the cell keeps its own selection and editing gestures. Ctrl/Cmd+Enter opens the active cell's full value when its predicate is true; plain Enter retains editing behavior. TalkBack can invoke **Show full value** on either the cell's custom action or the separate button. The dialog follows the current `cellText`, survives saved-state restoration while the row and column remain, and closes if the caller removes either key, disables the predicate, begins editing, or enters loading. Closing returns keyboard focus to the table navigation stop. A preformatted dialog preserves line breaks and left-to-right code text.

The catalog includes short, long, and null values and the independent Maven consumer compiles the table API. Device checks cover separate touch targets, full spoken and copied values, selection and editing independence, keyboard and mouse reveal, loading/removal behavior, saved state, frozen panes, RTL, large text, and high contrast. Manual TalkBack traversal and release review remain open.
