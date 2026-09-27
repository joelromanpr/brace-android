package io.github.joelromanpr.brace.table

import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
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
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isShiftPressed as isPointerShiftPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.CollectionItemInfo
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.collectionItemInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import io.github.braceandroid.foundation.BraceTheme
import kotlinx.coroutines.launch

/**
 * A typed column with a stable [key], visible [title], and base [width].
 * [cellText] is required even with custom [cellContent] so TalkBack always receives the full value.
 * Keep custom content noninteractive until dedicated cell-action APIs are available.
 */
class BraceTableColumn<Row>(
    val key: String,
    val title: String,
    val width: Dp,
    val cellText: (Row) -> String,
    val cellContent: (@Composable (Row) -> Unit)? = null,
)

/** Controlled table selection identified by stable row and column keys. */
sealed interface BraceTableSelection {
    /** One selected cell. */
    data class Cell(val rowKey: String, val columnKey: String) : BraceTableSelection

    /** One selected row, activated through its fixed row header. */
    data class Row(val rowKey: String) : BraceTableSelection

    /** One selected column, activated through its fixed column header. */
    data class Column(val columnKey: String) : BraceTableSelection

    /** Inclusive rectangular selection. The anchor stays fixed during Shift+arrow extension. */
    data class Range(
        val anchorRowKey: String,
        val anchorColumnKey: String,
        val extentRowKey: String,
        val extentColumnKey: String,
    ) : BraceTableSelection
}

/** Saveable scroll positions for a [BraceDataTable]. A remembered viewport survives configuration changes. */
@Stable
class BraceTableViewport internal constructor(
    val horizontal: ScrollState,
    val vertical: LazyListState,
)

/** Remember both scroll axes using Compose's saveable scroll-state savers. */
@Composable
fun rememberBraceTableViewport(): BraceTableViewport {
    val horizontal = rememberScrollState()
    val vertical = rememberLazyListState()
    return remember(horizontal, vertical) { BraceTableViewport(horizontal, vertical) }
}

/**
 * A data-dense table with lazy rows, viewport-only columns, fixed column and row headers,
 * controlled selection, and one keyboard focus stop for arrow navigation.
 *
 * [rowKey] and column keys must be unique and stable across sorting and data refreshes.
 * Pass an immutable row list and replace it when data changes. Keys are checked once per list
 * replacement to avoid scanning all rows during viewport scroll.
 * Supply a human-readable [rowLabel] when row keys are opaque. [BraceTableColumn.cellText]
 * is also the TalkBack value when a column supplies custom [BraceTableColumn.cellContent].
 * Arrow keys move the selected cell, Home/End move within a row, and Page Up/Down move by a
 * viewport. Logical left/right movement follows layout direction. The viewport scrolls to
 * reveal a keyboard-selected cell. Row headers select one whole row. Provide a saveable
 * [selection] from the caller when selection should survive process recreation.
 *
 * Shift+arrow extends an inclusive rectangular selection. Long-press one cell and tap an
 * endpoint for touch range selection. Row and column headers select their full axis.
 * Optional controlled width/height maps and callbacks enable 48dp resize handles. Callers
 * must update those maps in response to callbacks for live visual resizing.
 * Editing, copying, disjoint selections, and frozen data regions are not yet exposed.
 */
