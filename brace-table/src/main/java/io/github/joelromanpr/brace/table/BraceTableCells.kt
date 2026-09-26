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
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.stateDescription
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
 * range/edit callbacks are also exposed to touch, mouse, and TalkBack here.
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
    onExtendSelection: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
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
        hovered -> semantic.hover
        rowIndex % 2 == 0 -> colors.row
        else -> colors.alternateRow
    }
    val selectLabel = stringResource(R.string.brace_table_select)
    val extendLabel = stringResource(R.string.brace_table_extend_range)
    val editLabel = stringResource(R.string.brace_table_edit)
    val description = stringResource(R.string.brace_table_cell_description, columnTitle,
        rowIndex + 1, rowLabel, value)
    Box(
        modifier.background(background)
            .hoverable(interaction)
            .border(if (selected && focused) BraceTheme.sizing.focusRingWidth else metrics.gridLineWidth,
                if (selected && focused) semantic.focusRing else colors.gridLine)
            .pointerSelect(rowKey, columnKey, onSelect, onExtendSelection, onEdit)
            .clearAndSetSemantics {
                collectionItemInfo = CollectionItemInfo(rowIndex + 1, 1, columnIndex + 1, 1)
                this.selected = selected
                contentDescription = description
                onClick(selectLabel) { onSelect(); true }
                customActions = listOfNotNull(
                    onExtendSelection?.let { action ->
                        CustomAccessibilityAction(extendLabel) { action(); true }
                    },
                    onEdit?.let { action ->
                        CustomAccessibilityAction(editLabel) { action(); true }
                    },
                )
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
    val description = stringResource(R.string.brace_table_column_description, title, columnIndex + 1)
    Box(
        modifier.background(if (selected) colors.selectedRow else colors.header)
            .border(metrics.gridLineWidth, colors.gridLine)
            .pointerSelect("column:$columnKey", null, if (enabled) onSelect else ({}),
                onDoubleTap = if (enabled) onEdit else null)
            .clearAndSetSemantics {
                collectionItemInfo = CollectionItemInfo(0, 1, columnIndex + 1, 1)
                this.selected = selected
                contentDescription = description
                if (sortState != null) stateDescription = sortState
                if (enabled) onClick(selectLabel) { onSelect(); true }
                customActions = listOfNotNull(
                    (if (enabled) onEdit else null)?.let { action ->
                        CustomAccessibilityAction(editLabel) { action(); true }
                    },
                    (if (enabled) onSort else null)?.let { action ->
                        CustomAccessibilityAction(requireNotNull(sortActionLabel)) { action(); true }
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
 * caller, while the table retains keyboard focus and any separate resize handle.
 */
@Composable
fun BraceRowHeader(
    rowLabel: String,
    rowIndex: Int,
    rowKey: String,
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
    content: (@Composable () -> Unit)? = null,
) {
    require(rowIndex >= 0) { "Row index must be nonnegative" }
    require(rowKey.isNotBlank()) { "Row key must not be blank" }
    val colors = BraceTheme.colors.components.table
    val semantic = BraceTheme.colors.semantic
    val metrics = BraceTheme.componentMetrics.table
    val selectLabel = stringResource(R.string.brace_table_select)
    val description = stringResource(R.string.brace_table_row_description, rowIndex + 1, rowLabel)
    Box(
        modifier.background(if (selected) colors.selectedRow else colors.header)
            .border(metrics.gridLineWidth, colors.gridLine)
            .pointerSelect(rowKey, null, onSelect)
            .clearAndSetSemantics {
                collectionItemInfo = CollectionItemInfo(rowIndex + 1, 1, 0, 1)
                this.selected = selected
                contentDescription = description
                onClick(selectLabel) { onSelect(); true }
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
