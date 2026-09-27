package io.github.joelromanpr.brace.core

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import io.github.braceandroid.foundation.BraceDensity
import io.github.braceandroid.foundation.BraceTheme

/** Purpose and severity of a [BraceCallout]. */
public enum class BraceCalloutIntent { Neutral, Primary, Success, Warning, Danger }

/** Relative size of the decorative [BraceEmptyState] icon. */
public enum class BraceEmptyStateIconSize { Standard, Small, ExtraSmall }

/** Arrangement of a [BraceEmptyState]'s icon, text, action, and extra content. */
public enum class BraceEmptyStateLayout { Vertical, Horizontal }

/**
 * A highlighted message with an optional [title], body [content], and separate [action].
 *
 * Primary, success, warning, and danger intents draw a simple Brace icon unless [showIcon]
 * is false. A custom [icon] replaces the default and is treated as decorative. [compact]
 * reduces token-based padding; [minimal] removes the colored fill while retaining the
 * intent accent. Interactive content belongs in [action] so it has its own focus stop.
 * The callout never takes focus when its message changes. Set [announceChanges] for a
 * polite live-region announcement when a dynamically updated message needs one.
 */
@Composable
public fun BraceCallout(
    title: String? = null,
    modifier: Modifier = Modifier,
    intent: BraceCalloutIntent = BraceCalloutIntent.Neutral,
    compact: Boolean = false,
    minimal: Boolean = false,
    showIcon: Boolean = true,
    icon: (@Composable () -> Unit)? = null,
    action: (@Composable () -> Unit)? = null,
    announceChanges: Boolean = false,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    val semantic = BraceTheme.colors.semantic
    val colors = BraceTheme.colors.components.callout
    val metrics = BraceTheme.componentMetrics.callout
    val (filledContainer, filledContent, accent) = when (intent) {
        BraceCalloutIntent.Neutral -> Triple(colors.neutralContainer, colors.neutralContent, semantic.borderStrong)
        BraceCalloutIntent.Primary -> Triple(colors.primaryContainer, colors.primaryContent, semantic.primary)
        BraceCalloutIntent.Success -> Triple(colors.successContainer, colors.successContent, semantic.success)
        BraceCalloutIntent.Warning -> Triple(colors.warningContainer, colors.warningContent, semantic.warning)
        BraceCalloutIntent.Danger -> Triple(colors.dangerContainer, colors.dangerContent, semantic.danger)
    }
    val container = if (minimal) Color.Transparent else filledContainer
    val bodyColor = if (minimal) semantic.onSurface else filledContent
    val emphasisColor = if (minimal) accent else filledContent
    val inset = if (compact || BraceTheme.density == BraceDensity.Compact) {
        BraceTheme.densityTokens.contentPaddingDp
    } else metrics.contentPadding
    val gap = BraceTheme.densityTokens.itemGapDp
    val shape = RoundedCornerShape(metrics.cornerRadius)
    val accentWidth = metrics.accentWidth

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(container)
            .drawBehind {
                val width = accentWidth.toPx()
                drawRect(
                    color = accent,
                    topLeft = Offset(if (layoutDirection == LayoutDirection.Rtl) size.width - width else 0f, 0f),
                    size = Size(width, size.height),
                )
            }
            .padding(start = accentWidth + inset, end = inset, top = inset, bottom = inset)
            .semantics {
                isTraversalGroup = true
                if (announceChanges) liveRegion = LiveRegionMode.Polite
            },
        horizontalArrangement = Arrangement.spacedBy(gap),
        verticalAlignment = Alignment.Top,
    ) {
        if (showIcon && (icon != null || intent != BraceCalloutIntent.Neutral)) {
            Box(
                modifier = Modifier
                    .size(BraceTheme.sizing.iconMd)
                    .clearAndSetSemantics { },
                contentAlignment = Alignment.Center,
            ) {
                CompositionLocalProvider(LocalContentColor provides emphasisColor) {
                    if (icon != null) icon() else DefaultCalloutIcon(intent, emphasisColor)
                }
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(gap),
        ) {
            CompositionLocalProvider(LocalContentColor provides bodyColor) {
                if (title != null) {
                    Text(
                        text = title,
                        color = emphasisColor,
                        style = BraceTheme.typography.subtitle,
                        modifier = Modifier.semantics { heading() },
                    )
                }
                content()
                action?.invoke()
            }
        }
    }
}

/**
 * A placeholder for an empty, loading, unavailable, or failed content area.
 *
 * The visual [icon] is decorative and its [iconSize] follows Brace sizing tokens.
 * [iconMuted] supplies a muted `LocalContentColor` to the icon slot; caller-drawn colors
 * remain under caller control. Reading order is icon, [title], [description], [action], then
 * extra [content], in either [layout]. Put a focusable control such as [BraceButton] in
 * [action] to provide a visible focus ring and a 48 dp target. Dynamic updates remain quiet
 * unless [announceChanges] is explicitly enabled.
 */
