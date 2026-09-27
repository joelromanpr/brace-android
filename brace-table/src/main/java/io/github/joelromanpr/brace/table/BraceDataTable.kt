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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.CollectionItemInfo
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.collectionItemInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.selected
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
 * A typed column with a stable [key], visible [title], and fixed [width].
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

/** Controlled single-cell or whole-row selection, identified by stable row and column keys. */
sealed interface BraceTableSelection {
    /** One selected cell. */
    data class Cell(val rowKey: String, val columnKey: String) : BraceTableSelection

    /** One selected row, activated through its fixed row header. */
    data class Row(val rowKey: String) : BraceTableSelection
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
 * The parent must bound its width. Column widths must fit within Compose measured layout
 * constraints; pass a narrower column subset when a schema exceeds that limit.
 * This first table slice does not yet expose range selection, editing, resize, or copying.
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
) {
    require(height > 0.dp) { "Table height must be positive" }
    val rowIndex = remember(rows) { validateRowKeys(rows, rowKey) }
    val minColumnWidth = BraceTheme.sizing.tableMinColumnWidth
    val columnIndex = remember(columns, minColumnWidth) { indexColumns(columns, minColumnWidth) }

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
    val rowHeight = maxOf(densityTokens.rowHeightDp + densityTokens.itemGapDp,
        BraceTheme.sizing.touchTarget, maxOf(bodyLineHeight, headerLineHeight) + spacing.sm)
    val headerHeight = rowHeight
    val tableLabel = label ?: stringResource(R.string.brace_table_default_label)
    require(tableLabel.isNotBlank()) { "Table label must not be blank" }
    val rowHeaderName = stringResource(R.string.brace_table_row_header)
    val textMeasurer = rememberTextMeasurer()
    val rowHeaderTextWidth = with(density) {
        maxOf(
            textMeasurer.measure(rowHeaderName, style = typography.label, maxLines = 1).size.width,
            textMeasurer.measure(rows.size.toString(), style = typography.label, maxLines = 1).size.width,
        ).toDp()
    }
    val rowHeaderWidth = maxOf(BraceTheme.sizing.touchTarget + spacing.sm,
        rowHeaderTextWidth + metrics.cellHorizontalPadding * 2)
    val totalWidth = rowHeaderWidth + columnIndex.totalWidth
    val selectedAction = stringResource(R.string.brace_table_select)
    val emptyText = stringResource(R.string.brace_table_empty)
    val rowDescription = stringResource(R.string.brace_table_row_description)
    val cellDescription = stringResource(R.string.brace_table_cell_description)
    val headerDescription = stringResource(R.string.brace_table_column_description)

    BoxWithConstraints(
        modifier.fillMaxWidth().height(height)
            .background(colors.row)
            .border(if (focused) BraceTheme.sizing.focusRingWidth else metrics.gridLineWidth,
                if (focused) semantic.focusRing else colors.gridLine)
            .onSizeChanged { viewportWidthPx = it.width; viewportHeightPx = it.height }
            .focusRequester(requester)
            .onFocusChanged { focused = it.isFocused }
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown || rows.isEmpty() || columns.isEmpty() ||
                    event.isAltPressed || event.isCtrlPressed || event.isMetaPressed || event.isShiftPressed) {
                    return@onPreviewKeyEvent false
                }
                val currentRow = when (selection) {
                    is BraceTableSelection.Cell -> rowIndex.byKey[selection.rowKey]
                    is BraceTableSelection.Row -> rowIndex.byKey[selection.rowKey]
                    null -> null
                } ?: 0
                val currentCol = when (selection) {
                    is BraceTableSelection.Cell -> columnIndex.byKey[selection.columnKey]
                    else -> null
                } ?: 0
                val page = maxOf(1, (viewportHeightPx - with(density) { headerHeight.roundToPx() }) /
                    with(density) { rowHeight.roundToPx() }.coerceAtLeast(1))
                val next = when (event.nativeKeyEvent.keyCode) {
                    AndroidKeyEvent.KEYCODE_DPAD_UP -> (currentRow - 1).coerceAtLeast(0) to currentCol
                    AndroidKeyEvent.KEYCODE_DPAD_DOWN -> (currentRow + 1).coerceAtMost(rows.lastIndex) to currentCol
                    AndroidKeyEvent.KEYCODE_DPAD_LEFT -> currentRow to
                        (currentCol + if (direction == LayoutDirection.Rtl) 1 else -1).coerceIn(0, columns.lastIndex)
                    AndroidKeyEvent.KEYCODE_DPAD_RIGHT -> currentRow to
                        (currentCol + if (direction == LayoutDirection.Rtl) -1 else 1).coerceIn(0, columns.lastIndex)
                    AndroidKeyEvent.KEYCODE_MOVE_HOME -> currentRow to 0
                    AndroidKeyEvent.KEYCODE_MOVE_END -> currentRow to columns.lastIndex
                    AndroidKeyEvent.KEYCODE_PAGE_UP -> (currentRow - page).coerceAtLeast(0) to currentCol
                    AndroidKeyEvent.KEYCODE_PAGE_DOWN -> (currentRow + page).coerceAtMost(rows.lastIndex) to currentCol
                    else -> null
                } ?: return@onPreviewKeyEvent false
                val (targetRow, targetCol) = next
                onSelectionChange(BraceTableSelection.Cell(rowIndex.keys[targetRow], columns[targetCol].key))
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
            .semantics { collectionInfo = CollectionInfo(rowIndex.keys.size + 1, columns.size + 1)
                contentDescription = tableLabel; isTraversalGroup = true }
            .testTag("brace-table"),
    ) {
        require(maxWidth != Dp.Infinity) { "BraceDataTable requires a bounded parent width" }
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
                        Box(
                            Modifier.offset(x = rowHeaderWidth + columnIndex.starts[index]).width(column.width).height(headerHeight)
                                .border(metrics.gridLineWidth, colors.gridLine)
                                .testTag("brace-table-header:${column.key}")
                                .clearAndSetSemantics { collectionItemInfo = CollectionItemInfo(0, 1, index + 1, 1)
                                    traversalIndex = (index + 1).toFloat()
                                    contentDescription = headerDescription.format(column.title, index + 1) },
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            Text(column.title, modifier = Modifier.padding(horizontal = metrics.cellHorizontalPadding),
                                color = colors.headerContent, style = typography.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
                        val rowColor = if (rowSelected) colors.selectedRow
                            else if (rowNumber % 2 == 0) colors.row else colors.alternateRow
                        Box(Modifier.width(maxOf(totalWidth, viewportWidth)).height(rowHeight).background(rowColor)) {
                            visibleColumns.forEach { columnNumber ->
                                val column = columns[columnNumber]
                                val cellSelected = selection == BraceTableSelection.Cell(key, column.key)
                                val value = column.cellText(row)
                                val interaction = remember(key, column.key) { MutableInteractionSource() }
                                val hovered by interaction.collectIsHoveredAsState()
                                val cellColor = when {
                                    cellSelected || rowSelected -> colors.selectedRow
                                    hovered -> semantic.hover
                                    else -> rowColor
                                }
                                val textColor = if (cellSelected || rowSelected) semantic.onSelection else semantic.onSurface
                                val selectCell: () -> Unit = {
                                    onSelectionChange(BraceTableSelection.Cell(key, column.key))
                                    requester.requestFocus()
                                }
                                Box(
                                    Modifier.offset(x = rowHeaderWidth + columnIndex.starts[columnNumber])
                                        .width(column.width).height(rowHeight)
                                        .background(cellColor)
                                        .hoverable(interaction)
                                        .border(if (cellSelected && focused) BraceTheme.sizing.focusRingWidth else metrics.gridLineWidth,
                                            if (cellSelected && focused) semantic.focusRing else colors.gridLine)
                                        .pointerSelect(key, column.key, selectCell)
                                        .testTag("brace-table-cell:$key:${column.key}")
                                        .clearAndSetSemantics {
                                            collectionItemInfo = CollectionItemInfo(rowNumber + 1, 1, columnNumber + 1, 1)
                                            traversalIndex = ((rowNumber + 1).toLong() * (columns.size + 1) + columnNumber + 1).toFloat()
                                            selected = cellSelected || rowSelected
                                            contentDescription = cellDescription.format(column.title, rowNumber + 1, accessibleRowLabel, value)
                                            onClick(selectedAction) { selectCell(); true }
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
                viewport.vertical.layoutInfo.visibleItemsInfo.map { it.index to it.offset }
            }
        }
        Box(Modifier.align(Alignment.TopStart).width(rowHeaderWidth).height(height).clipToBounds()) {
            Box(Modifier.offset(y = headerHeight).width(rowHeaderWidth).height(bodyHeight).clipToBounds()) {
                visibleRows.forEach { (rowNumber, offsetPx) ->
                    if (rowNumber in rows.indices) {
                        val row = rows[rowNumber]
                        val key = rowIndex.keys[rowNumber]
                        val rowSelected = selection == BraceTableSelection.Row(key)
                        val selectRow: () -> Unit = {
                            onSelectionChange(BraceTableSelection.Row(key))
                            requester.requestFocus()
                        }
                        Box(
                            Modifier.offset(y = with(density) { offsetPx.toDp() })
                                .width(rowHeaderWidth).height(rowHeight)
                                .background(if (rowSelected) colors.selectedRow else colors.header)
                                .border(metrics.gridLineWidth, colors.gridLine)
                                .pointerSelect(key, null, selectRow)
                                .testTag("brace-table-row:$key")
                                .clearAndSetSemantics {
                                    collectionItemInfo = CollectionItemInfo(rowNumber + 1, 1, 0, 1)
                                    traversalIndex = ((rowNumber + 1).toLong() * (columns.size + 1)).toFloat()
                                    selected = rowSelected
                                    contentDescription = rowDescription.format(rowNumber + 1,
                                        if (rowLabel === rowKey) key else rowLabel(row))
                                    onClick(selectedAction) { selectRow(); true }
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text((rowNumber + 1).toString(), color = if (rowSelected) semantic.onSelection else colors.headerContent,
                                style = typography.label)
                        }
                    }
                }
            }
            Box(
                Modifier.width(rowHeaderWidth).height(headerHeight)
                    .background(colors.header).border(metrics.gridLineWidth, colors.gridLine)
                    .testTag("brace-table-corner")
                    .clearAndSetSemantics { collectionItemInfo = CollectionItemInfo(0, 1, 0, 1)
                        traversalIndex = 0f
                        contentDescription = rowHeaderName },
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
private fun Modifier.pointerSelect(rowKey: String, columnKey: String?, select: () -> Unit): Modifier {
    val latestSelect = rememberUpdatedState(select)
    return pointerInput(rowKey, columnKey) { detectTapGestures(onTap = { latestSelect.value() }) }
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

/** Contiguous fixed-width boundaries permit logarithmic viewport lookup. */
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

internal fun <Row> indexColumns(columns: List<BraceTableColumn<Row>>, minimumWidth: Dp): TableColumnIndex {
    val starts = ArrayList<Dp>(columns.size)
    val ends = ArrayList<Dp>(columns.size)
    val byKey = HashMap<String, Int>(columns.size)
    var position = 0.dp
    columns.forEachIndexed { index, column ->
        require(column.key.isNotBlank() && column.title.isNotBlank()) { "Column keys and titles must not be blank" }
        require(byKey.putIfAbsent(column.key, index) == null) { "Column keys must be unique" }
        require(column.width.value.isFinite() && column.width >= minimumWidth) {
            "Column width must be finite and at least $minimumWidth"
        }
        starts += position
        position += column.width
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
