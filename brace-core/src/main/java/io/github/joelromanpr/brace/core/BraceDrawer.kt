package io.github.joelromanpr.brace.core

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.braceandroid.foundation.BraceDensity
import io.github.braceandroid.foundation.BraceTheme

/** Logical edge of the window occupied by a [BraceDrawer]. Start and End follow RTL. */
public enum class BraceDrawerPosition { Start, End, Top, Bottom }

/** Adaptive drawer extent. [BraceDrawer] also accepts an explicit custom extent. */
public enum class BraceDrawerSize { Small, Standard, Large }

/**
 * A controlled modal edge sheet for related content or a focused workflow.
 *
 * [position] chooses a logical side or the top/bottom edge. [size] uses the drawer tokens
 * and available window size; [customExtent] replaces that width or height in dp and is
 * clamped to a content-safe minimum and the window. Content scrolls while [footer] remains
 * at the edge of the sheet.
 * The Android dialog window contains focus and prevents interaction with the screen behind
 * it. The drawer participates in [BraceOverlayHost] ordering, so only the topmost overlay
 * receives Back, outside-touch, or close requests. The caller must set [open] to false in
 * [onDismissRequest] to close it.
 *
 * [title] is a visible heading and TalkBack pane name. For a titleless drawer, supply
 * [accessibilityTitle] and render a visible heading in [content]. The optional [headerIcon]
 * is decorative. The close action is a 48 dp touch/keyboard target; localize
 * [closeContentDescription] for the app language.
 *
 * This is a modal sheet. Persistent split-pane behavior is a separate future API.
 */
