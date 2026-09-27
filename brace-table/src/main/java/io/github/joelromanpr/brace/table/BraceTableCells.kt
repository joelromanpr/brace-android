package io.github.joelromanpr.brace.table

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.CollectionItemInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.collectionItemInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.braceandroid.foundation.BraceTheme

/**
 * Selectable, token-styled table body cell with one TalkBack node.
 *
 * [value] is always spoken in full, even when the default visual text ellipsizes or [content]
 * renders a badge or other noninteractive presentation. Coordinates are zero-based; the spoken
 * row number is one-based. [rowKey] and [columnKey] must remain stable as data moves. The
 * parent [BraceDataTable] owns keyboard navigation and viewport focus; [onSelect] and optional
 * range/edit callbacks are also exposed to touch, mouse, and TalkBack here. [enabled]
 * pauses those actions during a controlled table load. [onAddRegion] exposes a separate
 * TalkBack action for adding this cell to a disjoint selection. [pinState] can announce
 * that the cell remains visible in a frozen row, column, or their intersection. [traversalIndex]
 * lets a parent grid order cells across independently composed panes. [focused] draws the keyboard
 * focus ring; [activeCellLabel] names the current navigation target alongside any pinned state,
 * even while keyboard focus is outside the grid. [onReveal] adds a separate 48 dp
 * full-value button and a TalkBack custom action on the cell without changing selection.
 */
@Composable
fun BraceTableCell(
    value: String,
    columnTitle: String,
    rowLabel: String,
    rowIndex: Int,
    columnIndex: Int,
    rowKey: String,
    columnKey: String,
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
    focused: Boolean = false,
    enabled: Boolean = true,
    onExtendSelection: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
    onAddRegion: (() -> Unit)? = null,
    onReveal: (() -> Unit)? = null,
    pinState: String? = null,
    content: (@Composable () -> Unit)? = null,
    traversalIndex: Float = 0f,
    activeCellLabel: String? = null,
) {
    require(rowIndex >= 0 && columnIndex >= 0) { "Table cell coordinates must be nonnegative" }
    require(rowKey.isNotBlank() && columnKey.isNotBlank()) { "Table cell keys must not be blank" }
    require(traversalIndex.isFinite()) { "Table traversal index must be finite" }
    val colors = BraceTheme.colors.components.table
    val semantic = BraceTheme.colors.semantic
    val metrics = BraceTheme.componentMetrics.table
    val interaction = remember(rowKey, columnKey) { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val background = when {
        selected -> colors.selectedRow
        hovered && enabled -> semantic.hover
        rowIndex % 2 == 0 -> colors.row
        else -> colors.alternateRow
    }
    val selectLabel = stringResource(R.string.brace_table_select)
    val extendLabel = stringResource(R.string.brace_table_extend_range)
    val editLabel = stringResource(R.string.brace_table_edit)
    val addRegionLabel = stringResource(R.string.brace_table_add_region)
    val description = stringResource(R.string.brace_table_cell_description, columnTitle,
        rowIndex + 1, rowLabel, value)
    val revealLabel = stringResource(R.string.brace_table_show_full_value)
    val revealInteraction = remember(rowKey, columnKey) { MutableInteractionSource() }
    val revealHovered by revealInteraction.collectIsHoveredAsState()
    val revealPressed by revealInteraction.collectIsPressedAsState()
    var revealFocused by remember(rowKey, columnKey) { mutableStateOf(false) }
    val revealShape = RoundedCornerShape(BraceTheme.shape.sm)
    val revealBackground = when {
        revealPressed -> semantic.primaryPressed
        revealHovered -> semantic.primaryHover
        else -> semantic.primarySubtle
    }
    val revealContentColor = if (revealPressed || revealHovered) semantic.onPrimary
        else semantic.onPrimarySubtle
    val revealWidth = if (enabled && onReveal != null) BraceTheme.sizing.touchTarget else 0.dp
    val targetBounds = if (enabled && onReveal != null)
        Modifier.widthIn(min = BraceTheme.sizing.touchTarget * 2)
            .heightIn(min = BraceTheme.sizing.touchTarget)
    else Modifier
    Box(targetBounds.then(modifier).background(background)
        .hoverable(interaction)
        .border(if (selected && focused) BraceTheme.sizing.focusRingWidth else metrics.gridLineWidth,
            if (selected && focused) semantic.focusRing else colors.gridLine)) {
        Box(Modifier.fillMaxSize().padding(end = revealWidth)
            .then(if (enabled) Modifier.pointerSelect(rowKey, columnKey, onSelect,
                onExtendSelection, onEdit) else Modifier)
            .clearAndSetSemantics {
                testTag = "brace-table-cell:$rowKey:$columnKey"
                this.traversalIndex = traversalIndex
                collectionItemInfo = CollectionItemInfo(rowIndex + 1, 1, columnIndex + 1, 1)
                this.selected = selected
                contentDescription = description
                if (!enabled) disabled()
                val cellState = listOfNotNull(activeCellLabel, pinState)
                if (cellState.isNotEmpty()) stateDescription = cellState.joinToString(", ")
                if (enabled) onClick(selectLabel) { onSelect(); true }
                customActions = if (enabled) listOfNotNull(
                    onExtendSelection?.let { action ->
                        CustomAccessibilityAction(extendLabel) { action(); true }
                    },
                    onEdit?.let { action ->
                        CustomAccessibilityAction(editLabel) { action(); true }
                    },
                    onAddRegion?.let { action ->
                        CustomAccessibilityAction(addRegionLabel) { action(); true }
                    },
                    onReveal?.let { action ->
                        CustomAccessibilityAction(revealLabel) { action(); true }
                    },
                ) else emptyList()
            }, contentAlignment = Alignment.CenterStart) {
            Box(Modifier.fillMaxWidth().padding(horizontal = metrics.cellHorizontalPadding)) {
                if (content == null) {
                    Text(value, color = if (selected) semantic.onSelection else semantic.onSurface,
                        style = BraceTheme.typography.body, maxLines = 1, overflow = TextOverflow.Ellipsis)
                } else CompositionLocalProvider(
                    LocalContentColor provides if (selected) semantic.onSelection else semantic.onSurface,
                ) { content() }
            }
        }
        if (enabled && onReveal != null) {
            Box(Modifier.align(Alignment.CenterEnd).width(BraceTheme.sizing.touchTarget)
                .fillMaxHeight().background(revealBackground, revealShape)
                .then(if (revealFocused) Modifier.border(BraceTheme.sizing.focusRingWidth,
                    semantic.focusRing, revealShape) else Modifier)
                .onFocusChanged { revealFocused = it.isFocused }
                .clickable(role = Role.Button, interactionSource = revealInteraction,
                    indication = null, onClickLabel = revealLabel, onClick = onReveal)
                .semantics {
                    contentDescription = revealLabel
                    this.traversalIndex = traversalIndex + 1f
                }
                .testTag("brace-table-reveal:$rowKey:$columnKey"),
                contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.brace_table_more), color = revealContentColor,
                    style = BraceTheme.typography.label,
                    modifier = Modifier.clearAndSetSemantics { })
            }
        }
    }

}

