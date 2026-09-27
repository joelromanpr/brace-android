package io.github.joelromanpr.brace.select

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.res.stringResource
import io.github.braceandroid.foundation.BraceTheme

/** Field-shaped anchor shared by the suggestion and multiple-choice Android adaptations. */
@Composable
internal fun BraceSelectTrigger(
    label: String,
    onClick: () -> Unit,
    enabled: Boolean,
    expanded: Boolean,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val hovered by interaction.collectIsHoveredAsState()
    val pressed by interaction.collectIsPressedAsState()
    val colors = BraceTheme.colors.components.select
    val semantic = BraceTheme.colors.semantic
    val metrics = BraceTheme.componentMetrics.select
    val expansionText = stringResource(if (expanded && enabled) R.string.brace_select_expanded
        else R.string.brace_select_collapsed)
    val shape = RoundedCornerShape(metrics.cornerRadius)
    val container = when {
        !enabled -> semantic.disabledContainer
        pressed -> semantic.pressed
        hovered -> colors.activeContainer
        else -> colors.searchContainer
    }
    val border = if (focused) colors.focusRing else colors.searchBorder
    Box(
        modifier = modifier.fillMaxWidth()
            .defaultMinSize(minHeight = BraceTheme.sizing.touchTarget)
            .background(container, shape)
            .border(if (focused) BraceTheme.sizing.focusRingWidth else BraceTheme.sizing.borderWidth,
                border, shape)
            .clickable(enabled = enabled, role = Role.Button, interactionSource = interaction,
                indication = null, onClick = onClick)
            .semantics {
                contentDescription = label
                stateDescription = expansionText
                if (!enabled) disabled()
            }
            .padding(horizontal = metrics.searchHorizontalPadding, vertical = BraceTheme.spacing.sm),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(label, color = if (enabled) colors.content else colors.disabledContent,
            style = BraceTheme.typography.body)
    }
}