@Composable
public fun BraceEmptyState(
    title: String? = null,
    modifier: Modifier = Modifier,
    description: String? = null,
    icon: (@Composable () -> Unit)? = null,
    iconSize: BraceEmptyStateIconSize = BraceEmptyStateIconSize.Standard,
    iconMuted: Boolean = true,
    layout: BraceEmptyStateLayout = BraceEmptyStateLayout.Vertical,
    action: (@Composable () -> Unit)? = null,
    announceChanges: Boolean = false,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    val semantic = BraceTheme.colors.semantic
    val gap = if (BraceTheme.density == BraceDensity.Compact) BraceTheme.spacing.md else BraceTheme.spacing.lg
    val iconDp = when (iconSize) {
        BraceEmptyStateIconSize.Standard -> BraceTheme.sizing.iconSm * 3
        BraceEmptyStateIconSize.Small -> BraceTheme.sizing.iconSm * 2
        BraceEmptyStateIconSize.ExtraSmall -> BraceTheme.sizing.iconLg
    }
    val iconContent: @Composable () -> Unit = {
        if (icon != null) {
            Box(
                modifier = Modifier.size(iconDp).clearAndSetSemantics { },
                contentAlignment = Alignment.Center,
            ) {
                CompositionLocalProvider(
                    LocalContentColor provides if (iconMuted) semantic.onSurfaceMuted else semantic.onSurface,
                ) { icon() }
            }
        }
    }
    val rootModifier = modifier
        .fillMaxWidth()
        .padding(BraceTheme.densityTokens.contentPaddingDp)
        .semantics {
            isTraversalGroup = true
            if (announceChanges) liveRegion = LiveRegionMode.Polite
        }
    if (layout == BraceEmptyStateLayout.Vertical) {
        Column(
            modifier = rootModifier,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(gap),
        ) {
            iconContent()
            EmptyStateBody(
                title = title,
                description = description,
                action = action,
                textAlign = TextAlign.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
                content = content,
            )
        }
    } else {
        Row(
            modifier = rootModifier,
            horizontalArrangement = Arrangement.spacedBy(gap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            iconContent()
            EmptyStateBody(
                title = title,
                description = description,
                action = action,
                textAlign = TextAlign.Start,
                horizontalAlignment = Alignment.Start,
                modifier = Modifier.weight(1f),
                content = content,
            )
        }
    }
}

@Composable
private fun EmptyStateBody(
    title: String?,
    description: String?,
    action: (@Composable () -> Unit)?,
    textAlign: TextAlign,
    horizontalAlignment: Alignment.Horizontal,
    content: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
) {
    val semantic = BraceTheme.colors.semantic
    Column(
        modifier = modifier,
        horizontalAlignment = horizontalAlignment,
        verticalArrangement = Arrangement.spacedBy(BraceTheme.densityTokens.itemGapDp),
    ) {
        if (title != null) {
            Text(
                text = title,
                color = semantic.onSurface,
                style = BraceTheme.typography.title,
                textAlign = textAlign,
                modifier = Modifier.semantics { heading() },
            )
        }
        if (description != null) {
            Text(
                text = description,
                color = semantic.onSurfaceMuted,
                style = BraceTheme.typography.body,
                textAlign = textAlign,
            )
        }
        action?.invoke()
        content()
    }
}

@Composable
private fun DefaultCalloutIcon(intent: BraceCalloutIntent, color: Color) {
    Canvas(Modifier.size(BraceTheme.sizing.iconMd)) {
        val stroke = size.minDimension * 0.1f
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension * 0.38f
        when (intent) {
            BraceCalloutIntent.Neutral -> Unit
            BraceCalloutIntent.Warning -> {
                val triangle = Path().apply {
                    moveTo(center.x, size.height * 0.1f)
                    lineTo(size.width * 0.9f, size.height * 0.83f)
                    lineTo(size.width * 0.1f, size.height * 0.83f)
                    close()
                }
                drawPath(triangle, color, style = Stroke(stroke))
                drawLine(color, Offset(center.x, size.height * 0.4f), Offset(center.x, size.height * 0.6f), stroke)
                drawCircle(color, stroke * 0.55f, Offset(center.x, size.height * 0.72f))
            }
            BraceCalloutIntent.Primary -> {
                drawCircle(color, radius, center, style = Stroke(stroke))
                drawLine(color, Offset(center.x, size.height * 0.44f), Offset(center.x, size.height * 0.72f), stroke)
                drawCircle(color, stroke * 0.55f, Offset(center.x, size.height * 0.3f))
            }
            BraceCalloutIntent.Success -> {
                drawCircle(color, radius, center, style = Stroke(stroke))
                drawLine(color, Offset(size.width * 0.28f, size.height * 0.52f),
                    Offset(size.width * 0.44f, size.height * 0.67f), stroke)
                drawLine(color, Offset(size.width * 0.44f, size.height * 0.67f),
                    Offset(size.width * 0.74f, size.height * 0.34f), stroke)
            }
            BraceCalloutIntent.Danger -> {
                drawCircle(color, radius, center, style = Stroke(stroke))
                drawLine(color, Offset(size.width * 0.37f, size.height * 0.37f),
                    Offset(size.width * 0.63f, size.height * 0.63f), stroke)
                drawLine(color, Offset(size.width * 0.63f, size.height * 0.37f),
                    Offset(size.width * 0.37f, size.height * 0.63f), stroke)
            }
        }
    }
}