/**
 * Fixed column header used by [BraceDataTable]. [title] remains the full TalkBack label when
 * [content] replaces the visible title with a custom noninteractive presentation.
 * The table supplies its controlled selection, keyboard navigation, and optional resize handle.
 * Set [enabled] false while another header editor owns interaction. [sortState] and
 * [sortActionLabel] announce an optional sort action without changing the selection target.
 * [trailingInset] reserves room for adjacent sort and resize controls.
 * [onAddRegion] exposes a distinct TalkBack selection action. [pinState] announces a
 * frozen column without changing its sort state. [traversalIndex] orders independently
 * composed frozen and scrolling panes in a single logical grid.
 */
@Composable
fun BraceColumnHeader(
    title: String,
    columnIndex: Int,
    columnKey: String,
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onEdit: (() -> Unit)? = null,
    content: (@Composable () -> Unit)? = null,
    sortState: String? = null,
    sortActionLabel: String? = null,
    onSort: (() -> Unit)? = null,
    trailingInset: Dp = 0.dp,
    pinState: String? = null,
    onAddRegion: (() -> Unit)? = null,
    traversalIndex: Float = 0f,
) {
    require((sortActionLabel == null) == (onSort == null)) {
        "Sort action and label must be supplied together"
    }
    require(trailingInset >= 0.dp) { "Trailing inset cannot be negative" }
    require(columnIndex >= 0) { "Column index must be nonnegative" }
    require(columnKey.isNotBlank()) { "Column key must not be blank" }
    require(traversalIndex.isFinite()) { "Table traversal index must be finite" }
    val colors = BraceTheme.colors.components.table
    val semantic = BraceTheme.colors.semantic
    val metrics = BraceTheme.componentMetrics.table
    val selectLabel = stringResource(R.string.brace_table_select)
    val editLabel = stringResource(R.string.brace_table_edit_column_name)
    val addRegionLabel = stringResource(R.string.brace_table_add_region)
    val description = stringResource(R.string.brace_table_column_description, title, columnIndex + 1)
    Box(
        modifier.background(if (selected) colors.selectedRow else colors.header)
            .border(metrics.gridLineWidth, colors.gridLine)
            .pointerSelect("column:$columnKey", null, if (enabled) onSelect else ({}),
                onDoubleTap = if (enabled) onEdit else null)
            .clearAndSetSemantics {
                testTag = "brace-table-header:$columnKey"
                this.traversalIndex = traversalIndex
                heading()
                collectionItemInfo = CollectionItemInfo(0, 1, columnIndex + 1, 1)
                this.selected = selected
                contentDescription = description
                if (!enabled) disabled()
                if (sortState != null || pinState != null)
                    stateDescription = listOfNotNull(sortState, pinState).joinToString(", ")
                if (enabled) onClick(selectLabel) { onSelect(); true }
                customActions = listOfNotNull(
                    (if (enabled) onEdit else null)?.let { action ->
                        CustomAccessibilityAction(editLabel) { action(); true }
                    },
                    (if (enabled) onSort else null)?.let { action ->
                        CustomAccessibilityAction(requireNotNull(sortActionLabel)) { action(); true }
                    },
                    (if (enabled) onAddRegion else null)?.let { action ->
                        CustomAccessibilityAction(addRegionLabel) { action(); true }
                    },
                )
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(Modifier.fillMaxWidth().padding(end = trailingInset)) {
            if (content == null) {
                Text(title, modifier = Modifier.padding(horizontal = metrics.cellHorizontalPadding),
                    color = if (selected) semantic.onSelection else colors.headerContent,
                    style = BraceTheme.typography.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
            } else CompositionLocalProvider(
                LocalContentColor provides if (selected) semantic.onSelection else colors.headerContent,
            ) { content() }
        }
    }
}

/**
 * Fixed row header used by [BraceDataTable]. [rowLabel] is announced with the one-based row
 * number even when [content] replaces the visible ordinal. Selection is controlled by the
 * caller, while the table retains keyboard focus and any separate resize handle. [enabled]
 * pauses the selection action during a controlled table load. [onAddRegion] adds this
 * row to a disjoint selection through a separate TalkBack action. [pinState] announces
 * a frozen row. [traversalIndex] keeps its place before that row's cells in a split pane.
 */
@Composable
fun BraceRowHeader(
    rowLabel: String,
    rowIndex: Int,
    rowKey: String,
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onAddRegion: (() -> Unit)? = null,
    pinState: String? = null,
    content: (@Composable () -> Unit)? = null,
    traversalIndex: Float = 0f,
) {
    require(rowIndex >= 0) { "Row index must be nonnegative" }
    require(rowKey.isNotBlank()) { "Row key must not be blank" }
    require(traversalIndex.isFinite()) { "Table traversal index must be finite" }
    val colors = BraceTheme.colors.components.table
    val semantic = BraceTheme.colors.semantic
    val metrics = BraceTheme.componentMetrics.table
    val selectLabel = stringResource(R.string.brace_table_select)
    val addRegionLabel = stringResource(R.string.brace_table_add_region)
    val description = stringResource(R.string.brace_table_row_description, rowIndex + 1, rowLabel)
    Box(
        modifier.background(if (selected) colors.selectedRow else colors.header)
            .border(metrics.gridLineWidth, colors.gridLine)
            .then(if (enabled) Modifier.pointerSelect(rowKey, null, onSelect) else Modifier)
            .clearAndSetSemantics {
                testTag = "brace-table-row:$rowKey"
                this.traversalIndex = traversalIndex
                heading()
                collectionItemInfo = CollectionItemInfo(rowIndex + 1, 1, 0, 1)
                this.selected = selected
                contentDescription = description
                if (!enabled) disabled()
                if (pinState != null) stateDescription = pinState
                if (enabled) onClick(selectLabel) { onSelect(); true }
                customActions = if (enabled && onAddRegion != null)
                    listOf(CustomAccessibilityAction(addRegionLabel) { onAddRegion(); true })
                else emptyList()
            },
        contentAlignment = Alignment.Center,
    ) {
        if (content == null) {
            Text((rowIndex + 1).toString(),
                color = if (selected) semantic.onSelection else colors.headerContent,
                style = BraceTheme.typography.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
        } else CompositionLocalProvider(
            LocalContentColor provides if (selected) semantic.onSelection else colors.headerContent,
        ) { content() }
    }
}
