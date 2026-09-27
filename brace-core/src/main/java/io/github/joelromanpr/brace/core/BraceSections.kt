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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.collapse
import androidx.compose.ui.semantics.expand
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import io.github.braceandroid.foundation.BraceDensity
import io.github.braceandroid.foundation.BraceTheme

/**
 * A titled content region with an optional collapsible body.
 *
 * [title] is a semantic heading. [subtitle] is announced after it. [icon] is decorative;
 * place an interactive control in [trailingAction] instead. The trailing action is a separate
 * focus stop and does not toggle the section.
 *
 * When [collapsible] is true, omit [expanded] for saveable internal state starting at
 * [initiallyExpanded]. Pass [expanded] and [onExpandedChange] together for controlled state;
 * the caller must save that state across recreation. The body leaves composition when closed,
 * so its controls leave keyboard focus and the accessibility tree. Saveable body state is kept
 * across a close/reopen cycle.
 *
 * [contentPadding] applies inside the body; it defaults to zero because [BraceSectionCard]
 * supplies its own padding. Header insets and card spacing follow [BraceTheme.density] while the
 * toggle retains a 48 dp minimum target. [elevation] defaults to the Brace none token.
 */
@Composable
public fun BraceSection(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: (@Composable () -> Unit)? = null,
    trailingAction: (@Composable () -> Unit)? = null,
    collapsible: Boolean = false,
    expanded: Boolean? = null,
    onExpandedChange: ((Boolean) -> Unit)? = null,
    initiallyExpanded: Boolean = true,
    elevation: Dp? = null,
    contentPadding: PaddingValues? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    require(!collapsible || expanded == null || onExpandedChange != null) {
        "A controlled, collapsible BraceSection requires onExpandedChange"
    }
    var internalExpanded by rememberSaveable { mutableStateOf(initiallyExpanded) }
    val isExpanded = !collapsible || (expanded ?: internalExpanded)
    val requestExpansion: (Boolean) -> Unit = { next ->
        if (expanded == null) internalExpanded = next
        onExpandedChange?.invoke(next)
    }
    val bodyState = rememberSaveableStateHolder()
    val card = BraceTheme.colors.components.card
    val semantic = BraceTheme.colors.semantic
    val compact = BraceTheme.density == BraceDensity.Compact
    val headerHorizontalPadding = if (compact) BraceTheme.spacing.md else BraceTheme.spacing.lg
    val headerVerticalPadding = if (compact) BraceTheme.spacing.xs else BraceTheme.spacing.sm
    val shape = RoundedCornerShape(BraceTheme.componentMetrics.card.cornerRadius)
    val resolvedElevation = elevation ?: BraceTheme.elevation.none
    val bodyPadding = contentPadding ?: PaddingValues(BraceTheme.spacing.none)
    val expandLabel = stringResource(R.string.brace_section_expand)
    val collapseLabel = stringResource(R.string.brace_section_collapse)
    val expandedState = stringResource(R.string.brace_section_expanded)
    val collapsedState = stringResource(R.string.brace_section_collapsed)
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val hovered by interactionSource.collectIsHoveredAsState()
    val pressed by interactionSource.collectIsPressedAsState()
    val headerShape = RoundedCornerShape(BraceTheme.shape.sm)
    val toggleBackground = when {
        pressed -> semantic.pressed
        hovered -> semantic.hover
        else -> Color.Transparent
    }

    Column(
        modifier = modifier
            .shadow(resolvedElevation, shape)
            .clip(shape)
            .background(card.container)
            .border(BraceTheme.sizing.borderWidth, card.border, shape)
            .semantics { isTraversalGroup = true },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = headerHorizontalPadding, vertical = headerVerticalPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val titleModifier = Modifier
                .weight(1f)
                .defaultMinSize(minHeight = BraceTheme.sizing.touchTarget)
                .clip(headerShape)
                .background(if (collapsible) toggleBackground else Color.Transparent)
                .then(
                    if (collapsible && focused) {
                        Modifier.border(BraceTheme.sizing.focusRingWidth, semantic.focusRing, headerShape)
                    } else Modifier
                )
                .then(
                    if (collapsible) {
                        Modifier
                            .clickable(
                                role = Role.Button,
                                onClickLabel = if (isExpanded) collapseLabel else expandLabel,
                                interactionSource = interactionSource,
                                indication = null,
                                onClick = { requestExpansion(!isExpanded) },
                            )
                            .semantics(mergeDescendants = true) {
                                heading()
                                stateDescription = if (isExpanded) expandedState else collapsedState
                                if (isExpanded) {
                                    collapse(label = collapseLabel) {
                                        requestExpansion(false)
                                        true
                                    }
                                } else {
                                    expand(label = expandLabel) {
                                        requestExpansion(true)
                                        true
                                    }
                                }
                            }
                    } else Modifier
                )
            Row(
                modifier = titleModifier,
                horizontalArrangement = Arrangement.spacedBy(BraceTheme.densityTokens.itemGapDp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (icon != null) {
                    Box(Modifier.clearAndSetSemantics { }) { icon() }
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = card.content,
                        style = BraceTheme.typography.subtitle,
                        modifier = if (collapsible) Modifier else Modifier.semantics { heading() },
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            color = semantic.onSurfaceMuted,
                            style = BraceTheme.typography.body,
                        )
                    }
                }
                if (collapsible) {
                    SectionChevron(isExpanded, semantic.onSurfaceMuted)
                }
            }
            if (trailingAction != null) {
                Spacer(Modifier.width(BraceTheme.densityTokens.itemGapDp))
                trailingAction()
            }
        }
        if (isExpanded) {
            bodyState.SaveableStateProvider("body") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bodyPadding)
                        .semantics { isTraversalGroup = true },
                    verticalArrangement = Arrangement.spacedBy(BraceTheme.densityTokens.itemGapDp),
                    content = content,
                )
            }
        }
    }
}

