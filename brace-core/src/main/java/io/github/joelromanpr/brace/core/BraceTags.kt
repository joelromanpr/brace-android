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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.focused
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.requestFocus
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import io.github.braceandroid.foundation.BraceTheme

/** The semantic intent used by [BraceTag] and [BraceCompoundTag]. */
public enum class BraceTagIntent { Default, Primary, Success, Warning, Danger }

/** The medium and large sizes documented for Blueprint tags. */
public enum class BraceTagSize { Medium, Large }

/**
 * A short label with optional independent activation and remove actions.
 *
 * [onClick] makes the label one button-like keyboard, mouse, and touch target;
 * [onRemove] adds a separate target so removing a tag never activates it.
 * The two actions retain 48 dp hit areas in compact density. The caller owns
 * [selected] and removal from its data set. [active] is a visual pressed state.
 * Leading and trailing icon slots are decorative; for an icon-only tag, supply
 * a non-empty [accessibilityLabel]. The default remove description is English;
 * apps should pass a localized [removeContentDescription].
 *
 * [multiline] allows text to wrap at large font scales; otherwise the visual
 * label ellipsizes while accessibility retains its full string. [fill] expands
 * to the available width. Compose [Modifier] replaces HTML span attributes,
 * and [size] replaces Blueprint's deprecated `large` property.
 * [content] may replace the visual label with rich Compose content; [label]
 * remains the spoken label. Do not put independently interactive controls
 * inside that slot.
 */
@Composable
public fun BraceTag(
    label: String,
    modifier: Modifier = Modifier,
    accessibilityLabel: String = label,
    intent: BraceTagIntent = BraceTagIntent.Default,
    size: BraceTagSize = BraceTagSize.Medium,
    minimal: Boolean = false,
    fill: Boolean = false,
    rounded: Boolean = false,
    multiline: Boolean = false,
    active: Boolean = false,
    selected: Boolean = false,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    onRemove: (() -> Unit)? = null,
    removeContentDescription: String? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    content: (@Composable RowScope.() -> Unit)? = null,
) {
    TagScaffold(
        modifier = modifier,
        accessibilityLabel = accessibilityLabel,
        intent = intent,
        size = size,
        minimal = minimal,
        fill = fill,
        rounded = rounded,
        active = active,
        selected = selected,
        enabled = enabled,
        onClick = onClick,
        onRemove = onRemove,
        removeContentDescription = removeContentDescription,
    ) { appearance ->
        if (leadingIcon != null) DecorativeTagIcon(leadingIcon)
        if (content != null) {
            CompositionLocalProvider(LocalContentColor provides appearance.content) { content() }
        } else {
            Text(
                text = label,
                modifier = if (fill) Modifier.weight(1f) else Modifier,
                color = appearance.content,
                style = if (size == BraceTagSize.Large) BraceTheme.typography.bodyStrong else BraceTheme.typography.label,
                maxLines = if (multiline) Int.MAX_VALUE else 1,
                overflow = if (multiline) TextOverflow.Clip else TextOverflow.Ellipsis,
            )
        }
        if (trailingIcon != null) DecorativeTagIcon(trailingIcon)
    }
}

/**
 * A two-part tag that presents [label] and [value] as a key-value pair.
 *
 * The pair is one reading unit in logical label-then-value order, including
 * in RTL layouts. [onClick] and [onRemove] create separate focus stops and
 * 48 dp hit targets. [selected] is caller-controlled; [active] is a visual
 * pressed state. Icon slots are decorative. Pass a localized
 * [removeContentDescription] when removable. The HTML `span`, `title`,
 * `tabIndex`, and deprecated `rightIcon` APIs map to Compose modifiers,
 * Android semantics, and [trailingIcon] rather than separate components.
 * [labelContent] and [valueContent] may replace their visual strings with
 * rich Compose content while [accessibilityLabel] preserves a single spoken
 * key-value description. Built-in strings share bounded space and ellipsize
 * independently in narrow layouts; rich slots should manage their own
 * overflow. Keep nested controls outside these slots.
 */
