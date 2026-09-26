package io.github.joelromanpr.brace.core

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.LayoutDirection
import io.github.braceandroid.foundation.BraceContrast
import io.github.braceandroid.foundation.BraceTheme

/** Solid, outlined, or quiet treatment shared by all actions in a [BraceButtonGroup]. */
public enum class BraceButtonGroupVariant { Solid, Outline, Minimal }

/** Visual size of a group action. The interactive target remains at least 48 dp. */
public enum class BraceButtonGroupSize { Small, Medium, Large }

/** Logical horizontal content alignment. Start and end mirror in RTL. */
public enum class BraceButtonGroupAlignment { Start, Center, End }

/**
 * One independently operable action in a [BraceButtonGroup].
 *
 * [key] is stable across reorder and must be unique inside its group. [label] is always the
 * accessible action name. Set [showLabel] to false only when an icon slot gives visual context;
 * the label remains spoken to TalkBack. The caller owns [enabled], [loading], and [selected].
 * `selected = null` means an ordinary command; true or false exposes a controlled toggle state.
 * Icons are decorative and should use null content descriptions.
 */
public class BraceButtonGroupAction(
    public val key: String,
    public val label: String,
    public val onClick: () -> Unit,
    public val enabled: Boolean = true,
    public val loading: Boolean = false,
    public val selected: Boolean? = null,
    public val intent: BraceButtonIntent = BraceButtonIntent.Secondary,
    public val showLabel: Boolean = true,
    public val leadingIcon: (@Composable () -> Unit)? = null,
    public val trailingIcon: (@Composable () -> Unit)? = null,
)

/**
 * Connected native action group corresponding to Blueprint ButtonGroup 6.18.0.
 *
 * The group arranges related actions horizontally or vertically while retaining one button
 * semantics node, keyboard focus stop, and 48 dp touch target per action. [fill] gives horizontal
 * actions equal width or stretches a vertical group and its children to available width. Unlike
 * Blueprint's CSS `fill` in a vertical flexbox, it never requests infinite height in a scrolling
 * Compose parent. [accessibilityLabel] names the traversal group without merging child actions.
 * The caller owns every action and state; this component does not enforce single selection.
 * [loadingDescription] should be localized by the host application.
 *
 * `modifier` changes the whole group. For mixed form controls use BraceControlGroup instead.
 * For icon-only actions set [BraceButtonGroupAction.showLabel] false and provide an icon slot.
 */
@Composable
public fun BraceButtonGroup(
    actions: List<BraceButtonGroupAction>,
    modifier: Modifier = Modifier,
    vertical: Boolean = false,
    fill: Boolean = false,
    variant: BraceButtonGroupVariant = BraceButtonGroupVariant.Solid,
    size: BraceButtonGroupSize = BraceButtonGroupSize.Medium,
    alignment: BraceButtonGroupAlignment = BraceButtonGroupAlignment.Center,
    accessibilityLabel: String? = null,
    loadingDescription: String = "Loading",
) {
    require(actions.isNotEmpty()) { "Button group needs at least one action" }
    require(actions.all { it.key.isNotBlank() && it.label.isNotBlank() }) {
        "Button group actions need nonblank keys and labels"
    }
    require(actions.map { it.key }.distinct().size == actions.size) { "Button group action keys must be unique" }
    require(actions.all { it.showLabel || it.leadingIcon != null || it.trailingIcon != null }) {
        "An icon-only button needs an icon slot"
    }
    require(accessibilityLabel == null || accessibilityLabel.isNotBlank()) {
        "Button group accessibility label must not be blank"
    }
    require(loadingDescription.isNotBlank()) { "Loading description must not be blank" }

    val colors = BraceTheme.colors.components.buttonGroup
    val metrics = BraceTheme.componentMetrics.buttonGroup
    val shape = RoundedCornerShape(metrics.cornerRadius)
    val outlined = variant != BraceButtonGroupVariant.Minimal || BraceTheme.contrast == BraceContrast.High
    val groupModifier = modifier
        .then(if (fill) Modifier.fillMaxWidth() else if (vertical) Modifier.width(IntrinsicSize.Max) else Modifier)
        .focusGroup()
        .semantics {
            isTraversalGroup = true
            if (accessibilityLabel != null) contentDescription = accessibilityLabel
        }
        .clip(shape)
        .background(colors.container)
        .then(if (outlined) Modifier.border(BraceTheme.sizing.borderWidth, colors.border, shape) else Modifier)

    if (vertical) {
        Column(groupModifier) {
            actions.forEachIndexed { index, action ->
                key(action.key) {
                    GroupAction(
                        action = action,
                        modifier = Modifier.fillMaxWidth(),
                        vertical = true,
                        showSeparator = index > 0,
                        variant = variant,
                        size = size,
                        alignment = alignment,
                        loadingDescription = loadingDescription,
                    )
                }
            }
        }
    } else {
        Row(groupModifier, verticalAlignment = Alignment.CenterVertically) {
            actions.forEachIndexed { index, action ->
                key(action.key) {
                    GroupAction(
                        action = action,
                        modifier = if (fill) Modifier.weight(1f) else Modifier,
                        vertical = false,
                        showSeparator = index > 0,
                        variant = variant,
                        size = size,
                        alignment = alignment,
                        loadingDescription = loadingDescription,
                    )
                }
            }
        }
    }
}

