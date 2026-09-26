package io.github.joelromanpr.brace.table

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import io.github.braceandroid.foundation.BraceTheme

/**
 * Controlled loading mask for [BraceDataTable]. The closest explicit override wins:
 * cell, then row, then column, then table. A map value of false keeps a cell visible even
 * if a broader scope is loading. Column/row header overrides take precedence over their
 * table-level flags. Use immutable maps and stable row/column keys.
 */
data class BraceTableLoading(
    val cells: Boolean = false,
    val columnHeaders: Boolean = false,
    val rowHeaders: Boolean = false,
    val columnCells: Map<String, Boolean> = emptyMap(),
    val rowCells: Map<String, Boolean> = emptyMap(),
    val cellOverrides: Map<BraceTableSelection.Cell, Boolean> = emptyMap(),
    val columnHeaderOverrides: Map<String, Boolean> = emptyMap(),
    val rowHeaderOverrides: Map<String, Boolean> = emptyMap(),
) {
    /** Whether one body cell displays a static skeleton. */
    fun bodyCell(rowKey: String, columnKey: String): Boolean =
        cellOverrides[BraceTableSelection.Cell(rowKey, columnKey)]
            ?: rowCells[rowKey]
            ?: columnCells[columnKey]
            ?: cells

    /** Whether one fixed column header displays a static skeleton. */
    fun columnHeader(columnKey: String): Boolean = columnHeaderOverrides[columnKey] ?: columnHeaders

    /** Whether one fixed row header displays a static skeleton. */
    fun rowHeader(rowKey: String): Boolean = rowHeaderOverrides[rowKey] ?: rowHeaders

    companion object {
        /** Mask every body cell and both header axes. */
        val All = BraceTableLoading(cells = true, columnHeaders = true, rowHeaders = true)
    }
}

/**
 * Caller-owned data-table state. [Ready] renders data, [Loading] draws static per-scope
 * skeletons, and [Empty]/[Error] replace the grid with one accessible status panel.
 *
 * A loading table is read-only: its selection, copy, edit, resize, and keyboard navigation
 * actions pause without changing caller-owned selection or viewport state. This avoids
 * announcing stale values as selectable while refresh is in progress. The caller may retain
 * existing rows to render in-place skeletons; an empty row list shows a loading panel.
 */
sealed interface BraceTableState {
    data object Ready : BraceTableState

    data class Loading(
        val loading: BraceTableLoading = BraceTableLoading.All,
        val message: String? = null,
    ) : BraceTableState {
        init { require(message == null || message.isNotBlank()) { "Loading message must not be blank" } }
    }

    data class Empty(val message: String? = null) : BraceTableState {
        init { require(message == null || message.isNotBlank()) { "Empty message must not be blank" } }
    }

    data class Error(
        val message: String,
        val retryLabel: String? = null,
        val onRetry: (() -> Unit)? = null,
    ) : BraceTableState {
        init {
            require(message.isNotBlank()) { "Error message must not be blank" }
            require(retryLabel == null || retryLabel.isNotBlank()) { "Retry label must not be blank" }
        }
    }
}

@Composable
internal fun BraceTableSkeleton(
    header: Boolean,
    tag: String,
    modifier: Modifier = Modifier,
) {
    val colors = BraceTheme.colors.components.table
    val semantic = BraceTheme.colors.semantic
    val metrics = BraceTheme.componentMetrics.table
    Box(
        modifier.background(if (header) colors.header else colors.row)
            .border(metrics.gridLineWidth, colors.gridLine)
            .clearAndSetSemantics { testTag = tag },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier.fillMaxWidth(0.6f)
                .padding(horizontal = metrics.cellHorizontalPadding)
                .height(BraceTheme.spacing.md)
                .clip(RoundedCornerShape(BraceTheme.shape.sm))
                .background(semantic.disabledContent),
        )
    }
}

@Composable
internal fun BraceTableStatusPanel(
    state: BraceTableState,
    label: String,
    height: Dp,
    modifier: Modifier = Modifier,
) {
    val semantic = BraceTheme.colors.semantic
    val colors = BraceTheme.colors.components.table
    val metrics = BraceTheme.componentMetrics.table
    val message = when (state) {
        is BraceTableState.Loading -> state.message ?: stringResource(R.string.brace_table_loading)
        is BraceTableState.Empty -> state.message ?: stringResource(R.string.brace_table_empty)
        is BraceTableState.Error -> state.message
        BraceTableState.Ready -> error("Ready has no status panel")
    }
    val retryLabel = if (state is BraceTableState.Error && state.onRetry != null)
        state.retryLabel ?: stringResource(R.string.brace_table_retry)
    else null
    Box(
        modifier.fillMaxWidth().height(height)
            .background(colors.row)
            .border(metrics.gridLineWidth, colors.gridLine)
            .testTag("brace-table-status"),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(BraceTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(BraceTheme.spacing.sm),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(label, modifier = Modifier.clearAndSetSemantics {},
                color = semantic.onSurfaceMuted, style = BraceTheme.typography.label)
            Text(
                message,
                modifier = Modifier.clearAndSetSemantics {
                        testTag = "brace-table-status-message"
                        contentDescription = "$label, $message"
                        liveRegion = if (state is BraceTableState.Error) LiveRegionMode.Assertive
                            else LiveRegionMode.Polite
                    },
                color = if (state is BraceTableState.Error) semantic.danger else semantic.onSurface,
                style = BraceTheme.typography.body,
            )
            if (state is BraceTableState.Error && retryLabel != null) {
                val interaction = remember { MutableInteractionSource() }
                val focused by interaction.collectIsFocusedAsState()
                val hovered by interaction.collectIsHoveredAsState()
                val pressed by interaction.collectIsPressedAsState()
                val button = BraceTheme.colors.components.button
                val shape = RoundedCornerShape(BraceTheme.componentMetrics.button.cornerRadius)
                val container = when {
                    pressed -> button.primaryPressedContainer
                    hovered -> button.primaryHoverContainer
                    else -> button.primaryContainer
                }
                Box(
                    Modifier.defaultMinSize(minWidth = BraceTheme.sizing.touchTarget,
                        minHeight = BraceTheme.sizing.touchTarget)
                        .clip(shape)
                        .background(container)
                        .border(if (focused) BraceTheme.sizing.focusRingWidth else
                            BraceTheme.sizing.borderWidth,
                            if (focused) button.focusRing else container, shape)
                        .clickable(interactionSource = interaction, indication = null,
                            role = Role.Button, onClick = state.onRetry!!)
                        .padding(horizontal = BraceTheme.componentMetrics.button.horizontalPadding,
                            vertical = BraceTheme.spacing.xs)
                        .testTag("brace-table-retry"),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(retryLabel, color = button.primaryContent, style = BraceTheme.typography.label)
                }
            }
        }
    }
}
