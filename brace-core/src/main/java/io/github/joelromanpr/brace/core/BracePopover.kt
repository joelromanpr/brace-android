package io.github.joelromanpr.brace.core

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import io.github.braceandroid.foundation.BraceTheme
import kotlin.math.max

/**
 * Logical placement relative to a [BracePopover] target. Start and End mirror in RTL.
 * Requested sides flip when the opposite side has enough room, then clamp to the window.
 */
public enum class BracePopoverPlacement {
    Auto, Top, TopStart, TopEnd, Bottom, BottomStart, BottomEnd,
    Start, StartTop, StartBottom, End, EndTop, EndBottom,
}

/**
 * A controlled, anchored surface for interactive content such as a filter form.
 *
 * [target] remains in normal layout and owns the trigger action; the caller changes [expanded]
 * in response to that action and to [onDismissRequest]. A focusable Compose popup handles
 * outside click and system Back. Escape is handled for hardware keyboards. The popup flips
 * and clamps within the visible window above the software keyboard, and an open popup registers
 * in [BraceOverlayHost] if present.
 * On close, focus is requested back to the first focusable descendant of [target].
 *
 * Give [title] a localized pane name for TalkBack. [content] should contain focusable controls
 * with their own labels. Large bodies should provide their own scrolling behavior. Unlike
 * Blueprint's PopoverNext, there are no DOM target wrappers, Floating UI middleware, or arrows.
 */
@Composable
public fun BracePopover(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    target: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    surfaceModifier: Modifier = Modifier,
    placement: BracePopoverPlacement = BracePopoverPlacement.Auto,
    title: String? = null,
    dismissOnBackPress: Boolean = true,
    dismissOnClickOutside: Boolean = true,
    content: @Composable () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    var wasExpanded by remember { mutableStateOf(false) }
    val currentDismiss by rememberUpdatedState(onDismissRequest)
    val stack = LocalBraceOverlayState.current
    val id = remember { Any() }
    val metrics = BraceTheme.componentMetrics.popover
    val colors = BraceTheme.colors.components.popover
    val sizing = BraceTheme.sizing
    val shape = RoundedCornerShape(metrics.cornerRadius)
    val density = LocalDensity.current
    val callerLayoutDirection = LocalLayoutDirection.current
    val screen = LocalConfiguration.current
    val maxWidth = max(48, screen.screenWidthDp - 2 * BraceTheme.spacing.sm.value.toInt()).dp
    val gapPx = with(density) { BraceTheme.spacing.xs.roundToPx() }
    val edgePx = with(density) { BraceTheme.spacing.sm.roundToPx() }
    val hostImeBottomPx = WindowInsets.ime.getBottom(density)
    var popupImeBottomPx by remember { mutableIntStateOf(0) }
    val imeBottomPx = maxOf(hostImeBottomPx, popupImeBottomPx)
    val imeBottomDp = with(density) { imeBottomPx.toDp() }
    val maxHeight = minOf(
        (screen.screenHeightDp * 0.8f).dp,
        maxOf(sizing.touchTarget, screen.screenHeightDp.dp - imeBottomDp - BraceTheme.spacing.sm * 2),
    )
    val positioner = remember(placement, gapPx, edgePx, callerLayoutDirection, imeBottomPx) {
        BracePopoverPositionProvider(placement, gapPx, edgePx, callerLayoutDirection, imeBottomPx)
    }

    LaunchedEffect(expanded) {
        if (!expanded && wasExpanded) focusRequester.requestFocus()
        wasExpanded = expanded
    }

    Box(modifier = modifier.focusRequester(focusRequester)) {
        target()
        if (expanded) {
            if (stack != null) {
                DisposableEffect(stack, id) {
                    stack.register(id)
                    onDispose { stack.unregister(id) }
                }
            }
            Popup(
                popupPositionProvider = positioner,
                onDismissRequest = {
                    if (stack == null || stack.isTopmost(id)) currentDismiss()
                },
                properties = PopupProperties(
                    focusable = true,
                    dismissOnBackPress = dismissOnBackPress,
                    dismissOnClickOutside = dismissOnClickOutside,
                ),
            ) {
                // A focusable Popup owns a separate Android window. Its IME insets may update
                // even when the anchor activity window reports zero.
                val popupIme = WindowInsets.ime.getBottom(LocalDensity.current)
                SideEffect { popupImeBottomPx = popupIme }
                DisposableEffect(Unit) { onDispose { popupImeBottomPx = 0 } }
                CompositionLocalProvider(LocalLayoutDirection provides callerLayoutDirection) {
                    Box(
                        modifier = surfaceModifier
                        .widthIn(max = minOf(metrics.maxWidth, maxWidth))
                        .heightIn(max = maxHeight)
                        .shadow(BraceTheme.elevation.floating, shape)
                        .clip(shape)
                        .background(colors.container)
                        .border(sizing.borderWidth, colors.border, shape)
                        .focusGroup()
                        .onPreviewKeyEvent { event ->
                            if (event.type == KeyEventType.KeyDown && event.key == Key.Escape &&
                                dismissOnBackPress && (stack == null || stack.isTopmost(id))
                            ) {
                                currentDismiss()
                                true
                            } else false
                        }
                        .then(if (title == null) Modifier else Modifier.semantics { paneTitle = title })
                        .padding(metrics.contentPadding),
                    ) {
                        CompositionLocalProvider(LocalContentColor provides colors.content, content = content)
                    }
                }
            }
        }
    }
}

