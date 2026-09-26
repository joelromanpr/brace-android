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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import io.github.braceandroid.foundation.BraceTheme

/** Visual intent of a [BraceButton]. */
public enum class BraceButtonIntent { Primary, Secondary, Danger }

/** Filled or outlined treatment for a [BraceButton]. */
public enum class BraceButtonVariant { Solid, Outline }

/**
 * A keyboard, mouse, and touch operable action with a minimum accessible hit target.
 *
 * The caller owns the enabled and loading states. While [loading] is true, activation is
 * suppressed and an announced loading state replaces the label visually. Icon slots are
 * decorative to accessibility services because the button always announces [label].
 */
@Composable
public fun BraceButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    intent: BraceButtonIntent = BraceButtonIntent.Primary,
    variant: BraceButtonVariant = BraceButtonVariant.Solid,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val focused by interactionSource.collectIsFocusedAsState()
    val hovered by interactionSource.collectIsHoveredAsState()
    val button = BraceTheme.colors.components.button
    val semantic = BraceTheme.colors.semantic
    val container: Color
    val content: Color
    val outline: Color
    if (!enabled || loading) {
        container = button.disabledContainer
        content = button.disabledContent
        outline = semantic.border
    } else when (intent) {
        BraceButtonIntent.Primary -> {
            container = when {
                pressed -> button.primaryPressedContainer
                hovered -> button.primaryHoverContainer
                else -> button.primaryContainer
            }
            content = button.primaryContent
            outline = container
        }
        BraceButtonIntent.Secondary -> {
            container = when {
                pressed -> button.secondaryPressedContainer
                hovered -> button.secondaryHoverContainer
                else -> button.secondaryContainer
            }
            content = button.secondaryContent
            outline = button.secondaryBorder
        }
        BraceButtonIntent.Danger -> {
            container = when {
                pressed -> button.dangerPressedContainer
                hovered -> button.dangerHoverContainer
                else -> button.dangerContainer
            }
            content = button.dangerContent
            outline = container
        }
    }
    val shape = RoundedCornerShape(BraceTheme.componentMetrics.button.cornerRadius)
    val actualContainer = if (enabled && !loading && variant == BraceButtonVariant.Outline) semantic.surface else container
    val actualContent = if (!enabled || loading) content else if (variant == BraceButtonVariant.Outline) when (intent) {
        BraceButtonIntent.Primary -> semantic.primary
        BraceButtonIntent.Secondary -> button.secondaryContent
        BraceButtonIntent.Danger -> button.dangerContainer
    } else content
    val borderColor = if (focused) button.focusRing else outline
    Box(
        modifier = modifier
            .defaultMinSize(minWidth = BraceTheme.sizing.touchTarget, minHeight = BraceTheme.sizing.touchTarget)
            .clickable(
                enabled = enabled && !loading,
                role = Role.Button,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .semantics {
                contentDescription = label
                if (loading) stateDescription = "Loading"
            },
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = BraceTheme.densityTokens.controlHeightDp)
                .background(actualContainer, shape)
                .border(BorderStroke(if (focused) BraceTheme.sizing.focusRingWidth else BraceTheme.sizing.borderWidth, borderColor), shape)
                .padding(horizontal = BraceTheme.componentMetrics.button.horizontalPadding, vertical = BraceTheme.spacing.xs),
            horizontalArrangement = Arrangement.spacedBy(BraceTheme.componentMetrics.button.iconGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            leadingIcon?.invoke()
            Text(
                text = if (loading) "…" else label,
                color = actualContent,
                style = BraceTheme.typography.label,
                textAlign = TextAlign.Center,
            )
            trailingIcon?.invoke()
        }
    }
}
