package io.github.joelromanpr.brace.core

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import io.github.braceandroid.foundation.BraceTheme

/** Visual scale of a [BraceMenu]. Every size retains a 48 dp minimum action target. */
public enum class BraceMenuSize { Small, Medium, Large }

/** Semantic color intent of a [BraceMenuItem]. */
public enum class BraceMenuIntent { Default, Primary, Success, Warning, Danger }

private val LocalBraceMenuSize = staticCompositionLocalOf { BraceMenuSize.Medium }
private val LocalBraceMenuDismiss = staticCompositionLocalOf<(() -> Unit)?> { null }

/**
 * A static, vertically ordered list of [BraceMenuItem] actions and [BraceMenuDivider] groups.
 *
 * Use this inside a screen, dialog, or [BraceMenuPopup]. The caller owns menu contents and
 * item state; this component supplies Brace color, density, traversal grouping, and Up/Down
 * hardware-key focus movement. Unlike a web list, it has no DOM role or element reference.
 * [size] changes visual text and spacing while preserving Android touch targets.
 */
@Composable
public fun BraceMenu(
    modifier: Modifier = Modifier,
    size: BraceMenuSize = BraceMenuSize.Medium,
    content: @Composable ColumnScope.() -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val dismiss = LocalBraceMenuDismiss.current
    val colors = BraceTheme.colors.components.menu
    CompositionLocalProvider(LocalBraceMenuSize provides size) {
        Column(
            modifier = modifier
                .clip(RoundedCornerShape(BraceTheme.componentMetrics.menu.cornerRadius))
                .background(colors.container)
                .focusGroup()
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when (event.key) {
                        Key.DirectionDown -> focusManager.moveFocus(FocusDirection.Down)
                        Key.DirectionUp -> focusManager.moveFocus(FocusDirection.Up)
                        Key.Escape -> if (dismiss != null) { dismiss(); true } else false
                        else -> false
                    }
                }
                .semantics { isTraversalGroup = true }
                .padding(vertical = BraceTheme.spacing.xxs),
            content = content,
        )
    }
}

/**
 * An anchored menu for a button, field, or other [anchor].
 *
 * [expanded] is caller-owned so it can be restored with screen state. Android handles popup
 * placement, outside click, Back/Escape, and RTL placement. An enabled [BraceMenuItem] calls
 * [onDismissRequest] after its action by default. The popup surface consumes Brace tokens.
 */
@Composable
public fun BraceMenuPopup(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    anchor: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    size: BraceMenuSize = BraceMenuSize.Medium,
    content: @Composable ColumnScope.() -> Unit,
) {
    val semantic = BraceTheme.colors.semantic
    Box(modifier) {
        anchor()
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismissRequest,
            shape = RoundedCornerShape(BraceTheme.componentMetrics.menu.cornerRadius),
            containerColor = BraceTheme.colors.components.menu.container,
            tonalElevation = BraceTheme.elevation.none,
            shadowElevation = BraceTheme.elevation.floating,
            border = BorderStroke(BraceTheme.sizing.borderWidth, semantic.border),
        ) {
            CompositionLocalProvider(LocalBraceMenuDismiss provides onDismissRequest) {
                BraceMenu(size = size, content = content)
            }
        }
    }
}

/**
 * One keyboard, pointer, touch, and TalkBack operable menu action.
 *
 * [label] is required spoken text; [endLabel] is also spoken and can show a shortcut or
 * supporting value. [leadingIcon] and [trailingIcon] are decorative. [selected] is optional
 * because command menus do not always represent a choice; when supplied it is announced and
 * a check appears, independently of [active]'s transient hover/focus appearance. [multiline]
 * permits the primary label to wrap at large font scales. [accessibilityLabel] can replace
 * the combined spoken label with localized wording. [dismissOnClick] applies only in a
 * [BraceMenuPopup]. A navigation item calls the app's route action in [onClick], replacing
 * Blueprint's web link and target props.
 */
