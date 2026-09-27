package io.github.joelromanpr.brace.core

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.CollectionItemInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.collectionItemInfo
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import io.github.braceandroid.foundation.BraceDensity
import io.github.braceandroid.foundation.BraceTheme

/** Five visual depths for [BraceCard], corresponding to the pinned Blueprint Card scale. */
public enum class BraceCardElevation {
    Zero, One, Two, Three, Four
}

/** Layout direction of a decorative [BraceDivider]. */
public enum class BraceDividerOrientation {
    Horizontal, Vertical
}

/**
 * A token-styled container for related content.
 *
 * A card without [onClick] is static and adds no focus stop. Supplying [onClick]
 * creates one button-like target with a 48 dp minimum size, pointer hover and
 * press states, a visible keyboard focus outline, and a controlled [selected]
 * state. Put one action in a clickable card; use a static card when its content
 * contains independently interactive controls. Content uses the caller's
 * reading order and can grow with Android font scaling.
 *
 * [compact] reduces padding but never the interactive target. [elevation]
 * selects one of five token-derived depths.
 */
@Composable
public fun BraceCard(
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    elevation: BraceCardElevation = BraceCardElevation.Zero,
    selected: Boolean = false,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    CardSurface(
        modifier = modifier,
        compact = compact,
        elevation = elevation,
        selected = selected,
        enabled = enabled,
        onClick = onClick,
        grouped = false,
        content = content,
    )
}

/**
 * A contiguous list of card items. [bordered] draws one outer boundary;
 * [compact] applies reduced padding to every item. Each item remains a separate
 * accessibility target when [onItemClick] is supplied. [isSelected] and
 * [isEnabled] are controlled by the caller. Supply [itemKey] when items may be
 * reordered so remembered item content follows its identity.
 *
 * This component lays out a short, bounded list. For a very large data set,
 * use a lazy list of [BraceCard] items so the viewport only composes visible
 * rows.
 */
@Composable
public fun <T> BraceCardList(
    items: List<T>,
    modifier: Modifier = Modifier,
    bordered: Boolean = true,
    compact: Boolean = false,
    itemKey: ((T) -> Any)? = null,
    isSelected: (T) -> Boolean = { false },
    isEnabled: (T) -> Boolean = { true },
    onItemClick: ((T) -> Unit)? = null,
    content: @Composable ColumnScope.(T) -> Unit,
) {
    val colors = BraceTheme.colors.components.card
    val metrics = BraceTheme.componentMetrics.card
    val outline = BraceTheme.sizing.borderWidth
    val shape: Shape = if (bordered) RoundedCornerShape(metrics.cornerRadius) else RectangleShape
    Column(
        modifier = modifier
            .clip(shape)
            .background(colors.container)
            .then(if (bordered) Modifier.border(outline, colors.border, shape) else Modifier)
            .semantics { collectionInfo = CollectionInfo(items.size, 1) },
    ) {
        items.forEachIndexed { index, item ->
            key(itemKey?.invoke(item) ?: index) {
                CardSurface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            collectionItemInfo = CollectionItemInfo(index, 1, 0, 1)
                        },
                    compact = compact,
                    elevation = BraceCardElevation.Zero,
                    selected = isSelected(item),
                    enabled = isEnabled(item),
                    onClick = onItemClick?.let { action -> { action(item) } },
                    grouped = true,
                ) {
                    content(item)
                }
            }
            if (index < items.lastIndex) {
                BraceDivider(compact = true)
            }
        }
    }
}

/**
 * A decorative separator. It creates no focus or TalkBack stop.
 *
 * [orientation] is explicit because Compose layout does not infer the
 * direction of a CSS flex parent. A vertical divider should be placed in a
 * height-constrained row, or given a height with [modifier]. [compact] removes
 * the surrounding spacing while retaining the token border width.
 */
