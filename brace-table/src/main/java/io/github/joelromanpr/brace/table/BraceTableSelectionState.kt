package io.github.joelromanpr.brace.table

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable

/**
 * Saves a mutable controlled selection using only primitive strings and stable row and column
 * keys. The state wrapper preserves an explicitly cleared (`null`) selection across recreation.
 * A restored region is re-resolved against current data when [BraceDataTable] renders it.
 */
val BraceTableSelectionSaver: Saver<MutableState<BraceTableSelection?>, ArrayList<String>> = Saver(
    save = { state -> ArrayList(encodeTableSelection(state.value)) },
    restore = { saved -> mutableStateOf(decodeTableSelection(saved)) },
)

/** Remember a controlled selection across configuration changes and process recreation. */
@Composable
fun rememberBraceTableSelection(initial: BraceTableSelection? = null): MutableState<BraceTableSelection?> =
    rememberSaveable(saver = BraceTableSelectionSaver) { mutableStateOf(initial) }

private fun encodeTableSelection(selection: BraceTableSelection?): List<String> = when (selection) {
    null -> listOf("none")
    is BraceTableSelection.Cell -> listOf("cell", selection.rowKey, selection.columnKey)
    is BraceTableSelection.Row -> listOf("row", selection.rowKey)
    is BraceTableSelection.Column -> listOf("column", selection.columnKey)
    is BraceTableSelection.Range -> listOf("range", selection.anchorRowKey,
        selection.anchorColumnKey, selection.extentRowKey, selection.extentColumnKey)
    is BraceTableSelection.Regions -> buildList {
        add("regions")
        add(selection.regions.size.toString())
        selection.regions.forEach { region ->
            when (region) {
                is BraceTableRegion.Cells -> addAll(listOf("cells", region.anchorRowKey,
                    region.anchorColumnKey, region.extentRowKey, region.extentColumnKey))
                is BraceTableRegion.Rows -> addAll(listOf("rows", region.firstRowKey, region.lastRowKey))
                is BraceTableRegion.Columns -> addAll(listOf("columns", region.firstColumnKey, region.lastColumnKey))
                BraceTableRegion.Table -> add("table")
            }
        }
    }
}

private fun decodeTableSelection(saved: List<String>): BraceTableSelection? = runCatching {
    if (saved.isEmpty()) return@runCatching null
    var index = 0
    fun next(): String = saved[index++]
    val result = when (next()) {
        "none" -> null
        "cell" -> BraceTableSelection.Cell(next(), next())
        "row" -> BraceTableSelection.Row(next())
        "column" -> BraceTableSelection.Column(next())
        "range" -> BraceTableSelection.Range(next(), next(), next(), next())
        "regions" -> {
            val count = next().toInt()
            require(count in 1..saved.size) { "Invalid region count" }
            BraceTableSelection.Regions(List(count) {
                when (next()) {
                    "cells" -> BraceTableRegion.Cells(next(), next(), next(), next())
                    "rows" -> BraceTableRegion.Rows(next(), next())
                    "columns" -> BraceTableRegion.Columns(next(), next())
                    "table" -> BraceTableRegion.Table
                    else -> error("Invalid region kind")
                }
            })
        }
        else -> error("Invalid selection kind")
    }
    require(index == saved.size) { "Trailing selection data" }
    result
}.getOrNull()
