package io.github.joelromanpr.brace.table

/**
 * A key-based table region. Bounds are inclusive and follow the current row and column order.
 * Stable keys let a region survive a host-owned sort or data refresh; a missing bound makes
 * the whole controlled selection stale until its owner updates it.
 *
 * This is Brace's Android adaptation of Blueprint's index-based `Region` cardinalities.
 */
sealed interface BraceTableRegion {
    /** A rectangular group of cells, including both endpoints. */
    data class Cells(
        val anchorRowKey: String,
        val anchorColumnKey: String,
        val extentRowKey: String = anchorRowKey,
        val extentColumnKey: String = anchorColumnKey,
    ) : BraceTableRegion {
        init {
            require(listOf(anchorRowKey, anchorColumnKey, extentRowKey, extentColumnKey).all(String::isNotBlank)) {
                "Cell region keys must not be blank"
            }
        }
    }

    /** Every column in the inclusive row interval. */
    data class Rows(val firstRowKey: String, val lastRowKey: String = firstRowKey) : BraceTableRegion {
        init { require(firstRowKey.isNotBlank() && lastRowKey.isNotBlank()) { "Row region keys must not be blank" } }
    }

    /** Every row in the inclusive column interval. */
    data class Columns(val firstColumnKey: String, val lastColumnKey: String = firstColumnKey) : BraceTableRegion {
        init { require(firstColumnKey.isNotBlank() && lastColumnKey.isNotBlank()) { "Column region keys must not be blank" } }
    }

    /** Every cell, row header, and column header in the current table. */
    data object Table : BraceTableRegion
}

/** Immutable operations for building a controlled [BraceTableSelection.Regions] value. */
object BraceTableRegions {
    /** Append [region] unless it is already present. Legacy single selections are preserved. */
    fun add(selection: BraceTableSelection?, region: BraceTableRegion): BraceTableSelection.Regions {
        val existing = when (selection) {
            is BraceTableSelection.Regions -> selection.regions
            is BraceTableSelection.Cell -> listOf(BraceTableRegion.Cells(selection.rowKey, selection.columnKey))
            is BraceTableSelection.Row -> listOf(BraceTableRegion.Rows(selection.rowKey))
            is BraceTableSelection.Column -> listOf(BraceTableRegion.Columns(selection.columnKey))
            is BraceTableSelection.Range -> listOf(BraceTableRegion.Cells(
                selection.anchorRowKey, selection.anchorColumnKey,
                selection.extentRowKey, selection.extentColumnKey,
            ))
            null -> emptyList()
        }
        return BraceTableSelection.Regions(if (region in existing) existing else existing + region)
    }

    /** Replace the last region, preserving the earlier disjoint regions. */
    fun updateLast(selection: BraceTableSelection.Regions, region: BraceTableRegion): BraceTableSelection.Regions =
        BraceTableSelection.Regions(selection.regions.dropLast(1) + region)
}

internal data class ResolvedTableRegion(
    val rows: IntRange,
    val columns: IntRange,
    val fullRows: Boolean,
    val fullColumns: Boolean,
) {
    fun contains(row: Int, column: Int): Boolean = row in rows && column in columns
}

/** Return null for any missing bound: stale selections never silently copy a smaller region. */
internal fun resolveTableRegions(
    regions: List<BraceTableRegion>,
    rowIndexes: Map<String, Int>,
    columnIndexes: Map<String, Int>,
    rowCount: Int,
    columnCount: Int,
): List<ResolvedTableRegion>? {
    if (rowCount == 0 || columnCount == 0) return null
    return regions.map { region ->
        when (region) {
            is BraceTableRegion.Cells -> {
                val firstRow = rowIndexes[region.anchorRowKey] ?: return null
                val lastRow = rowIndexes[region.extentRowKey] ?: return null
                val firstColumn = columnIndexes[region.anchorColumnKey] ?: return null
                val lastColumn = columnIndexes[region.extentColumnKey] ?: return null
                ResolvedTableRegion(
                    minOf(firstRow, lastRow)..maxOf(firstRow, lastRow),
                    minOf(firstColumn, lastColumn)..maxOf(firstColumn, lastColumn),
                    fullRows = false, fullColumns = false,
                )
            }
            is BraceTableRegion.Rows -> {
                val first = rowIndexes[region.firstRowKey] ?: return null
                val last = rowIndexes[region.lastRowKey] ?: return null
                ResolvedTableRegion(minOf(first, last)..maxOf(first, last), 0 until columnCount,
                    fullRows = true, fullColumns = false)
            }
            is BraceTableRegion.Columns -> {
                val first = columnIndexes[region.firstColumnKey] ?: return null
                val last = columnIndexes[region.lastColumnKey] ?: return null
                ResolvedTableRegion(0 until rowCount, minOf(first, last)..maxOf(first, last),
                    fullRows = false, fullColumns = true)
            }
            BraceTableRegion.Table -> ResolvedTableRegion(0 until rowCount, 0 until columnCount,
                fullRows = true, fullColumns = true)
        }
    }
}
