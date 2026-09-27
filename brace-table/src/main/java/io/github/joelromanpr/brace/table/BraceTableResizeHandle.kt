package io.github.joelromanpr.brace.table

import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.focused
import androidx.compose.ui.semantics.requestFocus
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import io.github.braceandroid.foundation.BraceTheme

internal enum class BraceResizeAxis { Column, Row }

/** Visual grip is token-sized; its whole 48dp or larger box accepts touch, mouse, and focus. */
@Composable
internal fun BraceTableResizeHandle(
    axis: BraceResizeAxis,
    id: String,
    name: String,
    size: Dp,
    minimum: Dp,
    maximum: Dp?,
    width: Dp,
    height: Dp,
    description: String,
    stateLabel: String,
    increaseLabel: String,
    decreaseLabel: String,
    onSizeChange: (Dp) -> Unit,
    onFocusedChange: (Boolean) -> Unit,
    traversalIndex: Float = 0f,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val direction = LocalLayoutDirection.current
    val metrics = BraceTheme.componentMetrics.table
    val colors = BraceTheme.colors.components.table
    val semantic = BraceTheme.colors.semantic
    val step = BraceTheme.spacing.md
    val canIncrease = maximum == null || size < maximum
    val canDecrease = size > minimum
    val latestSize by rememberUpdatedState(size)
    val latestChange by rememberUpdatedState(onSizeChange)
    var focused by remember(axis, id) { mutableStateOf(false) }
    val focusRequester = remember(axis, id) { FocusRequester() }
    fun update(candidate: Dp) {
        val lower = maxOf(minimum, candidate)
        latestChange(if (maximum == null) lower else minOf(maximum, lower))
    }
    Box(
        modifier.width(width).height(height)
            .focusRequester(focusRequester)
            .onFocusChanged { focused = it.isFocused; onFocusedChange(it.isFocused) }
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown || event.isCtrlPressed || event.isAltPressed ||
                    event.isMetaPressed || event.isShiftPressed) return@onPreviewKeyEvent false
                val delta = when (axis) {
                    BraceResizeAxis.Column -> when (event.nativeKeyEvent.keyCode) {
                        AndroidKeyEvent.KEYCODE_DPAD_RIGHT -> if (direction == LayoutDirection.Rtl) -step else step
                        AndroidKeyEvent.KEYCODE_DPAD_LEFT -> if (direction == LayoutDirection.Rtl) step else -step
                        else -> null
                    }
                    BraceResizeAxis.Row -> when (event.nativeKeyEvent.keyCode) {
                        AndroidKeyEvent.KEYCODE_DPAD_DOWN -> step
                        AndroidKeyEvent.KEYCODE_DPAD_UP -> -step
                        else -> null
                    }
                } ?: return@onPreviewKeyEvent false
                update(latestSize + delta)
                true
            }
            .background(if (focused) colors.selectedRow else colors.header)
            .border(if (focused) BraceTheme.sizing.focusRingWidth else 0.dp,
                if (focused) semantic.focusRing else colors.header)
            .pointerInput(axis, id, density, direction, minimum, maximum) {
                var rawSizePx = 0f
                detectDragGestures(
                    onDragStart = { rawSizePx = with(density) { latestSize.toPx() } },
                    onDrag = { change, amount ->
                        change.consume()
                        val movement = if (axis == BraceResizeAxis.Row) amount.y else
                            amount.x * if (direction == LayoutDirection.Rtl) -1f else 1f
                        rawSizePx += movement
                        update(with(density) { rawSizePx.toDp() })
                    },
                )
            }
            .then(if (canIncrease) Modifier.clickable(onClickLabel = increaseLabel) {
                update(latestSize + step)
            } else Modifier)
            .clearAndSetSemantics {
                testTag = "brace-table-resize-${axis.name.lowercase()}:$id"
                this.traversalIndex = traversalIndex
                contentDescription = description.format(name)
                stateDescription = stateLabel
                if (!canIncrease && !canDecrease) disabled()
                this.focused = focused
                requestFocus { focusRequester.requestFocus(); true }
                if (canIncrease) onClick(increaseLabel) { update(latestSize + step); true }
                customActions = if (canDecrease) listOf(CustomAccessibilityAction(decreaseLabel) {
                    update(latestSize - step)
                    true
                }) else emptyList()
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.width(if (axis == BraceResizeAxis.Column) metrics.resizeHandleWidth else BraceTheme.spacing.lg)
                .height(if (axis == BraceResizeAxis.Column) BraceTheme.spacing.lg else metrics.resizeHandleWidth)
                .background(if (focused) semantic.focusRing else colors.resizeHandle),
        )
    }
}