@Composable
public fun BraceCompoundTag(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    accessibilityLabel: String = "$label: $value",
    intent: BraceTagIntent = BraceTagIntent.Default,
    size: BraceTagSize = BraceTagSize.Medium,
    minimal: Boolean = false,
    fill: Boolean = false,
    rounded: Boolean = false,
    active: Boolean = false,
    selected: Boolean = false,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    onRemove: (() -> Unit)? = null,
    removeContentDescription: String? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    labelContent: (@Composable RowScope.() -> Unit)? = null,
    valueContent: (@Composable RowScope.() -> Unit)? = null,
) {
    TagScaffold(
        modifier = modifier,
        accessibilityLabel = accessibilityLabel,
        intent = intent,
        size = size,
        minimal = minimal,
        fill = fill,
        rounded = rounded,
        active = active,
        selected = selected,
        enabled = enabled,
        onClick = onClick,
        onRemove = onRemove,
        removeContentDescription = removeContentDescription,
        horizontalPadding = BraceTheme.spacing.none,
        gap = BraceTheme.spacing.none,
    ) { appearance ->
        val segmentPadding = BraceTheme.componentMetrics.tag.horizontalPadding +
            if (size == BraceTagSize.Large) BraceTheme.spacing.xs else BraceTheme.spacing.none
        Row(
            modifier = Modifier
                .weight(1f, fill = fill)
                .clipToBounds()
                .background(appearance.leftContainer)
                .padding(horizontal = segmentPadding, vertical = BraceTheme.spacing.xxs),
            horizontalArrangement = Arrangement.spacedBy(BraceTheme.spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leadingIcon != null) DecorativeTagIcon(leadingIcon)
            if (labelContent != null) {
                CompositionLocalProvider(LocalContentColor provides appearance.leftContent) {
                    labelContent()
                }
            } else {
                Text(
                    text = label,
                    color = appearance.leftContent,
                    style = if (size == BraceTagSize.Large) BraceTheme.typography.bodyStrong else BraceTheme.typography.label,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Row(
            modifier = Modifier
                .weight(1f, fill = fill)
                .clipToBounds()
                .padding(horizontal = segmentPadding, vertical = BraceTheme.spacing.xxs),
            horizontalArrangement = Arrangement.spacedBy(BraceTheme.spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (valueContent != null) {
                CompositionLocalProvider(LocalContentColor provides appearance.content) {
                    valueContent()
                }
            } else {
                Text(
                    text = value,
                    modifier = if (fill) Modifier.weight(1f) else Modifier,
                    color = appearance.content,
                    style = if (size == BraceTagSize.Large) BraceTheme.typography.bodyStrong else BraceTheme.typography.label,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (trailingIcon != null) DecorativeTagIcon(trailingIcon)
        }
    }
}

private data class TagAppearance(
    val container: Color,
    val content: Color,
    val border: Color,
    val leftContainer: Color,
    val leftContent: Color,
)

@Composable
private fun TagScaffold(
    modifier: Modifier,
    accessibilityLabel: String,
    intent: BraceTagIntent,
    size: BraceTagSize,
    minimal: Boolean,
    fill: Boolean,
    rounded: Boolean,
    active: Boolean,
    selected: Boolean,
    enabled: Boolean,
    onClick: (() -> Unit)?,
    onRemove: (() -> Unit)?,
    removeContentDescription: String?,
    horizontalPadding: Dp = BraceTheme.componentMetrics.tag.horizontalPadding,
    gap: Dp = BraceTheme.spacing.xs,
    content: @Composable RowScope.(TagAppearance) -> Unit,
) {
    val semantic = BraceTheme.colors.semantic
    val interactionSource = remember { MutableInteractionSource() }
    val mainFocusRequester = remember { FocusRequester() }
    val pressed by interactionSource.collectIsPressedAsState()
    val hovered by interactionSource.collectIsHoveredAsState()
    val focused by interactionSource.collectIsFocusedAsState()
    val appearance = tagAppearance(intent, minimal, enabled, pressed, hovered, active, selected)
    val shape: Shape = RoundedCornerShape(
        if (rounded) BraceTheme.shape.pill else BraceTheme.componentMetrics.tag.cornerRadius,
    )
    val visualHeight = BraceTheme.densityTokens.controlHeightDp -
        if (size == BraceTagSize.Medium) BraceTheme.spacing.sm else BraceTheme.spacing.none
    val padding = horizontalPadding + if (size == BraceTagSize.Large) BraceTheme.spacing.xs else BraceTheme.spacing.none
    val spacing = if (size == BraceTagSize.Large) gap + BraceTheme.spacing.xs else gap
    val hasAction = onClick != null
    val minimumMainHeight = if (hasAction) {
        maxOf(visualHeight, BraceTheme.sizing.touchTarget)
    } else visualHeight
    val description = removeContentDescription ?: if (accessibilityLabel.isBlank()) {
        "Remove tag"
    } else "Remove $accessibilityLabel"

    Row(
        modifier = modifier
            .then(if (fill) Modifier.fillMaxWidth() else Modifier)
            .clip(shape)
            .background(appearance.container)
            .border(BraceTheme.sizing.borderWidth, appearance.border, shape),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = (if (fill || onRemove != null) Modifier.weight(1f, fill = fill) else Modifier)
                .heightIn(min = minimumMainHeight)
                .then(if (hasAction) Modifier.focusRequester(mainFocusRequester) else Modifier)
                .then(if (hasAction) Modifier.defaultMinSize(
                    minWidth = BraceTheme.sizing.touchTarget,
                ) else Modifier)
                .then(if (focused) Modifier.border(
                    BraceTheme.sizing.focusRingWidth,
                    semantic.focusRing,
                    shape,
                ) else Modifier)
                .then(if (hasAction) Modifier.clearAndSetSemantics {
                    contentDescription = accessibilityLabel
                    role = Role.Button
                    if (selected) this.selected = true
                    if (!enabled) disabled() else {
                        this.focused = focused
                        onClick(accessibilityLabel) { requireNotNull(onClick).invoke(); true }
                        requestFocus { mainFocusRequester.requestFocus() }
                    }
                } else Modifier.semantics(mergeDescendants = true) {
                    contentDescription = accessibilityLabel
                    if (selected) this.selected = true
                    if (!enabled) disabled()
                })
                .then(if (hasAction) Modifier.clickable(
                    enabled = enabled,
                    role = Role.Button,
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = requireNotNull(onClick),
                ) else Modifier)
                .padding(horizontal = padding, vertical = BraceTheme.spacing.xxs),
            horizontalArrangement = Arrangement.spacedBy(spacing),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            content(appearance)
        }
        if (onRemove != null) {
            val removeInteraction = remember { MutableInteractionSource() }
            val removeFocusRequester = remember { FocusRequester() }
            val removeFocused by removeInteraction.collectIsFocusedAsState()
            Box(
                modifier = Modifier
                    .defaultMinSize(
                        minWidth = BraceTheme.sizing.touchTarget,
                        minHeight = BraceTheme.sizing.touchTarget,
                    )
                    .focusRequester(removeFocusRequester)
                    .then(if (removeFocused) Modifier.border(
                        BraceTheme.sizing.focusRingWidth,
                        semantic.focusRing,
                        shape,
                    ) else Modifier)
                    .clearAndSetSemantics {
                        contentDescription = description
                        role = Role.Button
                        if (!enabled) disabled() else {
                            this.focused = removeFocused
                            onClick(description) { onRemove(); true }
                            requestFocus { removeFocusRequester.requestFocus() }
                        }
                    }
                    .clickable(
                        enabled = enabled,
                        role = Role.Button,
                        interactionSource = removeInteraction,
                        indication = null,
                        onClick = onRemove,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                val markColor = appearance.content
                val stroke = BraceTheme.sizing.borderStrongWidth
                Canvas(Modifier.size(BraceTheme.sizing.iconSm).clearAndSetSemantics { }) {
                    val canvasWidth = this.size.width
                    val canvasHeight = this.size.height
                    drawLine(
                        color = markColor,
                        start = androidx.compose.ui.geometry.Offset(canvasWidth * 0.25f, canvasHeight * 0.25f),
                        end = androidx.compose.ui.geometry.Offset(canvasWidth * 0.75f, canvasHeight * 0.75f),
                        strokeWidth = stroke.toPx(),
                        cap = StrokeCap.Round,
                    )
                    drawLine(
                        color = markColor,
                        start = androidx.compose.ui.geometry.Offset(canvasWidth * 0.75f, canvasHeight * 0.25f),
                        end = androidx.compose.ui.geometry.Offset(canvasWidth * 0.25f, canvasHeight * 0.75f),
                        strokeWidth = stroke.toPx(),
                        cap = StrokeCap.Round,
                    )
                }
            }
        }
    }
}

@Composable
private fun DecorativeTagIcon(content: @Composable () -> Unit) {
    Box(Modifier.clearAndSetSemantics { }) { content() }
}

@Composable
private fun tagAppearance(
    intent: BraceTagIntent,
    minimal: Boolean,
    enabled: Boolean,
    pressed: Boolean,
    hovered: Boolean,
    active: Boolean,
    selected: Boolean,
): TagAppearance {
    val semantic = BraceTheme.colors.semantic
    val tag = BraceTheme.colors.components.tag
    val intentContainer: Color
    val intentContent: Color
    val intentBorder: Color
    when (intent) {
        BraceTagIntent.Default -> {
            intentContainer = tag.container
            intentContent = tag.content
            intentBorder = tag.border
        }
        BraceTagIntent.Primary -> {
            intentContainer = semantic.primary
            intentContent = semantic.onPrimary
            intentBorder = semantic.primary
        }
        BraceTagIntent.Success -> {
            intentContainer = semantic.success
            intentContent = semantic.onSuccess
            intentBorder = semantic.success
        }
        BraceTagIntent.Warning -> {
            intentContainer = semantic.warning
            intentContent = semantic.onWarning
            intentBorder = semantic.warning
        }
        BraceTagIntent.Danger -> {
            intentContainer = semantic.danger
            intentContent = semantic.onDanger
            intentBorder = semantic.danger
        }
    }
    val container: Color
    val content: Color
    val border: Color
    when {
        !enabled -> {
            container = semantic.disabledContainer
            content = semantic.disabledContent
            border = semantic.border
        }
        pressed || active || selected -> {
            container = semantic.selection
            content = semantic.onSelection
            border = semantic.primary
        }
        hovered -> {
            container = semantic.hover
            content = semantic.onSurface
            border = semantic.borderStrong
        }
        minimal -> {
            container = semantic.surface
            content = semantic.onSurface
            border = intentBorder
        }
        else -> {
            container = intentContainer
            content = intentContent
            border = intentBorder
        }
    }
    return TagAppearance(
        container = container,
        content = content,
        border = border,
        leftContainer = if (enabled) semantic.surfaceInset else semantic.disabledContainer,
        leftContent = if (enabled) semantic.onSurface else semantic.disabledContent,
    )
}
