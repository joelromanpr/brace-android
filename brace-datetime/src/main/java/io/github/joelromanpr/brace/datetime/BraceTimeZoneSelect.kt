package io.github.joelromanpr.brace.datetime

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.focused
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.requestFocus
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import io.github.braceandroid.foundation.BraceTheme
import io.github.joelromanpr.brace.core.BracePopover
import io.github.joelromanpr.brace.core.braceShortcutEditable
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

private data class ZoneLabels(
    val choose: String,
    val search: String,
    val empty: String,
    val selected: String,
    val local: String,
    val expanded: String,
    val collapsed: String,
    val position: String,
)

@Composable
private fun zoneLabels(locale: Locale): ZoneLabels {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val localized = remember(context, configuration, locale) {
        val copy = Configuration(configuration)
        copy.setLocale(locale)
        context.createConfigurationContext(copy)
    }
    return remember(localized) {
        ZoneLabels(
            choose = localized.getString(R.string.brace_zone_choose),
            search = localized.getString(R.string.brace_zone_search),
            empty = localized.getString(R.string.brace_zone_empty),
            selected = localized.getString(R.string.brace_zone_selected),
            local = localized.getString(R.string.brace_zone_local),
            expanded = localized.getString(R.string.brace_zone_expanded),
            collapsed = localized.getString(R.string.brace_zone_collapsed),
            position = localized.getString(R.string.brace_zone_position),
        )
    }
}

/**
 * Controlled, searchable IANA time-zone selection in a native Compose popover.
 *
 * [value] keeps a [ZoneId] identity across daylight-saving transitions. Offset and localized
 * long name are computed at [referenceInstant], or once from [clock] at first composition.
 * Changing [referenceInstant] updates the labels without changing the selected identity.
 * [showLocalTimeZone] moves [systemZone] to the top of the full searchable list, which uses
 * Android's time-zone database instead of bundling a second web list. The query and open state
 * survive activity recreation; callers should save [value] as its zone ID string.
 *
 * The search field keeps keyboard focus while Up/Down, Home/End, and Enter navigate and choose;
 * touch, mouse, Tab, and TalkBack can activate 48 dp rows. Escape, Back, and outside click dismiss
 * the popover. Search matches IANA IDs, names, and UTC offsets. [label] and [placeholder] should
 * describe the form field in the application's language. The visual surface follows Brace tokens.
 */
