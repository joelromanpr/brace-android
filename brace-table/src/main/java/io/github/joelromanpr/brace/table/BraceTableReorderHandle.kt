package io.github.joelromanpr.brace.table

import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.focused
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.requestFocus
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import io.github.braceandroid.foundation.BraceTheme

internal enum class BraceReorderAxis { Column, Row }

/** Dedicated touch target so selection, editing, and resizing keep their own gestures. */
@Composable
internal fun BraceTableReorderHandle(
    axis: BraceReorderAxis,
    id: String,
    name: String,
    index: Int,
    count: Int,
    width: Dp,
    height: Dp,
    description: String,
    positionDescription: String,
    earlierLabel: String,
    laterLabel: String,
    onMoveTo: (Int) -> Unit,
    targetIndexForDrag: (Float) -> Int,
    onFocusedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val direction = LocalLayoutDirection.current
    val colors = BraceTheme.colors.components.table
    val semantic = BraceTheme.colors.semantic
    val metrics = BraceTheme.componentMetrics.table
    val focusRequester = remember(axis, id) { FocusRequester() }
    var isFocused by remember(axis, id) { mutableStateOf(false) }
    val latestIndex by rememberUpdatedState(index)
    val latestCount by rememberUpdatedState(count)
    val latestMove by rememberUpdatedState(onMoveTo)
    val latestTarget by rememberUpdatedState(targetIndexForDrag)
    val primaryTarget = if (index < count - 1) index + 1 else index - 1
    val primaryLabel = if (index < count - 1) laterLabel else earlierLabel
    fun moveTo(target: Int) {
        if (target in 0 until latestCount && target != latestIndex) latestMove(target)
    }
    LaunchedEffect(index, isFocused) {
        if (isFocused) {
            withFrameNanos { }
            focusRequester.requestFocus()
        }
    }
    Box(
        modifier.width(width).height(height)
            .focusRequester(focusRequester)
            .onFocusChanged { change ->
                isFocused = change.isFocused
                onFocusedChange(change.isFocused)
            }
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown || event.isAltPressed || event.isCtrlPressed ||
                    event.isMetaPressed || event.isShiftPressed) return@onPreviewKeyEvent false
                val target = when (axis) {
                    BraceReorderAxis.Row -> when (event.nativeKeyEvent.keyCode) {
                        AndroidKeyEvent.KEYCODE_DPAD_UP -> index - 1
                        AndroidKeyEvent.KEYCODE_DPAD_DOWN -> index + 1
                        AndroidKeyEvent.KEYCODE_MOVE_HOME -> 0
                        AndroidKeyEvent.KEYCODE_MOVE_END -> count - 1
                        else -> return@onPreviewKeyEvent false
                    }
                    BraceReorderAxis.Column -> when (event.nativeKeyEvent.keyCode) {
                        AndroidKeyEvent.KEYCODE_DPAD_LEFT -> index + if (direction == LayoutDirection.Rtl) 1 else -1
                        AndroidKeyEvent.KEYCODE_DPAD_RIGHT -> index + if (direction == LayoutDirection.Rtl) -1 else 1
                        AndroidKeyEvent.KEYCODE_MOVE_HOME -> 0
                        AndroidKeyEvent.KEYCODE_MOVE_END -> count - 1
                        else -> return@onPreviewKeyEvent false
                    }
                }
                moveTo(target)
                true
            }
            .background(if (isFocused) colors.selectedRow else colors.header)
            .border(if (isFocused) BraceTheme.sizing.focusRingWidth else 0.dp,
                if (isFocused) semantic.focusRing else colors.header)
            .pointerInput(axis, id, direction) {
                var delta = 0f
                detectDragGestures(
                    onDragStart = { delta = 0f },
                    onDragEnd = { moveTo(latestTarget(delta)) },
                    onDrag = { change, amount ->
                        change.consume()
                        delta += if (axis == BraceReorderAxis.Row) amount.y else
                            amount.x * if (direction == LayoutDirection.Rtl) -1f else 1f
                    },
                )
            }
            .testTag("brace-table-reorder-${axis.name.lowercase()}:$id")
            .clearAndSetSemantics {
                contentDescription = description.format(name)
                stateDescription = positionDescription.format(index + 1, count)
                liveRegion = LiveRegionMode.Polite
                focused = isFocused
                requestFocus { focusRequester.requestFocus(); true }
                onClick(primaryLabel) { moveTo(primaryTarget); true }
                customActions = listOfNotNull(
                    if (index > 0 && primaryTarget != index - 1)
                        CustomAccessibilityAction(earlierLabel) { moveTo(index - 1); true }
                    else null,
                    if (index < count - 1 && primaryTarget != index + 1)
                        CustomAccessibilityAction(laterLabel) { moveTo(index + 1); true }
                    else null,
                )
            }
            .clickable(onClickLabel = primaryLabel) { moveTo(primaryTarget) },
        contentAlignment = Alignment.Center,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(BraceTheme.spacing.xs)) {
            repeat(3) {
                Box(Modifier.width(BraceTheme.spacing.lg).height(metrics.resizeHandleWidth)
                    .background(if (isFocused) semantic.focusRing else colors.resizeHandle))
            }
        }
    }
}
