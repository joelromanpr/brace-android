package io.github.joelromanpr.brace.catalog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import io.github.braceandroid.foundation.BraceBrandColors
import io.github.braceandroid.foundation.BraceColorMode
import io.github.braceandroid.foundation.BraceContrast
import io.github.braceandroid.foundation.BraceDensity
import io.github.braceandroid.foundation.BraceMotion
import io.github.braceandroid.foundation.BraceTheme
import io.github.joelromanpr.brace.core.BraceButton
import io.github.joelromanpr.brace.core.BraceButtonIntent
import io.github.joelromanpr.brace.core.BraceButtonVariant
import io.github.joelromanpr.brace.core.BraceCheckbox
import io.github.joelromanpr.brace.core.BraceControlGroup
import io.github.joelromanpr.brace.core.BraceFieldLabel
import io.github.joelromanpr.brace.core.BraceRadio
import io.github.joelromanpr.brace.core.BraceRadioGroup
import io.github.joelromanpr.brace.core.BraceRadioOption
import io.github.joelromanpr.brace.core.BraceSegmentedControl
import io.github.joelromanpr.brace.core.BraceSegmentedOption
import io.github.joelromanpr.brace.core.BraceSegmentedIntent
import io.github.joelromanpr.brace.core.BraceSegmentedSize
import io.github.joelromanpr.brace.core.BraceCard
import io.github.joelromanpr.brace.core.BraceBreadcrumb
import io.github.joelromanpr.brace.core.BraceBreadcrumbItem
import io.github.joelromanpr.brace.core.BraceBreadcrumbs
import io.github.joelromanpr.brace.core.BraceTopBar
import io.github.joelromanpr.brace.core.BraceTopBarGroup
import io.github.joelromanpr.brace.core.BraceTopBarTitle
import io.github.joelromanpr.brace.core.BraceTopBarDivider
import io.github.joelromanpr.brace.core.BraceCallout
import io.github.joelromanpr.brace.core.BraceCalloutIntent
import io.github.joelromanpr.brace.core.BraceCompoundTag
import io.github.joelromanpr.brace.core.BraceEmptyState
import io.github.joelromanpr.brace.core.BraceTag
import io.github.joelromanpr.brace.core.BraceTagIntent
import io.github.joelromanpr.brace.core.BraceCardElevation
import io.github.joelromanpr.brace.core.BraceCardList
import io.github.joelromanpr.brace.core.BraceDivider
import io.github.joelromanpr.brace.core.BraceDividerOrientation
import io.github.joelromanpr.brace.core.BraceProgressBar
import io.github.joelromanpr.brace.core.BraceSpinner
import io.github.joelromanpr.brace.core.BraceSpinnerSize
import io.github.joelromanpr.brace.core.BraceSkeleton
import io.github.joelromanpr.brace.core.BraceMenu
import io.github.joelromanpr.brace.core.BraceMenuPopup
import io.github.joelromanpr.brace.core.BraceMenuItem
import io.github.joelromanpr.brace.core.BraceMenuDivider
import io.github.joelromanpr.brace.core.BraceOverlay
import io.github.joelromanpr.brace.core.BraceOverlayHost
import io.github.joelromanpr.brace.core.BraceDialog
import io.github.joelromanpr.brace.core.BraceDrawer
import io.github.joelromanpr.brace.core.BraceDrawerPosition
import io.github.joelromanpr.brace.core.BracePopover
import io.github.joelromanpr.brace.core.BracePopoverPlacement
import io.github.joelromanpr.brace.core.BraceContextMenu
import io.github.joelromanpr.brace.core.BraceContextMenuPopup
import io.github.joelromanpr.brace.core.BraceShortcut
import io.github.joelromanpr.brace.core.BraceShortcutLabel
import io.github.joelromanpr.brace.core.BraceShortcutRegistry
import io.github.joelromanpr.brace.core.BraceShortcutScope
import io.github.joelromanpr.brace.core.rememberBraceShortcutRegistryState
import io.github.joelromanpr.brace.core.braceShortcuts
import io.github.joelromanpr.brace.core.BraceTooltip
import io.github.joelromanpr.brace.core.BraceToastHost
import io.github.joelromanpr.brace.core.BraceToastIntent
import io.github.joelromanpr.brace.core.BraceToastPosition
import io.github.joelromanpr.brace.core.BraceToastSpec
import io.github.joelromanpr.brace.core.BraceToastState
import io.github.joelromanpr.brace.core.BraceAlertDialog
import io.github.joelromanpr.brace.core.rememberBraceOverlayState
import io.github.joelromanpr.brace.core.BraceProgressIntent
import io.github.joelromanpr.brace.core.BraceSection
import io.github.joelromanpr.brace.core.BraceSectionCard
import io.github.joelromanpr.brace.core.BraceSwitch
import io.github.joelromanpr.brace.core.BraceTextField
import io.github.joelromanpr.brace.core.BraceFormField
import io.github.joelromanpr.brace.core.BraceFormIntent
import io.github.joelromanpr.brace.core.BraceTextArea
import io.github.joelromanpr.brace.core.BraceTextAreaSize
import io.github.joelromanpr.brace.core.BraceEditableText
import io.github.joelromanpr.brace.core.BraceEditableTextIntent
import io.github.joelromanpr.brace.core.BraceNumericField
import io.github.joelromanpr.brace.core.BraceNumericButtonPosition
import io.github.joelromanpr.brace.core.BraceNumericFieldSize
import io.github.joelromanpr.brace.core.BraceNumericIntent
import io.github.joelromanpr.brace.icons.BraceIcon
import io.github.joelromanpr.brace.icons.BraceIconByName
import io.github.joelromanpr.brace.icons.BraceIconButton
import io.github.joelromanpr.brace.icons.BraceIconIntent
import io.github.joelromanpr.brace.icons.BraceIconRegistry
import io.github.joelromanpr.brace.icons.BraceIconRegistryProvider
import io.github.joelromanpr.brace.icons.BraceIconSize
import io.github.joelromanpr.brace.icons.BraceIcons
import io.github.joelromanpr.brace.blueprinticons.BraceBlueprintIcon
import io.github.joelromanpr.brace.blueprinticons.BraceBlueprintIconByName
import io.github.joelromanpr.brace.blueprinticons.BraceBlueprintIconNames
import io.github.joelromanpr.brace.blueprinticons.BraceBlueprintIconPack
import io.github.joelromanpr.brace.blueprinticons.BraceBlueprintIconResolution
import io.github.joelromanpr.brace.blueprinticonsnext.BraceBlueprintNextIcon
import io.github.joelromanpr.brace.blueprinticonsnext.BraceBlueprintNextIconByName
import io.github.joelromanpr.brace.blueprinticonsnext.BraceBlueprintNextIconNames
import io.github.joelromanpr.brace.blueprinticonsnext.BraceBlueprintNextIconPack
import io.github.joelromanpr.brace.blueprinticonsnext.BraceBlueprintNextIconVariant
import io.github.joelromanpr.brace.select.BraceSelect
import io.github.joelromanpr.brace.select.BraceSelectOption
import io.github.joelromanpr.brace.select.rememberBraceQueryListState
import io.github.joelromanpr.brace.select.braceQueryNavigation
import io.github.joelromanpr.brace.core.BraceLink
import io.github.joelromanpr.brace.core.BraceLinkButton
import io.github.joelromanpr.brace.core.BraceLinkColor
import io.github.joelromanpr.brace.core.BraceLinkDestination
import io.github.joelromanpr.brace.core.BraceLinkUnderline
import io.github.joelromanpr.brace.datetime.BraceDateField
import io.github.joelromanpr.brace.datetime.BraceDatePicker
import io.github.joelromanpr.brace.datetime.BraceDateRange
import io.github.joelromanpr.brace.datetime.BraceDateRangePicker
import io.github.joelromanpr.brace.datetime.BraceDateRangeField
import io.github.joelromanpr.brace.datetime.BraceDateRangeShortcut
import io.github.joelromanpr.brace.datetime.BraceDateShortcut
import io.github.joelromanpr.brace.datetime.BraceTimeField
import io.github.joelromanpr.brace.datetime.BraceTimePicker
import io.github.joelromanpr.brace.datetime.BraceTimePrecision
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import org.json.JSONObject
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch

/** Interactive catalog whose component names and availability come from the pinned inventory. */
class CatalogActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { Catalog() }
    }
}

internal data class CatalogEntry(
    val id: String,
    val name: String,
    val family: String,
    val status: String,
    val api: String,
    val behavior: String,
    val classification: String,
    val reason: String,
    val url: String,
)