internal class BracePopoverPositionProvider(
    private val placement: BracePopoverPlacement,
    private val gap: Int,
    private val edge: Int,
    private val callerLayoutDirection: LayoutDirection? = null,
    private val imeBottom: Int = 0,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val a = anchorBounds
        val p = popupContentSize
        val visibleHeight = max(edge * 2 + 1, windowSize.height - imeBottom.coerceAtLeast(0))
        val rtl = (callerLayoutDirection ?: layoutDirection) == LayoutDirection.Rtl
        val side = when (placement) {
            BracePopoverPlacement.Auto -> {
                val below = visibleHeight - a.bottom - edge - gap
                val above = a.top - edge - gap
                val logicalEnd = if (rtl) a.left else windowSize.width - a.right
                val logicalStart = if (rtl) windowSize.width - a.right else a.left
                when {
                    below >= p.height -> BracePopoverPlacement.BottomStart
                    above >= p.height -> BracePopoverPlacement.TopStart
                    logicalEnd >= p.width -> BracePopoverPlacement.EndTop
                    logicalStart >= p.width -> BracePopoverPlacement.StartTop
                    below >= above -> BracePopoverPlacement.BottomStart
                    else -> BracePopoverPlacement.TopStart
                }
            }
            else -> placement
        }
        val top = side in setOf(BracePopoverPlacement.Top, BracePopoverPlacement.TopStart, BracePopoverPlacement.TopEnd)
        val bottom = side in setOf(BracePopoverPlacement.Bottom, BracePopoverPlacement.BottomStart, BracePopoverPlacement.BottomEnd)
        val start = side in setOf(BracePopoverPlacement.Start, BracePopoverPlacement.StartTop, BracePopoverPlacement.StartBottom)
        val end = side in setOf(BracePopoverPlacement.End, BracePopoverPlacement.EndTop, BracePopoverPlacement.EndBottom)
        val oppositeRoom = when {
            top -> visibleHeight - a.bottom - edge - gap
            bottom -> a.top - edge - gap
            start -> if (rtl) a.left - edge - gap else windowSize.width - a.right - edge - gap
            else -> if (rtl) windowSize.width - a.right - edge - gap else a.left - edge - gap
        }
        val preferredRoom = when {
            top -> a.top - edge - gap
            bottom -> visibleHeight - a.bottom - edge - gap
            start -> if (rtl) windowSize.width - a.right - edge - gap else a.left - edge - gap
            else -> if (rtl) a.left - edge - gap else windowSize.width - a.right - edge - gap
        }
        val flip = preferredRoom < (if (top || bottom) p.height else p.width) &&
            oppositeRoom > preferredRoom
        val horizontalStart = if (rtl) a.right - p.width else a.left
        val horizontalEnd = if (rtl) a.left else a.right - p.width
        val x = when {
            top || bottom -> when (side) {
                BracePopoverPlacement.TopStart, BracePopoverPlacement.BottomStart -> horizontalStart
                BracePopoverPlacement.TopEnd, BracePopoverPlacement.BottomEnd -> horizontalEnd
                else -> a.left + (a.width - p.width) / 2
            }
            start -> if ((!rtl).xor(flip)) a.left - p.width - gap else a.right + gap
            else -> if (rtl.xor(flip)) a.left - p.width - gap else a.right + gap
        }
        val y = when {
            top -> if (flip) a.bottom + gap else a.top - p.height - gap
            bottom -> if (flip) a.top - p.height - gap else a.bottom + gap
            side == BracePopoverPlacement.StartTop || side == BracePopoverPlacement.EndTop -> a.top
            side == BracePopoverPlacement.StartBottom || side == BracePopoverPlacement.EndBottom -> a.bottom - p.height
            else -> a.top + (a.height - p.height) / 2
        }
        return IntOffset(
            x.coerceIn(edge, max(edge, windowSize.width - p.width - edge)),
            y.coerceIn(edge, max(edge, visibleHeight - p.height - edge)),
        )
    }
}
