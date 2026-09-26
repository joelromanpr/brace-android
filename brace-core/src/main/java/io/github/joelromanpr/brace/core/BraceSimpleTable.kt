package io.github.joelromanpr.brace.core

import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.CollectionItemInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.collectionItemInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntrinsicSize
import androidx.compose.ui.unit.LayoutDirection
import io.github.braceandroid.foundation.BraceDensity
import io.github.braceandroid.foundation.BraceTheme
import io.github.braceandroid.foundation.BraceTokenDefaults

/** A labeled column in a small, fully composed [BraceSimpleTable]. */
@Immutable
public data class BraceSimpleTableColumn(
    val key: String,
    val heading: String,
    val minWidth: Dp? = null,
)

/**
 * One small-table row. [values] uses column keys; missing values are displayed as empty cells.
 * [key] must be unique within the table. Disabled rows remain readable but cannot activate.
 */
public data class BraceSimpleTableRow(
    val key: String,
    val values: Map<String, String>,
    val enabled: Boolean = true,
)

/**
 * A structured, fully composed table for a small set of records.
 *
 * This is the Android adaptation of Blueprint core's CSS-only `HTMLTable`. [bordered],
 * [striped], [interactive], and [compact] correspond to its styling flags. [compact] defaults
 * to the surrounding [BraceTheme] density. Table colors, dimensions and type come from Brace
 * tokens; cells expand at large font sizes and overflow horizontally when their minimum widths
 * exceed the viewport. The caller supplies a concise [label] for the accessibility collection.
 *
 * Each header and body cell is a keyboard focus stop. Arrow keys move between cells in logical
 * reading order, including RTL. TalkBack receives collection coordinates, column headings, and
 * full cell values. If [onRowClick] is provided, every enabled body cell activates its row with
 * touch, mouse, Enter, or Space. [selectedRowKey] is caller-owned state, retained across screen
 * recreation by the caller. [cellContent] may replace the visual text; [BraceSimpleTableRow.values] still supplies
 * its spoken label. Avoid nesting interactive controls inside a cell. Use `brace-table` for
 * viewport rendering, editing, resizing, large datasets, and cell-range selection.
 */
