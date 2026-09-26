package io.github.joelromanpr.brace.core

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.braceandroid.foundation.BraceDensity
import io.github.braceandroid.foundation.BraceTheme

/**
 * A controlled modal window with a visible heading, scrollable body, and fixed action area.
 *
 * Set [open] from caller-owned state and close it in [onDismissRequest]. Back and outside-click
 * dismissal can be controlled separately. The underlying [BraceOverlay] supplies a separate
 * Android dialog window, modal focus behavior, and TalkBack pane semantics. [body] may contain
 * independently focusable fields and controls; it scrolls when the available height is small.
 * [actions] stays below that scroll area and wraps at narrow widths or large font sizes.
 *
 * [headerIcon] is decorative because the spoken [title] supplies its meaning.
 * The close control is a 48 dp touch and keyboard target. Localize [closeContentDescription]
 * for the current app language. When [title] is omitted, supply [accessibilityTitle]
 * to name the TalkBack pane and put a visible heading in [body].
 */
@Composable
public fun BraceDialog(
    open: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    accessibilityTitle: String? = title,
    headerIcon: (@Composable () -> Unit)? = null,
    showCloseButton: Boolean = true,
    closeContentDescription: String = "Close dialog",
    dismissOnBackPress: Boolean = true,
    dismissOnClickOutside: Boolean = true,
    actions: (@Composable () -> Unit)? = null,
    body: @Composable ColumnScope.() -> Unit,
) {
    if (!open) return
    val inset = dialogInset()
    val maxHeight = LocalConfiguration.current.screenHeightDp.dp * 0.9f
    BraceOverlay(
        open = open,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        title = accessibilityTitle,
        dismissOnBackPress = dismissOnBackPress,
        dismissOnClickOutside = dismissOnClickOutside,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxHeight)
                .semantics { isTraversalGroup = true },
        ) {
            if (title != null || headerIcon != null || showCloseButton) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = inset, end = inset, top = inset),
                    horizontalArrangement = Arrangement.spacedBy(BraceTheme.densityTokens.itemGapDp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (headerIcon != null) {
                        Box(
                            modifier = Modifier.size(BraceTheme.sizing.iconMd).clearAndSetSemantics { },
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
                            color = BraceTheme.colors.components.dialog.content,
                            style = BraceTheme.typography.title,
                        )
                    } else {
                        Spacer(Modifier.weight(1f))
                    }
                    if (showCloseButton) {
                        DialogCloseButton(closeContentDescription, onDismissRequest)
                    }
                }
            }
            BraceDialogBody(
                modifier = Modifier.weight(1f, fill = false),
                content = body,
            )
            if (actions != null) {
                BraceDialogActions(content = actions)
            }
        }
    }
}

/**
 * Scrollable, padded dialog content. It keeps child semantics and focus order intact.
 *
 * [BraceDialog] measures this body against the remaining dialog height so long content and
 * large text scroll while the action area remains visible. In a custom layout, the parent
 * must give it a finite height for scrolling to take effect.
 */
@Composable
public fun BraceDialogBody(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(dialogInset()),
        verticalArrangement = Arrangement.spacedBy(BraceTheme.densityTokens.itemGapDp),
        content = content,
    )
}

/**
 * A separated dialog footer whose actions wrap in reading order at narrow widths.
 *
 * Place less prominent actions before the primary action. Each supplied action must provide
 * its own semantics and touch target, for example [BraceButton]. The footer uses logical
 * end alignment, which follows RTL layout direction.
 */
@Composable
public fun BraceDialogActions(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth().semantics { isTraversalGroup = true }) {
        Spacer(
            Modifier
                .fillMaxWidth()
                .height(BraceTheme.sizing.borderWidth)
                .background(BraceTheme.colors.semantic.border),
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(dialogInset()),
            horizontalArrangement = Arrangement.spacedBy(
                BraceTheme.densityTokens.itemGapDp,
                Alignment.End,
            ),
            verticalArrangement = Arrangement.spacedBy(BraceTheme.densityTokens.itemGapDp),
            content = { content() },
        )
    }
}

/**
 * A controlled acknowledgement or confirmation alert.
 *
 * The confirm action is always present. [onCancel] adds a cancel action. As in Blueprint's
 * Alert, system Back and outside clicks do not cancel by default; enable either dismissal
 * flag only when [onCancel] is supplied. [loading] disables both cancellation and confirm
 * activation while showing the confirm button's loading state. The caller owns state changes
 * after [onConfirm] or [onCancel]. The optional [icon] is decorative; [title] and [message]
 * carry the spoken meaning. Localize button labels in the app language.
 */
@Composable
public fun BraceAlertDialog(
    open: Boolean,
    title: String,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    message: String? = null,
    confirmLabel: String = "OK",
    onCancel: (() -> Unit)? = null,
    cancelLabel: String = "Cancel",
    confirmIntent: BraceButtonIntent = BraceButtonIntent.Primary,
    loading: Boolean = false,
    dismissOnBackPress: Boolean = false,
    dismissOnClickOutside: Boolean = false,
    icon: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    if (!open) return
    val onDismiss = { if (!loading) onCancel?.invoke() }
    BraceDialog(
        open = open,
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = title,
        showCloseButton = false,
        dismissOnBackPress = dismissOnBackPress && onCancel != null && !loading,
        dismissOnClickOutside = dismissOnClickOutside && onCancel != null && !loading,
        actions = {
            if (onCancel != null) {
                BraceButton(
                    label = cancelLabel,
                    onClick = onCancel,
                    enabled = !loading,
                    intent = BraceButtonIntent.Secondary,
                )
            }
            BraceButton(
                label = confirmLabel,
                onClick = onConfirm,
                loading = loading,
                intent = confirmIntent,
            )
        },
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(BraceTheme.spacing.md),
            verticalAlignment = Alignment.Top,
        ) {
            if (icon != null) {
                val iconColor = when (confirmIntent) {
                    BraceButtonIntent.Primary -> BraceTheme.colors.semantic.primary
                    BraceButtonIntent.Secondary -> BraceTheme.colors.semantic.onSurfaceMuted
                    BraceButtonIntent.Danger -> BraceTheme.colors.semantic.danger
                }
                Box(
                    modifier = Modifier
                        .size(BraceTheme.sizing.iconLg)
                        .clearAndSetSemantics { },
                    contentAlignment = Alignment.Center,
                ) {
                    CompositionLocalProvider(LocalContentColor provides iconColor) { icon() }
                }
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(BraceTheme.densityTokens.itemGapDp),
            ) {
                if (message != null) {
                    Text(
                        text = message,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive },
                        color = BraceTheme.colors.components.dialog.content,
                        style = BraceTheme.typography.body,
                    )
                }
                content()
            }
        }
    }
}

@Composable
private fun dialogInset(): Dp = if (BraceTheme.density == BraceDensity.Compact) {
    BraceTheme.componentMetrics.dialog.contentPadding / 2
} else {
    BraceTheme.componentMetrics.dialog.contentPadding
}

@Composable
private fun DialogCloseButton(label: String, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val hovered by interactionSource.collectIsHoveredAsState()
    val pressed by interactionSource.collectIsPressedAsState()
    val iconColor = BraceTheme.colors.semantic.onSurfaceMuted
    val focusRing = BraceTheme.colors.semantic.focusRing
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(
        BraceTheme.componentMetrics.button.cornerRadius,
    )
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
                    focusRing,
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