private val usageExamples = mapOf(
    "core-icon" to """BraceIcon(BraceIcons.Info, contentDescription = null, intent = BraceIconIntent.Primary)
BraceIconButton(BraceIcons.Search, label = "Search records", onClick = { openSearch() })""".trimIndent(),
    "icons-icon-glyph-catalog" to """val context = LocalContext.current
val pack by produceState<BraceBlueprintIconPack?>(null, context) {
    value = withContext(Dispatchers.IO) { BraceBlueprintIconPack.load(context) }
}
pack?.let { icons ->
    BraceBlueprintIcon(icons, BraceBlueprintIconNames.Search, contentDescription = null)
    BraceBlueprintIconByName(icons, iconNameFromData, contentDescription = "Selected icon",
        resolution = BraceBlueprintIconResolution.Px16)
    val matchingNames = icons.search("map", limit = 20)
}""".trimIndent(),
    "icons-next-glyph-catalog" to """val context = LocalContext.current
val pack by produceState<BraceBlueprintNextIconPack?>(null, context) {
    value = withContext(Dispatchers.IO) { BraceBlueprintNextIconPack.load(context) }
}
pack?.let { icons ->
    val iconNameFromData = "magnifying-glass"
    BraceBlueprintNextIcon(icons, BraceBlueprintNextIconNames.MagnifyingGlass,
        contentDescription = null, variant = BraceBlueprintNextIconVariant.Filled)
    BraceBlueprintNextIconByName(icons, iconNameFromData,
        contentDescription = "Selected icon")
    val migrated = icons.nextNameForLegacy("search") // magnifying-glass
} """.trimIndent(),
    "icons-icon-loading" to """val custom = remember(customVector) {
    BraceIconRegistry.Default.register("workspace-mark", customVector)
}
BraceIconRegistryProvider(custom) {
    BraceIconByName(iconNameFromData, contentDescription = "Record status", fallback = BraceIcons.Help)
}""".trimIndent(),
    "select-select" to """var selectedKey by rememberSaveable { mutableStateOf<String?>(null) }
var expanded by rememberSaveable { mutableStateOf(false) }
val options = listOf(BraceSelectOption("east", "east", "East"), BraceSelectOption("west", "west", "West"))
BraceSelect(options, selectedKey, { selectedKey = it.key }, expanded, { expanded = it }, label = "Region")""".trimIndent(),
    "select-querylist" to """val state = rememberBraceQueryListState()
val options = listOf(BraceSelectOption("east", "east", "East"), BraceSelectOption("west", "west", "West"))
val visible = state.filter(options)
Column(Modifier.braceQueryNavigation(state, visible.map { it.key },
    onActivate = { state.activeKey = it }, onDismiss = { state.query = "" })) {
    BraceTextField(state.query, { state.query = it }, label = "Filter regions")
    visible.forEach { option -> BraceButton(option.label, onClick = { state.activeKey = option.key }) }
}""".trimIndent(),
    "table-table" to "BraceDataTable(rows, { it.id }, columns, selection, { selection = it })",
    "table-column" to "BraceTableColumn<Record>(\"name\", \"Name\", 140.dp, { it.name })",
    "table-viewport-rendering" to "val viewport = rememberBraceTableViewport(); BraceDataTable(rows, { it.id }, columns, selection, { selection = it }, viewport = viewport)",
    "table-fixed-headers" to "BraceDataTable(rows, { it.id }, columns, selection, { selection = it }) // row and column headers stay visible",
    "table-keyboard-navigation" to "BraceDataTable(rows, { it.id }, columns, selection, { selection = it }) // arrows/Home/End/Page; Shift extends a range",
    "table-cell-selection" to """var selection by remember { mutableStateOf<BraceTableSelection?>(null) }
BraceDataTable(rows, { it.id }, columns, selection, { selection = it })
// Tap a cell or header; Shift+arrows extend a rectangular range.""".trimIndent(),
    "table-copying" to "BraceTableClipboard.formatSelection(rows, { it.id }, columns, selection) // Ctrl/Cmd+C also copies in BraceDataTable",
    "table-column-and-row-resizing" to """var selection by remember { mutableStateOf<BraceTableSelection?>(null) }
var widths by remember { mutableStateOf<Map<String, Dp>>(emptyMap()) }
var heights by remember { mutableStateOf<Map<String, Dp>>(emptyMap()) }
BraceDataTable(rows, { it.id }, columns, selection, { selection = it },
    columnWidths = widths,
    onColumnWidthChange = { key, width -> widths = widths + (key to width) },
    rowHeights = heights,
    onRowHeightChange = { key, height -> heights = heights + (key to height) })""".trimIndent(),
    "core-css-utility-classes" to """val semantic = BraceTheme.colors.semantic
Box(Modifier.background(semantic.surface).padding(BraceTheme.spacing.md)) {
    BraceButton("Retry", onClick = ::retry, variant = BraceButtonVariant.Outline)
}""".trimIndent(),
    "core-resizesensor" to """var measured by remember { mutableStateOf(IntSize.Zero) }
Box(Modifier.onSizeChanged { measured = it }) { Text("Measured content") }
Text("Width: ${'$'}{measured.width} px")""".trimIndent(),
    "core-blueprintprovider" to """var open by rememberSaveable { mutableStateOf(false) }
BraceTheme {
    BraceShortcutRegistry(listOf(BraceShortcut("ctrl+r", "Refresh", onKeyDown = ::refresh))) {
        BraceOverlayHost(rememberBraceOverlayState()) {
            WorkspaceContent()
            BraceOverlay(open, { open = false }, title = "Details") { DetailsContent() }
        }
    }
}""".trimIndent(),
    "core-button" to "BraceTheme { BraceButton(label = \"Save\", onClick = { save() }) }",
    "core-link" to "BraceLink(\"Read guide\", BraceLinkDestination.Uri(\"https://example.org/guide\", \"Guide\"))",
    "core-anchorbutton" to "BraceLinkButton(\"Open reports\", BraceLinkDestination.Action(\"Reports\") { navigateToReports() })",
    "core-checkbox" to "BraceCheckbox(checked = checked, onCheckedChange = { checked = it }, label = \"Include archived\")",
    "core-switch" to "BraceSwitch(checked = enabled, onCheckedChange = { enabled = it }, label = \"Notifications\")",
    "core-radio" to "BraceRadio(selected = meal == \"soup\", onSelect = { meal = \"soup\" }, label = \"Soup\")",
    "core-radiogroup" to "BraceRadioGroup(options = listOf(BraceRadioOption(\"soup\", \"Soup\"), BraceRadioOption(\"salad\", \"Salad\")), selectedValue = meal, onValueChange = { meal = it }, label = \"Lunch special\")",
    "core-segmentedcontrol" to "BraceSegmentedControl(options = listOf(BraceSegmentedOption(\"list\", \"List\"), BraceSegmentedOption(\"grid\", \"Grid\")), value = layout, onValueChange = { layout = it }, label = \"Layout\")",
    "core-inputgroup" to "BraceTextField(value = query, onValueChange = { query = it }, label = \"Search\")",
    "core-formgroup" to """var notes by rememberSaveable { mutableStateOf("") }
BraceFormField(label = "Case notes", helperText = "Include the event time", required = true, requiredDescription = "Required") { controlModifier ->
    BraceTextArea(notes, { notes = it }, accessibilityLabel = "Case notes", modifier = controlModifier)
}""".trimIndent(),
    "core-textarea" to """var details by rememberSaveable { mutableStateOf("") }
BraceTextArea(details, { details = it }, accessibilityLabel = "Details", minLines = 2, maxLines = 6,
    autoResize = true, intent = BraceFormIntent.Primary, size = BraceTextAreaSize.Medium)""".trimIndent(),
    "core-editabletext" to """var title by rememberSaveable { mutableStateOf("Quarterly report") }
BraceEditableText(title, { title = it }, label = "Report title", editActionLabel = "Edit report title", onConfirm = { saveTitle(it) })""".trimIndent(),
    "core-label" to """BraceFieldLabel("Export format", spokenLabel = "Export format, ${'$'}format") { controlModifier -> BraceButton(format, onClick = { format = "JSON" }, modifier = controlModifier) }""",
    "core-controlgroup" to """BraceControlGroup(fill = true, accessibilityLabel = "Report actions") { Item { controlModifier -> BraceButton("Preview", onClick = ::preview, modifier = controlModifier) }; Item(fill = false) { controlModifier -> BraceButton("Export", onClick = ::export, modifier = controlModifier) } }""",
    "core-numericinput" to "var amount by rememberSaveable { mutableStateOf(\"0.2\") }; BraceNumericField(value = amount, onValueChange = { amount = it }, label = \"Amount\", min = 0.0, max = 100.0, stepSize = 1.0, majorStepSize = 10.0, minorStepSize = 0.1)",
    "core-card" to "BraceCard(elevation = BraceCardElevation.One, onClick = { open() }) { Text(\"Open project\") }",
    "core-cardlist" to "BraceCardList(items = projects, itemKey = { it.id }, onItemClick = { open(it) }) { project -> Text(project.name) }",
    "core-divider" to "BraceDivider(orientation = BraceDividerOrientation.Horizontal)",
    "core-progressbar" to "BraceProgressBar(label = \"Uploading files\", value = progress, intent = BraceProgressIntent.Primary)",
    "core-spinner" to "BraceSpinner(label = \"Loading records\", value = progress, size = BraceSpinnerSize.Large)",
    "core-skeleton" to "BraceSkeleton(label = \"Loading report title\"); BraceSkeleton(width = 180.dp)",
    "core-section" to "BraceSection(title = \"Projects\", collapsible = true) { Text(\"Section content\") }",
    "core-sectioncard" to "BraceSectionCard { Text(\"Project settings\") }",
    "core-breadcrumbs" to "BraceBreadcrumbs(listOf(BraceBreadcrumb(\"Home\", onClick = { home() }), BraceBreadcrumb(\"Projects\")))",
    "core-navbar" to "BraceTopBar(startContent = { BraceTopBarGroup { BraceTopBarTitle(\"Reports\") } }, endContent = { BraceTopBarGroup { BraceButton(\"Edit\", onClick = ::edit) } })",
    "core-navbargroup" to "BraceTopBarGroup { BraceTopBarTitle(\"Reports\"); BraceTopBarDivider() }",
    "core-navbarheading" to "BraceTopBarGroup { BraceTopBarTitle(\"Reports\") }",
    "core-navbardivider" to "BraceTopBarGroup { BraceTopBarTitle(\"Reports\"); BraceTopBarDivider() }",
    "core-breadcrumb" to "BraceBreadcrumbItem(label = \"Home\", onClick = { home() })",
    "core-tag" to "BraceTag(label = \"Finance\", intent = BraceTagIntent.Primary, onRemove = { removeFilter() })",
    "core-compoundtag" to "BraceCompoundTag(label = \"Status\", value = \"Active\", onRemove = { clearStatus() })",
    "core-callout" to "BraceCallout(title = \"Saved\", intent = BraceCalloutIntent.Success) { Text(\"Your changes are ready.\") }",
    "core-nonidealstate" to "BraceEmptyState(title = \"No results\", description = \"Try another query.\")",
    "core-menu" to "BraceMenu { BraceMenuItem(\"Open report\", onClick = { openReport() }) }",
    "core-menuitem" to "BraceMenuItem(\"Archived\", onClick = { archived = !archived }, selected = archived)",
    "core-menudivider" to "BraceMenuDivider(title = \"Workspace\")",
    "core-overlay2" to "BraceOverlay(open = open, onDismissRequest = { open = false }, title = \"Details\") { Text(\"Details\") }",
    "core-overlay" to "BraceOverlay(open = open, onDismissRequest = { open = false }, title = \"Details\") { Text(\"Details\") }",
    "core-portal" to "BraceOverlay(open = open, onDismissRequest = { open = false }, title = \"Details\") { Text(\"Details\") }",
    "core-overlaysprovider" to "BraceOverlayHost(rememberBraceOverlayState()) { /* modal content */ }",
    "core-portalprovider" to "BraceOverlayHost(rememberBraceOverlayState()) { /* modal content */ }",
    "core-useoverlaystack" to "val overlays = rememberBraceOverlayState(); BraceOverlayHost(overlays) { /* modal content */ }",
    "core-dialog" to "BraceDialog(open = open, onDismissRequest = { open = false }, title = \"Edit project\", actions = { BraceButton(\"Save\", onClick = save) }) { Text(\"Details\") }",
    "core-dialogbody" to "BraceDialogBody { Text(\"Scrollable details\") }",
    "core-dialogfooter" to "BraceDialogActions { BraceButton(\"Save\", onClick = save) }",
    "core-alert" to "BraceAlertDialog(open = open, title = \"Delete report?\", onConfirm = delete, onCancel = cancel, confirmIntent = BraceButtonIntent.Danger)",
    "core-drawer" to "BraceDrawer(open = open, onDismissRequest = { open = false }, title = \"Filters\", position = BraceDrawerPosition.End) { Text(\"Filter options\") }",
    "core-popover" to "BracePopover(expanded = open, onDismissRequest = { open = false }, target = { BraceButton(\"Filters\", onClick = { open = true }) }, title = \"Filter options\") { Text(\"Filter options\") }",
    "core-popovernext" to "BracePopover(expanded = open, onDismissRequest = { open = false }, target = { BraceButton(\"Filters\", onClick = { open = true }) }, title = \"Filter options\") { Text(\"Filter options\") }",
    "core-contextmenu" to """var open by rememberSaveable { mutableStateOf(false) }; BraceContextMenu(open, { open = it }, title = "Row actions", targetIsFocusable = true, target = { targetModifier -> BraceButton("Record", onClick = {}, modifier = targetModifier) }) { dismiss -> BraceMenuItem("Copy link", onClick = { copyLink(); dismiss() }) }""",
    "core-contextmenupopover" to """val trigger = remember { FocusRequester() }; var wasOpen by remember { mutableStateOf(false) }; LaunchedEffect(open) { if (open) wasOpen = true else if (wasOpen) { trigger.requestFocus(); wasOpen = false } }; BraceButton("More actions", onClick = { open = true }, modifier = Modifier.focusRequester(trigger)); BraceContextMenuPopup(expanded = open, onDismissRequest = { open = false }, targetOffset = IntOffset(80, 220), title = "More actions") { dismiss -> BraceMenuItem("Refresh", onClick = { refresh(); dismiss() }) }""",
    "core-hotkeystarget" to """BraceShortcutScope(listOf(BraceShortcut("ctrl+e", "Export", onKeyDown = ::export))) { BraceButton("Export", onClick = ::export) }""",
    "core-hotkeysprovider" to """BraceShortcutRegistry(shortcuts = listOf(BraceShortcut("ctrl+r", "Refresh", spokenComboLabel = "Control plus R", onKeyDown = ::refresh))) { ScreenContent() }""",
    "core-usehotkeys" to """Column(Modifier.braceShortcuts(listOf(BraceShortcut("ctrl+k", "Search", onKeyDown = ::focusSearch)))) { ScreenContent() }""",
    "core-keycombotag" to """BraceShortcutLabel(combo = "Ctrl+R", spokenLabel = "Control plus R")""",
    "core-tooltip" to "BraceTooltip(text = \"Imports include archived records\", target = { BraceButton(\"Import help\", onClick = {}) })",
    "core-toast" to "val toasts = rememberBraceToastState(); Box(Modifier.fillMaxSize()) { BraceButton(\"Save\", onClick = { toasts.show(BraceToastSpec(\"Saved\", intent = BraceToastIntent.Success)) }); BraceToastHost(toasts) }",
    "core-overlaytoaster" to "val toasts = rememberBraceToastState(); Box(Modifier.fillMaxSize()) { BraceButton(\"Notify\", onClick = { toasts.show(BraceToastSpec(\"Ready\"), key = \"status\") }); BraceToastHost(toasts, position = BraceToastPosition.BottomEnd) }",
    "datetime-datepicker" to "var day by rememberSaveable { mutableStateOf<String?>(null) }; BraceDatePicker(day?.let(LocalDate::parse), { day = it?.toString() }, locale = Locale.US)",
    "datetime-dateinput" to "var day by rememberSaveable { mutableStateOf<String?>(null) }; BraceDateField(day?.let(LocalDate::parse), { day = it?.toString() }, label = \"Due date\", locale = Locale.US)",
    "datetime-daterangepicker" to "var start by rememberSaveable { mutableStateOf<String?>(null) }; var end by rememberSaveable { mutableStateOf<String?>(null) }; BraceDateRangePicker(BraceDateRange(start?.let(LocalDate::parse), end?.let(LocalDate::parse)), { start = it.start?.toString(); end = it.end?.toString() }, locale = Locale.US)",
    "datetime-daterangeinput" to "var start by rememberSaveable { mutableStateOf<String?>(null) }; var end by rememberSaveable { mutableStateOf<String?>(null) }; BraceDateRangeField(BraceDateRange(start?.let(LocalDate::parse), end?.let(LocalDate::parse)), { start = it.start?.toString(); end = it.end?.toString() }, label = \"Travel dates\", locale = Locale.US)",
    "datetime-timepicker" to """var time by rememberSaveable { mutableStateOf("23:30") }
BraceTimePicker(LocalTime.parse(time), { time = it.toString() }, locale = Locale.US,
    use24Hour = true, minTime = LocalTime.of(22, 0), maxTime = LocalTime.of(2, 0))
BraceTimeField(LocalTime.parse(time), { time = it?.toString() ?: "23:30" },
    label = "Time", locale = Locale.US)""",

)

