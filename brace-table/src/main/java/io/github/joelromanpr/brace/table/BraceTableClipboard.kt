package io.github.joelromanpr.brace.table

/**
 * Formats a controlled table selection for copying as plain text.
 *
 * Cells are ordered by their current visible row and column order. A row or range becomes
 * tab-separated lines; tabs, line breaks, and quotes inside values are quoted so paste targets
 * can distinguish them from cell boundaries. Column titles and row keys are not included.
 * Return `null` when the selection refers to data no longer present in the table.
 */
object BraceTableClipboard {
    /** Format [selection] from the current [rows] and [columns], or return null if it is stale. */
    fun <Row> formatSelection(
        rows: List<Row>,
        rowKey: (Row) -> String,
        columns: List<BraceTableColumn<Row>>,
        selection: BraceTableSelection?,
    ): String? {
        if (selection == null || rows.isEmpty() || columns.isEmpty()) return null
        val rowIndices = validateRowKeys(rows, rowKey).byKey
        return formatSelectionWithIndexes(rows, columns, selection, rowIndices)
    }

    /** The table reuses its remembered row and column key indexes for focused copying. */
    internal fun <Row> formatSelectionWithIndexes(
        rows: List<Row>,
        columns: List<BraceTableColumn<Row>>,
        selection: BraceTableSelection?,
        rowIndices: Map<String, Int>,
        columnIndices: Map<String, Int> = columns.indices.associateBy { columns[it].key },
    ): String? {
        if (selection == null || rows.isEmpty() || columns.isEmpty()) return null
        require(columnIndices.size == columns.size) { "Column keys must be unique" }
        val selectedRows: IntRange
        val selectedColumns: IntRange
        when (selection) {
            is BraceTableSelection.Cell -> {
                val row = rowIndices[selection.rowKey] ?: return null
                val column = columnIndices[selection.columnKey] ?: return null
                selectedRows = row..row
                selectedColumns = column..column
            }
            is BraceTableSelection.Row -> {
                val row = rowIndices[selection.rowKey] ?: return null
                selectedRows = row..row
                selectedColumns = columns.indices
            }
            is BraceTableSelection.Column -> {
                val column = columnIndices[selection.columnKey] ?: return null
                selectedRows = rows.indices
                selectedColumns = column..column
            }
            is BraceTableSelection.Range -> {
                val startRow = rowIndices[selection.anchorRowKey] ?: return null
                val endRow = rowIndices[selection.extentRowKey] ?: return null
                val startColumn = columnIndices[selection.anchorColumnKey] ?: return null
                val endColumn = columnIndices[selection.extentColumnKey] ?: return null
                selectedRows = minOf(startRow, endRow)..maxOf(startRow, endRow)
                selectedColumns = minOf(startColumn, endColumn)..maxOf(startColumn, endColumn)
            }
        }
        return selectedRows.joinToString("\n") { rowIndex ->
            selectedColumns.joinToString("\t") { columnIndex ->
                quote(columns[columnIndex].cellText(rows[rowIndex]))
            }
        }
    }

    private fun quote(value: String): String =
        if (value.any { it == '\t' || it == '\n' || it == '\r' || it == '"' })
            "\"${value.replace("\"", "\"\"")}\"" else value
}
