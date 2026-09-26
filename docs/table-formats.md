# Table value formatters

Pinned Blueprint [TruncatedFormat and JSONFormat](https://github.com/palantir/blueprint/blob/a60d4c92257612808fbfac81cfeee4fcba91a8b4/packages/table/src/docs/table-api.mdx) display long cell values and offer a full-value popover. Brace exposes `BraceTruncatedCell`, `BraceJsonCell`, `BraceTruncatedFormatter` and `BraceJsonFormatter` in `brace-table`. These inventory rows are **in progress** and unpublished.

```kotlin
val details = "A long log line that needs room to read..."
BraceTruncatedCell(details, Modifier.width(220.dp), maxCharacters = 24)

val payload = linkedMapOf<String, Any?>("status" to "ready", "items" to listOf(1, null, true))
BraceJsonCell(payload, Modifier.width(220.dp), maxCharacters = 30)
```

`BraceTruncatedCell` counts Unicode code points. `maxCharacters = 0` disables character clipping. It applies `suffix` only when clipping, then uses Compose's measured text layout to detect line or width overflow. `BraceRevealMode.WhenTruncated` offers the 48 dp **More** action only when needed; `Always` and `Never` override that. Unlike Blueprint's approximate DOM mode, Compose can measure the text at the actual Android width, font scale and style. The preview keeps the full value in TalkBack semantics even while visually clipped. **More** opens a token-styled dialog with selectable full text. Touch, mouse, Enter or Space open it; Close, Back, outside touch or Escape dismiss it. The trigger regains keyboard focus. Uncontrolled open state is saveable; controlled `expanded` requires `onExpandedChange` and caller restoration.

`BraceJsonFormatter.format` accepts null, strings, finite numbers, booleans, `Map<String, *>`, lists, arrays, `JSONObject` and `JSONArray`. It rejects cycles, non-string map keys, unsupported objects, non-finite numbers and nesting beyond 128 levels instead of producing invalid JSON. Maps keep their iteration order. Replace mutable maps or lists with a new instance when data changes so Compose can refresh the remembered formatted value. The default two-space indentation and unquoted top-level strings follow Blueprint; `omitQuotesOnStrings = false` adds JSON quotes. `indent = 0` produces compact JSON. `BraceJsonCell` formats a value and renders it with code typography, left-to-right JSON text, and the same reveal behavior. Null displays `null` and has no reveal action.

For a viewport table, pass a full text value to `BraceTableColumn.cellText` and use a formatter for visual `cellContent`:

```kotlin
val columns = listOf(
    BraceTableColumn<Record>("payload", "Payload", 180.dp,
        cellText = { BraceJsonFormatter.format(it.payload) },
        cellContent = { row -> BraceJsonCell(row.payload, maxCharacters = 24,
            revealMode = BraceRevealMode.Never) }),
)
```

`BraceDataTable` intentionally owns its cell pointer gesture and one native accessibility node. `cellText` therefore carries the complete spoken and copied value, while `cellContent` supplies the shortened visual value. A separate reveal action inside a viewport cell is not yet exposed; place `BraceJsonCell` or `BraceTruncatedCell` outside the table for an interactive full-value dialog. This integration limit keeps the rows in progress. The catalog includes standalone reveal samples and table formatting, and the independent Maven consumer compiles both APIs. Device tests cover Unicode truncation, measured overflow, JSON formatting, touch/mouse/keyboard reveal, focus, RTL, large text, high contrast and table cell accessibility. Manual TalkBack and release review remain.
