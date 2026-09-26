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
import androidx.compose.ui.semantics.collectionItemInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
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
    val validatedRowKeys = remember(rows) { validateRowKeys(rows, rowKey) }
    require(columns.all { it.key.isNotBlank() && it.title.isNotBlank() }) { "Column keys and titles must not be blank" }
    require(columns.map { it.key }.distinct().size == columns.size) { "Column keys must be unique" }
    val minColumnWidth = BraceTheme.sizing.tableMinColumnWidth
    require(columns.all { it.width >= minColumnWidth }) { "Column width must be at least $minColumnWidth" }

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
    val rowHeaderWidth = BraceTheme.sizing.touchTarget + spacing.sm
    val starts = remember(columns) {
        val positions = ArrayList<Dp>(columns.size)
        var position = 0.dp
        columns.forEach { column -> positions += position; position += column.width }
        positions
    }
    val columnWidth = columns.fold(0.dp) { sum, column -> sum + column.width }
    val totalWidth = rowHeaderWidth + columnWidth
    val tableLabel = label ?: stringResource(R.string.brace_table_default_label)
    require(tableLabel.isNotBlank()) { "Table label must not be blank" }
    val rowHeaderName = stringResource(R.string.brace_table_row_header)
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
                    is BraceTableSelection.Cell -> rows.indexOfFirst { rowKey(it) == selection.rowKey }
                    is BraceTableSelection.Row -> rows.indexOfFirst { rowKey(it) == selection.rowKey }
                    null -> -1
                }.coerceAtLeast(0)
                val currentCol = when (selection) {
                    is BraceTableSelection.Cell -> columns.indexOfFirst { it.key == selection.columnKey }
                    else -> 0
                }.coerceAtLeast(0)
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
                onSelectionChange(BraceTableSelection.Cell(rowKey(rows[targetRow]), columns[targetCol].key))
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
                    val startPx = with(density) { starts[targetCol].roundToPx() }
                    val endPx = startPx + with(density) { columns[targetCol].width.roundToPx() }
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
            .semantics { collectionInfo = CollectionInfo(validatedRowKeys.size + 1, columns.size + 1); contentDescription = tableLabel }
            .testTag("brace-table"),
    ) {
        val viewportWidth = maxWidth
        val visibleBodyWidth = (viewportWidth - rowHeaderWidth).coerceAtLeast(0.dp)
        val scrollDp = with(density) { viewport.horizontal.value.toDp() }
        val first = columns.indices.indexOfFirst { starts[it] + columns[it].width > scrollDp }
        val last = columns.indices.indexOfLast { starts[it] < scrollDp + visibleBodyWidth }
        val visibleColumns = if (first < 0 || last < 0) emptyList()
            else ((first - 1).coerceAtLeast(0)..(last + 1).coerceAtMost(columns.lastIndex)).toList()
        val bodyHeight = (maxHeight - headerHeight).coerceAtLeast(0.dp)

        Column(Modifier.horizontalScroll(viewport.horizontal)) {
            Column(Modifier.width(maxOf(totalWidth, viewportWidth))) {
                Box(Modifier.width(maxOf(totalWidth, viewportWidth)).height(headerHeight).background(colors.header)) {
                    visibleColumns.forEach { index ->
                        val column = columns[index]
                        Box(
                            Modifier.offset(x = rowHeaderWidth + starts[index]).width(column.width).height(headerHeight)
                                .border(metrics.gridLineWidth, colors.gridLine)
                                .semantics { collectionItemInfo = CollectionItemInfo(0, 1, index + 1, 1)
                                    contentDescription = headerDescription.format(column.title, index + 1) }
                                .testTag("brace-table-header:${column.key}"),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            Text(column.title, modifier = Modifier.padding(horizontal = metrics.cellHorizontalPadding),
                                color = colors.headerContent, style = typography.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
                LazyColumn(
                    state = viewport.vertical,
                    modifier = Modifier.width(maxOf(totalWidth, viewportWidth)).height(bodyHeight),
                ) {
                    items(count = rows.size, key = { index -> rowKey(rows[index]) }) { rowIndex ->
                        val row = rows[rowIndex]
                        val key = rowKey(row)
                        val rowSelected = selection == BraceTableSelection.Row(key)
                        val rowColor = if (rowSelected) colors.selectedRow
                            else if (rowIndex % 2 == 0) colors.row else colors.alternateRow
                        Box(Modifier.width(maxOf(totalWidth, viewportWidth)).height(rowHeight).background(rowColor)) {
                            visibleColumns.forEach { columnIndex ->
                                val column = columns[columnIndex]
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
                                    Modifier.offset(x = rowHeaderWidth + starts[columnIndex])
                                        .width(column.width).height(rowHeight)
                                        .background(cellColor)
                                        .hoverable(interaction)
                                        .border(if (cellSelected && focused) BraceTheme.sizing.focusRingWidth else metrics.gridLineWidth,
                                            if (cellSelected && focused) semantic.focusRing else colors.gridLine)
                                        .pointerSelect(key, column.key, selectCell)
                                        .semantics(mergeDescendants = true) {
                                            collectionItemInfo = CollectionItemInfo(rowIndex + 1, 1, columnIndex + 1, 1)
                                            selected = cellSelected || rowSelected
                                            contentDescription = cellDescription.format(column.title, rowIndex + 1, rowLabel(row), value)
                                            onClick(selectedAction) { selectCell(); true }
                                        }
                                        .testTag("brace-table-cell:$key:${column.key}"),
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
        Box(Modifier.align(Alignment.TopStart).width(rowHeaderWidth).height(height).clipToBounds().zIndex(1f)) {
            visibleRows.forEach { (rowIndex, offsetPx) ->
                if (rowIndex in rows.indices) {
                    val row = rows[rowIndex]
                    val key = rowKey(row)
                    val rowSelected = selection == BraceTableSelection.Row(key)
                    val selectRow: () -> Unit = {
                        onSelectionChange(BraceTableSelection.Row(key))
                        requester.requestFocus()
                    }
                    Box(
                        Modifier.offset(y = headerHeight + with(density) { offsetPx.toDp() })
                            .width(rowHeaderWidth).height(rowHeight)
                            .background(if (rowSelected) colors.selectedRow else colors.header)
                            .border(metrics.gridLineWidth, colors.gridLine)
                            .pointerSelect(key, null, selectRow)
                            .semantics(mergeDescendants = true) {
                                collectionItemInfo = CollectionItemInfo(rowIndex + 1, 1, 0, 1)
                                selected = rowSelected
                                contentDescription = rowDescription.format(rowIndex + 1, rowLabel(row))
                                onClick(selectedAction) { selectRow(); true }
                            }
                            .testTag("brace-table-row:$key"),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text((rowIndex + 1).toString(), color = if (rowSelected) semantic.onSelection else colors.headerContent,
                            style = typography.label)
                    }
                }
            }
            Box(
                Modifier.width(rowHeaderWidth).height(headerHeight)
                    .background(colors.header).border(metrics.gridLineWidth, colors.gridLine)
                    .semantics { collectionItemInfo = CollectionItemInfo(0, 1, 0, 1)
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

/** Validate the stable-key contract once for each immutable data-list version. */
internal fun <Row> validateRowKeys(rows: List<Row>, rowKey: (Row) -> String): Set<String> {
    val seen = HashSet<String>(rows.size)
    rows.forEach { row ->
        val key = rowKey(row)
        require(key.isNotBlank()) { "Row keys must not be blank" }
        require(seen.add(key)) { "Duplicate row key: $key" }
    }
    return seen
}