@Composable
public fun BraceTimeZoneSelect(
    value: ZoneId?,
    onValueChange: (ZoneId) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    locale: Locale? = null,
    referenceInstant: Instant? = null,
    clock: Clock = Clock.systemUTC(),
    systemZone: ZoneId = ZoneId.systemDefault(),
    enabled: Boolean = true,
    showLocalTimeZone: Boolean = false,
    display: BraceTimeZoneDisplay = BraceTimeZoneDisplay.Composite,
    placeholder: String? = null,
    fill: Boolean = false,
) {
    require(label.isNotBlank()) { "Time-zone label must not be blank" }
    val resolvedLocale = locale ?: LocalConfiguration.current.locales[0] ?: Locale.getDefault()
    val labels = zoneLabels(resolvedLocale)
    val instant = referenceInstant ?: remember(clock) { clock.instant() }
    val options = remember(instant, resolvedLocale, systemZone, value, showLocalTimeZone) {
        timeZoneOptions(instant, resolvedLocale, systemZone, value, showLocalTimeZone)
    }
    val selectedOption = options.firstOrNull { it.zone == value }
    val colors = BraceTheme.colors.components.timeZoneSelect
    val metrics = BraceTheme.componentMetrics.timeZoneSelect
    val shape = RoundedCornerShape(metrics.cornerRadius)
    var expanded by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var queryField by remember { mutableStateOf(TextFieldValue(query, TextRange(query.length))) }
    SideEffect {
        if (queryField.text != query) queryField = TextFieldValue(query, TextRange(query.length))
    }
    val filtered = remember(query, options) { filterTimeZones(query, options) }
    var activeId by rememberSaveable { mutableStateOf<String?>(null) }
    val activeIndex = filtered.indexOfFirst { it.zone.id == activeId }
    val listState = rememberLazyListState()
    val queryFocus = remember { FocusRequester() }
    val triggerFocus = remember { FocusRequester() }
    val effectiveExpanded = expanded && enabled

    fun dismiss() {
        expanded = false
        query = ""
    }
    fun select(option: BraceTimeZoneOption) {
        onValueChange(option.zone)
        dismiss()
    }
    fun chooseActive() {
        filtered.firstOrNull { it.zone.id == activeId }?.let(::select)
            ?: filtered.firstOrNull()?.let(::select)
    }
    LaunchedEffect(enabled) { if (!enabled && expanded) dismiss() }
    fun moveActive(delta: Int) {
        if (filtered.isEmpty()) return
        val index = filtered.indexOfFirst { it.zone.id == activeId }.let { if (it < 0) 0 else it }
        activeId = filtered[(index + delta).coerceIn(0, filtered.lastIndex)].zone.id
    }
    LaunchedEffect(effectiveExpanded, value) {
        if (effectiveExpanded && query.isEmpty()) {
            activeId = filtered.firstOrNull { it.zone == value }?.zone?.id
                ?: filtered.firstOrNull()?.zone?.id
        }
    }
    LaunchedEffect(effectiveExpanded, query, options) {
        if (effectiveExpanded && filtered.none { it.zone.id == activeId }) {
            activeId = filtered.firstOrNull { it.zone == value }?.zone?.id ?: filtered.firstOrNull()?.zone?.id
        }
    }
    LaunchedEffect(effectiveExpanded, activeId, filtered) {
        if (effectiveExpanded && activeIndex >= 0 &&
            listState.layoutInfo.visibleItemsInfo.none { it.index == activeIndex }) {
            listState.scrollToItem(activeIndex)
        }
    }
    val triggerText = "$label: ${selectedOption?.let { displayTimeZone(it, display) } ?: placeholder ?: labels.choose}"
    val triggerSpoken = if (display != BraceTimeZoneDisplay.Identifier &&
        display != BraceTimeZoneDisplay.Composite && selectedOption != null)
        "$label: ${selectedOption.zone.id}, ${selectedOption.offsetLabel}" else triggerText
    val triggerSource = remember { MutableInteractionSource() }
    val triggerFocused by triggerSource.collectIsFocusedAsState()
    val triggerHovered by triggerSource.collectIsHoveredAsState()
    BracePopover(
        expanded = effectiveExpanded,
        onDismissRequest = ::dismiss,
        title = label,
        modifier = if (fill) modifier.fillMaxWidth() else modifier,
        surfaceModifier = Modifier.widthIn(min = metrics.minWidth),
        target = {
            Row(
                modifier = Modifier.then(if (fill) Modifier.fillMaxWidth() else Modifier)
                    .heightIn(min = BraceTheme.sizing.touchTarget)
                    .focusRequester(triggerFocus)
                    .clearAndSetSemantics {
                        contentDescription = triggerSpoken
                        role = Role.Button
                        stateDescription = if (effectiveExpanded) labels.expanded else labels.collapsed
                        if (!enabled) disabled() else {
                            onClick { expanded = true; true }
                            requestFocus { triggerFocus.requestFocus() }
                        }
                    }
                    .clickable(enabled = enabled, role = Role.Button,
                        interactionSource = triggerSource, indication = null) { expanded = true }
                    .background(if (triggerHovered && enabled) colors.activeContainer else colors.container, shape)
                    .border(if (triggerFocused) BraceTheme.sizing.focusRingWidth else BraceTheme.sizing.borderWidth,
                        if (triggerFocused) colors.focusRing else colors.border, shape)
                    .padding(horizontal = metrics.horizontalPadding, vertical = BraceTheme.spacing.xs),
                horizontalArrangement = Arrangement.spacedBy(BraceTheme.spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(triggerText, modifier = if (fill) Modifier.weight(1f) else Modifier,
                    color = if (enabled) colors.content else colors.disabledContent,
                    style = BraceTheme.typography.body,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("▾", color = if (enabled) colors.content else colors.disabledContent,
                    style = BraceTheme.typography.body,
                    modifier = Modifier.clearAndSetSemantics {})
            }
        },
    ) {
        Column(
            Modifier.onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown || queryField.composition != null) false
                else when (event.key) {
                    Key.DirectionDown -> { moveActive(1); true }
                    Key.DirectionUp -> { moveActive(-1); true }
                    Key.MoveHome -> { activeId = filtered.firstOrNull()?.zone?.id; true }
                    Key.MoveEnd -> { activeId = filtered.lastOrNull()?.zone?.id; true }
                    Key.Enter -> { chooseActive(); true }
                    Key.Escape -> { dismiss(); true }
                    else -> false
                }
            },
            verticalArrangement = Arrangement.spacedBy(BraceTheme.spacing.xs),
        ) {
            val searchSource = remember { MutableInteractionSource() }
            val searchFocused by searchSource.collectIsFocusedAsState()
            val position = if (activeIndex >= 0) String.format(resolvedLocale, labels.position,
                activeIndex + 1, filtered.size, filtered[activeIndex].zone.id) else null
            BasicTextField(
                value = queryField,
                onValueChange = { next -> queryField = next; query = next.text },
                modifier = Modifier.fillMaxWidth().heightIn(min = BraceTheme.sizing.touchTarget)
                    .background(colors.searchContainer, shape)
                    .border(if (searchFocused) BraceTheme.sizing.focusRingWidth else BraceTheme.sizing.borderWidth,
                        if (searchFocused) colors.focusRing else colors.searchBorder, shape)
                    .focusRequester(queryFocus)
                    .braceShortcutEditable()
                    .semantics {
                        contentDescription = labels.search
                        if (position != null) stateDescription = position
                    },
                singleLine = true,
                textStyle = BraceTheme.typography.body.copy(color = colors.content),
                cursorBrush = SolidColor(colors.focusRing),
                interactionSource = searchSource,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    if (queryField.composition == null) chooseActive()
                }),
                decorationBox = { inner ->
                    Box(Modifier.padding(horizontal = metrics.horizontalPadding),
                        contentAlignment = Alignment.CenterStart) {
                        if (query.isEmpty()) Text(labels.search, color = colors.mutedContent,
                            style = BraceTheme.typography.body)
                        inner()
                    }
                },
            )
            LaunchedEffect(Unit) { queryFocus.requestFocus() }
            if (filtered.isEmpty()) {
                Text(labels.empty, Modifier.padding(BraceTheme.spacing.md),
                    color = colors.mutedContent, style = BraceTheme.typography.body)
            } else {
                LazyColumn(Modifier.heightIn(max = metrics.listMaxHeight), state = listState) {
                    items(filtered, key = { it.zone.id }) { option ->
                        val selected = option.zone == value
                        val active = option.zone.id == activeId
                        val source = remember { MutableInteractionSource() }
                        val focused by source.collectIsFocusedAsState()
                        val hovered by source.collectIsHoveredAsState()
                        val requester = remember { FocusRequester() }
                        val spoken = listOfNotNull(
                            option.zone.id, option.offsetLabel, option.displayName, option.shortName,
                            labels.local.takeIf { option.isLocal },
                        ).joinToString(", ")
                        Column(
                            Modifier.fillMaxWidth().heightIn(min = metrics.rowHeight)
                                .background(when {
                                    selected -> colors.selectedContainer
                                    active || focused || hovered -> colors.activeContainer
                                    else -> colors.container
                                }, shape)
                                .then(if (focused || active) Modifier.border(
                                    if (focused) BraceTheme.sizing.focusRingWidth else BraceTheme.sizing.borderWidth,
                                    colors.focusRing, shape) else Modifier)
                                .clearAndSetSemantics {
                                    contentDescription = spoken
                                    role = Role.RadioButton
                                    this.selected = selected
                                    this.focused = focused
                                    if (selected) stateDescription = labels.selected
                                    onClick { select(option); true }
                                    requestFocus { requester.requestFocus() }
                                }
                                .focusRequester(requester)
                                .onFocusChanged { if (it.isFocused) activeId = option.zone.id }
                                .focusable(interactionSource = source)
                                .selectable(selected = selected, role = Role.RadioButton,
                                    interactionSource = source, indication = null) { select(option) }
                                .padding(horizontal = metrics.horizontalPadding,
                                    vertical = BraceTheme.spacing.xs),
                            verticalArrangement = Arrangement.spacedBy(BraceTheme.spacing.xxs),
                        ) {
                            Text(option.zone.id,
                                color = if (selected) colors.selectedContent else colors.content,
                                style = BraceTheme.typography.body)
                            Text("${option.offsetLabel} · ${option.displayName}",
                                color = if (selected) colors.selectedContent else colors.mutedContent,
                                style = BraceTheme.typography.label)
                        }
                    }
                }
            }
        }
    }
}
