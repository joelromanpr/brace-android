package io.github.joelromanpr.brace.catalog

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import io.github.braceandroid.foundation.BraceTheme
import io.github.joelromanpr.brace.core.BraceButton
import io.github.joelromanpr.brace.core.BraceButtonGroup
import io.github.joelromanpr.brace.core.BraceButtonGroupAction
import io.github.joelromanpr.brace.core.BraceButtonGroupSize
import io.github.joelromanpr.brace.core.BraceButtonGroupVariant
import io.github.joelromanpr.brace.core.BraceButtonIntent
import io.github.joelromanpr.brace.core.BraceButtonVariant
import io.github.joelromanpr.brace.icons.BraceIcon
import io.github.joelromanpr.brace.icons.BraceIcons

/** Interactive ButtonGroup states for the Android catalog. */
@Composable
internal fun ButtonGroupCatalogSample() {
    var selected by rememberSaveable { mutableStateOf("list") }
    var vertical by rememberSaveable { mutableStateOf(false) }
    var variant by rememberSaveable { mutableStateOf(BraceButtonGroupVariant.Solid) }
    var large by rememberSaveable { mutableStateOf(false) }
    var canShare by rememberSaveable { mutableStateOf(true) }
    var count by rememberSaveable { mutableStateOf(0) }
    Column(verticalArrangement = Arrangement.spacedBy(BraceTheme.spacing.sm)) {
        BraceButtonGroup(
            actions = listOf(
                BraceButtonGroupAction("list", "List", onClick = { selected = "list"; count++ },
                    selected = selected == "list"),
                BraceButtonGroupAction("grid", "Grid", onClick = { selected = "grid"; count++ },
                    selected = selected == "grid"),
                BraceButtonGroupAction("share", "Share", onClick = { count++ },
                    enabled = canShare, intent = BraceButtonIntent.Primary),
            ),
            fill = true,
            vertical = vertical,
            variant = variant,
            size = if (large) BraceButtonGroupSize.Large else BraceButtonGroupSize.Small,
            accessibilityLabel = "Report actions",
        )
        Text("Selected: $selected · actions: $count", color = BraceTheme.colors.semantic.onSurface)
        BraceButtonGroup(
            actions = listOf(
                BraceButtonGroupAction("search", "Search records", onClick = { count++ },
                    showLabel = false, leadingIcon = { BraceIcon(BraceIcons.Search, null) }),
                BraceButtonGroupAction("close", "Close search", onClick = { count++ },
                    showLabel = false, leadingIcon = { BraceIcon(BraceIcons.Close, null) }),
            ),
            variant = BraceButtonGroupVariant.Outline,
            accessibilityLabel = "Icon actions",
        )
        Row(Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(BraceTheme.spacing.xs)) {
            BraceButton(if (vertical) "Horizontal" else "Vertical", onClick = { vertical = !vertical },
                variant = BraceButtonVariant.Outline)
            BraceButton("Variant: ${variant.name}", onClick = {
                variant = when (variant) {
                    BraceButtonGroupVariant.Solid -> BraceButtonGroupVariant.Outline
                    BraceButtonGroupVariant.Outline -> BraceButtonGroupVariant.Minimal
                    BraceButtonGroupVariant.Minimal -> BraceButtonGroupVariant.Solid
                }
            }, variant = BraceButtonVariant.Outline)
            BraceButton(if (large) "Small" else "Large", onClick = { large = !large },
                variant = BraceButtonVariant.Outline)
            BraceButton(if (canShare) "Disable share" else "Enable share", onClick = { canShare = !canShare },
                variant = BraceButtonVariant.Outline)
        }
    }
}