/**
 * A token-styled group of section content. Several cards can be placed inside one [BraceSection].
 *
 * [padded] uses the Brace card-content padding token in Comfortable density and the compact
 * density content inset in Compact mode. Set it to false for edge-to-edge content, or pass
 * [padding] for a custom logical inset. [elevation] defaults to none.
 */
@Composable
public fun BraceSectionCard(
    modifier: Modifier = Modifier,
    padded: Boolean = true,
    padding: PaddingValues? = null,
    elevation: Dp? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val card = BraceTheme.colors.components.card
    val shape = RoundedCornerShape(BraceTheme.componentMetrics.card.cornerRadius)
    val defaultPadding = if (BraceTheme.density == BraceDensity.Compact) {
        BraceTheme.densityTokens.contentPaddingDp
    } else BraceTheme.componentMetrics.card.contentPadding
    val resolvedPadding = padding ?: PaddingValues(
        if (padded) defaultPadding else BraceTheme.spacing.none,
    )
    Column(
        modifier = modifier
            .shadow(elevation ?: BraceTheme.elevation.none, shape)
            .clip(shape)
            .background(card.container)
            .border(BraceTheme.sizing.borderWidth, card.border, shape)
            .padding(resolvedPadding)
            .semantics { isTraversalGroup = true },
        content = content,
    )
}

@Composable
private fun SectionChevron(expanded: Boolean, color: Color) {
    val strokeWidth = BraceTheme.sizing.borderStrongWidth
    Canvas(Modifier.size(BraceTheme.sizing.iconSm)) {
        val yOuter = if (expanded) size.height * 0.65f else size.height * 0.35f
        val yMiddle = if (expanded) size.height * 0.35f else size.height * 0.65f
        drawLine(color, start = androidx.compose.ui.geometry.Offset(size.width * 0.2f, yOuter),
            end = androidx.compose.ui.geometry.Offset(size.width * 0.5f, yMiddle),
            strokeWidth = strokeWidth.toPx())
        drawLine(color, start = androidx.compose.ui.geometry.Offset(size.width * 0.5f, yMiddle),
            end = androidx.compose.ui.geometry.Offset(size.width * 0.8f, yOuter),
            strokeWidth = strokeWidth.toPx())
    }
}
