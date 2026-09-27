package io.github.joelromanpr.brace.consumer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.IntOffset
import io.github.braceandroid.foundation.BraceTheme
import io.github.joelromanpr.brace.datetime.BraceDatePicker
import io.github.joelromanpr.brace.datetime.BraceDateRange
import io.github.joelromanpr.brace.datetime.BraceDateRangePicker
import io.github.joelromanpr.brace.datetime.BraceDateRangeField
import io.github.joelromanpr.brace.datetime.BraceDateField
import io.github.joelromanpr.brace.datetime.BraceTimeField
import io.github.joelromanpr.brace.datetime.BraceTimePicker
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import io.github.joelromanpr.brace.core.BraceAlertDialog
import io.github.joelromanpr.brace.core.BraceBreadcrumb
import io.github.joelromanpr.brace.core.BraceBreadcrumbs
import io.github.joelromanpr.brace.core.BraceTopBar
import io.github.joelromanpr.brace.core.BraceTopBarGroup
import io.github.joelromanpr.brace.core.BraceTopBarTitle
import io.github.joelromanpr.brace.core.BraceTopBarDivider
import io.github.joelromanpr.brace.core.BraceButton
import io.github.joelromanpr.brace.core.BraceButtonIntent
import io.github.joelromanpr.brace.core.BraceCallout
import io.github.joelromanpr.brace.core.BraceCalloutIntent
import io.github.joelromanpr.brace.core.BraceCard
import io.github.joelromanpr.brace.core.BraceContextMenu
import io.github.joelromanpr.brace.core.BraceContextMenuPopup
import io.github.joelromanpr.brace.core.BraceControlGroup
import io.github.joelromanpr.brace.core.BraceFieldLabel
import io.github.joelromanpr.brace.core.BraceDialog
import io.github.joelromanpr.brace.core.BraceDrawer
import io.github.joelromanpr.brace.core.BraceDrawerPosition
import io.github.joelromanpr.brace.core.BraceEditableText
import io.github.joelromanpr.brace.core.BraceLink
import io.github.joelromanpr.brace.core.BraceLinkButton
import io.github.joelromanpr.brace.core.BraceLinkDestination
import io.github.joelromanpr.brace.core.BraceFormField
import io.github.joelromanpr.brace.core.BraceFormIntent
import io.github.joelromanpr.brace.core.BraceMenu
import io.github.joelromanpr.brace.core.BraceMenuIntent
import io.github.joelromanpr.brace.core.BraceMenuItem
import io.github.joelromanpr.brace.core.BraceOverlayHost
import io.github.joelromanpr.brace.core.BracePopover
import io.github.joelromanpr.brace.core.BraceProgressBar
import io.github.joelromanpr.brace.core.BraceSpinner
import io.github.joelromanpr.brace.core.BraceSkeleton
import io.github.joelromanpr.brace.core.BraceSection
import io.github.joelromanpr.brace.core.BraceShortcut
import io.github.joelromanpr.brace.core.BraceShortcutLabel
import io.github.joelromanpr.brace.core.BraceShortcutRegistry
import io.github.joelromanpr.brace.core.BraceShortcutScope
import io.github.joelromanpr.brace.core.BraceTextField
import io.github.joelromanpr.brace.core.BraceTextArea
import io.github.joelromanpr.brace.core.BraceTextAreaSize
import io.github.joelromanpr.brace.core.BraceNumericField
import io.github.joelromanpr.brace.core.BraceRadio
import io.github.joelromanpr.brace.core.BraceRadioGroup
import io.github.joelromanpr.brace.core.BraceRadioOption
import io.github.joelromanpr.brace.core.BraceSegmentedControl
import io.github.joelromanpr.brace.core.BraceSegmentedOption
import io.github.joelromanpr.brace.core.braceShortcuts
import io.github.joelromanpr.brace.core.rememberBraceShortcutRegistryState
import io.github.joelromanpr.brace.core.BraceTag
import io.github.joelromanpr.brace.core.BraceToastHost
import io.github.joelromanpr.brace.core.BraceToastIntent
import io.github.joelromanpr.brace.core.BraceToastSpec
import io.github.joelromanpr.brace.core.BraceTooltip
import io.github.joelromanpr.brace.core.rememberBraceToastState
import io.github.joelromanpr.brace.icons.BraceIcon
import io.github.joelromanpr.brace.icons.BraceIconByName
import io.github.joelromanpr.brace.icons.BraceIconButton
import io.github.joelromanpr.brace.icons.BraceIconRegistry
import io.github.joelromanpr.brace.icons.BraceIconRegistryProvider
import io.github.joelromanpr.brace.icons.BraceIcons
import io.github.joelromanpr.brace.blueprinticons.BraceBlueprintIcon
import io.github.joelromanpr.brace.blueprinticons.BraceBlueprintIconNames
import io.github.joelromanpr.brace.blueprinticons.BraceBlueprintIconPack
import io.github.joelromanpr.brace.blueprinticonsnext.BraceBlueprintNextIcon
import io.github.joelromanpr.brace.blueprinticonsnext.BraceBlueprintNextIconNames
import io.github.joelromanpr.brace.blueprinticonsnext.BraceBlueprintNextIconPack
import io.github.joelromanpr.brace.blueprinticonsnext.BraceBlueprintNextIconVariant
import io.github.joelromanpr.brace.select.BraceSelect
import io.github.joelromanpr.brace.select.BraceSelectOption
import io.github.joelromanpr.brace.select.BraceSuggest
import io.github.joelromanpr.brace.select.BraceMultiSelect
import io.github.joelromanpr.brace.select.rememberBraceQueryListState
import io.github.joelromanpr.brace.table.BraceDataTable
import io.github.joelromanpr.brace.table.BraceTableColumn
import io.github.joelromanpr.brace.table.BraceTableSelection
import io.github.joelromanpr.brace.table.rememberBraceTableViewport
import androidx.compose.ui.unit.dp

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
                var amount by rememberSaveable { mutableStateOf("0.2") }
                var iconName by remember { mutableStateOf("search") }
                val blueprintIconPack by produceState<BraceBlueprintIconPack?>(null) {
                    value = withContext(Dispatchers.IO) { BraceBlueprintIconPack.load(applicationContext) }
                }
                val blueprintNextIconPack by produceState<BraceBlueprintNextIconPack?>(null) {
                    value = withContext(Dispatchers.IO) { BraceBlueprintNextIconPack.load(applicationContext) }
                }
                val iconRegistry = remember {
                    BraceIconRegistry.Default.register("custom-check",
                        BraceIconRegistry.Default.resolve("check"))
                }
                var regionKey by remember { mutableStateOf<String?>(null) }
                var regionExpanded by remember { mutableStateOf(false) }
                val regionQuery = rememberBraceQueryListState()
                var dueDate by remember { mutableStateOf<LocalDate?>(null) }
                var travelRange by remember { mutableStateOf(BraceDateRange()) }
                var dueTime by rememberSaveable { mutableStateOf("14:30") }
                var meal by remember { mutableStateOf("soup") }
                var layout by remember { mutableStateOf("list") }
                val shortcutState = rememberBraceShortcutRegistryState()
                val toasts = rememberBraceToastState()
                Box(Modifier.fillMaxSize()) {
                    Column {
                        BraceTopBar(
                            startContent = { BraceTopBarGroup {
                                BraceTopBarTitle("Imports")
                                BraceTopBarDivider()
                            } },
                            endContent = { BraceTopBarGroup {
                                BraceButton("Refresh", onClick = { count++ })
                            } },
                        )
                        BraceCard {
                            BraceButton(label = "Saved $count", onClick = { count++ })
                        }
                        val tableRows = remember { listOf("Ready", "Review") }
                        var selectedTable: BraceTableSelection? by remember {
                            mutableStateOf(BraceTableSelection.Range("Ready", "status", "Review", "status"))
                        }
                        var tableColumnWidth by remember { mutableStateOf(120.dp) }
                        var tableRowHeight by remember { mutableStateOf(64.dp) }
                        val tableViewport = rememberBraceTableViewport()
                        BraceDataTable(
                            rows = tableRows,
                            rowKey = { it },
                            columns = listOf(BraceTableColumn<String>("status", "Status", 120.dp, { it })),
                            selection = selectedTable,
                            onSelectionChange = { selectedTable = it },
                            viewport = tableViewport,
                            height = 160.dp,
                            columnWidths = mapOf("status" to tableColumnWidth),
                            onColumnWidthChange = { _, width -> tableColumnWidth = width },
                            rowHeights = mapOf("Ready" to tableRowHeight),
                            onRowHeightChange = { _, height -> tableRowHeight = height },
                        )
                        BraceButton("Select status column", onClick = {
                            selectedTable = BraceTableSelection.Column("status")
                        })
                        BraceSection(title = "Job status", collapsible = true) {
                            BraceProgressBar(label = "Import progress", value = 0.5f)
                            BraceSpinner(label = "Indexing records", value = 0.5f)
                            BraceSkeleton(label = "Loading next batch")
                        }
                        BraceBreadcrumbs(listOf(BraceBreadcrumb("Home", onClick = {}), BraceBreadcrumb("Imports")))
                        BraceLink("Open reports", BraceLinkDestination.Action("Reports") { count++ })
                        BraceLinkButton("Open guide", BraceLinkDestination.Uri("https://example.org/guide", "Guide"),
                            onOpenUri = { count++ })
                        BraceTag("Active")
                        BraceFieldLabel("Export format", spokenLabel = "Export format, CSV") { controlModifier ->
                            BraceButton("CSV", onClick = {}, modifier = controlModifier)
                        }
                        BraceControlGroup(fill = true, accessibilityLabel = "Report actions") {
                            Item { controlModifier ->
                                BraceButton("Preview", onClick = { count++ }, modifier = controlModifier)
                            }
                            Item(fill = false) { controlModifier ->
                                BraceButton("Export", onClick = { count++ }, modifier = controlModifier)
                            }
                        }
                        BraceNumericField(amount, { amount = it }, label = "Amount",
                            min = 0.0, max = 100.0, minorStepSize = 0.1)
                        BraceDateField(dueDate, { dueDate = it }, "Due date", locale = Locale.US)
                        BraceDatePicker(dueDate, { dueDate = it }, locale = Locale.US,
                            minDate = LocalDate.of(2026, 1, 1))
                        BraceDateRangeField(travelRange, { travelRange = it }, "Travel dates", locale = Locale.US)
                        BraceDateRangePicker(travelRange, { travelRange = it }, locale = Locale.US)
                        BraceTimeField(LocalTime.parse(dueTime), { dueTime = it?.toString() ?: "14:30" },
                            label = "Due time", locale = Locale.US, use24Hour = true)
                        BraceTimePicker(LocalTime.parse(dueTime), { dueTime = it.toString() },
                            locale = Locale.US, use24Hour = true)
                        BraceRadio(selected = meal == "soup", onSelect = { meal = "soup" }, label = "Soup")
                        BraceRadioGroup(
                            options = listOf(BraceRadioOption("soup", "Soup"), BraceRadioOption("salad", "Salad")),
                            selectedValue = meal, onValueChange = { meal = it }, label = "Lunch special",
                        )
                        BraceSegmentedControl(
                            options = listOf(BraceSegmentedOption("list", "List"), BraceSegmentedOption("grid", "Grid")),
                            value = layout, onValueChange = { layout = it }, label = "Layout",
                        )
                        BraceCallout(title = "Ready", intent = BraceCalloutIntent.Success)
                        BraceIconRegistryProvider(iconRegistry) {
                            Row {
                                BraceIcon(BraceIcons.Info, contentDescription = null)
                                blueprintIconPack?.let { pack ->
                                    BraceBlueprintIcon(pack, BraceBlueprintIconNames.Search,
                                        contentDescription = null)
                                }
                                blueprintNextIconPack?.let { pack ->
                                    BraceBlueprintNextIcon(pack, BraceBlueprintNextIconNames.MagnifyingGlass,
                                        contentDescription = null,
                                        variant = BraceBlueprintNextIconVariant.Filled)
                                }
                                BraceIconByName(iconName, contentDescription = "Status icon")
                                BraceIconButton(BraceIcons.Search, label = "Search records",
                                    onClick = { iconName = "custom-check" })
                            }
                        }
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
                                BraceSuggest(
                                    value = TextFieldValue("North"),
                                    onValueChange = {},
                                    options = listOf(BraceSelectOption("north", "north", "North")),
                                    selectedKey = "north",
                                    onSelect = {},
                                    expanded = false,
                                    onExpandedChange = {},
                                    label = "Suggested region",
                                )
                                BraceMultiSelect(
                                    options = listOf(BraceSelectOption("east", "east", "East"),
                                        BraceSelectOption("west", "west", "West")),
                                    selectedKeys = listOf("east"),
                                    onSelectedKeysChange = {},
                                    expanded = false,
                                    onExpandedChange = {},
                                    label = "Regions",
                                )
                                BraceSelect(
                                    options = listOf(BraceSelectOption("east", "east", "East"),
                                        BraceSelectOption("west", "west", "West")),
                                    selectedKey = regionKey,
                                    onSelect = { regionKey = it.key },
                                    expanded = regionExpanded,
                                    onExpandedChange = { regionExpanded = it },
                                    label = "Region",
                                    state = regionQuery,
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
