package io.github.joelromanpr.brace.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import io.github.braceandroid.foundation.BraceTheme
import io.github.joelromanpr.brace.core.BraceButton
import io.github.joelromanpr.brace.core.BraceButtonVariant
import io.github.joelromanpr.brace.table.BraceDataTable
import io.github.joelromanpr.brace.table.BraceTableColumn
import io.github.joelromanpr.brace.table.BraceTableClipboard
import io.github.joelromanpr.brace.table.BraceTableSelection
import io.github.joelromanpr.brace.table.BraceTableRegion
import io.github.joelromanpr.brace.table.BraceTableRegions
import io.github.joelromanpr.brace.table.BraceTableReorder
import io.github.joelromanpr.brace.table.rememberBraceTableSelection

private data class DemoTableRecord(val id: String, val case: String, val status: String)

/** Interactive 120-row table with selection, resizing, copying, cell editing, and header renaming. */
@Composable
internal fun TableCatalogSample() {
    var records by remember { mutableStateOf(List(120) { DemoTableRecord("record-$it", "Case ${1000 + it}", if (it % 3 == 0) "Review" else "Ready") }) }
    var columnTitles by remember { mutableStateOf(mapOf("case" to "Case", "status" to "Status", "owner" to "Owner")) }
    var columnOrder by remember { mutableStateOf(listOf("case", "status", "owner")) }
    val tableColumns = remember(columnTitles, columnOrder) { BraceTableReorder.applyOrder(listOf(
        BraceTableColumn<DemoTableRecord>("case", columnTitles.getValue("case"), 140.dp,
            { it.case }, editable = true, editableName = true),
        BraceTableColumn<DemoTableRecord>("status", columnTitles.getValue("status"), 130.dp,
            { it.status },
            cellContent = { row -> Text("● ${row.status}", style = BraceTheme.typography.body) },
            editable = true, editableName = true,
            headerContent = { Text("◆ ${columnTitles.getValue("status")}",
                style = BraceTheme.typography.label) }),
        BraceTableColumn<DemoTableRecord>("owner", columnTitles.getValue("owner"), 130.dp,
            { "Team ${(it.id.substringAfter('-').toInt() % 4) + 1}" }, editableName = true),
    ), { it.key }, columnOrder) }
    var selection by rememberBraceTableSelection()
    var columnWidths by remember { mutableStateOf<Map<String, androidx.compose.ui.unit.Dp>>(emptyMap()) }
    var rowHeights by remember { mutableStateOf<Map<String, androidx.compose.ui.unit.Dp>>(emptyMap()) }
    val catalogClipboard = LocalClipboardManager.current
    var copiedPreview by remember { mutableStateOf<String?>(null) }
    var editingRow by rememberSaveable { mutableStateOf("") }
    var editingColumn by rememberSaveable { mutableStateOf("") }
    var savedValue by rememberSaveable { mutableStateOf<String?>(null) }
    var editingName by rememberSaveable { mutableStateOf("") }
    var savedColumnTitle by rememberSaveable { mutableStateOf<String?>(null) }
    val editingCell = if (editingRow.isBlank() || editingColumn.isBlank()) null
        else BraceTableSelection.Cell(editingRow, editingColumn)
    val rowNames = remember(records) { records.associate { it.id to it.case } }
    val columnNames = remember(tableColumns) { tableColumns.associate { it.key to it.title } }
    val selectionSummary = when (selection) {
        is BraceTableSelection.Cell -> "${rowNames[selection.rowKey]} · ${columnNames[selection.columnKey]}"
        is BraceTableSelection.Row -> "Row ${rowNames[selection.rowKey]}"
        is BraceTableSelection.Column -> "Column ${columnNames[selection.columnKey]}"
        is BraceTableSelection.Range ->
            "${rowNames[selection.anchorRowKey]} · ${columnNames[selection.anchorColumnKey]} → " +
                "${rowNames[selection.extentRowKey]} · ${columnNames[selection.extentColumnKey]}"
        is BraceTableSelection.Regions -> if (BraceTableRegion.Table in selection.regions)
            "Entire table" else "${selection.regions.size} regions"
        null -> "None"
    }
    Column(verticalArrangement = Arrangement.spacedBy(BraceTheme.spacing.sm)) {
        Text("Scroll both ways. Tap headers to select a row or column. Ctrl/Cmd+click adds a region, and Ctrl/Cmd+A selects all. Drag or focus the header grips to reorder. Long-press a cell then tap an endpoint for a range; keyboard Shift+arrows extend it. Double-tap an editable cell or column header, press Enter/F2, or use an Edit action. Drag or focus resize grips.",
            color = BraceTheme.colors.semantic.onSurfaceMuted, style = BraceTheme.typography.body)
        BraceDataTable(records, { it.id }, tableColumns, selection, { selection = it        }, modifier = Modifier.fillMaxWidth(), height = 260.dp, label = "Cases", rowLabel = { it.case },
            rowHeaderContent = { _, index -> Text("R${index + 1}", style = BraceTheme.typography.label) },
            onRowOrderChange = { order -> records = BraceTableReorder.applyOrder(records, { it.id }, order) },
            onColumnOrderChange = { order -> columnOrder = order },
            columnWidths = columnWidths,
            onColumnWidthChange = { key, width -> columnWidths = columnWidths + (key to width) },
            rowHeights = rowHeights,
            onRowHeightChange = { key, height -> rowHeights = rowHeights + (key to height) },
            editingCell = editingCell,
            onEditingCellChange = { cell ->
                editingRow = cell?.rowKey.orEmpty()
                editingColumn = cell?.columnKey.orEmpty()
                if (cell != null) editingName = ""
            },
            onCellCommit = { cell, value ->
                records = records.map { record -> if (record.id != cell.rowKey) record
                    else when (cell.columnKey) {
                        "case" -> record.copy(case = value)
                        "status" -> record.copy(status = value)
                        else -> record
                    } }
                savedValue = value
            },
            validateCell = { _, value -> if (value.isBlank()) "Enter a value" else null },
            editingColumnName = editingName.ifBlank { null },
            onEditingColumnNameChange = { key ->
                editingName = key.orEmpty()
                if (key != null) { editingRow = ""; editingColumn = "" }
            },
            onColumnNameCommit = { key, title ->
                columnTitles = columnTitles + (key to title)
                savedColumnTitle = "$key: $title"
            },
            validateColumnName = { _, title -> if (title.length < 3) "Use at least 3 characters" else null })
        Text("Selection: $selectionSummary", color = BraceTheme.colors.semantic.onSurface,
            style = BraceTheme.typography.body)
        Row(horizontalArrangement = Arrangement.spacedBy(BraceTheme.spacing.sm)) {
            BraceButton("Clear selection", onClick = { selection = null }, variant = BraceButtonVariant.Outline)
            BraceButton("Select all", onClick = {
                selection = BraceTableSelection.Regions(listOf(BraceTableRegion.Table))
            }, variant = BraceButtonVariant.Outline)
            BraceButton("Add review row", onClick = {
                selection = BraceTableRegions.add(selection, BraceTableRegion.Rows("record-3"))
            }, variant = BraceButtonVariant.Outline)
            BraceButton("Reset sizes", onClick = { columnWidths = emptyMap(); rowHeights = emptyMap() },
                variant = BraceButtonVariant.Outline)
        }
        BraceButton("Edit selected cell", onClick = {
            (selection as? BraceTableSelection.Cell)?.let { cell ->
                if (cell.columnKey == "case" || cell.columnKey == "status") {
                    editingRow = cell.rowKey
                    editingColumn = cell.columnKey
                }
            }
        }, enabled = selection is BraceTableSelection.Cell &&
            (selection.columnKey == "case" || selection.columnKey == "status"),
            variant = BraceButtonVariant.Outline)
        BraceButton("Edit selected column name", onClick = {
            (selection as? BraceTableSelection.Column)?.let { editingName = it.columnKey; editingRow = ""; editingColumn = "" }
        }, enabled = selection is BraceTableSelection.Column, variant = BraceButtonVariant.Outline)
        savedColumnTitle?.let { Text("Renamed: $it", color = BraceTheme.colors.semantic.onSurfaceMuted,
            style = BraceTheme.typography.body) }
        savedValue?.let { Text("Saved: $it", color = BraceTheme.colors.semantic.onSurfaceMuted,
            style = BraceTheme.typography.body) }
        BraceButton("Copy selected cells", onClick = {
            BraceTableClipboard.formatSelection(records, { it.id }, tableColumns, selection)?.let { value ->
                catalogClipboard.setText(AnnotatedString(value))
                copiedPreview = value.replace("\n", " ↵ ").take(80)
            }
        }, enabled = selection != null, variant = BraceButtonVariant.Outline)
        copiedPreview?.let { Text("Copied: $it", color = BraceTheme.colors.semantic.onSurfaceMuted,
            style = BraceTheme.typography.body) }
    }
}