@Composable
public fun BraceDrawer(
    open: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    accessibilityTitle: String? = title,
    position: BraceDrawerPosition = BraceDrawerPosition.End,
    size: BraceDrawerSize = BraceDrawerSize.Standard,
    customExtent: Dp? = null,
    headerIcon: (@Composable () -> Unit)? = null,
    showCloseButton: Boolean = true,
    closeContentDescription: String = "Close drawer",
    dismissOnBackPress: Boolean = true,
    dismissOnClickOutside: Boolean = true,
    footer: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (!open) return

    val callerLayoutDirection = LocalLayoutDirection.current
    val sharedState = LocalBraceOverlayState.current
    val localState = rememberBraceOverlayState()
    val stack = sharedState ?: localState
    val id = remember { Any() }
    val currentDismissRequest by rememberUpdatedState(onDismissRequest)
    DisposableEffect(stack, id) {
        stack.register(id)
        onDispose { stack.unregister(id) }
    }

    val colors = BraceTheme.colors.components.drawer
    val metrics = BraceTheme.componentMetrics.drawer
    val inset = if (BraceTheme.density == BraceDensity.Compact) {
        metrics.contentPadding / 2
    } else {
        metrics.contentPadding
    }
    val shape = when (position) {
        BraceDrawerPosition.Start -> RoundedCornerShape(
            topEnd = metrics.cornerRadius,
            bottomEnd = metrics.cornerRadius,
        )
        BraceDrawerPosition.End -> RoundedCornerShape(
            topStart = metrics.cornerRadius,
            bottomStart = metrics.cornerRadius,
        )
        BraceDrawerPosition.Top -> RoundedCornerShape(
            bottomStart = metrics.cornerRadius,
            bottomEnd = metrics.cornerRadius,
        )
        BraceDrawerPosition.Bottom -> RoundedCornerShape(
            topStart = metrics.cornerRadius,
            topEnd = metrics.cornerRadius,
        )
    }
    val alignment = when (position) {
        BraceDrawerPosition.Start -> Alignment.CenterStart
        BraceDrawerPosition.End -> Alignment.CenterEnd
        BraceDrawerPosition.Top -> Alignment.TopCenter
        BraceDrawerPosition.Bottom -> Alignment.BottomCenter
    }
    val isSide = position == BraceDrawerPosition.Start || position == BraceDrawerPosition.End

    Dialog(
        onDismissRequest = {
            if (stack.isTopmost(id)) currentDismissRequest()
        },
        properties = DialogProperties(
            dismissOnBackPress = dismissOnBackPress,
            // A full-window scrim handles outside taps, including inner nested overlays.
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
        ),
    ) {
        CompositionLocalProvider(
            LocalBraceOverlayState provides stack,
            LocalLayoutDirection provides callerLayoutDirection,
        ) {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val availableExtent = if (isSide) maxWidth else maxHeight
                val fractionCap = availableExtent * 0.9f
                val touch = BraceTheme.sizing.touchTarget
                val headerPresent = title != null || headerIcon != null || showCloseButton
                val safeMinimum = if (isSide) {
                    touch * 2 + inset * 2 + BraceTheme.densityTokens.itemGapDp
                } else {
                    (if (headerPresent) touch + inset else 0.dp) +
                        touch + inset * 2 +
                        (if (footer != null) touch + inset * 2 + BraceTheme.sizing.borderWidth else 0.dp)
                }
                val resolvedExtent = (customExtent ?: when (size) {
                    BraceDrawerSize.Small -> minOf(metrics.maxWidth * 0.75f, fractionCap)
                    BraceDrawerSize.Standard -> if (isSide) {
                        minOf(metrics.maxWidth, fractionCap)
                    } else {
                        availableExtent * 0.5f
                    }
                    BraceDrawerSize.Large -> fractionCap
                }).coerceIn(minOf(safeMinimum, availableExtent), availableExtent)

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("BraceDrawerScrim")
                        .pointerInput(dismissOnClickOutside, stack) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                val beganOnTop = stack.isTopmost(id)
                                down.consume()
                                val up = waitForUpOrCancellation()
                                up?.consume()
                                if (
                                    up != null &&
                                    beganOnTop &&
                                    dismissOnClickOutside &&
                                    stack.isTopmost(id)
                                ) {
                                    currentDismissRequest()
                                }
                            }
                        },
                )
                Box(
                    modifier = modifier
                        .align(alignment)
                        .then(
                            if (isSide) Modifier.width(resolvedExtent).fillMaxHeight()
                            else Modifier.fillMaxWidth().height(resolvedExtent),
                        )
                        .shadow(BraceTheme.elevation.modal, shape)
                        .clip(shape)
                        .background(colors.container)
                        .border(BraceTheme.sizing.borderWidth, colors.border, shape)
                        .pointerInput(Unit) {
                            awaitPointerEventScope { while (true) awaitPointerEvent() }
                        }
                        .then(
                            if (accessibilityTitle != null) {
                                Modifier.semantics { paneTitle = accessibilityTitle }
                            } else {
                                Modifier
                            },
                        )
                        .semantics { isTraversalGroup = true },
                ) {
                    CompositionLocalProvider(LocalContentColor provides colors.content) {
                        Column(Modifier.fillMaxSize()) {
                            if (title != null || headerIcon != null || showCloseButton) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = inset, end = inset, top = inset),
                                    horizontalArrangement = Arrangement.spacedBy(
                                        BraceTheme.densityTokens.itemGapDp,
                                    ),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    if (headerIcon != null) {
                                        Box(
                                            modifier = Modifier
                                                .size(BraceTheme.sizing.iconMd)
                                                .clearAndSetSemantics { },
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            CompositionLocalProvider(
                                                LocalContentColor provides BraceTheme.colors.semantic.onSurfaceMuted,
                                            ) { headerIcon() }
                                        }
                                    }
                                    if (title != null) {
                                        Text(
                                            text = title,
                                            modifier = Modifier.weight(1f).semantics { heading() },
                                            color = colors.content,
                                            style = BraceTheme.typography.title,
                                        )
                                    } else {
                                        Spacer(Modifier.weight(1f))
                                    }
                                    if (showCloseButton) {
                                        DrawerCloseButton(
                                            label = closeContentDescription,
                                            onClick = {
                                                if (stack.isTopmost(id)) currentDismissRequest()
                                            },
                                        )
                                    }
                                }
                            }
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState())
                                    .padding(inset),
                                verticalArrangement = Arrangement.spacedBy(
                                    BraceTheme.densityTokens.itemGapDp,
                                ),
                                content = content,
                            )
                            if (footer != null) {
                                Column {
                                    Spacer(
                                        Modifier
                                            .fillMaxWidth()
                                            .height(BraceTheme.sizing.borderWidth)
                                            .background(colors.border),
                                    )
                                    FlowRow(
                                        modifier = Modifier.fillMaxWidth().padding(inset),
                                        horizontalArrangement = Arrangement.spacedBy(
                                            BraceTheme.densityTokens.itemGapDp,
                                            Alignment.End,
                                        ),
                                        verticalArrangement = Arrangement.spacedBy(
                                            BraceTheme.densityTokens.itemGapDp,
                                        ),
                                        content = { footer() },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DrawerCloseButton(label: String, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val hovered by interactionSource.collectIsHoveredAsState()
    val pressed by interactionSource.collectIsPressedAsState()
    val iconColor = BraceTheme.colors.semantic.onSurfaceMuted
    val shape = RoundedCornerShape(BraceTheme.componentMetrics.button.cornerRadius)
    Box(
        modifier = Modifier
            .size(BraceTheme.sizing.touchTarget)
            .clip(shape)
            .background(
                when {
                    pressed -> BraceTheme.colors.semantic.pressed
                    hovered -> BraceTheme.colors.semantic.hover
                    else -> Color.Transparent
                },
            )
            .then(
                if (focused) Modifier.border(
                    BraceTheme.sizing.focusRingWidth,
                    BraceTheme.colors.semantic.focusRing,
                    shape,
                ) else Modifier,
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(BraceTheme.sizing.iconSm)) {
            val stroke = size.minDimension / 9f
            drawLine(
                color = iconColor,
                start = Offset(size.width * 0.2f, size.height * 0.2f),
                end = Offset(size.width * 0.8f, size.height * 0.8f),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = iconColor,
                start = Offset(size.width * 0.8f, size.height * 0.2f),
                end = Offset(size.width * 0.2f, size.height * 0.8f),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
        }
    }
}