@Composable
private fun GroupAction(
    action: BraceButtonGroupAction,
    modifier: Modifier,
    vertical: Boolean,
    showSeparator: Boolean,
    variant: BraceButtonGroupVariant,
    size: BraceButtonGroupSize,
    alignment: BraceButtonGroupAlignment,
    loadingDescription: String,
) {
    val source = remember(action.key) { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val hovered by source.collectIsHoveredAsState()
    val focused by source.collectIsFocusedAsState()
    val tokens = BraceTheme.colors.components.buttonGroup
    val button = BraceTheme.colors.components.button
    val semantic = BraceTheme.colors.semantic
    val metrics = BraceTheme.componentMetrics.buttonGroup
    val unavailable = !action.enabled || action.loading
    val container: Color
    val content: Color
    when {
        unavailable -> {
            container = tokens.disabledContainer
            content = tokens.disabledContent
        }
        action.selected == true -> {
            container = tokens.selectedContainer
            content = tokens.selectedContent
        }
        variant == BraceButtonGroupVariant.Solid && action.intent == BraceButtonIntent.Primary -> {
            container = when {
                pressed -> button.primaryPressedContainer
                hovered -> button.primaryHoverContainer
                else -> button.primaryContainer
            }
            content = button.primaryContent
        }
        variant == BraceButtonGroupVariant.Solid && action.intent == BraceButtonIntent.Danger -> {
            container = when {
                pressed -> button.dangerPressedContainer
                hovered -> button.dangerHoverContainer
                else -> button.dangerContainer
            }
            content = button.dangerContent
        }
        else -> {
            container = when {
                pressed -> tokens.pressedContainer
                hovered -> tokens.hoverContainer
                variant == BraceButtonGroupVariant.Minimal -> Color.Transparent
                else -> tokens.container
            }
            content = when (action.intent) {
                BraceButtonIntent.Primary -> semantic.primary
                BraceButtonIntent.Secondary -> tokens.content
                BraceButtonIntent.Danger -> semantic.danger
            }
        }
    }
    val visualHeight = when (size) {
        BraceButtonGroupSize.Small -> metrics.smallVisualHeight
        BraceButtonGroupSize.Medium -> metrics.mediumVisualHeight
        BraceButtonGroupSize.Large -> metrics.largeVisualHeight
    }
    val contentAlignment = when (alignment) {
        BraceButtonGroupAlignment.Start -> Alignment.CenterStart
        BraceButtonGroupAlignment.Center -> Alignment.Center
        BraceButtonGroupAlignment.End -> Alignment.CenterEnd
    }
    val separatorColor = tokens.divider
    val separatorWidth = metrics.separatorWidth
    Box(
        modifier = modifier
            .defaultMinSize(minWidth = BraceTheme.sizing.touchTarget, minHeight = BraceTheme.sizing.touchTarget)
            .heightIn(min = visualHeight)
            .background(container)
            .drawWithContent {
                drawContent()
                if (showSeparator && variant != BraceButtonGroupVariant.Minimal) {
                    val width = separatorWidth.toPx()
                    if (vertical) {
                        drawLine(separatorColor, Offset(0f, width / 2f), Offset(this.size.width, width / 2f), width)
                    } else {
                        val x = if (layoutDirection == LayoutDirection.Rtl) this.size.width - width / 2f else width / 2f
                        drawLine(separatorColor, Offset(x, 0f), Offset(x, this.size.height), width)
                    }
                }
            }
            .onKeyEvent { event ->
                if (!unavailable && event.type == KeyEventType.KeyUp &&
                    (event.key == Key.Enter || event.key == Key.Spacebar)
                ) {
                    action.onClick()
                    true
                } else false
            }
            .focusable(enabled = !unavailable, interactionSource = source)
            .clearAndSetSemantics {
                contentDescription = action.label
                role = Role.Button
                if (!unavailable) onClick { action.onClick(); true } else disabled()
                if (action.selected != null) selected = action.selected
                if (action.loading) stateDescription = loadingDescription
            }
            .clickable(
                enabled = !unavailable,
                role = Role.Button,
                interactionSource = source,
                indication = null,
                onClick = action.onClick,
            )
            .then(if (focused) Modifier.border(BraceTheme.sizing.focusRingWidth, tokens.focusRing) else Modifier),
        contentAlignment = contentAlignment,
    ) {
        Row(
            modifier = Modifier
                .widthIn(min = BraceTheme.sizing.touchTarget)
                .padding(horizontal = metrics.horizontalPadding, vertical = BraceTheme.spacing.xs)
                .clearAndSetSemantics { },
            horizontalArrangement = Arrangement.spacedBy(metrics.iconGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (!action.loading) action.leadingIcon?.invoke()
            if (action.showLabel || action.loading) {
                Text(
                    text = if (action.loading) "…" else action.label,
                    color = content,
                    style = BraceTheme.typography.label,
                )
            }
            if (!action.loading) action.trailingIcon?.invoke()
        }
    }
}
