package io.github.joelromanpr.brace.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import io.github.braceandroid.foundation.BraceTheme
import io.github.joelromanpr.brace.core.BraceButton
import io.github.joelromanpr.brace.core.BraceButtonVariant
import io.github.joelromanpr.brace.select.BraceCommand
import io.github.joelromanpr.brace.select.BraceCommandPalette
import io.github.joelromanpr.brace.select.rememberBraceQueryListState

/** Live command-palette states for the catalog. */
@Composable
internal fun CommandPaletteCatalogSample() {
    var open by rememberSaveable { mutableStateOf(false) }
    var loading by rememberSaveable { mutableStateOf(false) }
    var executed by rememberSaveable { mutableStateOf<String?>(null) }
    val state = rememberBraceQueryListState()
    val triggerFocus = androidx.compose.runtime.remember { FocusRequester() }
    val commands = listOf(
        BraceCommand("open", "open", "Open record", group = "Records",
            description = "Open the selected record", shortcut = "Ctrl+O"),
        BraceCommand("archive", "archive", "Archive record", group = "Records",
            enabled = false),
        BraceCommand("export", "export", "Export CSV", group = "Reports",
            description = "Download the current report"),
    )
    Column(verticalArrangement = Arrangement.spacedBy(BraceTheme.spacing.sm)) {
        Row(horizontalArrangement = Arrangement.spacedBy(BraceTheme.spacing.sm)) {
            BraceButton("Open commands", onClick = { open = true },
                modifier = Modifier.focusRequester(triggerFocus))
            BraceButton(if (loading) "Show results" else "Loading state",
                onClick = { loading = !loading }, variant = BraceButtonVariant.Outline)
        }
        Text("Executed: ${executed ?: "none"} · Query: ${state.query.ifBlank { "none" }}",
            color = BraceTheme.colors.semantic.onSurfaceMuted)
        BraceButton("Reset query", onClick = { state.query = "" },
            variant = BraceButtonVariant.Outline)
        BraceCommandPalette(commands, open, { open = it },
            onExecute = { executed = it.key }, title = "Commands", state = state,
            loading = loading, selectedKey = executed, restoreFocusTo = triggerFocus)
    }
}