@Composable
public fun BraceMenuItem(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    selected: Boolean? = null,
    active: Boolean = false,
    intent: BraceMenuIntent = BraceMenuIntent.Default,
    endLabel: String? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    multiline: Boolean = false,
    dismissOnClick: Boolean = true,
    accessibilityLabel: String? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val hovered by interaction.collectIsHoveredAsState()
    val pressed by interaction.collectIsPressedAsState()
    val size = LocalBraceMenuSize.current
    val dismiss = LocalBraceMenuDismiss.current
    val semantic = BraceTheme.colors.semantic
    val colors = BraceTheme.colors.components.menu
    val metrics = BraceTheme.componentMetrics.menu
    val shape = RoundedCornerShape(BraceTheme.shape.sm)
    val selectedPair: Pair<Color, Color> = when (intent) {
        BraceMenuIntent.Default -> colors.itemSelected to semantic.onSelection
        BraceMenuIntent.Primary -> semantic.primarySubtle to semantic.onPrimarySubtle
        BraceMenuIntent.Success -> semantic.successSubtle to semantic.onSuccessSubtle
        BraceMenuIntent.Warning -> semantic.warningSubtle to semantic.onWarningSubtle
        BraceMenuIntent.Danger -> semantic.dangerSubtle to semantic.onDangerSubtle
    }
    val foreground = when {
        !enabled -> semantic.disabledContent
        selected == true -> selectedPair.second
        else -> when (intent) {
            BraceMenuIntent.Default -> colors.itemContent
            BraceMenuIntent.Primary -> semantic.primary
            BraceMenuIntent.Success -> semantic.success
            BraceMenuIntent.Warning -> semantic.warning
            BraceMenuIntent.Danger -> semantic.danger
        }
    }
    val background = when {
        !enabled -> Color.Transparent
        pressed -> semantic.pressed
        selected == true -> selectedPair.first
        focused || hovered || active -> colors.itemHover
        else -> Color.Transparent
    }
    val textStyle = when (size) {
        BraceMenuSize.Small -> BraceTheme.typography.label
        BraceMenuSize.Medium -> BraceTheme.typography.body
        BraceMenuSize.Large -> BraceTheme.typography.subtitle
    }
    val gap = when (size) {
        BraceMenuSize.Small -> BraceTheme.spacing.xs
        BraceMenuSize.Medium -> BraceTheme.spacing.sm
        BraceMenuSize.Large -> BraceTheme.spacing.md
    }
    val minHeight = when (size) {
        BraceMenuSize.Large -> BraceTheme.sizing.touchTarget + BraceTheme.spacing.sm
        else -> BraceTheme.sizing.touchTarget
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = BraceTheme.spacing.xxs)
            .defaultMinSize(minHeight = minHeight, minWidth = BraceTheme.sizing.touchTarget)
            .clip(shape)
            .background(background)
            .then(if (focused) Modifier.border(BraceTheme.sizing.focusRingWidth, semantic.focusRing, shape) else Modifier)
            .clickable(
                enabled = enabled,
                role = Role.Button,
                interactionSource = interaction,
                indication = null,
            ) {
                onClick()
                if (dismissOnClick) dismiss?.invoke()
            }
            .semantics(mergeDescendants = true) {
                if (selected != null) this.selected = selected
                if (accessibilityLabel != null) contentDescription = accessibilityLabel
            }
            .padding(
                horizontal = metrics.itemHorizontalPadding,
                vertical = if (size == BraceMenuSize.Large) BraceTheme.spacing.sm else BraceTheme.spacing.xs,
            ),
        horizontalArrangement = Arrangement.spacedBy(gap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selected != null) {
            Box(Modifier.size(BraceTheme.sizing.iconSm).clearAndSetSemantics { }) {
                if (selected) {
                    Canvas(Modifier.size(BraceTheme.sizing.iconSm)) {
                        val path = Path().apply {
                            moveTo(this@Canvas.size.width * 0.18f, this@Canvas.size.height * 0.52f)
                            lineTo(this@Canvas.size.width * 0.42f, this@Canvas.size.height * 0.76f)
                            lineTo(this@Canvas.size.width * 0.84f, this@Canvas.size.height * 0.25f)
                        }
                        drawPath(path, foreground, style = Stroke(width = this.size.minDimension * 0.12f, cap = StrokeCap.Round))
                    }
                }
            }
        }
        if (leadingIcon != null) {
            Box(Modifier.size(BraceTheme.sizing.iconSm).clearAndSetSemantics { }, contentAlignment = Alignment.Center) {
                leadingIcon()
            }
        }
        Text(
            text = label,
            color = foreground,
            style = textStyle,
            maxLines = if (multiline) Int.MAX_VALUE else 1,
            overflow = if (multiline) TextOverflow.Clip else TextOverflow.Ellipsis,
            modifier = Modifier.weight(if (endLabel == null) 1f else 2f),
        )
        if (endLabel != null) {
            Text(
                text = endLabel,
                color = if (enabled) colors.shortcutContent else semantic.disabledContent,
                style = BraceTheme.typography.caption,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f),
            )
        }
        if (trailingIcon != null) {
            Box(Modifier.size(BraceTheme.sizing.iconSm).clearAndSetSemantics { }, contentAlignment = Alignment.Center) {
                trailingIcon()
            }
        }
    }
}

/**
 * A noninteractive separator, optionally carrying a section [title].
 *
 * An untitled divider is excluded from TalkBack traversal. A titled divider announces a
 * nonactionable heading so the next group has context; its decorative rule is hidden.
 */
@Composable
public fun BraceMenuDivider(
    modifier: Modifier = Modifier,
    title: String? = null,
) {
    val semantic = BraceTheme.colors.semantic
    val spacing = BraceTheme.spacing
    if (title == null) {
        Spacer(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = BraceTheme.componentMetrics.menu.itemHorizontalPadding, vertical = spacing.xs)
                .height(BraceTheme.sizing.borderWidth)
                .background(semantic.border)
                .clearAndSetSemantics { },
        )
    } else {
        Column(
            modifier = modifier.fillMaxWidth()
                .padding(horizontal = BraceTheme.componentMetrics.menu.itemHorizontalPadding, vertical = spacing.sm),
            verticalArrangement = Arrangement.spacedBy(spacing.xs),
        ) {
            Text(title, color = semantic.onSurfaceMuted, style = BraceTheme.typography.label,
                modifier = Modifier.semantics { heading() })
            Spacer(
                Modifier.fillMaxWidth().height(BraceTheme.sizing.borderWidth)
                    .background(semantic.border).clearAndSetSemantics { },
            )
        }
    }
}
