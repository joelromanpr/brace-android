package io.github.joelromanpr.brace.core

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
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.requestFocus
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import io.github.braceandroid.foundation.BraceTheme

/** Token-backed visual prominence for the [BraceEntityTitle] heading. */
public enum class BraceEntityTitleStyle { Body, Subtitle, Title, Display }

/**
 * A compact entity heading with optional icon, subtitle, tags, and native navigation.
 *
 * [title] is the localized spoken heading. [titleContent] and [subtitleContent] may replace
 * the visual strings with rich Compose content; both slots are decorative to accessibility
 * services and must not contain interactive controls. The [icon] is also decorative.
 * Keep actionable tags in [tags] so they remain separate focus stops after the title.
 *
 * [onTitleClick] creates a keyboard, pointer, and touch operable title with a visible
 * focus ring and a 48 dp target. The caller performs navigation; Blueprint's titleURL
 * has no separate browser anchor on Android. [loading] replaces content with static,
 * token-styled placeholders, removes title and tag actions, and announces loading.
 * Static placeholders honor reduced motion without a separate animation.
 *
 * [ellipsize] constrains built-in title and subtitle strings to one line. The full
 * strings remain in accessibility semantics and measured overflow offers tooltip help.
 * Rich visual slots own their own truncation. [fill] expands the row to its container;
 * otherwise it wraps its content. Compose [Modifier] replaces Blueprint's HTML props.
 */
@Composable
public fun BraceEntityTitle(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: (@Composable () -> Unit)? = null,
    tags: (@Composable () -> Unit)? = null,
    titleContent: (@Composable () -> Unit)? = null,
    subtitleContent: (@Composable () -> Unit)? = null,
    style: BraceEntityTitleStyle = BraceEntityTitleStyle.Body,
    ellipsize: Boolean = false,
    fill: Boolean = false,
    loading: Boolean = false,
    onTitleClick: (() -> Unit)? = null,
) {
    require(subtitleContent == null || subtitle != null) {
        "subtitleContent requires a spoken subtitle"
    }
    val colors = BraceTheme.colors.components.entityTitle
    val metrics = BraceTheme.componentMetrics.entityTitle
    val contentGap = BraceTheme.densityTokens.itemGapDp
    val headingStyle = when (style) {
        BraceEntityTitleStyle.Body -> BraceTheme.typography.bodyStrong
        BraceEntityTitleStyle.Subtitle -> BraceTheme.typography.subtitle
        BraceEntityTitleStyle.Title -> BraceTheme.typography.title
        BraceEntityTitleStyle.Display -> BraceTheme.typography.display
    }
    val loadingLabel = stringResource(R.string.brace_entity_title_loading)
    val openLabel = stringResource(R.string.brace_entity_title_open, title)
    val rootModifier = modifier.then(if (fill) Modifier.fillMaxWidth() else Modifier)
    Row(
        modifier = rootModifier.then(
            if (loading) Modifier.clearAndSetSemantics { contentDescription = loadingLabel }
            else Modifier.semantics { isTraversalGroup = true },
        ),
        horizontalArrangement = Arrangement.spacedBy(metrics.iconGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Box(
                modifier = Modifier
                    .size(metrics.iconSize)
                    .clearAndSetSemantics {},
                contentAlignment = Alignment.Center,
            ) {
                if (loading) {
                    EntityPlaceholder(Modifier.size(metrics.iconSize))
                } else {
                    CompositionLocalProvider(LocalContentColor provides colors.icon) {
                        icon()
                    }
                }
            }
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(metrics.subtitleGap),
        ) {
            if (loading) {
                EntityPlaceholder(
                    Modifier
                        .width(metrics.loadingTitleWidth)
                        .height(metrics.loadingTitleHeight),
                )
                if (subtitle != null) {
                    EntityPlaceholder(
                        Modifier
                            .width(metrics.loadingSubtitleWidth)
                            .height(metrics.loadingSubtitleHeight),
                    )
                }
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(minOf(metrics.titleTagsGap, contentGap)),
                    verticalArrangement = Arrangement.spacedBy(metrics.subtitleGap),
                ) {
                    EntityTitleLine(
                        title = title,
                        style = headingStyle,
                        ellipsize = ellipsize,
                        visualContent = titleContent,
                        onTitleClick = onTitleClick,
                        onClickLabel = openLabel,
                    )
                    tags?.invoke()
                }
                if (subtitle != null) {
                    EntitySubtitleLine(
                        subtitle = subtitle,
                        ellipsize = ellipsize,
                        visualContent = subtitleContent,
                    )
                }
            }
        }
    }
}

