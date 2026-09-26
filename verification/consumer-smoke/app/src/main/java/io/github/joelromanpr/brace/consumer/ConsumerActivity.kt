package io.github.joelromanpr.brace.consumer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import io.github.braceandroid.foundation.BraceTheme
import io.github.joelromanpr.brace.core.BraceButton
import io.github.joelromanpr.brace.core.BraceBreadcrumb
import io.github.joelromanpr.brace.core.BraceBreadcrumbs
import io.github.joelromanpr.brace.core.BraceCallout
import io.github.joelromanpr.brace.core.BraceCalloutIntent
import io.github.joelromanpr.brace.core.BraceTag
import io.github.joelromanpr.brace.core.BraceCard
import io.github.joelromanpr.brace.core.BraceProgressBar
import io.github.joelromanpr.brace.core.BraceSection
import io.github.joelromanpr.brace.core.BraceMenu
import io.github.joelromanpr.brace.core.BraceMenuItem
import io.github.joelromanpr.brace.core.BraceDialog
import io.github.joelromanpr.brace.core.BraceDrawer
import io.github.joelromanpr.brace.core.BraceDrawerPosition
import io.github.joelromanpr.brace.core.BracePopover
import io.github.joelromanpr.brace.core.BraceAlertDialog
import io.github.joelromanpr.brace.core.BraceOverlayHost
import io.github.joelromanpr.brace.core.BraceButtonIntent

/** Compiles against Maven coordinates only, with no dependency on the source checkout. */
class ConsumerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BraceTheme {
                var count by remember { mutableStateOf(0) }
                var dialogOpen by remember { mutableStateOf(false) }
                var alertOpen by remember { mutableStateOf(false) }
                var drawerOpen by remember { mutableStateOf(false) }
                var popoverOpen by remember { mutableStateOf(false) }
                Column {
                    BraceCard {
                        BraceButton(label = "Saved $count", onClick = { count++ })
                    }
                    BraceSection(title = "Job status", collapsible = true) {
                        BraceProgressBar(label = "Import progress", value = 0.5f)
                    }
                    BraceBreadcrumbs(listOf(BraceBreadcrumb("Home", onClick = {}), BraceBreadcrumb("Imports")))
                    BraceTag("Active")
                    BraceCallout(title = "Ready", intent = BraceCalloutIntent.Success)
                    BraceMenu {
                        BraceMenuItem("Edit project", onClick = { dialogOpen = true })
                        BraceMenuItem("Delete report", onClick = { alertOpen = true },
                            intent = io.github.joelromanpr.brace.core.BraceMenuIntent.Danger)
                    }
                    BraceOverlayHost {
                        BracePopover(
                            expanded = popoverOpen,
                            onDismissRequest = { popoverOpen = false },
                            title = "Filter options",
                            target = { BraceButton("Popover filters", onClick = { popoverOpen = true }) },
                        ) { BraceButton("Apply", onClick = { popoverOpen = false }) }
                        BraceButton("Drawer filters", onClick = { drawerOpen = true })
                        BraceDrawer(
                            open = drawerOpen,
                            onDismissRequest = { drawerOpen = false },
                            title = "Filters",
                            position = BraceDrawerPosition.End,
                            footer = { BraceButton("Apply", onClick = { drawerOpen = false }) },
                        ) { BraceTag("Active records") }
                        BraceDialog(
                            open = dialogOpen,
                            onDismissRequest = { dialogOpen = false },
                            title = "Edit project",
                            actions = { BraceButton("Close", onClick = { dialogOpen = false }) },
                        ) { BraceTag("Editable content") }
                        BraceAlertDialog(
                            open = alertOpen,
                            title = "Delete report?",
                            onConfirm = { alertOpen = false },
                            onCancel = { alertOpen = false },
                            confirmLabel = "Delete",
                            confirmIntent = BraceButtonIntent.Danger,
                        )
                    }
                }
            }
        }
    }
}