@Composable
fun <Row> BraceDataTable(
    rows: List<Row>,
    rowKey: (Row) -> String,
    columns: List<BraceTableColumn<Row>>,
    selection: BraceTableSelection?,
    onSelectionChange: (BraceTableSelection) -> Unit,
    modifier: Modifier = Modifier,
    viewport: BraceTableViewport = rememberBraceTableViewport(),
    height: Dp = 320.dp,
    label: String? = null,
    rowLabel: (Row) -> String = rowKey,
    columnWidths: Map<String, Dp> = emptyMap(),
    onColumnWidthChange: ((String, Dp) -> Unit)? = null,
    rowHeights: Map<String, Dp> = emptyMap(),
    onRowHeightChange: ((String, Dp) -> Unit)? = null,
    maxColumnWidth: Dp? = null,
    maxRowHeight: Dp? = null,
) {
    remember(height, maxColumnWidth, maxRowHeight, columnWidths, rowHeights) {
        validateTableDimensions(height, maxColumnWidth, maxRowHeight, columnWidths, rowHeights)
    }
    val rowIndex = remember(rows) { validateRowKeys(rows, rowKey) }
    val rowIndexes = rowIndex.byKey
    val baseMinColumnWidth = BraceTheme.sizing.tableMinColumnWidth
    val minColumnWidth = maxOf(baseMinColumnWidth,
        if (onColumnWidthChange == null) 0.dp else BraceTheme.sizing.touchTarget * 2)
    val effectiveMaxColumnWidth = maxColumnWidth?.coerceAtLeast(minColumnWidth)

    val colors = BraceTheme.colors.components.table
    val semantic = BraceTheme.colors.semantic
    val metrics = BraceTheme.componentMetrics.table
    val spacing = BraceTheme.spacing
    val densityTokens = BraceTheme.densityTokens
    val typography = BraceTheme.typography
    val density = LocalDensity.current
    val direction = LocalLayoutDirection.current
    val scope = rememberCoroutineScope()
    val requester = remember { FocusRequester() }
    var focused by remember { mutableStateOf(false) }
    var viewportWidthPx by remember { mutableIntStateOf(0) }
    var viewportHeightPx by remember { mutableIntStateOf(0) }
    val bodyLineHeight = if (typography.body.lineHeight == TextUnit.Unspecified) 0.dp
        else with(density) { typography.body.lineHeight.toDp() }
    val headerLineHeight = if (typography.label.lineHeight == TextUnit.Unspecified) 0.dp
        else with(density) { typography.label.lineHeight.toDp() }
    val defaultRowHeight = maxOf(densityTokens.rowHeightDp + densityTokens.itemGapDp,
        BraceTheme.sizing.touchTarget, maxOf(bodyLineHeight, headerLineHeight) + spacing.sm)
    val effectiveMaxRowHeight = maxRowHeight?.coerceAtLeast(defaultRowHeight)
    val headerHeight = defaultRowHeight
    val rowHeaderName = stringResource(R.string.brace_table_row_header)
    val textMeasurer = rememberTextMeasurer()
    val rowHeaderTextWidth = with(density) {
        maxOf(
            textMeasurer.measure(rowHeaderName, style = typography.label, maxLines = 1).size.width,
            textMeasurer.measure(rows.size.toString(), style = typography.label, maxLines = 1).size.width,
        ).toDp()
    }
    val rowSelectWidth = maxOf(BraceTheme.sizing.touchTarget + spacing.sm,
        rowHeaderTextWidth + metrics.cellHorizontalPadding * 2)
    val rowHeaderWidth = rowSelectWidth + if (onRowHeightChange == null) 0.dp else BraceTheme.sizing.touchTarget
    val widths = remember(columns, columnWidths, minColumnWidth, effectiveMaxColumnWidth) {
        columns.map { column ->
            val minimumApplied = maxOf(minColumnWidth, columnWidths[column.key] ?: column.width)
            if (effectiveMaxColumnWidth == null) minimumApplied else minOf(effectiveMaxColumnWidth, minimumApplied)
        }
    }
    val columnIndex = remember(columns, widths, baseMinColumnWidth) {
        indexColumns(columns, baseMinColumnWidth, widths)
    }
    val totalWidth = rowHeaderWidth + columnIndex.totalWidth
    val tableLabel = label ?: stringResource(R.string.brace_table_default_label)
    require(tableLabel.isNotBlank()) { "Table label must not be blank" }
    val selectedAction = stringResource(R.string.brace_table_select)
    val extendRangeAction = stringResource(R.string.brace_table_extend_range)
    val emptyText = stringResource(R.string.brace_table_empty)
    val rowDescription = stringResource(R.string.brace_table_row_description)
    val cellDescription = stringResource(R.string.brace_table_cell_description)
    val headerDescription = stringResource(R.string.brace_table_column_description)
    val columnResizeDescription = stringResource(R.string.brace_table_column_resize)
    val rowResizeDescription = stringResource(R.string.brace_table_row_resize)
    val increaseSizeLabel = stringResource(R.string.brace_table_increase_size)
    val decreaseSizeLabel = stringResource(R.string.brace_table_decrease_size)
    val sizeMinDescription = stringResource(R.string.brace_table_size_min)
    val sizeRangeDescription = stringResource(R.string.brace_table_size_range)
    val rangeDescription = stringResource(R.string.brace_table_range_description)
    val columnSelectedDescription = stringResource(R.string.brace_table_selected_column_description)
    val cellSelectedDescription = stringResource(R.string.brace_table_selected_cell_description)
    val rowSelectedDescription = stringResource(R.string.brace_table_selected_row_description)
    fun validAnchor(anchor: BraceTableSelection.Cell?): BraceTableSelection.Cell? = anchor?.takeIf {
        it.rowKey in rowIndexes && it.columnKey in columnIndex.byKey
    }
    var pendingTouchRangeAnchor by remember { mutableStateOf<BraceTableSelection.Cell?>(null) }
    var focusedResizeHandleId by remember { mutableStateOf<String?>(null) }
    val range = (selection as? BraceTableSelection.Range)?.let { selected ->
        val anchorRow = rowIndexes[selected.anchorRowKey] ?: return@let null
        val extentRow = rowIndexes[selected.extentRowKey] ?: return@let null
        val anchorColumn = columnIndex.byKey[selected.anchorColumnKey] ?: return@let null
        val extentColumn = columnIndex.byKey[selected.extentColumnKey] ?: return@let null
        IntRange(minOf(anchorRow, extentRow), maxOf(anchorRow, extentRow)) to
            IntRange(minOf(anchorColumn, extentColumn), maxOf(anchorColumn, extentColumn))
    }
    val selectionAnnouncement = when (selection) {
        is BraceTableSelection.Range -> range?.let { (selectedRows, selectedColumns) ->
            rangeDescription.format(selectedRows.first + 1, selectedRows.last + 1,
                selectedColumns.first + 1, selectedColumns.last + 1)
        }
        is BraceTableSelection.Column -> columnIndex.byKey[selection.columnKey]?.let {
            columnSelectedDescription.format(it + 1)
        }
        is BraceTableSelection.Cell -> rowIndexes[selection.rowKey]?.let { rowNumber ->
            columnIndex.byKey[selection.columnKey]?.let { columnNumber ->
                cellSelectedDescription.format(columns[columnNumber].title, rowNumber + 1,
                    if (rowLabel === rowKey) rowIndex.keys[rowNumber] else rowLabel(rows[rowNumber]),
                    columns[columnNumber].cellText(rows[rowNumber]))
            }
        }
        is BraceTableSelection.Row -> rowIndexes[selection.rowKey]?.let { rowNumber ->
            rowSelectedDescription.format(rowNumber + 1,
                if (rowLabel === rowKey) rowIndex.keys[rowNumber] else rowLabel(rows[rowNumber]))
        }
        else -> null
    }

    BoxWithConstraints(
        modifier.fillMaxWidth().height(height)
            .background(colors.row)
            .border(if (focused) BraceTheme.sizing.focusRingWidth else metrics.gridLineWidth,
                if (focused) semantic.focusRing else colors.gridLine)
            .onSizeChanged { viewportWidthPx = it.width; viewportHeightPx = it.height }
            .focusRequester(requester)
            .onFocusChanged { focused = it.isFocused }
            .onPreviewKeyEvent { event ->
                if (focusedResizeHandleId != null || event.type != KeyEventType.KeyDown ||
                    rows.isEmpty() || columns.isEmpty() || event.isAltPressed ||
                    event.isCtrlPressed || event.isMetaPressed) return@onPreviewKeyEvent false
                val currentRow = when (selection) {
                    is BraceTableSelection.Cell -> rowIndex.byKey[selection.rowKey]
                    is BraceTableSelection.Row -> rowIndex.byKey[selection.rowKey]
                    is BraceTableSelection.Range -> rowIndex.byKey[selection.extentRowKey]
                    is BraceTableSelection.Column, null -> 0
                }?.coerceAtLeast(0) ?: 0
                val currentCol = when (selection) {
                    is BraceTableSelection.Cell -> columnIndex.byKey[selection.columnKey]
                    is BraceTableSelection.Column -> columnIndex.byKey[selection.columnKey]
                    is BraceTableSelection.Range -> columnIndex.byKey[selection.extentColumnKey]
                    is BraceTableSelection.Row, null -> 0
                }?.coerceAtLeast(0) ?: 0
                val pageBudgetPx = (viewportHeightPx - with(density) { headerHeight.roundToPx() })
                    .coerceAtLeast(1)
                fun pageJump(direction: Int): Int {
                    var index = currentRow + direction
                    var count = 0
                    var coveredPx = 0
                    while (index in rows.indices) {
                        val key = rowIndex.keys[index]
                        val requested = maxOf(defaultRowHeight, rowHeights[key] ?: defaultRowHeight)
                        val itemHeight = if (effectiveMaxRowHeight == null) requested else
                            minOf(effectiveMaxRowHeight, requested)
                        val rowPx = with(density) { itemHeight.roundToPx() }.coerceAtLeast(1)
                        if (count > 0 && coveredPx + rowPx > pageBudgetPx) break
                        coveredPx += rowPx
                        count++
                        index += direction
                    }
                    return count.coerceAtLeast(1)
                }
                val next = when (event.nativeKeyEvent.keyCode) {
                    AndroidKeyEvent.KEYCODE_DPAD_UP -> (currentRow - 1).coerceAtLeast(0) to currentCol
                    AndroidKeyEvent.KEYCODE_DPAD_DOWN -> (currentRow + 1).coerceAtMost(rows.lastIndex) to currentCol
                    AndroidKeyEvent.KEYCODE_DPAD_LEFT -> currentRow to
                        (currentCol + if (direction == LayoutDirection.Rtl) 1 else -1).coerceIn(0, columns.lastIndex)
                    AndroidKeyEvent.KEYCODE_DPAD_RIGHT -> currentRow to
                        (currentCol + if (direction == LayoutDirection.Rtl) -1 else 1).coerceIn(0, columns.lastIndex)
                    AndroidKeyEvent.KEYCODE_MOVE_HOME -> currentRow to 0
                    AndroidKeyEvent.KEYCODE_MOVE_END -> currentRow to columns.lastIndex
                    AndroidKeyEvent.KEYCODE_PAGE_UP -> (currentRow - pageJump(-1)).coerceAtLeast(0) to currentCol
                    AndroidKeyEvent.KEYCODE_PAGE_DOWN -> (currentRow + pageJump(1)).coerceAtMost(rows.lastIndex) to currentCol
                    else -> null
                } ?: return@onPreviewKeyEvent false
                val (targetRow, targetCol) = next
                val anchor = validAnchor(when (selection) {
                    is BraceTableSelection.Range -> BraceTableSelection.Cell(selection.anchorRowKey, selection.anchorColumnKey)
                    is BraceTableSelection.Cell -> selection
                    else -> null
                }) ?: BraceTableSelection.Cell(rowIndex.keys[currentRow], columns[currentCol].key)
                pendingTouchRangeAnchor = null
                onSelectionChange(if (event.isShiftPressed) BraceTableSelection.Range(
                    anchor.rowKey, anchor.columnKey, rowIndex.keys[targetRow], columns[targetCol].key,
                ) else BraceTableSelection.Cell(rowIndex.keys[targetRow], columns[targetCol].key))
                scope.launch {
                    val layout = viewport.vertical.layoutInfo
                    val visibleRow = layout.visibleItemsInfo.firstOrNull { it.index == targetRow }
                    when {
                        visibleRow == null -> viewport.vertical.scrollToItem(targetRow)
                        visibleRow.offset < layout.viewportStartOffset ->
                            viewport.vertical.scrollBy((visibleRow.offset - layout.viewportStartOffset).toFloat())
                        visibleRow.offset + visibleRow.size > layout.viewportEndOffset ->
                            viewport.vertical.scrollBy((visibleRow.offset + visibleRow.size - layout.viewportEndOffset).toFloat())
                    }
                    val startPx = with(density) { columnIndex.starts[targetCol].roundToPx() }
                    val endPx = with(density) { columnIndex.ends[targetCol].roundToPx() }
                    val visibleBodyPx = (viewportWidthPx - with(density) { rowHeaderWidth.roundToPx() }).coerceAtLeast(0)
                    val nextOffset = when {
                        startPx < viewport.horizontal.value -> startPx
                        endPx > viewport.horizontal.value + visibleBodyPx -> endPx - visibleBodyPx
                        else -> viewport.horizontal.value
                    }
                    viewport.horizontal.scrollTo(nextOffset.coerceAtLeast(0))
                }
                true
            }
            .focusable()
            .semantics {
                collectionInfo = CollectionInfo(rowIndex.keys.size + 1, columns.size + 1)
                contentDescription = tableLabel
                isTraversalGroup = true
                if (selectionAnnouncement != null) stateDescription = selectionAnnouncement
            }
            .testTag("brace-table"),
    ) {
        require(maxWidth.value.isFinite()) { "BraceDataTable requires a bounded parent width" }
        val contentWidth = maxOf(totalWidth, maxWidth)
        validateTableContentSize(contentWidth, height, density)
        val viewportWidth = maxWidth
        val visibleBodyWidth = (viewportWidth - rowHeaderWidth).coerceAtLeast(0.dp)
        val scrollDp = with(density) { viewport.horizontal.value.toDp() }
        val visibleColumns = columnIndex.visibleRange(scrollDp, scrollDp + visibleBodyWidth)
        val bodyHeight = (maxHeight - headerHeight).coerceAtLeast(0.dp)

        Column(Modifier.horizontalScroll(viewport.horizontal).semantics { isTraversalGroup = false }) {
            Column(Modifier.width(maxOf(totalWidth, viewportWidth))) {
                Box(Modifier.width(maxOf(totalWidth, viewportWidth)).height(headerHeight).background(colors.header)) {
                    visibleColumns.forEach { index ->
                        val column = columns[index]
                        val columnSelected = selection == BraceTableSelection.Column(column.key)
                        val selectColumn: () -> Unit = {
                            pendingTouchRangeAnchor = null
                            onSelectionChange(BraceTableSelection.Column(column.key))
                            requester.requestFocus()
                        }
                        Box(
                            Modifier.offset(x = rowHeaderWidth + columnIndex.starts[index])
                                .width(widths[index]).height(headerHeight)
                                .background(if (columnSelected) colors.selectedRow else colors.header)
                                .border(metrics.gridLineWidth, colors.gridLine)
                                .pointerSelect("column:${column.key}", null, selectColumn)
                                .testTag("brace-table-header:${column.key}")
                                .clearAndSetSemantics {
                                    collectionItemInfo = CollectionItemInfo(0, 1, index + 1, 1)
                                    traversalIndex = (index + 1).toFloat()
                                    selected = columnSelected
                                    contentDescription = headerDescription.format(column.title, index + 1)
                                    onClick(selectedAction) { selectColumn(); true }
                                },
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            Text(column.title, modifier = Modifier.padding(horizontal = metrics.cellHorizontalPadding),
                                color = if (columnSelected) semantic.onSelection else colors.headerContent,
                                style = typography.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    if (onColumnWidthChange != null) {
                        visibleColumns.forEach { index ->
                            val column = columns[index]
                            val currentWidth = widths[index]
                            BraceTableResizeHandle(
                                axis = BraceResizeAxis.Column,
                                id = column.key,
                                name = column.title,
                                size = currentWidth,
                                minimum = minColumnWidth,
                                maximum = effectiveMaxColumnWidth,
                                width = BraceTheme.sizing.touchTarget,
                                height = headerHeight,
                                description = columnResizeDescription,
                                stateLabel = if (effectiveMaxColumnWidth == null)
                                    sizeMinDescription.format(currentWidth.value.toInt(), minColumnWidth.value.toInt())
                                    else sizeRangeDescription.format(currentWidth.value.toInt(),
                                        minColumnWidth.value.toInt(), effectiveMaxColumnWidth.value.toInt()),
                                increaseLabel = increaseSizeLabel,
                                decreaseLabel = decreaseSizeLabel,
                                onSizeChange = { onColumnWidthChange(column.key, it) },
                                onFocusedChange = { isFocused ->
                                    if (isFocused) focusedResizeHandleId = "column:${column.key}"
                                    else if (focusedResizeHandleId == "column:${column.key}") focusedResizeHandleId = null
                                },
                                modifier = Modifier.offset(x = rowHeaderWidth + columnIndex.starts[index] + currentWidth - BraceTheme.sizing.touchTarget),
                            )
                        }
                    }
                }
                LazyColumn(
                    state = viewport.vertical,
                    modifier = Modifier.width(maxOf(totalWidth, viewportWidth)).height(bodyHeight)
                        .semantics { isTraversalGroup = false },
                ) {
                    items(count = rows.size, key = { index -> rowIndex.keys[index] }) { rowNumber ->
                        val row = rows[rowNumber]
                        val key = rowIndex.keys[rowNumber]
                        val accessibleRowLabel = if (rowLabel === rowKey) key else rowLabel(row)
                        val rowSelected = selection == BraceTableSelection.Row(key)
                        val requestedHeight = maxOf(defaultRowHeight, rowHeights[key] ?: defaultRowHeight)
                        val itemHeight = if (effectiveMaxRowHeight == null) requestedHeight else
                            minOf(effectiveMaxRowHeight, requestedHeight)
                        val rowColor = if (rowSelected) colors.selectedRow
                            else if (rowNumber % 2 == 0) colors.row else colors.alternateRow
                        Box(Modifier.width(maxOf(totalWidth, viewportWidth)).height(itemHeight).background(rowColor)) {
                            visibleColumns.forEach { columnNumber ->
                                val column = columns[columnNumber]
                                val cellSelected = when (selection) {
                                    is BraceTableSelection.Cell -> selection.rowKey == key && selection.columnKey == column.key
                                    is BraceTableSelection.Row -> selection.rowKey == key
                                    is BraceTableSelection.Column -> selection.columnKey == column.key
                                    is BraceTableSelection.Range -> range?.let { (selectedRows, selectedColumns) ->
                                        rowNumber in selectedRows && columnNumber in selectedColumns
                                    } == true
                                    null -> false
                                }
                                val value = column.cellText(row)
                                val interaction = remember(key, column.key) { MutableInteractionSource() }
                                val hovered by interaction.collectIsHoveredAsState()
                                var pointerShift by remember(key, column.key) { mutableStateOf(false) }
                                val cellColor = when {
                                    cellSelected || rowSelected -> colors.selectedRow
                                    hovered -> semantic.hover
                                    else -> rowColor
                                }
                                val textColor = if (cellSelected || rowSelected) semantic.onSelection else semantic.onSurface
                                val selectCell: () -> Unit = {
                                    val anchor = validAnchor(when {
                                        pointerShift && selection is BraceTableSelection.Range ->
                                            BraceTableSelection.Cell(selection.anchorRowKey, selection.anchorColumnKey)
                                        pointerShift && selection is BraceTableSelection.Cell -> selection
                                        else -> pendingTouchRangeAnchor
                                    })
                                    if (anchor == null) onSelectionChange(BraceTableSelection.Cell(key, column.key))
                                    else onSelectionChange(BraceTableSelection.Range(
                                        anchor.rowKey, anchor.columnKey, key, column.key,
                                    ))
                                    pendingTouchRangeAnchor = null
                                    pointerShift = false
                                    requester.requestFocus()
                                }
                                val beginTouchRange: () -> Unit = {
                                    val anchor = validAnchor(when (selection) {
                                        is BraceTableSelection.Range -> BraceTableSelection.Cell(
                                            selection.anchorRowKey, selection.anchorColumnKey)
                                        is BraceTableSelection.Cell -> selection
                                        else -> null
                                    }) ?: BraceTableSelection.Cell(key, column.key)
                                    pendingTouchRangeAnchor = anchor
                                    onSelectionChange(BraceTableSelection.Range(
                                        anchor.rowKey, anchor.columnKey, key, column.key,
                                    ))
                                    requester.requestFocus()
                                }
                                Box(
                                    Modifier.offset(x = rowHeaderWidth + columnIndex.starts[columnNumber])
                                        .width(widths[columnNumber]).height(itemHeight)
                                        .background(cellColor)
                                        .hoverable(interaction)
                                        .pointerInput(key, column.key) {
                                            awaitPointerEventScope {
                                                while (true) {
                                                    val event = awaitPointerEvent(PointerEventPass.Initial)
                                                    if (event.type == PointerEventType.Press) {
                                                        pointerShift = event.keyboardModifiers.isPointerShiftPressed
                                                    }
                                                }
                                            }
                                        }
                                        .border(if (cellSelected && focused) BraceTheme.sizing.focusRingWidth else metrics.gridLineWidth,
                                            if (cellSelected && focused) semantic.focusRing else colors.gridLine)
                                        .pointerSelect(key, column.key, selectCell, beginTouchRange)
                                        .testTag("brace-table-cell:$key:${column.key}")
                                        .clearAndSetSemantics {
                                            collectionItemInfo = CollectionItemInfo(rowNumber + 1, 1, columnNumber + 1, 1)
                                            traversalIndex = ((rowNumber + 1).toLong() * (columns.size + 1) + columnNumber + 1).toFloat()
                                            selected = cellSelected
                                            contentDescription = cellDescription.format(column.title, rowNumber + 1, accessibleRowLabel, value)
                                            onClick(selectedAction) { selectCell(); true }
                                            customActions = listOf(CustomAccessibilityAction(extendRangeAction) {
                                                beginTouchRange(); true
                                            })
                                        },
                                    contentAlignment = Alignment.CenterStart,
                                ) {
                                    Box(Modifier.fillMaxWidth().padding(horizontal = metrics.cellHorizontalPadding)) {
                                        if (column.cellContent == null) {
                                            Text(value, color = textColor, style = typography.body, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        } else column.cellContent.invoke(row)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        // Row headers must draw outside the horizontal scroll container so scrolled cells
        // cannot cover their pixels. Observe stable index/offset pairs; reading raw layoutInfo
        // in composition would recompose the LazyColumn on each measurement.
        val visibleRows by remember(viewport.vertical) {
            derivedStateOf(structuralEqualityPolicy()) {
                viewport.vertical.layoutInfo.visibleItemsInfo.map { Triple(it.index, it.offset, it.size) }
            }
        }
        Box(Modifier.align(Alignment.TopStart).width(rowHeaderWidth).height(height).clipToBounds()) {
            // The body clip keeps partially scrolled headers and resize handles below the fixed corner.
            Box(Modifier.offset(y = headerHeight).width(rowHeaderWidth).height(bodyHeight).clipToBounds()) {
                visibleRows.forEach { (rowNumber, offsetPx, itemSizePx) ->
                    if (rowNumber in rows.indices) {
                        val row = rows[rowNumber]
                        val key = rowIndex.keys[rowNumber]
                        val accessibleRowLabel = if (rowLabel === rowKey) key else rowLabel(row)
                        val rowSelected = selection == BraceTableSelection.Row(key)
                        val itemHeight = with(density) { itemSizePx.toDp() }
                        val selectRow: () -> Unit = {
                            pendingTouchRangeAnchor = null
                            onSelectionChange(BraceTableSelection.Row(key))
                            requester.requestFocus()
                        }
                        Box(
                            Modifier.offset(y = with(density) { offsetPx.toDp() })
                                .width(rowSelectWidth).height(itemHeight)
                                .background(if (rowSelected) colors.selectedRow else colors.header)
                                .border(metrics.gridLineWidth, colors.gridLine)
                                .pointerSelect(key, null, selectRow)
                                .testTag("brace-table-row:$key")
                                .clearAndSetSemantics {
                                    collectionItemInfo = CollectionItemInfo(rowNumber + 1, 1, 0, 1)
                                    traversalIndex = ((rowNumber + 1).toLong() * (columns.size + 1)).toFloat()
                                    selected = rowSelected
                                    contentDescription = rowDescription.format(rowNumber + 1, accessibleRowLabel)
                                    onClick(selectedAction) { selectRow(); true }
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text((rowNumber + 1).toString(),
                                color = if (rowSelected) semantic.onSelection else colors.headerContent,
                                style = typography.label)
                        }
                        if (onRowHeightChange != null) {
                            val minimum = defaultRowHeight
                            BraceTableResizeHandle(
                                axis = BraceResizeAxis.Row,
                                id = key,
                                name = accessibleRowLabel,
                                size = itemHeight,
                                minimum = minimum,
                                maximum = effectiveMaxRowHeight,
                                width = BraceTheme.sizing.touchTarget,
                                height = itemHeight,
                                description = rowResizeDescription,
                                stateLabel = if (effectiveMaxRowHeight == null)
                                    sizeMinDescription.format(itemHeight.value.toInt(), minimum.value.toInt())
                                    else sizeRangeDescription.format(itemHeight.value.toInt(),
                                        minimum.value.toInt(), effectiveMaxRowHeight.value.toInt()),
                                increaseLabel = increaseSizeLabel,
                                decreaseLabel = decreaseSizeLabel,
                                onSizeChange = { onRowHeightChange(key, it) },
                                onFocusedChange = { isFocused ->
                                    if (isFocused) focusedResizeHandleId = "row:$key"
                                    else if (focusedResizeHandleId == "row:$key") focusedResizeHandleId = null
                                },
                                modifier = Modifier.offset(x = rowSelectWidth,
                                    y = with(density) { offsetPx.toDp() }),
                            )
                        }
                    }
                }
            }
            Box(
                Modifier.width(rowHeaderWidth).height(headerHeight)
                    .background(colors.header).border(metrics.gridLineWidth, colors.gridLine)
                    .testTag("brace-table-corner")
                    .clearAndSetSemantics {
                        collectionItemInfo = CollectionItemInfo(0, 1, 0, 1)
                        traversalIndex = 0f
                        contentDescription = rowHeaderName
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(rowHeaderName, color = colors.headerContent, style = typography.label)
            }
        }
        if (rows.isEmpty()) {
            Box(Modifier.offset(y = headerHeight).height(bodyHeight).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(emptyText, color = semantic.onSurfaceMuted, style = typography.body)
            }
        }
    }
}

@Composable
private fun Modifier.pointerSelect(
    rowKey: String,
    columnKey: String?,
    select: () -> Unit,
    onLongPress: (() -> Unit)? = null,
): Modifier {
    val latestSelect = rememberUpdatedState(select)
    val latestLongPress = rememberUpdatedState(onLongPress)
    return pointerInput(rowKey, columnKey) {
        detectTapGestures(
            onTap = { latestSelect.value() },
            onLongPress = { latestLongPress.value?.invoke() },
        )
    }
}

/** Validate controlled dimensions once for each immutable size-map version. */
internal fun validateTableDimensions(
    height: Dp,
    maxColumnWidth: Dp?,
    maxRowHeight: Dp?,
    columnWidths: Map<String, Dp>,
    rowHeights: Map<String, Dp>,
) {
    require(height.value.isFinite() && height > 0.dp) { "Table height must be finite and positive" }
    require(maxColumnWidth == null || maxColumnWidth.value.isFinite() && maxColumnWidth > 0.dp) {
        "Maximum column width must be finite and positive"
    }
    require(maxRowHeight == null || maxRowHeight.value.isFinite() && maxRowHeight > 0.dp) {
        "Maximum row height must be finite and positive"
    }
    require(columnWidths.values.all { it.value.isFinite() }) { "Column width overrides must be finite" }
    require(rowHeights.values.all { it.value.isFinite() }) { "Row height overrides must be finite" }
}

/** Fail with an actionable contract before Compose rejects an oversized packed constraint. */
internal fun validateTableContentSize(contentWidth: Dp, tableHeight: Dp, density: Density) {
    require(contentWidth.value.isFinite()) { "Table content width must be finite" }
    try {
        Constraints.fixed(with(density) { contentWidth.roundToPx() }, with(density) { tableHeight.roundToPx() })
    } catch (cause: IllegalArgumentException) {
        throw IllegalArgumentException("Table content exceeds Compose measured layout limits; use fewer or narrower columns", cause)
    }
}

/** Validate keys and retain their positions once per immutable data-list version. */
internal data class TableRowIndex(val keys: List<String>, val byKey: Map<String, Int>)

internal fun <Row> validateRowKeys(rows: List<Row>, rowKey: (Row) -> String): TableRowIndex {
    val keys = ArrayList<String>(rows.size)
    val byKey = HashMap<String, Int>(rows.size)
    rows.forEachIndexed { index, row ->
        val key = rowKey(row)
        require(key.isNotBlank()) { "Row keys must not be blank" }
        require(byKey.putIfAbsent(key, index) == null) { "Duplicate row key: $key" }
        keys += key
    }
    return TableRowIndex(keys, byKey)
}

/** Contiguous column boundaries permit logarithmic viewport lookup, including resized widths. */
internal data class TableColumnIndex(
    val starts: List<Dp>,
    val ends: List<Dp>,
    val byKey: Map<String, Int>,
    val totalWidth: Dp,
) {
    fun visibleRange(left: Dp, right: Dp): IntRange {
        if (starts.isEmpty() || right <= left) return IntRange.EMPTY
        // First end strictly after the left edge; last start strictly before the right edge.
        val first = ends.binarySearchFirst { it > left }
        val last = starts.binarySearchFirst { it >= right } - 1
        return if (first > last || first == starts.size || last < 0) IntRange.EMPTY
            else (first - 1).coerceAtLeast(0)..(last + 1).coerceAtMost(starts.lastIndex)
    }
}

internal fun <Row> indexColumns(
    columns: List<BraceTableColumn<Row>>,
    minimumWidth: Dp,
    effectiveWidths: List<Dp>? = null,
): TableColumnIndex {
    require(effectiveWidths == null || effectiveWidths.size == columns.size) { "Column widths must match columns" }
    val starts = ArrayList<Dp>(columns.size)
    val ends = ArrayList<Dp>(columns.size)
    val byKey = HashMap<String, Int>(columns.size)
    var position = 0.dp
    columns.forEachIndexed { index, column ->
        require(column.key.isNotBlank() && column.title.isNotBlank()) { "Column keys and titles must not be blank" }
        require(byKey.putIfAbsent(column.key, index) == null) { "Column keys must be unique" }
        require(column.width.value.isFinite() && column.width >= minimumWidth) {
            "Column base width must be finite and at least $minimumWidth"
        }
        val width = effectiveWidths?.get(index) ?: column.width
        require(width.value.isFinite() && width >= minimumWidth) {
            "Column width must be finite and at least $minimumWidth"
        }
        starts += position
        position += width
        ends += position
    }
    return TableColumnIndex(starts, ends, byKey, position)
}

private inline fun <T> List<T>.binarySearchFirst(predicate: (T) -> Boolean): Int {
    var low = 0
    var high = size
    while (low < high) {
        val middle = (low + high) ushr 1
        if (predicate(this[middle])) high = middle else low = middle + 1
    }
    return low
}
