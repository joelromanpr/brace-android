package io.github.joelromanpr.brace.core

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import io.github.braceandroid.foundation.BraceDensity
import io.github.braceandroid.foundation.BraceTheme

/**
 * A tokenized top app bar with separate leading and trailing content.
 *
 * Place this in a screen's `Scaffold(topBar = { ... })` slot to keep it at the top. If a screen
 * draws behind the status bar, apply `statusBarsPadding()` at the host. [startContent] and
 * [endContent] preserve logical start/end placement in RTL. Put a [BraceTopBarTitle] in the
 * leading [BraceTopBarGroup], and use accessible controls such as [BraceButton] for actions.
 * A top bar is a layout container; it does not invent navigation actions or focus stops.
 *
 * Keep the number of actions appropriate for the available width. At large text sizes a long
 * title truncates, while actions retain their own minimum touch targets. Put extra actions in a
 * menu when needed. [raised] uses the Brace elevation token and can distinguish a bar above
 * scrolled content.
 */
@Composable
public fun BraceTopBar(
    startContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    endContent: (@Composable () -> Unit)? = null,
    raised: Boolean = false,
) {
    val colors = BraceTheme.colors.components.topBar
    val metrics = BraceTheme.componentMetrics.topBar
    val elevation = if (raised) BraceTheme.elevation.raised else BraceTheme.elevation.none
    val compact = BraceTheme.density == BraceDensity.Compact
    val minHeight = if (compact) BraceTheme.sizing.touchTarget else metrics.minHeight
    val horizontalPadding = if (compact) BraceTheme.spacing.md else metrics.horizontalPadding
    val verticalPadding = if (compact) BraceTheme.spacing.none else metrics.verticalPadding
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation)
            .background(colors.container)
            .semantics { isTraversalGroup = true },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = minHeight)
                .padding(
                    horizontal = horizontalPadding,
                    vertical = verticalPadding,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.weight(1f).clipToBounds(), contentAlignment = Alignment.CenterStart) {
                startContent()
            }
            if (endContent != null) {
                Box(contentAlignment = Alignment.CenterEnd) { endContent() }
            }
        }
        Spacer(
            Modifier
                .fillMaxWidth()
                .height(BraceTheme.sizing.borderWidth)
                .background(colors.border),
        )
    }
}

/**
 * Places related title or action content in logical reading and keyboard order.
 *
 * The group is a traversal group rather than a focus stop. Children own their labels and
 * interaction. Use [BraceTopBarDivider] between distinct controls where a visual separator helps.
 */
@Composable
public fun BraceTopBarGroup(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    val gap = if (BraceTheme.density == BraceDensity.Compact) {
        BraceTheme.densityTokens.itemGapDp
    } else BraceTheme.componentMetrics.topBar.groupGap
    Row(
        modifier = modifier.semantics { isTraversalGroup = true },
        horizontalArrangement = Arrangement.spacedBy(gap),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

/** A screen heading within [BraceTopBarGroup], ellipsized before trailing actions overlap. */
@Composable
public fun RowScope.BraceTopBarTitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    require(text.isNotBlank()) { "A top bar title must not be blank" }
    Text(
        text = text,
        modifier = modifier.weight(1f, fill = false).semantics { heading() },
        style = BraceTheme.typography.subtitle,
        color = BraceTheme.colors.components.topBar.title,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

/** A decorative top bar separator. It is absent from the accessibility tree. */
@Composable
public fun BraceTopBarDivider(modifier: Modifier = Modifier) {
    Spacer(
        modifier
            .width(BraceTheme.sizing.borderWidth)
            .height(BraceTheme.componentMetrics.topBar.dividerHeight)
            .background(BraceTheme.colors.components.topBar.divider),
    )
}