@Composable
private fun Catalog() {
    val context = LocalContext.current
    val entries = remember {
        val json = JSONObject(context.assets.open("coverage.json").bufferedReader().use { it.readText() })
        val rows = json.getJSONArray("entries")
        List(rows.length()) { index ->
            val row = rows.getJSONObject(index)
            CatalogEntry(
                id = row.getString("id"),
                name = row.getString("blueprintName"),
                family = row.getString("family"),
                status = row.getString("status"),
                api = row.getString("braceApi"),
                behavior = row.getString("behavior"),
                classification = row.getString("classification"),
                reason = row.optString("reason"),
                url = row.getString("blueprintUrl"),
            )
        }
    }
    var dark by rememberSaveable { mutableStateOf(false) }
    var highContrast by rememberSaveable { mutableStateOf(false) }
    var compact by rememberSaveable { mutableStateOf(false) }
    var teal by rememberSaveable { mutableStateOf(false) }
    var reducedMotion by rememberSaveable { mutableStateOf(false) }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var activeScenario by rememberSaveable { mutableStateOf<String?>(null) }
    var appearanceOpen by rememberSaveable { mutableStateOf(false) }
    var catalogSection by rememberSaveable { mutableStateOf("Components") }
    var statusFilter by rememberSaveable { mutableStateOf("All") }
    var search by rememberSaveable { mutableStateOf("") }
    BackHandler(activeScenario != null) { activeScenario = null }
    BackHandler(selectedId != null) { selectedId = null }
    BraceTheme(
        mode = if (dark) BraceColorMode.Dark else BraceColorMode.Light,
        contrast = if (highContrast) BraceContrast.High else BraceContrast.Standard,
        density = if (compact) BraceDensity.Compact else BraceDensity.Comfortable,
        motion = if (reducedMotion) BraceMotion.Reduced else BraceMotion.Full,
        brand = if (teal) BraceBrandColors(Color(0xFF006B5B), Color.White) else null,
    ) {
        val semantic = BraceTheme.colors.semantic
        when (activeScenario) {
            "fleet" -> FleetOperationsSample(onBack = { activeScenario = null }, dark = dark,
                onToggleTheme = { dark = !dark })
            "mission" -> MissionControlSample(onBack = { activeScenario = null }, dark = dark,
                onToggleTheme = { dark = !dark })
            else -> Column(
                Modifier.fillMaxSize().background(semantic.background)
                    .statusBarsPadding().navigationBarsPadding()
                    .padding(horizontal = BraceTheme.spacing.md, vertical = BraceTheme.spacing.sm),
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text("Brace Android", color = semantic.onBackground,
                            style = BraceTheme.typography.title)
                        Text("Component catalog", color = semantic.onSurfaceMuted,
                            style = BraceTheme.typography.label)
                    }
                    BraceButton(if (appearanceOpen) "Hide settings" else "Appearance",
                        onClick = { appearanceOpen = !appearanceOpen },
                        variant = BraceButtonVariant.Outline)
                }
                if (appearanceOpen) {
                    Spacer(Modifier.height(BraceTheme.spacing.sm))
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(BraceTheme.spacing.sm)) {
                        BraceButton(if (dark) "Dark" else "Light", onClick = { dark = !dark },
                            variant = BraceButtonVariant.Outline)
                        BraceButton(if (highContrast) "High contrast" else "Standard contrast",
                            onClick = { highContrast = !highContrast },
                            variant = BraceButtonVariant.Outline)
                        BraceButton(if (compact) "Compact" else "Comfortable",
                            onClick = { compact = !compact }, variant = BraceButtonVariant.Outline)
                        BraceButton(if (teal) "Teal brand" else "Indigo brand",
                            onClick = { teal = !teal }, variant = BraceButtonVariant.Outline)
                        BraceButton(if (reducedMotion) "Reduced motion" else "Full motion",
                            onClick = { reducedMotion = !reducedMotion },
                            variant = BraceButtonVariant.Outline)
                    }
                }
                Spacer(Modifier.height(BraceTheme.spacing.md))
                val selected = entries.firstOrNull { it.id == selectedId }
                if (selected != null) {
                    Detail(selected, onBack = { selectedId = null })
                } else {
                    Row(Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(BraceTheme.spacing.sm)) {
                        listOf("Components", "App examples").forEach { section ->
                            BraceTag(section, rounded = true, minimal = true,
                                selected = catalogSection == section, intent = BraceTagIntent.Primary,
                                onClick = { catalogSection = section })
                        }
                    }
                    Spacer(Modifier.height(BraceTheme.spacing.md))
                    if (catalogSection == "App examples") {
                        LazyColumn(Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(BraceTheme.spacing.md)) {
                            item {
                                Text("Brace in a working screen", color = semantic.onSurface,
                                    style = BraceTheme.typography.subtitle)
                                Text("Two fictional operations apps. Search, filter, select, and switch theme inside each one.",
                                    color = semantic.onSurfaceMuted, style = BraceTheme.typography.body)
                            }
                            item {
                                ScenarioCard("01 / SAN JUAN", "Electric fleet",
                                    "Search 48 vehicles, filter charging states, and inspect a selected row.",
                                    onClick = { activeScenario = "fleet" })
                            }
                            item {
                                ScenarioCard("02 / ORBITAL NETWORK", "Mission control",
                                    "Review spacecraft telemetry, watch a signal alert, and move through the table.",
                                    onClick = { activeScenario = "mission" })
                            }
                        }
                    } else {
                        BraceTextField(search, { search = it }, "Search components",
                            placeholder = "Name, family, or Brace API")
                        Spacer(Modifier.height(BraceTheme.spacing.sm))
                        FlowRow(Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(BraceTheme.spacing.sm),
                            verticalArrangement = Arrangement.spacedBy(BraceTheme.spacing.sm)) {
                            listOf("All", "in progress", "planned", "experimental", "stable").forEach { status ->
                                val count = if (status == "All") entries.size
                                    else entries.count { it.status == status }
                                BraceTag("${status.replaceFirstChar { it.uppercase() }} · $count",
                                    accessibilityLabel = "$status, $count inventory rows",
                                    rounded = true, minimal = true, selected = statusFilter == status,
                                    intent = catalogStatusIntent(status),
                                    onClick = { statusFilter = status })
                            }
                        }
                        val filtered = entries.filter { entry ->
                            (statusFilter == "All" || entry.status == statusFilter) &&
                                (search.isBlank() || listOf(entry.name, entry.family, entry.api)
                                    .any { it.contains(search, ignoreCase = true) })
                        }
                        Text("${filtered.size} of ${entries.size} inventory rows",
                            color = semantic.onSurfaceMuted, style = BraceTheme.typography.label,
                            modifier = Modifier.padding(top = BraceTheme.spacing.sm))
                        val families = filtered.groupBy { it.family }.toSortedMap()
                        LazyColumn(Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(BraceTheme.spacing.sm)) {
                            if (filtered.isEmpty()) {
                                item {
                                    BraceCard(Modifier.fillMaxWidth()) {
                                        Text("No components match this search", color = semantic.onSurface,
                                            style = BraceTheme.typography.body)
                                    }
                                }
                            }
                            families.forEach { (family, rows) ->
                                item {
                                    Row(Modifier.fillMaxWidth().padding(top = BraceTheme.spacing.sm),
                                        horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(family.replaceFirstChar { it.uppercase() },
                                            color = semantic.primary, style = BraceTheme.typography.subtitle)
                                        Text(rows.size.toString(), color = semantic.onSurfaceMuted,
                                            style = BraceTheme.typography.label)
                                    }
                                }
                                items(rows, key = { it.id }) { entry ->
                                    BraceCard(Modifier.fillMaxWidth(), compact = true,
                                        onClick = { selectedId = entry.id }) {
                                        Row(Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(BraceTheme.spacing.sm),
                                            verticalAlignment = Alignment.CenterVertically) {
                                            Column(Modifier.weight(1f)) {
                                                Text(entry.name, color = semantic.onSurface,
                                                    style = BraceTheme.typography.body)
                                                Text(entry.api, color = semantic.onSurfaceMuted,
                                                    style = BraceTheme.typography.label)
                                            }
                                            CatalogStatusBadge(entry.status)
                                            BraceIcon(BraceIcons.ChevronForward, null,
                                                intent = BraceIconIntent.Primary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun catalogStatusIntent(status: String): BraceTagIntent = when (status) {
    "in progress" -> BraceTagIntent.Primary
    "experimental" -> BraceTagIntent.Warning
    "stable" -> BraceTagIntent.Success
    else -> BraceTagIntent.Default
}

@Composable
private fun CatalogStatusBadge(status: String) {
    val semantic = BraceTheme.colors.semantic
    val color = when (status) {
        "in progress" -> semantic.primary
        "experimental" -> semantic.warning
        "stable" -> semantic.success
        else -> semantic.onSurfaceMuted
    }
    Row(
        modifier = Modifier.background(color.copy(alpha = 0.12f),
                RoundedCornerShape(BraceTheme.shape.pill))
            .padding(horizontal = BraceTheme.spacing.sm, vertical = BraceTheme.spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(BraceTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(6.dp).background(color, CircleShape).clearAndSetSemantics { })
        Text(status.replaceFirstChar { it.uppercase() }, color = color,
            style = BraceTheme.typography.label)
    }
}

@Composable
private fun ScenarioCard(eyebrow: String, title: String, description: String, onClick: () -> Unit) {
    val semantic = BraceTheme.colors.semantic
    BraceCard(Modifier.fillMaxWidth(), onClick = onClick) {
        Text(eyebrow, color = semantic.primary, style = BraceTheme.typography.label)
        Text(title, color = semantic.onSurface, style = BraceTheme.typography.subtitle)
        Spacer(Modifier.height(BraceTheme.spacing.xs))
        Text(description, color = semantic.onSurfaceMuted, style = BraceTheme.typography.body)
        Spacer(Modifier.height(BraceTheme.spacing.sm))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            BraceTag("Runnable sample", minimal = true, intent = BraceTagIntent.Success)
            BraceIcon(BraceIcons.ChevronForward, null, intent = BraceIconIntent.Primary)
        }
    }
}

@Composable
internal fun Detail(entry: CatalogEntry, onBack: () -> Unit) {
    val semantic = BraceTheme.colors.semantic
    val clipboard = LocalClipboardManager.current
    val toasts = remember(entry.id) { BraceToastState() }
    var toastPosition by rememberSaveable(entry.id) { mutableStateOf(BraceToastPosition.BottomEnd) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val hasLiveSample = entry.id in usageExamples
    val showNextIconJump = entry.id == "icons-next-glyph-catalog" && hasLiveSample
    // Back, title, status, jump, behavior, optional reason, source, and sample heading.
    val liveSampleIndex = 7 + (if (entry.reason.isNotBlank()) 1 else 0)
    Box(Modifier.fillMaxSize()) {
      LazyColumn(Modifier.fillMaxSize(), state = listState, verticalArrangement = Arrangement.spacedBy(BraceTheme.spacing.md)) {
        item { BraceButton("← All components", onClick = onBack, variant = BraceButtonVariant.Outline) }
        item { Text(entry.name, color = semantic.onSurface, style = BraceTheme.typography.title) }
        item { Text("${entry.status} · ${entry.classification} · ${entry.family}", color = semantic.onSurfaceMuted, style = BraceTheme.typography.label) }
        if (showNextIconJump) {
            item {
                BraceButton("Jump to live icon sample", onClick = {
                    scope.launch { listState.scrollToItem(liveSampleIndex) }
                }, variant = BraceButtonVariant.Outline)
            }
        }
        item { Text(entry.behavior, color = semantic.onSurface, style = BraceTheme.typography.body) }
        if (entry.reason.isNotBlank()) item { Text(entry.reason, color = semantic.onSurfaceMuted, style = BraceTheme.typography.body) }
        item { Text("Blueprint source: ${entry.url}", color = semantic.onSurfaceMuted, style = BraceTheme.typography.label) }
        if (hasLiveSample) {
            item { Text("Interactive states", color = semantic.onSurface, style = BraceTheme.typography.subtitle) }
            item {
                ComponentSample(entry.id, toasts, toastPosition) { toastPosition = it }
            }
            item { Text("Compose usage", color = semantic.onSurface, style = BraceTheme.typography.subtitle) }
            item { Text(usageExamples.getValue(entry.id), color = semantic.onSurface, style = BraceTheme.typography.body) }
            item {
                BraceButton("Copy usage", onClick = { clipboard.setText(AnnotatedString(usageExamples.getValue(entry.id))) },
                    intent = BraceButtonIntent.Secondary)
            }
        } else {
            item { Text("This component is on the roadmap. No Android API is available yet.", color = semantic.onSurfaceMuted, style = BraceTheme.typography.body) }
        }
      }
      if (entry.id == "core-toast" || entry.id == "core-overlaytoaster") {
          BraceToastHost(state = toasts, position = toastPosition)
      }
    }
}

@Composable
private fun LinkSample() {
            var destination by rememberSaveable { mutableStateOf("No navigation yet") }
            var underline by rememberSaveable { mutableStateOf(BraceLinkUnderline.Always) }
            var color by rememberSaveable { mutableStateOf(BraceLinkColor.Primary) }
            var enabled by rememberSaveable { mutableStateOf(true) }
            Column(verticalArrangement = Arrangement.spacedBy(BraceTheme.spacing.sm)) {
                Text("Destination: $destination", color = BraceTheme.colors.semantic.onSurface)
                BraceLink("Read the guide", BraceLinkDestination.Uri("https://example.org/guide", "Guide"),
                    enabled = enabled, underline = underline, color = color,
                    onOpenUri = { destination = it })
                BraceLink("Open reports", BraceLinkDestination.Action("Reports") { destination = "Reports" },
                    enabled = enabled, underline = underline, color = color)
                BraceButton(if (underline == BraceLinkUnderline.Always) "Underline on focus or hover" else "Always underline",
                    onClick = { underline = if (underline == BraceLinkUnderline.Always) BraceLinkUnderline.Hover else BraceLinkUnderline.Always },
                    variant = BraceButtonVariant.Outline)
                BraceButton(if (color == BraceLinkColor.Primary) "Success color" else "Primary color",
                    onClick = { color = if (color == BraceLinkColor.Primary) BraceLinkColor.Success else BraceLinkColor.Primary },
                    variant = BraceButtonVariant.Outline)
                BraceButton(if (enabled) "Disable links" else "Enable links", onClick = { enabled = !enabled },
                    variant = BraceButtonVariant.Outline)
            }
    }

@Composable
private fun AnchorButtonSample() {
            var destination by rememberSaveable { mutableStateOf("No navigation yet") }
            var outlined by rememberSaveable { mutableStateOf(false) }
            var enabled by rememberSaveable { mutableStateOf(true) }
            var loading by rememberSaveable { mutableStateOf(false) }
            Column(verticalArrangement = Arrangement.spacedBy(BraceTheme.spacing.sm)) {
                Text("Destination: $destination", color = BraceTheme.colors.semantic.onSurface)
                BraceLinkButton("Open reports", BraceLinkDestination.Action("Reports") { destination = "Reports" },
                    enabled = enabled, loading = loading,
                    variant = if (outlined) BraceButtonVariant.Outline else BraceButtonVariant.Solid)
                BraceLinkButton("Visit guide", BraceLinkDestination.Uri("https://example.org/guide", "Guide"),
                    enabled = enabled, loading = loading, onOpenUri = { destination = it },
                    intent = BraceButtonIntent.Secondary)
                BraceButton(if (outlined) "Solid buttons" else "Outlined buttons", onClick = { outlined = !outlined },
                    variant = BraceButtonVariant.Outline)
                BraceButton(if (enabled) "Disable navigation" else "Enable navigation", onClick = { enabled = !enabled },
                    variant = BraceButtonVariant.Outline)
                BraceButton(if (loading) "Stop loading" else "Show loading", onClick = { loading = !loading },
                    variant = BraceButtonVariant.Outline)
            }
    }
@Composable
private fun BlueprintNextGlyphSample() {
    val context = LocalContext.current
    val pack by produceState<BraceBlueprintNextIconPack?>(null, context) {
        value = withContext(Dispatchers.IO) { BraceBlueprintNextIconPack.load(context) }
    }
    val loadedPack = pack ?: run {
        Text("Loading licensed next icon artwork", color = BraceTheme.colors.semantic.onSurfaceMuted)
        return
    }
    var query by rememberSaveable { mutableStateOf("magnifying") }
    var chosen by rememberSaveable { mutableStateOf("magnifying-glass") }
    var filled by rememberSaveable { mutableStateOf(false) }
    var actions by rememberSaveable { mutableStateOf(0) }
    val actionRegistry = remember(loadedPack) {
        BraceIconRegistry.empty().register(BraceBlueprintNextIconNames.MagnifyingGlass,
            loadedPack.find(BraceBlueprintNextIconNames.MagnifyingGlass)!!)
    }
    val metadata = loadedPack.metadata(chosen)
    Column(verticalArrangement = Arrangement.spacedBy(BraceTheme.spacing.sm)) {
        Text("Opt-in /next artwork · ${loadedPack.size} outlined · ${loadedPack.filledCount} filled · Apache-2.0",
            color = BraceTheme.colors.semantic.onSurfaceMuted)
        BraceTextField(query, { query = it }, "Search next glyph names and tags")
        Row(horizontalArrangement = Arrangement.spacedBy(BraceTheme.spacing.sm)) {
            BraceBlueprintNextIconByName(loadedPack, chosen,
                if (metadata != null) "$chosen icon" else "Unknown icon, help shown",
                size = BraceIconSize.Large,
                variant = if (filled) BraceBlueprintNextIconVariant.Filled else
                    BraceBlueprintNextIconVariant.Outlined,
                intent = BraceIconIntent.Primary)
            Text(if (metadata == null) "Unknown: $chosen" else
                "$chosen · ${if (metadata.hasFilled) "filled available" else "outline only"}",
                color = BraceTheme.colors.semantic.onSurface)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(BraceTheme.spacing.sm)) {
            BraceBlueprintNextIcon(loadedPack, BraceBlueprintNextIconNames.ChevronRight, null,
                mirrorInRtl = true, intent = BraceIconIntent.Primary)
            Text("Directional artwork mirrors in RTL", color = BraceTheme.colors.semantic.onSurface)
        }
        BraceIconButton(BraceBlueprintNextIconNames.MagnifyingGlass,
            "Search with next icon", onClick = { actions++ }, registry = actionRegistry)
        Text("Icon action activated $actions times", color = BraceTheme.colors.semantic.onSurfaceMuted)
        Text("Legacy search → ${loadedPack.nextNameForLegacy("search")?.value}",
            color = BraceTheme.colors.semantic.onSurfaceMuted)
        Column(verticalArrangement = Arrangement.spacedBy(BraceTheme.spacing.sm)) {
            BraceButton(if (filled) "Outlined artwork" else "Filled artwork",
                onClick = { filled = !filled }, variant = BraceButtonVariant.Outline)
            BraceButton("Try fallback", onClick = { chosen = "not-in-pack" },
                variant = BraceButtonVariant.Outline)
        }
        loadedPack.search(query, limit = 8).forEach { glyph ->
            BraceButton("${glyph.name}${if (glyph.hasFilled) " · filled" else ""}",
                onClick = { chosen = glyph.name }, variant = BraceButtonVariant.Outline)
        }
    }
}

@Composable
private fun DateRangePickerSample() {
    var start by rememberSaveable { mutableStateOf<String?>(null) }
    var end by rememberSaveable { mutableStateOf<String?>(null) }
    var allowSingle by rememberSaveable { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(BraceTheme.spacing.sm)) {
        BraceButton(if (allowSingle) "Single day allowed" else "Require two days",
            onClick = { allowSingle = !allowSingle }, variant = BraceButtonVariant.Outline)
        BraceDateRangePicker(
            value = BraceDateRange(start?.let(LocalDate::parse), end?.let(LocalDate::parse)),
            onValueChange = { start = it.start?.toString(); end = it.end?.toString() },
            locale = Locale.US, allowSingleDayRange = allowSingle,
            minDate = LocalDate.of(2026, 1, 1), maxDate = LocalDate.of(2027, 12, 31),
            shortcuts = listOf(BraceDateRangeShortcut("Sept 14–18",
                BraceDateRange(LocalDate.of(2026, 9, 14), LocalDate.of(2026, 9, 18)))),
        )
        Text("Selected: ${start ?: "none"} → ${end ?: "none"}",
            color = BraceTheme.colors.semantic.onSurface)
    }
}

@Composable
private fun DateRangeFieldSample() {
    var start by rememberSaveable { mutableStateOf<String?>(null) }
    var end by rememberSaveable { mutableStateOf<String?>(null) }
    var errors by rememberSaveable { mutableStateOf(0) }
    Column(verticalArrangement = Arrangement.spacedBy(BraceTheme.spacing.sm)) {
        BraceDateRangeField(
            value = BraceDateRange(start?.let(LocalDate::parse), end?.let(LocalDate::parse)),
            onValueChange = { start = it.start?.toString(); end = it.end?.toString() },
            label = "Travel dates", locale = Locale.US,
            minDate = LocalDate.of(2026, 1, 1), maxDate = LocalDate.of(2027, 12, 31),
            onInvalidInput = { _, _ -> errors++ },
        )
        Text("Selected: ${start ?: "none"} → ${end ?: "none"} · invalid drafts: $errors",
            color = BraceTheme.colors.semantic.onSurface)
        BraceDateRangeField(BraceDateRange(), {}, "Unavailable range", enabled = false,
            locale = Locale.US)
    }
}

@Composable
private fun TimePickerSample() {
    var selected by rememberSaveable { mutableStateOf("23:30") }
    var fieldTime by rememberSaveable { mutableStateOf<String?>(null) }
    var errors by rememberSaveable { mutableStateOf(0) }
    Column(verticalArrangement = Arrangement.spacedBy(BraceTheme.spacing.sm)) {
        Text("Overnight window · 22:00–02:00", color = BraceTheme.colors.semantic.onSurfaceMuted,
            style = BraceTheme.typography.label)
        BraceTimePicker(LocalTime.parse(selected), { selected = it.toString() },
            locale = Locale.US, use24Hour = true,
            minTime = LocalTime.of(22, 0), maxTime = LocalTime.of(2, 0))
        Text("Selected: $selected", color = BraceTheme.colors.semantic.onSurface,
            style = BraceTheme.typography.body)
        BraceTimeField(fieldTime?.let(LocalTime::parse), { fieldTime = it?.toString() },
            label = "Meeting time", locale = Locale.US,
            precision = BraceTimePrecision.Second,
            supportingText = "Enter a time or open the picker",
            onInvalidInput = { errors++ })
        Text("Field: ${fieldTime ?: "none"} · invalid entries: $errors",
            color = BraceTheme.colors.semantic.onSurface,
            style = BraceTheme.typography.body)
        BraceTimeField(null, {}, label = "Unavailable time", enabled = false,
            locale = Locale.US)
    }
}

@Composable
private fun ComponentSample(
    id: String,
    toasts: BraceToastState,
    toastPosition: BraceToastPosition,
    onToastPositionChange: (BraceToastPosition) -> Unit,
) {
    when (id) {
        "core-css-utility-classes" -> {
            var outlined by rememberSaveable { mutableStateOf(false) }
            val semantic = BraceTheme.colors.semantic
            Column(verticalArrangement = Arrangement.spacedBy(BraceTheme.spacing.sm)) {
                Text("Typed parameters and scoped tokens replace CSS class names.",
                    color = semantic.onSurfaceMuted, style = BraceTheme.typography.body)
                Box(Modifier.background(semantic.surface).padding(BraceTheme.spacing.md)) {
                    Column(verticalArrangement = Arrangement.spacedBy(BraceTheme.spacing.sm)) {
                        Text("Token-styled content", color = semantic.onSurface,
                            style = BraceTheme.typography.subtitle)
                        BraceButton(
                            if (outlined) "Outlined action" else "Solid action",
                            onClick = { outlined = !outlined },
                            variant = if (outlined) BraceButtonVariant.Outline else BraceButtonVariant.Solid,
                        )
                    }
                }
                Text("Try the app-wide theme, contrast, brand, and density controls above.",
                    color = semantic.onSurfaceMuted, style = BraceTheme.typography.label)
            }
        }
        "core-resizesensor" -> {
            var wide by rememberSaveable { mutableStateOf(false) }
            var measured by remember { mutableStateOf(IntSize.Zero) }
            val semantic = BraceTheme.colors.semantic
            Column(verticalArrangement = Arrangement.spacedBy(BraceTheme.spacing.sm)) {
                BraceButton(if (wide) "Narrow preview" else "Widen preview",
                    onClick = { wide = !wide }, variant = BraceButtonVariant.Outline)
                Box(
                    Modifier
                        .width(if (wide) 224.dp else 144.dp)
                        .onSizeChanged { measured = it }
                        .background(semantic.surface)
                        .padding(BraceTheme.spacing.md),
                ) {
                    Text("Measured content", color = semantic.onSurface,
                        style = BraceTheme.typography.body)
                }
                Text("Measured: ${measured.width} × ${measured.height} px",
                    color = semantic.onSurfaceMuted, style = BraceTheme.typography.label)
            }
        }
        "core-blueprintprovider" -> {
            var localHighContrast by rememberSaveable { mutableStateOf(false) }
            var detailsOpen by rememberSaveable { mutableStateOf(false) }
            var refreshed by rememberSaveable { mutableStateOf(0) }
            val shortcutState = rememberBraceShortcutRegistryState()
            val overlayState = rememberBraceOverlayState()
            BraceShortcutRegistry(
                shortcuts = listOf(
                    BraceShortcut("ctrl+r", "Refresh local preview", group = "Preview",
                        onKeyDown = { refreshed++ }),
                ),
                state = shortcutState,
                discoveryTitle = "Preview shortcuts",
            ) {
                BraceOverlayHost(overlayState) {
                    BraceTheme(
                        contrast = if (localHighContrast) BraceContrast.High else BraceContrast.Standard,
                    ) {
                        val semantic = BraceTheme.colors.semantic
                        Column(verticalArrangement = Arrangement.spacedBy(BraceTheme.spacing.sm)) {
                            Text("This preview has its own theme and screen behavior scope.",
                                color = semantic.onSurface, style = BraceTheme.typography.body)
                            BraceButton(
                                if (localHighContrast) "Standard local contrast" else "High local contrast",
                                onClick = { localHighContrast = !localHighContrast },
                                variant = BraceButtonVariant.Outline,
                            )
                            BraceButton("Open scoped overlay", onClick = { detailsOpen = true })
                            BraceButton("Show shortcut guide",
                                onClick = shortcutState::showDiscovery,
                                variant = BraceButtonVariant.Outline)
                            Text("Ctrl+R refreshes while a preview control has focus · $refreshed",
                                color = semantic.onSurfaceMuted, style = BraceTheme.typography.label)
                        }
                        BraceOverlay(
                            open = detailsOpen,
                            onDismissRequest = { detailsOpen = false },
                            title = "Scoped details",
                        ) {
                            Column(Modifier.padding(BraceTheme.spacing.md)) {
                                Text("Scoped details", color = BraceTheme.colors.semantic.onSurface,
                                    style = BraceTheme.typography.subtitle)
                                BraceButton("Close", onClick = { detailsOpen = false })
                            }
                        }
                    }
                }
            }
        }
        "core-icon" -> {
            var large by rememberSaveable { mutableStateOf(false) }
            var danger by rememberSaveable { mutableStateOf(false) }
            var activations by rememberSaveable { mutableStateOf(0) }
            Column(verticalArrangement = Arrangement.spacedBy(BraceTheme.spacing.sm)) {
                Text("The symbols beside labels are decorative; the icon action has its own spoken label.",
                    color = BraceTheme.colors.semantic.onSurfaceMuted)
                Row(horizontalArrangement = Arrangement.spacedBy(BraceTheme.spacing.md)) {
                    BraceIcon(BraceIcons.Search, null,
                        size = if (large) BraceIconSize.Large else BraceIconSize.Small,
                        intent = if (danger) BraceIconIntent.Danger else BraceIconIntent.Primary)
                    Text("Search", color = BraceTheme.colors.semantic.onSurface)
                    BraceIcon(BraceIcons.Warning, null, intent = BraceIconIntent.Warning)
                    Text("Warning", color = BraceTheme.colors.semantic.onSurface)
                }
                BraceIconButton(BraceIcons.Check, "Confirm icon action",
                    onClick = { activations++ })
                BraceIconButton(BraceIcons.Close, "Unavailable icon action",
                    onClick = {}, enabled = false)
                Text("Activated $activations times", color = BraceTheme.colors.semantic.onSurfaceMuted)
                Row(horizontalArrangement = Arrangement.spacedBy(BraceTheme.spacing.sm)) {
                    BraceButton(if (large) "Small icons" else "Large icons",
                        onClick = { large = !large }, variant = BraceButtonVariant.Outline)
                    BraceButton(if (danger) "Primary" else "Danger",
                        onClick = { danger = !danger }, variant = BraceButtonVariant.Outline)
                }
            }
        }
        "icons-icon-glyph-catalog" -> {
            val context = LocalContext.current
            val pack by produceState<BraceBlueprintIconPack?>(null, context) {
                value = withContext(Dispatchers.IO) { BraceBlueprintIconPack.load(context) }
            }
            val loadedPack = pack ?: run {
                Text("Loading licensed icon artwork", color = BraceTheme.colors.semantic.onSurfaceMuted)
                return
            }
            var query by rememberSaveable { mutableStateOf("map") }
            var chosen by rememberSaveable { mutableStateOf("map") }
            var use16 by rememberSaveable { mutableStateOf(false) }
            var activations by rememberSaveable { mutableStateOf(0) }
            val actionRegistry = remember(loadedPack) {
                BraceIconRegistry.empty().register(BraceBlueprintIconNames.Search,
                    loadedPack.find(BraceBlueprintIconNames.Search)!!)
            }
            val available = loadedPack.find(chosen) != null
            Column(verticalArrangement = Arrangement.spacedBy(BraceTheme.spacing.sm)) {
                Text("Opt-in Blueprint artwork · ${loadedPack.size} pinned names · Apache-2.0",
                    color = BraceTheme.colors.semantic.onSurfaceMuted)
                BraceTextField(query, { query = it }, "Search glyph names and tags")
                Row(horizontalArrangement = Arrangement.spacedBy(BraceTheme.spacing.sm)) {
                    BraceBlueprintIconByName(loadedPack, chosen,
                        if (available) "${loadedPack.metadata(chosen)?.displayName} icon" else "Unknown icon, help shown",
                        size = BraceIconSize.Large,
                        resolution = if (use16) BraceBlueprintIconResolution.Px16
                            else BraceBlueprintIconResolution.Px20,
                        intent = BraceIconIntent.Primary)
                    Text(if (available) chosen else "Unknown: $chosen", color = BraceTheme.colors.semantic.onSurface)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(BraceTheme.spacing.sm)) {
                    BraceBlueprintIcon(loadedPack, BraceBlueprintIconNames.ChevronRight, null,
                        mirrorInRtl = true, intent = BraceIconIntent.Primary)
                    Text("Directional artwork mirrors in RTL", color = BraceTheme.colors.semantic.onSurface)
                }
                BraceIconButton(BraceBlueprintIconNames.Search, "Search with Blueprint icon",
                    onClick = { activations++ }, registry = actionRegistry)
                Text("Icon action activated $activations times",
                    color = BraceTheme.colors.semantic.onSurfaceMuted)
                Column(verticalArrangement = Arrangement.spacedBy(BraceTheme.spacing.sm)) {
                    BraceButton(if (use16) "Use 20px artwork" else "Use 16px artwork",
                        onClick = { use16 = !use16 }, variant = BraceButtonVariant.Outline)
                    BraceButton("Try fallback", onClick = { chosen = "not-in-pack" },
                        variant = BraceButtonVariant.Outline)
                }
                loadedPack.search(query, limit = 8).forEach { glyph ->
                    BraceButton("${glyph.displayName} · ${glyph.name}", onClick = { chosen = glyph.name },
                        variant = BraceButtonVariant.Outline)
                }
            }
        }
        "icons-next-glyph-catalog" -> BlueprintNextGlyphSample()
        "icons-icon-loading" -> {
            var iconName by rememberSaveable { mutableStateOf("search") }
            val registry = remember {
                BraceIconRegistry.Default.register("custom-check",
                    BraceIconRegistry.Default.resolve("check"))
            }
            Column(verticalArrangement = Arrangement.spacedBy(BraceTheme.spacing.sm)) {
                BraceTextField(iconName, { iconName = it }, "Runtime icon name",
                    placeholder = "Try custom-check or an unknown name")
                BraceIconRegistryProvider(registry) {
                    Row(horizontalArrangement = Arrangement.spacedBy(BraceTheme.spacing.sm)) {
                        BraceIconByName(iconName,
                            contentDescription = if (registry.find(iconName) == null)
                                "Unknown icon; showing help" else "Preview: $iconName",
                            size = BraceIconSize.Large)
                        Text(if (registry.find(iconName) == null) "Fallback: help" else "Found: $iconName",
                            color = BraceTheme.colors.semantic.onSurface)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(BraceTheme.spacing.sm)) {
                    BraceButton("Custom", onClick = { iconName = "custom-check" },
                        variant = BraceButtonVariant.Outline)
                    BraceButton("Fallback", onClick = { iconName = "missing" },
                        variant = BraceButtonVariant.Outline)
                }
                Text("Bundled: ${BraceIconRegistry.Default.names.sorted().joinToString()}",
                    color = BraceTheme.colors.semantic.onSurfaceMuted)
            }
        }
        "select-select", "select-querylist" -> {
            var selectedKey by rememberSaveable { mutableStateOf<String?>(null) }
            var expanded by rememberSaveable { mutableStateOf(false) }
            val state = rememberBraceQueryListState()
            val choices = listOf(
                BraceSelectOption("east", "east", "East", description = "Eastern region"),
                BraceSelectOption("west", "west", "West", description = "Western region"),
                BraceSelectOption("central", "central", "Central", enabled = false),
            )
            val queryKeys = state.filter(choices).filter { it.enabled }.map { it.key }
            Column(
                modifier = if (id == "select-querylist") Modifier.braceQueryNavigation(state, queryKeys,
                    onActivate = { selectedKey = it; state.activeKey = it },
                    onDismiss = { state.query = "" }) else Modifier,
                verticalArrangement = Arrangement.spacedBy(BraceTheme.spacing.sm),
            ) {
                if (id == "select-select") {
                    BraceSelect(choices, selectedKey, { selectedKey = it.key }, expanded,
                        { expanded = it }, label = "Region", state = state)
                    BraceSelect(choices, "west", {}, false, {}, label = "Unavailable", enabled = false)
                } else {
                    BraceTextField(state.query, { state.query = it }, label = "Filter regions")
                    state.filter(choices).forEach { option ->
                        BraceButton(option.label, onClick = { state.activeKey = option.key; selectedKey = option.key },
                            enabled = option.enabled, variant = BraceButtonVariant.Outline)
                    }
                }
                Text("Selected: ${selectedKey ?: "none"} · Query: ${state.query}",
                    color = BraceTheme.colors.semantic.onSurfaceMuted)
                BraceButton("Clear", onClick = { selectedKey = null; state.query = "" },
                    variant = BraceButtonVariant.Outline)
            }
        }
        "datetime-datepicker" -> {
            var selected by rememberSaveable { mutableStateOf<String?>("2026-09-18") }
            val start = LocalDate.of(2026, 9, 1)
            val end = LocalDate.of(2026, 10, 31)
            Column(verticalArrangement = Arrangement.spacedBy(BraceTheme.spacing.sm)) {
                Text("Weekends unavailable · September–October 2026",
                    color = BraceTheme.colors.semantic.onSurfaceMuted,
                    style = BraceTheme.typography.label)
                BraceDatePicker(
                    value = selected?.let(LocalDate::parse),
                    onValueChange = { selected = it?.toString() },
                    locale = Locale.US,
                    minDate = start,
                    maxDate = end,
                    isDateEnabled = { it.dayOfWeek.value <= 5 },
                    shortcuts = listOf(BraceDateShortcut("End of month", LocalDate.of(2026, 9, 30))),
                    initialMonth = YearMonth.of(2026, 9),
                )
                Text("Selected: ${selected ?: "none"}",
                    color = BraceTheme.colors.semantic.onSurface,
                    style = BraceTheme.typography.body)
            }
        }
        "datetime-dateinput" -> {
            var selected by rememberSaveable { mutableStateOf<String?>(null) }
            var errors by rememberSaveable { mutableStateOf(0) }
            Column(verticalArrangement = Arrangement.spacedBy(BraceTheme.spacing.sm)) {
                BraceDateField(
                    value = selected?.let(LocalDate::parse),
                    onValueChange = { selected = it?.toString() },
                    label = "Due date",
                    locale = Locale.US,
                    minDate = LocalDate.of(2026, 1, 1),
                    maxDate = LocalDate.of(2027, 12, 31),
                    supportingText = "Use your locale's short date format",
                    onInvalidInput = { errors++ },
                )
                Text("Selected: ${selected ?: "none"} · invalid entries: $errors",
                    color = BraceTheme.colors.semantic.onSurface,
                    style = BraceTheme.typography.body)
                BraceDateField(null, {}, label = "Unavailable date", enabled = false,
                    locale = Locale.US)
            }
        }
        "datetime-daterangepicker" -> DateRangePickerSample()
        "datetime-daterangeinput" -> DateRangeFieldSample()
        "datetime-timepicker" -> TimePickerSample()
        "table-table", "table-column", "table-viewport-rendering", "table-fixed-headers", "table-keyboard-navigation",
        "table-cell-selection", "table-column-and-row-resizing", "table-copying" -> TableCatalogSample()
        "core-button" -> {
            var count by rememberSaveable { mutableStateOf(0) }
            Column(verticalArrangement = Arrangement.spacedBy(BraceTheme.spacing.sm)) {
                BraceButton("Save ($count)", onClick = { count++ })
                BraceButton("Delete", onClick = { count = 0 }, intent = BraceButtonIntent.Danger)
                BraceButton("Secondary", onClick = { count++ }, intent = BraceButtonIntent.Secondary)
                BraceButton("Outlined", onClick = { count++ }, variant = BraceButtonVariant.Outline)
                BraceButton("Unavailable", onClick = {}, enabled = false)
                BraceButton("Loading", onClick = {}, loading = true)
            }
        }
        "core-navbar", "core-navbargroup", "core-navbarheading", "core-navbardivider" -> {
            var edited by rememberSaveable { mutableStateOf(false) }
            Column(verticalArrangement = Arrangement.spacedBy(BraceTheme.spacing.md)) {
                BraceTopBar(
                    startContent = {
                        BraceTopBarGroup {
                            BraceTopBarTitle(if (edited) "Edited report" else "Quarterly report")
                            BraceTopBarDivider()
                        }
                    },
                    endContent = {
                        BraceTopBarGroup {
                            BraceButton(if (edited) "Done" else "Edit", onClick = { edited = !edited })
                        }
                    },
                )
                BraceTopBar(
                    startContent = { BraceTopBarGroup { BraceTopBarTitle("Raised variant") } },
                    raised = true,
                )
            }
        }
        "core-link" -> LinkSample()
        "core-anchorbutton" -> AnchorButtonSample()
        "core-checkbox" -> {
            var checked by rememberSaveable { mutableStateOf(false) }
            Column { BraceCheckbox(checked, { checked = it }, "Include archived")
                BraceCheckbox(false, {}, "Disabled choice", enabled = false) }
        }
        "core-radio" -> {
            var meal by rememberSaveable { mutableStateOf("soup") }
            Column(verticalArrangement = Arrangement.spacedBy(BraceTheme.spacing.xs)) {
                BraceRadio(meal == "soup", { meal = "soup" }, "Soup", description = "Vegetarian")
                BraceRadio(meal == "sandwich", { meal = "sandwich" }, "Sandwich")
                BraceRadio(false, {}, "Unavailable", enabled = false)
            }
        }
        "core-radiogroup" -> {
            var meal by rememberSaveable { mutableStateOf("soup") }
            BraceRadioGroup(
                options = listOf(
                    BraceRadioOption("soup", "Soup", description = "Vegetarian"),
                    BraceRadioOption("salad", "Salad", enabled = false),
                    BraceRadioOption("sandwich", "Sandwich"),
                ),
                selectedValue = meal, onValueChange = { meal = it }, label = "Lunch special",
            )
        }
        "core-segmentedcontrol" -> {
            var layout by rememberSaveable { mutableStateOf("list") }
            Column(verticalArrangement = Arrangement.spacedBy(BraceTheme.spacing.sm)) {
                BraceSegmentedControl(
                    options = listOf(
                        BraceSegmentedOption("list", "List"),
                        BraceSegmentedOption("grid", "Grid", enabled = false),
                        BraceSegmentedOption("gallery", "Gallery"),
                    ),
                    value = layout, onValueChange = { layout = it }, label = "Layout",
                    fill = true, intent = BraceSegmentedIntent.Primary,
                )
                BraceSegmentedControl(
                    options = listOf(BraceSegmentedOption("day", "Day"), BraceSegmentedOption("week", "Week")),
                    value = "day", onValueChange = {}, label = "Small size", size = BraceSegmentedSize.Small,
                )
            }
        }
        "core-switch" -> {
            var checked by rememberSaveable { mutableStateOf(true) }
            Column { BraceSwitch(checked, { checked = it }, "Notifications")
                BraceSwitch(false, {}, "Unavailable setting", enabled = false) }
        }
        "core-inputgroup" -> {
            var value by rememberSaveable { mutableStateOf("") }
            Column(verticalArrangement = Arrangement.spacedBy(BraceTheme.spacing.md)) {
                BraceTextField(value, { value = it }, "Project name", placeholder = "Enter a name")
                BraceTextField(value, { value = it }, "Required project name", isError = value.isBlank(), supportingText = "A name is required")
                BraceTextField("Read only value", {}, "Read only", readOnly = true)
                BraceTextField("Unavailable", {}, "Disabled", enabled = false)
            }
        }
        "core-formgroup" -> {
            var notes by rememberSaveable { mutableStateOf("") }
            var showError by rememberSaveable { mutableStateOf(false) }
            var inline by rememberSaveable { mutableStateOf(false) }
            var disabled by rememberSaveable { mutableStateOf(false) }
            val error = showError && notes.isBlank()
            Column(verticalArrangement = Arrangement.spacedBy(BraceTheme.spacing.sm)) {
                Row(horizontalArrangement = Arrangement.spacedBy(BraceTheme.spacing.sm)) {
                    BraceButton("Validate", onClick = { showError = true })
                    BraceButton(if (inline) "Stack labels" else "Inline on wide screens",
                        onClick = { inline = !inline }, variant = BraceButtonVariant.Outline)
                }
                BraceButton(if (disabled) "Enable form" else "Disable form",
                    onClick = { disabled = !disabled }, variant = BraceButtonVariant.Outline)
                BraceFormField(
                    label = "Case notes",
                    helperText = "Include the event time",
                    errorText = if (error) "Case notes are required" else null,
                    required = true,
                    requiredDescription = "Required",
                    inline = inline,
                    disabled = disabled,
                ) { controlModifier ->
                    BraceTextArea(
                        value = notes,
                        onValueChange = { notes = it },
                        accessibilityLabel = "Case notes",
                        enabled = !disabled,
                        isError = error,
                        errorText = if (error) "Case notes are required" else null,
                        minLines = 2,
                        autoResize = true,
                        modifier = controlModifier,
                    )
                }
            }
        }
        "core-textarea" -> {
            var details by rememberSaveable { mutableStateOf("") }
            var autoResize by rememberSaveable { mutableStateOf(true) }
            var readOnly by rememberSaveable { mutableStateOf(false) }
            var error by rememberSaveable { mutableStateOf(false) }
            var large by rememberSaveable { mutableStateOf(false) }
            var success by rememberSaveable { mutableStateOf(false) }
            Column(verticalArrangement = Arrangement.spacedBy(BraceTheme.spacing.sm)) {
                Row(horizontalArrangement = Arrangement.spacedBy(BraceTheme.spacing.sm)) {
                    BraceButton(if (autoResize) "Fixed height" else "Auto resize",
                        onClick = { autoResize = !autoResize }, variant = BraceButtonVariant.Outline)
                    BraceButton(if (readOnly) "Editable" else "Read only",
                        onClick = { readOnly = !readOnly }, variant = BraceButtonVariant.Outline)
                }
                BraceButton(if (error) "Clear error" else "Show error",
                    onClick = { error = !error }, variant = BraceButtonVariant.Outline)
                Row(horizontalArrangement = Arrangement.spacedBy(BraceTheme.spacing.sm)) {
                    BraceButton(if (large) "Medium size" else "Large size",
                        onClick = { large = !large }, variant = BraceButtonVariant.Outline)
                    BraceButton(if (success) "Default intent" else "Success intent",
                        onClick = { success = !success }, variant = BraceButtonVariant.Outline)
                }
                BraceTextArea(
                    value = details,
                    onValueChange = { details = it },
                    accessibilityLabel = "Details",
                    placeholder = "Add details",
                    readOnly = readOnly,
                    isError = error,
                    errorText = if (error) "Review the details" else null,
                    minLines = 2,
                    maxLines = 6,
                    autoResize = autoResize,
                    size = if (large) BraceTextAreaSize.Large else BraceTextAreaSize.Medium,
                    intent = if (success) BraceFormIntent.Success else BraceFormIntent.Default,
                )
                if (error) Text("Review the details", modifier = Modifier.clearAndSetSemantics {},
                    color = BraceTheme.colors.semantic.danger, style = BraceTheme.typography.label)
                BraceTextArea("Unavailable notes", {}, "Disabled notes", enabled = false,
                    minLines = 2)
            }
        }
        "core-editabletext" -> {
            var title by rememberSaveable { mutableStateOf("Quarterly report") }
            var confirmed by rememberSaveable { mutableStateOf("No confirmed edit") }
            var multiline by rememberSaveable { mutableStateOf(false) }
            var warning by rememberSaveable { mutableStateOf(false) }
            var disabled by rememberSaveable { mutableStateOf(false) }
            Column(verticalArrangement = Arrangement.spacedBy(BraceTheme.spacing.sm)) {
                Text("Tap or focus the value to edit. Escape cancels; Enter confirms.",
                    color = BraceTheme.colors.semantic.onSurfaceMuted)
                BraceEditableText(
                    value = title,
                    onValueChange = { title = it },
                    label = "Report title",
                    editActionLabel = "Edit report title",
                    placeholder = "Add a title",
                    enabled = !disabled,
                    multiline = multiline,
                    minLines = if (multiline) 2 else 1,
                    maxLines = if (multiline) 5 else 1,
                    intent = if (warning) BraceEditableTextIntent.Warning
                        else BraceEditableTextIntent.Default,
                    supportingText = if (multiline) "Control or Command plus Enter confirms" else null,
                    onConfirm = { confirmed = "Confirmed: $it" },
                    onCancel = { confirmed = "Canceled edit" },
                )
                Text(confirmed, color = BraceTheme.colors.semantic.onSurfaceMuted)
                Row(horizontalArrangement = Arrangement.spacedBy(BraceTheme.spacing.sm)) {
                    BraceButton(if (multiline) "Single line" else "Multiline",
                        onClick = { multiline = !multiline }, variant = BraceButtonVariant.Outline)
                    BraceButton(if (warning) "Default intent" else "Warning intent",
                        onClick = { warning = !warning }, variant = BraceButtonVariant.Outline)
                }
                BraceButton(if (disabled) "Enable editing" else "Disable editing",
                    onClick = { disabled = !disabled }, variant = BraceButtonVariant.Outline)
            }
        }
        "core-label" -> {
            var format by rememberSaveable { mutableStateOf("CSV") }
            Column(verticalArrangement = Arrangement.spacedBy(BraceTheme.spacing.md)) {
                BraceFieldLabel(label = "Export format", spokenLabel = "Export format, $format") { controlModifier ->
                    BraceButton(format, onClick = {
                        format = if (format == "CSV") "JSON" else "CSV"
                    }, modifier = controlModifier)
                }
                BraceFieldLabel(label = "Unavailable format", enabled = false) { controlModifier ->
                    BraceButton("Unavailable", onClick = {}, enabled = false, modifier = controlModifier)
                }
            }
        }
        "core-controlgroup" -> {
            var vertical by rememberSaveable { mutableStateOf(false) }
            var equalFill by rememberSaveable { mutableStateOf(true) }
            var lastAction by rememberSaveable { mutableStateOf("None") }
            Column(verticalArrangement = Arrangement.spacedBy(BraceTheme.spacing.sm)) {
                BraceButton(if (vertical) "Use horizontal layout" else "Use vertical layout",
                    onClick = { vertical = !vertical }, variant = BraceButtonVariant.Outline)
                BraceButton(if (equalFill) "Use natural sizes" else "Use equal fill",
                    onClick = { equalFill = !equalFill }, variant = BraceButtonVariant.Outline)
                Box(Modifier.fillMaxWidth().then(if (vertical && equalFill) Modifier.height(220.dp) else Modifier)) {
                    BraceControlGroup(vertical = vertical, fill = equalFill,
                        accessibilityLabel = "Report actions") {
                        Item { controlModifier ->
                            BraceButton("Preview", onClick = { lastAction = "Preview" }, modifier = controlModifier)
                        }
                        Item { controlModifier ->
                            BraceButton("Share", onClick = { lastAction = "Share" }, modifier = controlModifier)
                        }
                        Item(fill = false) { controlModifier ->
                            BraceButton("Export", onClick = { lastAction = "Export" }, modifier = controlModifier)
                        }
                    }
                }
                Text("Last action: $lastAction", color = BraceTheme.colors.semantic.onSurface)
            }
        }
        "core-numericinput" -> {
            var amount by rememberSaveable { mutableStateOf("0.2") }
            var german by rememberSaveable { mutableStateOf(false) }
            var clamp by rememberSaveable { mutableStateOf(false) }
            var atStart by rememberSaveable { mutableStateOf(false) }
            var lastStep by rememberSaveable { mutableStateOf("No button step yet") }
            Column(verticalArrangement = Arrangement.spacedBy(BraceTheme.spacing.sm)) {
                Text("Type a partial draft, use ↑/↓, Shift/Alt + arrow, or the 48dp step buttons.",
                    color = BraceTheme.colors.semantic.onSurfaceMuted)
                BraceNumericField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = "Amount",
                    min = 0.0,
                    max = 20.0,
                    locale = if (german) Locale.GERMANY else Locale.US,
                    clampValueOnBlur = clamp,
                    buttonPosition = if (atStart) BraceNumericButtonPosition.Start else BraceNumericButtonPosition.End,
                    intent = BraceNumericIntent.Primary,
                    size = BraceNumericFieldSize.Medium,
                    supportingText = "Normal ±1 · Shift ±10 · Alt ±0.1",
                    onButtonClick = { lastStep = "Button selected $it" },
                )
                Text("Draft: $amount · $lastStep", color = BraceTheme.colors.semantic.onSurface)
                Row(horizontalArrangement = Arrangement.spacedBy(BraceTheme.spacing.sm)) {
                    BraceButton(if (german) "English decimal" else "German decimal", onClick = {
                        german = !german
                        amount = if (german) "0,2" else "0.2"
                    }, intent = BraceButtonIntent.Secondary)
                    BraceButton(if (clamp) "Clamp off" else "Clamp on", onClick = { clamp = !clamp },
                        intent = BraceButtonIntent.Secondary)
                }
                BraceButton(if (atStart) "Buttons at end" else "Buttons at start", onClick = { atStart = !atStart },
                    variant = BraceButtonVariant.Outline)
                BraceNumericField("4", {}, label = "Read only", readOnly = true)
                BraceNumericField("", {}, label = "Disabled", enabled = false)
            }
        }
        "core-card" -> {
            var selected by rememberSaveable { mutableStateOf(false) }
            Column(verticalArrangement = Arrangement.spacedBy(BraceTheme.spacing.sm)) {
                BraceCard(elevation = BraceCardElevation.One, onClick = { selected = !selected }, selected = selected) {
                    Text("Project overview", color = BraceTheme.colors.semantic.onSurface, style = BraceTheme.typography.subtitle)
                    Text("Tap, click, or press Enter to select", color = BraceTheme.colors.semantic.onSurfaceMuted)
                }
                BraceCard(enabled = false, onClick = {}) { Text("Unavailable card", color = BraceTheme.colors.semantic.disabledContent) }
            }
        }
        "core-cardlist" -> {
            var selected by rememberSaveable { mutableStateOf("Analysis") }
            BraceCardList(
                items = listOf("Analysis", "Operations", "Reports"),
                itemKey = { it },
                isSelected = { it == selected },
                onItemClick = { selected = it },
            ) { name -> Text(name, color = if (name == selected) BraceTheme.colors.semantic.onSelection else BraceTheme.colors.semantic.onSurface) }
        }
        "core-divider" -> {
            Column(verticalArrangement = Arrangement.spacedBy(BraceTheme.spacing.sm)) {
                Text("Overview", color = BraceTheme.colors.semantic.onSurface)
                BraceDivider()
                Text("Metrics", color = BraceTheme.colors.semantic.onSurface)
                Row(Modifier.height(56.dp)) {
                    Text("Left", color = BraceTheme.colors.semantic.onSurface)
                    BraceDivider(orientation = BraceDividerOrientation.Vertical)
                    Text("Right", color = BraceTheme.colors.semantic.onSurface)
                }
            }
        }
        "core-progressbar" -> {
            var progress by rememberSaveable { mutableStateOf(0.35f) }
            Column(verticalArrangement = Arrangement.spacedBy(BraceTheme.spacing.md)) {
                BraceProgressBar(label = "Uploading files", value = progress)
                BraceButton("Advance upload", onClick = { progress = (progress + 0.15f).coerceAtMost(1f) })
                BraceProgressBar(label = "Waiting for response", value = null, intent = BraceProgressIntent.Warning)
                BraceProgressBar(label = "Unavailable task", value = 0.6f, enabled = false)
            }
        }
        "core-spinner" -> {
            var progress by rememberSaveable { mutableStateOf(0.3f) }
            var determinate by rememberSaveable { mutableStateOf(false) }
            Column(verticalArrangement = Arrangement.spacedBy(BraceTheme.spacing.sm)) {
                Row(horizontalArrangement = Arrangement.spacedBy(BraceTheme.spacing.md)) {
                    BraceSpinner("Loading small records", size = BraceSpinnerSize.Small)
                    BraceSpinner("Loading records", if (determinate) progress else null)
                    BraceSpinner("Loading large reports", if (determinate) progress else null,
                        size = BraceSpinnerSize.Large, intent = BraceProgressIntent.Success)
                }
                Text(if (determinate) "Progress: ${(progress * 100).toInt()}%" else "Indeterminate",
                    color = BraceTheme.colors.semantic.onSurface)
                BraceButton("Toggle known progress", onClick = { determinate = !determinate })
                BraceButton("Advance", onClick = { progress = (progress + 0.2f).coerceAtMost(1f) })
            }
        }
        "core-skeleton" -> {
            var loading by rememberSaveable { mutableStateOf(true) }
            var animated by rememberSaveable { mutableStateOf(true) }
            Column(verticalArrangement = Arrangement.spacedBy(BraceTheme.spacing.sm)) {
                if (loading) {
                    BraceSkeleton(label = "Loading report title", animated = animated)
                    BraceSkeleton(width = 180.dp, animated = animated)
                    BraceSkeleton(width = 120.dp, animated = animated)
                } else {
                    Text("Quarterly report is ready", color = BraceTheme.colors.semantic.onSurface)
                }
                BraceButton("Toggle content", onClick = { loading = !loading })
                BraceButton("Toggle shimmer", onClick = { animated = !animated })
            }
        }
        "core-section" -> {
            BraceSection(title = "Projects", subtitle = "Three active workspaces", collapsible = true) {
                BraceSectionCard { Text("Tap the heading to collapse", color = BraceTheme.colors.semantic.onSurface) }
            }
        }
        "core-sectioncard" -> {
            Column(verticalArrangement = Arrangement.spacedBy(BraceTheme.spacing.sm)) {
                BraceSectionCard { Text("Padded content", color = BraceTheme.colors.semantic.onSurface) }
                BraceSectionCard(padded = false) { Text("Edge to edge content", color = BraceTheme.colors.semantic.onSurface) }
            }
        }
        "core-breadcrumbs" -> {
            var destination by rememberSaveable { mutableStateOf("Dashboard") }
            Column(verticalArrangement = Arrangement.spacedBy(BraceTheme.spacing.sm)) {
                BraceBreadcrumbs(listOf(
                    BraceBreadcrumb("Home", onClick = { destination = "Home" }),
                    BraceBreadcrumb("Workspaces", onClick = { destination = "Workspaces" }),
                    BraceBreadcrumb("Analytics", onClick = { destination = "Analytics" }),
                    BraceBreadcrumb("Dashboard"),
                ))
                Text("Opened: $destination", color = BraceTheme.colors.semantic.onSurfaceMuted)
            }
        }
        "core-breadcrumb" -> {
            var opened by rememberSaveable { mutableStateOf(false) }
            Column {
                BraceBreadcrumbItem("Projects", onClick = { opened = !opened })
                BraceBreadcrumbItem("Current view", current = true)
                Text(if (opened) "Projects opened" else "Tap Projects", color = BraceTheme.colors.semantic.onSurfaceMuted)
            }
        }
        "core-tag" -> {
            var visible by rememberSaveable { mutableStateOf(true) }
            var selected by rememberSaveable { mutableStateOf(false) }
            Column(verticalArrangement = Arrangement.spacedBy(BraceTheme.spacing.sm)) {
                if (visible) BraceTag("Finance", intent = BraceTagIntent.Primary, selected = selected,
                    onClick = { selected = !selected }, onRemove = { visible = false },
                    removeContentDescription = "Remove Finance filter")
                BraceTag("Warning", intent = BraceTagIntent.Warning)
                BraceTag("Unavailable", enabled = false, onClick = {})
                BraceButton("Restore tag", onClick = { visible = true })
            }
        }
        "core-compoundtag" -> {
            var visible by rememberSaveable { mutableStateOf(true) }
            Column(verticalArrangement = Arrangement.spacedBy(BraceTheme.spacing.sm)) {
                if (visible) BraceCompoundTag("Status", "Active", onRemove = { visible = false },
                    removeContentDescription = "Remove Status filter")
                BraceCompoundTag("Owner", "Team", intent = BraceTagIntent.Success, rounded = true)
                BraceButton("Restore filter", onClick = { visible = true })
            }
        }
        "core-callout" -> {
            var dismissed by rememberSaveable { mutableStateOf(false) }
            Column(verticalArrangement = Arrangement.spacedBy(BraceTheme.spacing.sm)) {
                if (!dismissed) BraceCallout(title = "Changes saved", intent = BraceCalloutIntent.Success,
                    action = { BraceButton("Dismiss", onClick = { dismissed = true }) }) {
                    Text("Your workspace is up to date.")
                }
                BraceCallout(title = "Review access", intent = BraceCalloutIntent.Warning, minimal = true) {
                    Text("One member still needs approval.")
                }
                if (dismissed) BraceButton("Show message", onClick = { dismissed = false })
            }
        }
        "core-nonidealstate" -> {
            var retried by rememberSaveable { mutableStateOf(false) }
            BraceEmptyState(title = if (retried) "Still no results" else "No results",
                description = "Try changing the query.",
                action = { BraceButton("Retry", onClick = { retried = true }) })
        }
        "core-menu", "core-menuitem", "core-menudivider" -> {
            var selected by rememberSaveable { mutableStateOf(false) }
            var expanded by rememberSaveable { mutableStateOf(false) }
            Column(verticalArrangement = Arrangement.spacedBy(BraceTheme.spacing.sm)) {
                BraceMenu {
                    BraceMenuItem("Open report", onClick = { selected = true })
                    BraceMenuDivider(title = "Workspace")
                    BraceMenuItem("Archived", onClick = { selected = !selected }, selected = selected)
                    BraceMenuItem("Unavailable", onClick = {}, enabled = false)
                }
                BraceMenuPopup(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    anchor = { BraceButton("Open anchored menu", onClick = { expanded = true }) },
                ) {
                    BraceMenuItem("Open report", onClick = { selected = true })
                    BraceMenuItem("Archived", onClick = { selected = !selected }, selected = selected)
                }
            }
        }
        "core-overlay", "core-overlay2", "core-portal", "core-overlaysprovider", "core-portalprovider", "core-useoverlaystack" -> {
            var open by rememberSaveable { mutableStateOf(false) }
            val overlays = rememberBraceOverlayState()
            BraceOverlayHost(overlays) {
                Column(verticalArrangement = Arrangement.spacedBy(BraceTheme.spacing.sm)) {
                    BraceButton("Open overlay", onClick = { open = true })
                    Text("Active layers: ${overlays.activeCount}", color = BraceTheme.colors.semantic.onSurface)
                    BraceOverlay(open = open, onDismissRequest = { open = false }, title = "Details") {
                        Column(Modifier.padding(BraceTheme.componentMetrics.dialog.contentPadding),
                            verticalArrangement = Arrangement.spacedBy(BraceTheme.spacing.sm)) {
                            Text("Details", color = BraceTheme.colors.semantic.onSurface,
                                style = BraceTheme.typography.title)
                            Text("Android dialog window with Brace tokens.",
                                color = BraceTheme.colors.semantic.onSurface)
                            BraceButton("Close", onClick = { open = false })
                        }
                    }
                }
            }
        }

        "core-drawer" -> {
            var open by rememberSaveable { mutableStateOf(false) }
            var position by rememberSaveable { mutableStateOf(BraceDrawerPosition.End) }
            var activeOnly by rememberSaveable { mutableStateOf(true) }
            Column(verticalArrangement = Arrangement.spacedBy(BraceTheme.spacing.sm)) {
                BraceButton("Open drawer", onClick = { open = true })
                BraceButton("Switch edge", onClick = {
                    position = if (position == BraceDrawerPosition.End) BraceDrawerPosition.Bottom
                        else BraceDrawerPosition.End
                }, variant = BraceButtonVariant.Outline)
                Text("Edge: $position", color = BraceTheme.colors.semantic.onSurfaceMuted)
                BraceDrawer(
                    open = open,
                    onDismissRequest = { open = false },
                    title = "Filter results",
                    position = position,
                    footer = { BraceButton("Apply filters", onClick = { open = false }) },
                ) {
                    Text("Choose the filters for this view.", color = BraceTheme.colors.semantic.onSurface)
                    BraceCheckbox(activeOnly, { activeOnly = it }, "Active records")
                }
            }
        }
        "core-popover", "core-popovernext" -> {
            var open by rememberSaveable { mutableStateOf(false) }
            var placement by rememberSaveable { mutableStateOf(BracePopoverPlacement.Auto) }
            Column(verticalArrangement = Arrangement.spacedBy(BraceTheme.spacing.sm)) {
                BraceButton("Switch placement", onClick = {
                    placement = if (placement == BracePopoverPlacement.Auto) BracePopoverPlacement.TopEnd
                        else BracePopoverPlacement.Auto
                }, variant = BraceButtonVariant.Outline)
                Text("Placement: $placement", color = BraceTheme.colors.semantic.onSurfaceMuted)
                BracePopover(
                    expanded = open,
                    onDismissRequest = { open = false },
                    placement = placement,
                    title = "Filter options",
                    target = { BraceButton("Open filters", onClick = { open = !open }) },
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(BraceTheme.spacing.sm)) {
                        Text("Only active records", color = BraceTheme.colors.semantic.onSurface)
                        BraceButton("Apply", onClick = { open = false })
                    }
                }
            }
        }
        "core-contextmenu" -> {
            var open by rememberSaveable { mutableStateOf(false) }
            var result by rememberSaveable { mutableStateOf("No action yet") }
            Column(verticalArrangement = Arrangement.spacedBy(BraceTheme.spacing.sm)) {
                Text("Right-click, long-press, or focus and press Shift+F10 on the report.",
                    color = BraceTheme.colors.semantic.onSurfaceMuted)
                BraceContextMenu(
                    expanded = open,
                    onExpandedChange = { open = it },
                    title = "Report actions",
                    targetIsFocusable = true,
                    target = { targetModifier -> BraceButton("Quarterly report", onClick = { result = "Opened report" }, modifier = targetModifier) },
                ) { dismiss ->
                    BraceMenuItem("Copy link", onClick = { result = "Link copied"; dismiss() })
                    BraceMenuItem("Archive", onClick = { result = "Report archived"; dismiss() })
                    BraceMenuItem("Unavailable", onClick = {}, enabled = false)
                }
                Text(result, color = BraceTheme.colors.semantic.onSurface)
            }
        }
        "core-contextmenupopover" -> {
            var open by rememberSaveable { mutableStateOf(false) }
            var result by rememberSaveable { mutableStateOf("No action yet") }
            val trigger = remember { FocusRequester() }
            var hadOpened by remember { mutableStateOf(false) }
            LaunchedEffect(open) {
                if (open) hadOpened = true
                else if (hadOpened) {
                    trigger.requestFocus()
                    hadOpened = false
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(BraceTheme.spacing.sm)) {
                Text("This lower-level surface opens at an explicit window position.",
                    color = BraceTheme.colors.semantic.onSurfaceMuted)
                BraceButton("Open point-anchored menu", onClick = { open = true },
                    modifier = Modifier.focusRequester(trigger))
                Text(result, color = BraceTheme.colors.semantic.onSurface)
                BraceContextMenuPopup(
                    expanded = open,
                    onDismissRequest = { open = false },
                    targetOffset = IntOffset(80, 220),
                    title = "More actions",
                ) { dismiss ->
                    BraceMenuItem("Refresh", onClick = { result = "Refreshed"; dismiss() })
                    BraceMenuItem("Pin", onClick = { result = "Pinned"; dismiss() })
                }
            }
        }
        "core-hotkeystarget", "core-hotkeysprovider", "core-usehotkeys", "core-keycombotag" -> {
            var refreshed by rememberSaveable { mutableStateOf(0) }
            var exported by rememberSaveable { mutableStateOf(0) }
            var searched by rememberSaveable { mutableStateOf(0) }
            var query by rememberSaveable { mutableStateOf("") }
            val shortcutState = rememberBraceShortcutRegistryState()
            BraceShortcutRegistry(
                shortcuts = listOf(BraceShortcut("ctrl+r", "Refresh records", spokenComboLabel = "Control plus R", group = "Records",
                    onKeyDown = { refreshed++ })),
                discoveryTitle = "Catalog shortcuts",
                state = shortcutState,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(BraceTheme.spacing.sm)) {
                    Text("Focus a control, then press Ctrl+R. Press ? to open the guide.",
                        color = BraceTheme.colors.semantic.onSurfaceMuted)
                    BraceButton("Show shortcut guide", onClick = { shortcutState.showDiscovery() },
                        variant = BraceButtonVariant.Outline)
                    Row(horizontalArrangement = Arrangement.spacedBy(BraceTheme.spacing.sm)) {
                        BraceShortcutLabel("Ctrl+R", spokenLabel = "Control plus R")
                        Text("Refresh records", color = BraceTheme.colors.semantic.onSurface)
                    }
                    BraceButton("Refresh records", onClick = { refreshed++ })
                    BraceShortcutScope(shortcuts = listOf(
                        BraceShortcut("ctrl+e", "Export here", group = "Records", onKeyDown = { exported++ }),
                        BraceShortcut("ctrl+g", "Export globally", group = "Records", global = true,
                            onKeyDown = { exported++ }),
                    )) {
                        BraceButton("Export records", onClick = { exported++ })
                    }
                    Column(Modifier.braceShortcuts(listOf(BraceShortcut("ctrl+k", "Search records",
                        group = "Records", onKeyDown = { searched++ })))) {
                        BraceButton("Focus search shortcut", onClick = { searched++ })
                    }
                    BraceTextField(query, { query = it }, "Type without triggering shortcuts")
                    Text("Ctrl+E is local; Ctrl+G works anywhere in this sample while mounted.",
                        color = BraceTheme.colors.semantic.onSurfaceMuted)
                    Text("Refresh $refreshed · Export $exported · Search $searched",
                        color = BraceTheme.colors.semantic.onSurface)
                }
            }
        }
        "core-tooltip" -> {
            var enabled by rememberSaveable { mutableStateOf(true) }
            var targetClicks by rememberSaveable { mutableStateOf(0) }
            Column(verticalArrangement = Arrangement.spacedBy(BraceTheme.spacing.sm)) {
                Text("Hover, long press, or focus the target for help.",
                    color = BraceTheme.colors.semantic.onSurfaceMuted)
                BraceTooltip(
                    text = "Imports include archived records when this option is selected.",
                    enabled = enabled,
                    target = { BraceButton("Import help", onClick = { targetClicks++ }) },
                )
                Text("Target taps: $targetClicks", color = BraceTheme.colors.semantic.onSurfaceMuted)
                BraceButton(if (enabled) "Disable tooltip" else "Enable tooltip",
                    onClick = { enabled = !enabled }, variant = BraceButtonVariant.Outline)
            }
        }
        "core-toast", "core-overlaytoaster" -> {
            var lastEvent by rememberSaveable { mutableStateOf("No toast dismissed") }
            Column(verticalArrangement = Arrangement.spacedBy(BraceTheme.spacing.sm)) {
                Text("Toast stack: ${toasts.visibleToasts.size} visible · $toastPosition",
                    color = BraceTheme.colors.semantic.onSurfaceMuted)
                BraceButton("Show neutral", onClick = {
                    toasts.show(BraceToastSpec("Import queued", intent = BraceToastIntent.Neutral,
                        onDismiss = { lastEvent = "Neutral: $it" }))
                }, variant = BraceButtonVariant.Outline)
                BraceButton("Show success", onClick = {
                    toasts.show(BraceToastSpec("Changes saved", intent = BraceToastIntent.Success,
                        onDismiss = { lastEvent = "Success: $it" }))
                })
                BraceButton("Show warning", onClick = {
                    toasts.show(BraceToastSpec("Review one missing field", intent = BraceToastIntent.Warning,
                        onDismiss = { lastEvent = "Warning: $it" }))
                }, intent = BraceButtonIntent.Secondary)
                BraceButton("Show danger with action", onClick = {
                    toasts.show(BraceToastSpec("Upload failed", intent = BraceToastIntent.Danger,
                        durationMillis = 0, actionLabel = "Retry",
                        onAction = { lastEvent = "Retry requested" },
                        onDismiss = { lastEvent = "Danger: $it" }))
                }, intent = BraceButtonIntent.Danger)
                BraceButton("Show or update keyed status", onClick = {
                    toasts.show(BraceToastSpec("Status refreshed", intent = BraceToastIntent.Primary,
                        onDismiss = { lastEvent = "Status: $it" }), key = "catalog-status")
                }, variant = BraceButtonVariant.Outline)
                BraceButton(if (toastPosition == BraceToastPosition.BottomEnd) "Move stack to top start"
                    else "Move stack to bottom end", onClick = {
                    onToastPositionChange(if (toastPosition == BraceToastPosition.BottomEnd)
                        BraceToastPosition.TopStart else BraceToastPosition.BottomEnd)
                }, variant = BraceButtonVariant.Outline)
                BraceButton("Clear notifications", onClick = { toasts.clear() },
                    variant = BraceButtonVariant.Outline)
                Text(lastEvent, color = BraceTheme.colors.semantic.onSurfaceMuted)
            }
        }
        "core-dialog", "core-dialogbody", "core-dialogfooter", "core-alert" -> {
            var dialogOpen by rememberSaveable { mutableStateOf(false) }
            var alertOpen by rememberSaveable { mutableStateOf(false) }
            var saved by rememberSaveable { mutableStateOf(false) }
            Column(verticalArrangement = Arrangement.spacedBy(BraceTheme.spacing.sm)) {
                BraceButton("Open edit dialog", onClick = { dialogOpen = true })
                BraceButton("Open confirmation", onClick = { alertOpen = true },
                    intent = BraceButtonIntent.Danger)
                Text(if (saved) "Saved" else "No change yet", color = BraceTheme.colors.semantic.onSurface)
                BraceDialog(
                    open = dialogOpen,
                    onDismissRequest = { dialogOpen = false },
                    title = "Edit project",
                    actions = {
                        BraceButton("Cancel", onClick = { dialogOpen = false },
                            intent = BraceButtonIntent.Secondary)
                        BraceButton("Save", onClick = { saved = true; dialogOpen = false })
                    },
                ) {
                    Text("Project details", color = BraceTheme.colors.semantic.onSurface)
                    Text("Actions remain visible while long content scrolls.",
                        color = BraceTheme.colors.semantic.onSurfaceMuted)
                }
                BraceAlertDialog(
                    open = alertOpen,
                    title = "Delete report?",
                    message = "This removes the report from this workspace.",
                    onConfirm = { saved = false; alertOpen = false },
                    onCancel = { alertOpen = false },
                    confirmLabel = "Delete",
                    confirmIntent = BraceButtonIntent.Danger,
                )
            }
        }

    }
}