@Composable
private fun EntityTitleLine(
    title: String,
    style: TextStyle,
    ellipsize: Boolean,
    visualContent: (@Composable () -> Unit)?,
    onTitleClick: (() -> Unit)?,
    onClickLabel: String,
) {
    val colors = BraceTheme.colors.components.entityTitle
    val interactionSource = remember { MutableInteractionSource() }
    val focusRequester = remember { FocusRequester() }
    val focused by interactionSource.collectIsFocusedAsState()
    val hovered by interactionSource.collectIsHoveredAsState()
    val pressed by interactionSource.collectIsPressedAsState()
    var overflow by remember(title, ellipsize) { mutableStateOf(false) }
    val shape = RoundedCornerShape(BraceTheme.shape.xs)
    val interactive = onTitleClick != null
    val titleColor = when {
        !interactive -> colors.title
        pressed -> colors.interactivePressed
        hovered -> colors.interactiveHover
        else -> colors.interactiveTitle
    }
    val actionModifier = if (onTitleClick != null) {
        Modifier
            .defaultMinSize(
                minWidth = BraceTheme.sizing.touchTarget,
                minHeight = BraceTheme.sizing.touchTarget,
            )
            .clip(shape)
            .focusRequester(focusRequester)
            .background(if (pressed) BraceTheme.colors.semantic.pressed else if (hovered) BraceTheme.colors.semantic.hover else Color.Transparent)
            .then(if (focused) Modifier.border(BraceTheme.sizing.focusRingWidth, colors.focusRing, shape) else Modifier)
            .clearAndSetSemantics {
                contentDescription = title
                heading()
                role = Role.Button
                onClick(onClickLabel) { onTitleClick(); true }
                requestFocus { focusRequester.requestFocus() }
            }
            .clickable(
                role = Role.Button,
                onClickLabel = onClickLabel,
                interactionSource = interactionSource,
                indication = null,
                onClick = onTitleClick,
            )
    } else Modifier.semantics {
        contentDescription = title
        heading()
    }
    val content: @Composable () -> Unit = {
        Box(
            modifier = actionModifier,
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(Modifier.clearAndSetSemantics {}) {
                if (visualContent != null) {
                    visualContent()
                } else {
                    Text(
                        text = title,
                        color = titleColor,
                        style = style,
                        maxLines = if (ellipsize) 1 else Int.MAX_VALUE,
                        softWrap = !ellipsize,
                        overflow = if (ellipsize) TextOverflow.Ellipsis else TextOverflow.Clip,
                        onTextLayout = { result ->
                            if (ellipsize) overflow = result.hasVisualOverflow
                        },
                    )
                }
            }
        }
    }
    if (ellipsize && visualContent == null && overflow) {
        BraceTooltip(
            text = title,
            target = content,
            enabled = overflow,
            modifier = Modifier.defaultMinSize(minHeight = BraceTheme.sizing.touchTarget),
        )
    } else content()
}

@Composable
private fun EntitySubtitleLine(
    subtitle: String,
    ellipsize: Boolean,
    visualContent: (@Composable () -> Unit)?,
) {
    val colors = BraceTheme.colors.components.entityTitle
    var overflow by remember(subtitle, ellipsize) { mutableStateOf(false) }
    val content: @Composable () -> Unit = {
        Box(
            Modifier
                .then(if (ellipsize) Modifier.fillMaxWidth() else Modifier)
                .semantics { contentDescription = subtitle },
        ) {
            Box(Modifier.clearAndSetSemantics {}) {
                if (visualContent != null) {
                    visualContent()
                } else {
                    Text(
                        text = subtitle,
                        color = colors.subtitle,
                        style = BraceTheme.typography.body,
                        maxLines = if (ellipsize) 1 else Int.MAX_VALUE,
                        softWrap = !ellipsize,
                        overflow = if (ellipsize) TextOverflow.Ellipsis else TextOverflow.Clip,
                        onTextLayout = { result ->
                            if (ellipsize) {
                                overflow = result.hasVisualOverflow
                            }
                        },
                    )
                }
            }
        }
    }
    if (ellipsize && visualContent == null && overflow) {
        BraceTooltip(
            text = subtitle,
            target = content,
            enabled = overflow,
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = BraceTheme.sizing.touchTarget),
        )
    } else content()
}

@Composable
private fun EntityPlaceholder(modifier: Modifier) {
    val colors = BraceTheme.colors.components.entityTitle
    val shape = RoundedCornerShape(BraceTheme.componentMetrics.entityTitle.loadingCornerRadius)
    Box(modifier.clip(shape).background(colors.loadingContainer).clearAndSetSemantics {})
}
