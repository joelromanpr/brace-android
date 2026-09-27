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
import androidx.compose.ui.unit.dp
import io.github.braceandroid.foundation.BraceTheme
import io.github.joelromanpr.brace.core.BraceButton
import io.github.joelromanpr.brace.core.BraceButtonVariant
import io.github.joelromanpr.brace.table.BraceDataTable
import io.github.joelromanpr.brace.table.BraceTableColumn
import io.github.joelromanpr.brace.table.BraceTableSelection

private data class DemoTableRecord(val id: String, val case: String, val status: String)

/** Interactive 120-row table with controlled selection and resizing. */
@Composable
internal fun TableCatalogSample() {
    val records = remember { List(120) { DemoTableRecord("record-$it", "Case ${1000 + it}", if (it % 3 == 0) "Review" else "Ready") } }
    val tableColumns = remember { listOf(
        BraceTableColumn<DemoTableRecord>("case", "Case", 140.dp, { it.case }),
        BraceTableColumn<DemoTableRecord>("status", "Status", 130.dp, { it.status }),
        BraceTableColumn<DemoTableRecord>("owner", "Owner", 130.dp, { "Team ${(it.id.substringAfter('-').toInt() % 4) + 1}" }),
    ) }
    var selectedKind by rememberSaveable { mutableStateOf("none") }
    var selectedRow by rememberSaveable { mutableStateOf("") }
    var selectedColumn by rememberSaveable { mutableStateOf("") }
    var extentRow by rememberSaveable { mutableStateOf("") }
    var extentColumn by rememberSaveable { mutableStateOf("") }
    var columnWidths by remember { mutableStateOf<Map<String, androidx.compose.ui.unit.Dp>>(emptyMap()) }
    var rowHeights by remember { mutableStateOf<Map<String, androidx.compose.ui.unit.Dp>>(emptyMap()) }
    val selection = when (selectedKind) {
        "cell" -> BraceTableSelection.Cell(selectedRow, selectedColumn)
        "row" -> BraceTableSelection.Row(selectedRow)
        "column" -> BraceTableSelection.Column(selectedColumn)
        "range" -> BraceTableSelection.Range(selectedRow, selectedColumn, extentRow, extentColumn)
        else -> null
    }
    val rowNames = remember(records) { records.associate { it.id to it.case } }
    val columnNames = remember(tableColumns) { tableColumns.associate { it.key to it.title } }
    val selectionSummary = when (selection) {
        is BraceTableSelection.Cell -> "${rowNames[selection.rowKey]} · ${columnNames[selection.columnKey]}"
        is BraceTableSelection.Row -> "Row ${rowNames[selection.rowKey]}"
        is BraceTableSelection.Column -> "Column ${columnNames[selection.columnKey]}"
        is BraceTableSelection.Range ->
            "${rowNames[selection.anchorRowKey]} · ${columnNames[selection.anchorColumnKey]} → " +
                "${rowNames[selection.extentRowKey]} · ${columnNames[selection.extentColumnKey]}"
        null -> "None"
    }
    Column(verticalArrangement = Arrangement.spacedBy(BraceTheme.spacing.sm)) {
        Text("Scroll both ways. Tap headers to select a row or column. Long-press a cell then tap an endpoint for a range; keyboard Shift+arrows extend it. Drag or focus resize grips.",
            color = BraceTheme.colors.semantic.onSurfaceMuted, style = BraceTheme.typography.body)
        BraceDataTable(records, { it.id }, tableColumns, selection, {
            when (it) {
                is BraceTableSelection.Cell -> {
                    selectedKind = "cell"; selectedRow = it.rowKey; selectedColumn = it.columnKey
                }
                is BraceTableSelection.Row -> { selectedKind = "row"; selectedRow = it.rowKey }
                is BraceTableSelection.Column -> { selectedKind = "column"; selectedColumn = it.columnKey }
                is BraceTableSelection.Range -> {
                    selectedKind = "range"; selectedRow = it.anchorRowKey; selectedColumn = it.anchorColumnKey
                    extentRow = it.extentRowKey; extentColumn = it.extentColumnKey
                }
            }
        }, modifier = Modifier.fillMaxWidth(), height = 260.dp, label = "Cases", rowLabel = { it.case },
            columnWidths = columnWidths,
            onColumnWidthChange = { key, width -> columnWidths = columnWidths + (key to width) },
            rowHeights = rowHeights,
            onRowHeightChange = { key, height -> rowHeights = rowHeights + (key to height) })
        Text("Selection: $selectionSummary", color = BraceTheme.colors.semantic.onSurface,
            style = BraceTheme.typography.body)
        Row(horizontalArrangement = Arrangement.spacedBy(BraceTheme.spacing.sm)) {
            BraceButton("Clear selection", onClick = { selectedKind = "none" }, variant = BraceButtonVariant.Outline)
            BraceButton("Reset sizes", onClick = { columnWidths = emptyMap(); rowHeights = emptyMap() },
                variant = BraceButtonVariant.Outline)
        }
    }
}
