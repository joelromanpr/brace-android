package io.github.joelromanpr.brace.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import io.github.braceandroid.foundation.BraceTheme
import io.github.joelromanpr.brace.core.BraceButton
import io.github.joelromanpr.brace.core.BraceButtonVariant
import io.github.joelromanpr.brace.core.BraceTab
import io.github.joelromanpr.brace.core.BraceTabPanel
import io.github.joelromanpr.brace.core.BraceTabSize
import io.github.joelromanpr.brace.core.BraceTabSpacer
import io.github.joelromanpr.brace.core.BraceTabs
import io.github.joelromanpr.brace.core.BraceTabsOrientation

@Composable
internal fun TabsCatalogSample() {
    var selected by rememberSaveable { mutableStateOf("overview") }
    var vertical by rememberSaveable { mutableStateOf(false) }
    var large by rememberSaveable { mutableStateOf(false) }
    var refreshes by rememberSaveable { mutableStateOf(0) }
    val tabs = listOf(
        BraceTab("overview", "Overview"),
        BraceTab("activity", "Recent activity", badge = "3", accessibilityLabel = "Recent activity, 3 updates"),
        BraceTab("locked", "Locked", enabled = false),
        BraceTab("settings", "Workspace settings"),
    )
    Column(verticalArrangement = Arrangement.spacedBy(BraceTheme.spacing.sm)) {
        Row(horizontalArrangement = Arrangement.spacedBy(BraceTheme.spacing.xs)) {
            BraceButton(
                if (vertical) "Horizontal" else "Vertical",
                onClick = { vertical = !vertical },
                variant = BraceButtonVariant.Outline,
            )
            BraceButton(
                if (large) "Medium" else "Large",
                onClick = { large = !large },
                variant = BraceButtonVariant.Outline,
            )
        }
        BraceTabs(
            tabs = tabs,
            selectedTabId = selected,
            onTabSelected = { selected = it },
            orientation = if (vertical) BraceTabsOrientation.Vertical else BraceTabsOrientation.Horizontal,
            size = if (large) BraceTabSize.Large else BraceTabSize.Medium,
            trailingContent = { BraceButton("Refresh", onClick = { refreshes++ }) },
        ) { tab ->
            Column(verticalArrangement = Arrangement.spacedBy(BraceTheme.spacing.xs)) {
                Text("${tab.label} panel", color = BraceTheme.colors.semantic.onSurface)
                var visits by rememberSaveable { mutableStateOf(0) }
                BraceButton("Panel visits: $visits", onClick = { visits++ })
            }
        }
        BraceTabPanel(tab = tabs.first { it.id == selected }, selectedTabId = selected) {
            Text("Detached panel: $selected · refreshed $refreshes times",
                color = BraceTheme.colors.semantic.onSurfaceMuted)
        }
        Row(Modifier.fillMaxWidth()) {
            Text("Start", color = BraceTheme.colors.semantic.onSurfaceMuted)
            BraceTabSpacer()
            Text("Trailing", color = BraceTheme.colors.semantic.onSurfaceMuted)
        }
    }
}