@Composable
public fun BraceDivider(
    modifier: Modifier = Modifier,
    orientation: BraceDividerOrientation = BraceDividerOrientation.Horizontal,
    compact: Boolean = false,
) {
    val gap = when {
        compact -> BraceTheme.spacing.none
        BraceTheme.density == BraceDensity.Compact -> BraceTheme.spacing.xs
        else -> BraceTheme.spacing.sm
    }
    val thickness = BraceTheme.sizing.borderWidth
    val color = BraceTheme.colors.semantic.border
    val line = when (orientation) {
        BraceDividerOrientation.Horizontal -> Modifier
            .padding(vertical = gap)
            .fillMaxWidth()
            .height(thickness)
        BraceDividerOrientation.Vertical -> Modifier
            .padding(horizontal = gap)
            .fillMaxHeight()
            .width(thickness)
    }
    Spacer(modifier.then(line).background(color))
}

@Composable
private fun CardSurface(
    modifier: Modifier,
    compact: Boolean,
    elevation: BraceCardElevation,
    selected: Boolean,
    enabled: Boolean,
    onClick: (() -> Unit)?,
    grouped: Boolean,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = BraceTheme.colors.components.card
    val semantic = BraceTheme.colors.semantic
    val metrics = BraceTheme.componentMetrics.card
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val hovered by interactionSource.collectIsHoveredAsState()
    val pressed by interactionSource.collectIsPressedAsState()
    val actionable = onClick != null
    val shape: Shape = if (grouped) RectangleShape else RoundedCornerShape(metrics.cornerRadius)
    val background = when {
        actionable && !enabled -> semantic.disabledContainer
        actionable && pressed -> semantic.pressed
        selected -> semantic.selection
        actionable && hovered -> semantic.hover
        else -> colors.container
    }
    val contentColor = when {
        actionable && !enabled -> semantic.disabledContent
        selected -> semantic.onSelection
        else -> colors.content
    }
    val borderColor = when {
        focused -> semantic.focusRing
        selected -> semantic.primary
        else -> colors.border
    }
    val borderWidth = if (focused) BraceTheme.sizing.focusRingWidth else BraceTheme.sizing.borderWidth
    val padding = if (compact || BraceTheme.density == BraceDensity.Compact) {
        BraceTheme.densityTokens.contentPaddingDp
    } else metrics.contentPadding
    val shadow = if (grouped) BraceTheme.elevation.none else cardDepth(elevation) +
        (if (actionable && enabled && hovered) BraceTheme.elevation.raised else BraceTheme.elevation.none)
    val selectedState = selected
    val surface = modifier
        .then(if (actionable) Modifier.defaultMinSize(
            minWidth = BraceTheme.sizing.touchTarget,
            minHeight = BraceTheme.sizing.touchTarget,
        ) else Modifier)
        .then(if (!grouped) Modifier.shadow(shadow, shape) else Modifier)
        .background(background, shape)
        .then(if (!grouped || focused) Modifier.border(borderWidth, borderColor, shape) else Modifier)
        .then(if (actionable) {
            Modifier.clickable(
                enabled = enabled,
                role = Role.Button,
                interactionSource = interactionSource,
                indication = null,
                onClick = requireNotNull(onClick),
            )
        } else Modifier)
        .then(if (actionable || selected) Modifier.semantics { this.selected = selectedState } else Modifier)
        .padding(padding)
    CompositionLocalProvider(LocalContentColor provides contentColor) {
        Column(modifier = surface, content = content)
    }
}

@Composable
private fun cardDepth(level: BraceCardElevation): Dp {
    val tokens = BraceTheme.elevation
    return when (level) {
        BraceCardElevation.Zero -> tokens.none
        BraceCardElevation.One -> tokens.raised
        BraceCardElevation.Two -> tokens.floating
        BraceCardElevation.Three -> tokens.modal
        BraceCardElevation.Four -> tokens.modal + tokens.floating
    }
}
