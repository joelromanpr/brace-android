package io.github.joelromanpr.brace.core

import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupPositionProvider
import io.github.braceandroid.foundation.BraceTheme
import kotlin.math.max

/**
 * Brief, noninteractive help anchored to [target].
 *
 * Android shows the tooltip on pointer hover, touch long press, or keyboard focus. Material 3
 * handles touch and pointer triggers and the TalkBack show action. Brace holds the tooltip open
 * while its target has keyboard focus and keeps the popup inside the window. Keep the
 * [target] itself accessible with its own label and at least a 48 dp touch target. For controls or
 * longer content use [BracePopover] instead. [text] must be localized.
 *
 * The tooltip uses Brace semantic and component tokens for all color, type, shape, and spacing.
 * [enabled] suppresses user input and dismisses an already visible tooltip.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun BraceTooltip(
    text: String,
    target: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val state = rememberTooltipState(isPersistent = true)
    var focusWithin by remember { mutableStateOf(false) }
    val direction = LocalLayoutDirection.current
    val density = LocalDensity.current
    val edge = BraceTheme.spacing.sm
    val gap = BraceTheme.spacing.xs
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val positioner = remember(density, edge, gap) {
        BraceTooltipPositionProvider(
            with(density) { gap.roundToPx() },
            with(density) { edge.roundToPx() },
        )
    }
    val colors = BraceTheme.colors.components.tooltip
    val metrics = BraceTheme.componentMetrics.tooltip
    val shape = RoundedCornerShape(metrics.cornerRadius)

    LaunchedEffect(enabled, focusWithin) {
        if (enabled && focusWithin) state.show() else state.dismiss()
    }

    TooltipBox(
        positionProvider = positioner,
        tooltip = {
            CompositionLocalProvider(LocalLayoutDirection provides direction) {
                Box(
                    Modifier
                        .widthIn(max = minOf(metrics.maxWidth, (screenWidth - edge * 2).coerceAtLeast(BraceTheme.sizing.touchTarget)))
                        .shadow(BraceTheme.elevation.floating, shape)
                        .clip(shape)
                        .background(colors.container)
                        .border(BraceTheme.sizing.borderWidth, colors.border, shape)
                        .semantics {
                            liveRegion = LiveRegionMode.Polite
                            paneTitle = text
                        }
                        .testTag("BraceTooltip")
                        .padding(metrics.contentPadding),
                ) {
                    Text(text, color = colors.content, style = BraceTheme.typography.caption)
                }
            }
        },
        state = state,
        modifier = modifier,
        enableUserInput = enabled,
        content = {
            Box(Modifier.onFocusChanged { focusWithin = it.hasFocus }.focusGroup()) {
                target()
            }
        },
    )
}

/** Centers a tooltip on its anchor, then keeps it inside the visible window. */
internal class BraceTooltipPositionProvider(
    private val gap: Int,
    private val edge: Int,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val maxX = max(0, windowSize.width - popupContentSize.width)
        val leftEdge = minOf(edge, maxX)
        val rightEdge = max(leftEdge, maxX - edge)
        val centered = anchorBounds.left + (anchorBounds.width - popupContentSize.width) / 2
        val x = centered.coerceIn(leftEdge, rightEdge)
        val above = anchorBounds.top - popupContentSize.height - gap
        val below = anchorBounds.bottom + gap
        val preferredY = when {
            above >= edge -> above
            below + popupContentSize.height + edge <= windowSize.height -> below
            anchorBounds.top - edge - gap >= windowSize.height - anchorBounds.bottom - edge - gap -> above
            else -> below
        }
        val y = preferredY.coerceIn(0, max(0, windowSize.height - popupContentSize.height))
        return IntOffset(x, y)
    }
}
