package io.github.joelromanpr.brace.table

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.CollectionItemInfo
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.collectionItemInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.testTag
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
 * that the cell remains visible in a frozen row, column, or their intersection.
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
    pinState: String? = null,
    content: (@Composable () -> Unit)? = null,
) {
    require(rowIndex >= 0 && columnIndex >= 0) { "Table cell coordinates must be nonnegative" }
    require(rowKey.isNotBlank() && columnKey.isNotBlank()) { "Table cell keys must not be blank" }
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
    Box(
        modifier.background(background)
            .hoverable(interaction)
            .border(if (selected && focused) BraceTheme.sizing.focusRingWidth else metrics.gridLineWidth,
                if (selected && focused) semantic.focusRing else colors.gridLine)
            .then(if (enabled) Modifier.pointerSelect(rowKey, columnKey, onSelect,
                onExtendSelection, onEdit) else Modifier)
            .clearAndSetSemantics {
                testTag = "brace-table-cell:$rowKey:$columnKey"
                collectionItemInfo = CollectionItemInfo(rowIndex + 1, 1, columnIndex + 1, 1)
                this.selected = selected
                contentDescription = description
                if (!enabled) disabled()
                if (pinState != null) stateDescription = pinState
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
                ) else emptyList()
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(Modifier.fillMaxWidth().padding(horizontal = metrics.cellHorizontalPadding)) {
            if (content == null) {
                Text(value, color = if (selected) semantic.onSelection else semantic.onSurface,
                    style = BraceTheme.typography.body, maxLines = 1, overflow = TextOverflow.Ellipsis)
            } else CompositionLocalProvider(
                LocalContentColor provides if (selected) semantic.onSelection else semantic.onSurface,
            ) { content() }
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
 * frozen column without changing its sort state.
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
) {
    require((sortActionLabel == null) == (onSort == null)) {
        "Sort action and label must be supplied together"
    }
    require(trailingInset >= 0.dp) { "Trailing inset cannot be negative" }
    require(columnIndex >= 0) { "Column index must be nonnegative" }
    require(columnKey.isNotBlank()) { "Column key must not be blank" }
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
 * a frozen row.
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
) {
    require(rowIndex >= 0) { "Row index must be nonnegative" }
    require(rowKey.isNotBlank()) { "Row key must not be blank" }
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