@Composable
public fun BraceSimpleTable(
    columns: List<BraceSimpleTableColumn>,
    rows: List<BraceSimpleTableRow>,
    label: String,
    modifier: Modifier = Modifier,
    bordered: Boolean = false,
    striped: Boolean = false,
    interactive: Boolean = false,
    compact: Boolean? = null,
    selectedRowKey: String? = null,
    onRowClick: ((String) -> Unit)? = null,
    cellContent: (@Composable (BraceSimpleTableRow, BraceSimpleTableColumn) -> Unit)? = null,
) {
    require(label.isNotBlank()) { "BraceSimpleTable needs a nonblank accessibility label" }
    require(columns.isNotEmpty()) { "BraceSimpleTable needs at least one column" }
    require(columns.all { it.key.isNotBlank() && it.heading.isNotBlank() &&
        (it.minWidth == null || it.minWidth > Dp.Hairline) }) { "Columns need keys, headings, and positive widths" }
    require(columns.map { it.key }.distinct().size == columns.size) { "Column keys must be unique" }
    require(rows.map { it.key }.distinct().size == rows.size) { "Row keys must be unique" }
    require(rows.all { it.key.isNotBlank() }) { "Row keys cannot be blank" }
    val table = BraceTheme.colors.components.table
    val semantic = BraceTheme.colors.semantic
    val metrics = BraceTheme.componentMetrics.table
    val isCompact = compact ?: (BraceTheme.density == BraceDensity.Compact)
    val minHeight = if (isCompact) BraceTokenDefaults.compact.rowHeightDp else
        BraceTokenDefaults.comfortable.rowHeightDp
    val verticalPadding = if (isCompact) BraceTheme.spacing.xs else BraceTheme.spacing.sm
    val actionHeight = BraceTheme.sizing.touchTarget
    val direction = LocalLayoutDirection.current
    val requesters = remember(columns.map { it.key }, rows.map { it.key }) {
        List(rows.size + 1) { List(columns.size) { FocusRequester() } }
    }
    val scroll = rememberScrollState()
    val activateRow = stringResource(R.string.brace_simple_table_activate_row)

    BoxWithConstraints(modifier.fillMaxWidth().semantics {
        collectionInfo = CollectionInfo(rows.size + 1, columns.size)
        contentDescription = label
        isTraversalGroup = true
    }.testTag("brace-simple-table")) {
        val minimums = columns.map { it.minWidth ?: BraceTheme.sizing.tableMinColumnWidth }
        val minimumTotal = minimums.fold(BraceTheme.spacing.none) { sum, width -> sum + width }
        val extra = (maxWidth - minimumTotal).coerceAtLeast(BraceTheme.spacing.none) / columns.size
        val widths = minimums.map { it + extra }
        val tableWidth = widths.fold(BraceTheme.spacing.none) { sum, width -> sum + width }
        Column(Modifier.horizontalScroll(scroll).width(tableWidth)) {
            Row(Modifier.height(IntrinsicSize.Min).background(table.header)) {
                columns.forEachIndexed { columnIndex, column ->
                    var focused by remember { mutableStateOf(false) }
                    Box(
                        Modifier.width(widths[columnIndex]).fillMaxHeight()
                            .defaultMinSize(minHeight = minHeight)
                            .then(if (bordered || focused) Modifier.border(
                                if (focused) BraceTheme.sizing.focusRingWidth else metrics.gridLineWidth,
                                if (focused) semantic.focusRing else table.gridLine,
                            ) else Modifier)
                            .focusRequester(requesters[0][columnIndex])
                            .onFocusChanged { focused = it.isFocused }
                            .onPreviewKeyEvent { event ->
                                if (event.type != KeyEventType.KeyDown || event.isAltPressed ||
                                    event.isCtrlPressed || event.isMetaPressed) return@onPreviewKeyEvent false
                                val next = nextTableCell(event.nativeKeyEvent.keyCode, 0, columnIndex,
                                    rows.lastIndex + 1, columns.lastIndex, direction) ?: return@onPreviewKeyEvent false
                                requesters[next.first][next.second].requestFocus()
                                true
                            }
                            .semantics(mergeDescendants = true) {
                                collectionItemInfo = CollectionItemInfo(0, 1, columnIndex, 1)
                                contentDescription = column.heading
                                heading()
                            }
                            .focusable()
                            .testTag("brace-simple-table-header:${column.key}"),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        Text(column.heading, color = table.headerContent,
                            style = BraceTheme.typography.label,
                            modifier = Modifier.padding(horizontal = metrics.cellHorizontalPadding,
                                vertical = verticalPadding).clearAndSetSemantics { })
                    }
                }
            }
            if (!bordered) HorizontalDivider(thickness = metrics.gridLineWidth, color = table.gridLine)
            rows.forEachIndexed { rowIndex, row ->
                val interaction = remember(row.key) { MutableInteractionSource() }
                val hovered by interaction.collectIsHoveredAsState()
                val rowSelected = selectedRowKey == row.key
                val rowBackground = when {
                    rowSelected -> table.selectedRow
                    interactive && hovered -> semantic.hover
                    striped && rowIndex % 2 == 1 -> table.alternateRow
                    else -> table.row
                }
                val contentColor = when {
                    !row.enabled -> semantic.disabledContent
                    rowSelected -> semantic.onSelection
                    else -> semantic.onSurface
                }
                Row(
                    Modifier.height(IntrinsicSize.Min).background(rowBackground)
                        .hoverable(interaction, enabled = interactive && row.enabled),
                ) {
                    columns.forEachIndexed { columnIndex, column ->
                        val value = row.values[column.key].orEmpty()
                        val spokenCell = stringResource(R.string.brace_simple_table_cell,
                            column.heading, rowIndex + 1, value)
                        var focused by remember { mutableStateOf(false) }
                        val cellModifier = Modifier.width(widths[columnIndex]).fillMaxHeight()
                            .defaultMinSize(minHeight = if (onRowClick != null) actionHeight else minHeight)
                            .then(if (bordered || focused) Modifier.border(
                                if (focused) BraceTheme.sizing.focusRingWidth else metrics.gridLineWidth,
                                if (focused) semantic.focusRing else table.gridLine,
                            ) else Modifier)
                            .focusRequester(requesters[rowIndex + 1][columnIndex])
                            .onFocusChanged { focused = it.isFocused }
                            .onPreviewKeyEvent { event ->
                                if (event.type != KeyEventType.KeyDown || event.isAltPressed ||
                                    event.isCtrlPressed || event.isMetaPressed) return@onPreviewKeyEvent false
                                val next = nextTableCell(event.nativeKeyEvent.keyCode, rowIndex + 1,
                                    columnIndex, rows.lastIndex + 1, columns.lastIndex, direction)
                                    ?: return@onPreviewKeyEvent false
                                requesters[next.first][next.second].requestFocus()
                                true
                            }
                            .then(if (onRowClick != null) Modifier.clickable(enabled = row.enabled,
                                role = Role.Button, onClickLabel = activateRow) { onRowClick(row.key) } else Modifier)
                            .semantics(mergeDescendants = true) {
                                collectionItemInfo = CollectionItemInfo(rowIndex + 1, 1, columnIndex, 1)
                                contentDescription = spokenCell
                                this.selected = rowSelected
                                if (!row.enabled) disabled()
                            }
                            .then(if (onRowClick == null || !row.enabled) Modifier.focusable() else Modifier)
                            .testTag("brace-simple-table-cell:${row.key}:${column.key}")
                        Box(cellModifier, contentAlignment = Alignment.CenterStart) {
                            if (cellContent == null) {
                                Text(value, color = contentColor, style = BraceTheme.typography.body,
                                    modifier = Modifier.padding(horizontal = metrics.cellHorizontalPadding,
                                        vertical = verticalPadding).clearAndSetSemantics { })
                            } else {
                                Box(Modifier.fillMaxWidth().padding(horizontal = metrics.cellHorizontalPadding,
                                    vertical = verticalPadding).clearAndSetSemantics { }) {
                                    cellContent(row, column)
                                }
                            }
                        }
                    }
                }
                if (!bordered) HorizontalDivider(thickness = metrics.gridLineWidth, color = table.gridLine)
            }
        }
    }
}

private fun nextTableCell(
    keyCode: Int,
    row: Int,
    column: Int,
    lastRow: Int,
    lastColumn: Int,
    direction: LayoutDirection,
): Pair<Int, Int>? {
    val next = when (keyCode) {
        AndroidKeyEvent.KEYCODE_DPAD_UP -> (row - 1).coerceAtLeast(0) to column
        AndroidKeyEvent.KEYCODE_DPAD_DOWN -> (row + 1).coerceAtMost(lastRow) to column
        AndroidKeyEvent.KEYCODE_DPAD_LEFT -> row to (column +
            if (direction == LayoutDirection.Rtl) 1 else -1).coerceIn(0, lastColumn)
        AndroidKeyEvent.KEYCODE_DPAD_RIGHT -> row to (column +
            if (direction == LayoutDirection.Rtl) -1 else 1).coerceIn(0, lastColumn)
        AndroidKeyEvent.KEYCODE_MOVE_HOME -> row to 0
        AndroidKeyEvent.KEYCODE_MOVE_END -> row to lastColumn
        else -> null
    }
    return next?.takeUnless { it.first == row && it.second == column }
}
