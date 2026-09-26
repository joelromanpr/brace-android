package io.github.joelromanpr.brace.core

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.BoxWithConstraints
import io.github.braceandroid.foundation.BraceDensity
import io.github.braceandroid.foundation.BraceTheme

/** One step in a [BraceBreadcrumbs] path. [onClick] is the native navigation action. */
public data class BraceBreadcrumb(
    val label: String,
    val enabled: Boolean = true,
    val onClick: (() -> Unit)? = null,
    val leadingIcon: (@Composable () -> Unit)? = null,
)

/** The edge whose items enter the overflow menu first. */
public enum class BraceBreadcrumbCollapseFrom { Start, End }

/**
 * One token-styled breadcrumb step. The current step is announced as the current location.
 *
 * A step with no [onClick] is static. A clickable step has a 48 dp minimum target,
 * keyboard/pointer behavior, and a visible focus outline. [leadingIcon] is decorative;
 * [label] is required for accessible text. Navigation is delegated to [onClick]
 * instead of exposing a web `href` or target attribute.
 */
@Composable
public fun BraceBreadcrumbItem(
    label: String,
    modifier: Modifier = Modifier,
    current: Boolean = false,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    accessibilityLabel: String? = null,
) {
    val semantic = BraceTheme.colors.semantic
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val hovered by interaction.collectIsHoveredAsState()
    val pressed by interaction.collectIsPressedAsState()
    val actionable = onClick != null && !current
    val shape = RoundedCornerShape(BraceTheme.shape.sm)
    val background = when {
        actionable && pressed -> semantic.pressed
        actionable && hovered -> semantic.hover
        else -> Color.Transparent
    }
    val textColor = when {
        !enabled -> semantic.disabledContent
        current -> semantic.onSurface
        actionable -> semantic.primary
        else -> semantic.onSurfaceMuted
    }
    val currentDescription = stringResource(R.string.brace_breadcrumb_current)
    val horizontalPadding = if (BraceTheme.density == BraceDensity.Compact) BraceTheme.spacing.xs else BraceTheme.spacing.sm
    val row = modifier
        .defaultMinSize(minHeight = BraceTheme.sizing.touchTarget,
            minWidth = if (actionable) BraceTheme.sizing.touchTarget else 0.dp)
        .background(background, shape)
        .then(if (focused) Modifier.border(BraceTheme.sizing.focusRingWidth, semantic.focusRing, shape) else Modifier)
        .then(if (actionable) Modifier.clickable(
            enabled = enabled,
            role = Role.Button,
            interactionSource = interaction,
            indication = null,
            onClick = requireNotNull(onClick),
        ) else Modifier)
        .semantics(mergeDescendants = true) {
            if (current) {
                selected = true
                stateDescription = currentDescription
            }
            if (!enabled) disabled()
            if (accessibilityLabel != null) contentDescription = accessibilityLabel
        }
        .padding(horizontal = horizontalPadding)
    Row(
        modifier = row,
        horizontalArrangement = Arrangement.spacedBy(BraceTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingIcon != null) {
            Box(Modifier.clearAndSetSemantics { }) { leadingIcon() }
        }
        Text(label, color = textColor, style = BraceTheme.typography.body,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/**
 * A width-aware path of [BraceBreadcrumb] items with native overflow navigation.
 *
 * The final item is the current destination. Items that do not fit move into a
 * keyboard and TalkBack reachable dropdown; [minVisibleItems] guarantees a
 * minimum number of visible steps. Text is measured with Compose's text measurer
 * at the active font scale, so wrapping and RTL changes recalculate overflow.
 * [collapseFrom] changes which edge collapses first. The overflow popup uses
 * Material 3 positioning mechanics with Brace semantic colors.
 *
 * This replaces Blueprint's DOM-measured OverflowList and `href`/renderer props
 * with Compose width measurement, native action callbacks, and an item model.
 */
@Composable
public fun BraceBreadcrumbs(
    items: List<BraceBreadcrumb>,
    modifier: Modifier = Modifier,
    collapseFrom: BraceBreadcrumbCollapseFrom = BraceBreadcrumbCollapseFrom.Start,
    minVisibleItems: Int = 1,
) {
    require(minVisibleItems >= 0) { "minVisibleItems must be non-negative" }
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val direction = LocalLayoutDirection.current
    val style = BraceTheme.typography.body
    val spacing = BraceTheme.spacing
    val touchTarget = BraceTheme.sizing.touchTarget
    val navDescription = stringResource(R.string.brace_breadcrumb_navigation)
    val overflowDescription = stringResource(R.string.brace_breadcrumb_overflow)
    val currentDescription = stringResource(R.string.brace_breadcrumb_current)
    val semantic = BraceTheme.colors.semantic
    var expanded by rememberSaveable { mutableStateOf(false) }

    BoxWithConstraints(modifier = modifier.semantics {
        isTraversalGroup = true
        contentDescription = navDescription
        collectionInfo = CollectionInfo(1, items.size)
    }) {
        val availableWidthDp = maxWidth
        val maxWidthPx = with(density) { availableWidthDp.toPx() }
        val touchPx = with(density) { touchTarget.roundToPx() }
        val itemPadding = if (BraceTheme.density == BraceDensity.Compact) spacing.xs else spacing.sm
        val paddingPx = with(density) { itemPadding.roundToPx() * 2 }
        val iconPx = with(density) { (BraceTheme.sizing.iconSm + spacing.xs).roundToPx() }
        val separatorPx = textMeasurer.measure(AnnotatedString(if (direction == LayoutDirection.Rtl) "‹" else "›"), style).size.width +
            with(density) { (spacing.xs * 2).roundToPx() }
        val widths = items.map { item ->
            (textMeasurer.measure(AnnotatedString(item.label), style, maxLines = 1).size.width +
                paddingPx + if (item.leadingIcon != null) iconPx else 0)
                .coerceAtLeast(if (item.onClick != null) touchPx else 0)
        }
        val visible = items.indices.toMutableList()
        fun needed(withOverflow: Boolean): Int {
            val count = visible.size + if (withOverflow) 1 else 0
            return visible.sumOf { widths[it] } +
                (if (withOverflow) touchPx else 0) +
                separatorPx * (count - 1).coerceAtLeast(0)
        }
        if (items.isNotEmpty() && needed(false) > maxWidthPx) {
            val minimum = minVisibleItems.coerceAtMost(items.size)
            while (visible.size > minimum && needed(true) > maxWidthPx) {
                if (collapseFrom == BraceBreadcrumbCollapseFrom.Start) visible.removeAt(0)
                else visible.removeAt(visible.lastIndex)
            }
        }
        val visibleSet = visible.toSet()
        val hidden = items.indices.filterNot(visibleSet::contains)
        LaunchedEffect(hidden.isEmpty()) {
            if (hidden.isEmpty()) expanded = false
        }
        val displayed = if (hidden.isEmpty()) visible.map { it as Int? } else if (collapseFrom == BraceBreadcrumbCollapseFrom.Start) {
            listOf(null) + visible.map { it as Int? }
        } else visible.map { it as Int? } + null
        Row(verticalAlignment = Alignment.CenterVertically) {
            displayed.forEachIndexed { displayIndex, sourceIndex ->
                if (displayIndex > 0) {
                    Text(if (direction == LayoutDirection.Rtl) "‹" else "›",
                        color = semantic.onSurfaceMuted, style = style,
                        modifier = Modifier.padding(horizontal = spacing.xs)
                            .clearAndSetSemantics { })
                }
                if (sourceIndex == null) {
                    Box {
                        BraceBreadcrumbItem("…", onClick = { expanded = true },
                            accessibilityLabel = overflowDescription)
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            shape = RoundedCornerShape(BraceTheme.shape.md),
                            containerColor = semantic.surface,
                            tonalElevation = BraceTheme.elevation.none,
                            shadowElevation = BraceTheme.elevation.floating,
                            border = BorderStroke(BraceTheme.sizing.borderWidth, semantic.border),
                        ) {
                            val menuOrder = if (collapseFrom == BraceBreadcrumbCollapseFrom.Start) hidden.reversed() else hidden
                            menuOrder.forEach { index ->
                                val item = items[index]
                                val current = index == items.lastIndex
                                DropdownMenuItem(
                                    text = {
                                        Text(item.label,
                                            color = if (!item.enabled) semantic.disabledContent else semantic.onSurface,
                                            style = style)
                                    },
                                    onClick = {
                                        expanded = false
                                        item.onClick?.invoke()
                                    },
                                    enabled = item.enabled && item.onClick != null && !current,
                                    modifier = Modifier.semantics {
                                        if (current) {
                                            selected = true
                                            stateDescription = currentDescription
                                        }
                                    },
                                    colors = MenuDefaults.itemColors(
                                        textColor = semantic.onSurface,
                                        disabledTextColor = semantic.disabledContent,
                                    ),
                                )
                            }
                        }
                    }
                } else {
                    val item = items[sourceIndex]
                    BraceBreadcrumbItem(
                        label = item.label,
                        current = sourceIndex == items.lastIndex,
                        enabled = item.enabled,
                        onClick = item.onClick,
                        leadingIcon = item.leadingIcon,
                        modifier = Modifier.widthIn(max = availableWidthDp),
                    )
                }
            }
        }
    }
}
