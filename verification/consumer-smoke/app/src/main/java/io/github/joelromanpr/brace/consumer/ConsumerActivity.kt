package io.github.joelromanpr.brace.consumer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.IntOffset
import io.github.braceandroid.foundation.BraceTheme
import io.github.joelromanpr.brace.core.BraceAlertDialog
import io.github.joelromanpr.brace.core.BraceBreadcrumb
import io.github.joelromanpr.brace.core.BraceBreadcrumbs
import io.github.joelromanpr.brace.core.BraceButton
import io.github.joelromanpr.brace.core.BraceButtonIntent
import io.github.joelromanpr.brace.core.BraceCallout
import io.github.joelromanpr.brace.core.BraceCalloutIntent
import io.github.joelromanpr.brace.core.BraceCard
import io.github.joelromanpr.brace.core.BraceContextMenu
import io.github.joelromanpr.brace.core.BraceContextMenuPopup
import io.github.joelromanpr.brace.core.BraceDialog
import io.github.joelromanpr.brace.core.BraceDrawer
import io.github.joelromanpr.brace.core.BraceDrawerPosition
import io.github.joelromanpr.brace.core.BraceEditableText
import io.github.joelromanpr.brace.core.BraceFormField
import io.github.joelromanpr.brace.core.BraceFormIntent
import io.github.joelromanpr.brace.core.BraceMenu
import io.github.joelromanpr.brace.core.BraceMenuIntent
import io.github.joelromanpr.brace.core.BraceMenuItem
import io.github.joelromanpr.brace.core.BraceOverlayHost
import io.github.joelromanpr.brace.core.BracePopover
import io.github.joelromanpr.brace.core.BraceProgressBar
import io.github.joelromanpr.brace.core.BraceSection
import io.github.joelromanpr.brace.core.BraceShortcut
import io.github.joelromanpr.brace.core.BraceShortcutLabel
import io.github.joelromanpr.brace.core.BraceShortcutRegistry
import io.github.joelromanpr.brace.core.BraceShortcutScope
import io.github.joelromanpr.brace.core.BraceTextField
import io.github.joelromanpr.brace.core.BraceTextArea
import io.github.joelromanpr.brace.core.BraceTextAreaSize
import io.github.joelromanpr.brace.core.braceShortcuts
import io.github.joelromanpr.brace.core.rememberBraceShortcutRegistryState
import io.github.joelromanpr.brace.core.BraceTag
import io.github.joelromanpr.brace.core.BraceToastHost
import io.github.joelromanpr.brace.core.BraceToastIntent
import io.github.joelromanpr.brace.core.BraceToastSpec
import io.github.joelromanpr.brace.core.BraceTooltip
import io.github.joelromanpr.brace.core.rememberBraceToastState

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
                var contextOpen by remember { mutableStateOf(false) }
                var pointMenuOpen by remember { mutableStateOf(false) }
                val pointMenuTrigger = remember { FocusRequester() }
                var hadPointMenuOpen by remember { mutableStateOf(false) }
                LaunchedEffect(pointMenuOpen) {
                    if (pointMenuOpen) hadPointMenuOpen = true
                    else if (hadPointMenuOpen) {
                        pointMenuTrigger.requestFocus()
                        hadPointMenuOpen = false
                    }
                }
                var search by remember { mutableStateOf("") }
                var caseNotes by remember { mutableStateOf("") }
                var reportTitle by remember { mutableStateOf("Quarterly report") }
                val shortcutState = rememberBraceShortcutRegistryState()
                val toasts = rememberBraceToastState()
                Box(Modifier.fillMaxSize()) {
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
                                intent = BraceMenuIntent.Danger)
                        }
                        BraceContextMenu(
                            expanded = contextOpen,
                            onExpandedChange = { contextOpen = it },
                            title = "Report actions",
                            targetIsFocusable = true,
                            target = { targetModifier -> BraceButton("Context actions", onClick = {}, modifier = targetModifier) },
                        ) { dismiss ->
                            BraceMenuItem("Open report", onClick = { count++; dismiss() })
                        }
                        BraceButton("Open point menu", onClick = { pointMenuOpen = true },
                            modifier = Modifier.focusRequester(pointMenuTrigger))
                        BraceContextMenuPopup(
                            expanded = pointMenuOpen,
                            onDismissRequest = { pointMenuOpen = false },
                            targetOffset = IntOffset(80, 220),
                            title = "More actions",
                        ) { dismiss ->
                            BraceMenuItem("Refresh", onClick = { count++; dismiss() })
                        }
                        BraceShortcutRegistry(
                            shortcuts = listOf(BraceShortcut("ctrl+r", "Refresh records", spokenComboLabel = "Control plus R",
                                onKeyDown = { count++ })),
                            discoveryTitle = "Keyboard shortcuts",
                            state = shortcutState,
                        ) {
                            Column {
                                BraceButton("Show shortcuts", onClick = { shortcutState.showDiscovery() })
                                BraceShortcutScope(shortcuts = listOf(
                                    BraceShortcut("ctrl+e", "Export here", onKeyDown = { count++ }),
                                    BraceShortcut("ctrl+g", "Global export", global = true,
                                        onKeyDown = { count++ }),
                                )) {
                                    BraceButton("Export", onClick = { count++ })
                                }
                                Column(Modifier.braceShortcuts(listOf(BraceShortcut("ctrl+k",
                                    "Find records", onKeyDown = { count++ })))) {
                                    BraceButton("Find", onClick = { count++ })
                                }
                                BraceShortcutLabel("Ctrl+R", spokenLabel = "Control plus R")
                                BraceTextField(search, { search = it }, "Search")
                                BraceFormField(
                                    label = "Case notes",
                                    helperText = "Include the event time",
                                    required = true,
                                    requiredDescription = "Required",
                                ) { controlModifier ->
                                    BraceTextArea(
                                        value = caseNotes,
                                        onValueChange = { caseNotes = it },
                                        accessibilityLabel = "Case notes",
                                        modifier = controlModifier,
                                        minLines = 2,
                                        maxLines = 6,
                                        autoResize = true,
                                        intent = BraceFormIntent.Primary,
                                        size = BraceTextAreaSize.Medium,
                                    )
                                }
                                BraceEditableText(
                                    value = reportTitle,
                                    onValueChange = { reportTitle = it },
                                    label = "Report title",
                                    editActionLabel = "Edit report title",
                                )
                            }
                        }
                        BraceTooltip(
                            text = "Imports include archived records",
                            target = { BraceButton("Import help", onClick = {}) },
                        )
                        BraceButton("Show notification", onClick = {
                            toasts.show(BraceToastSpec("Changes saved", intent = BraceToastIntent.Success))
                        })
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
                    BraceToastHost(toasts)
                }
            }
        }
    }
}
