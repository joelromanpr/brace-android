package io.github.joelromanpr.brace.select

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.focused
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.requestFocus
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import io.github.braceandroid.foundation.BraceTheme
import io.github.joelromanpr.brace.core.BraceButton
import io.github.joelromanpr.brace.core.BraceButtonIntent
import io.github.joelromanpr.brace.core.BraceButtonVariant
import io.github.joelromanpr.brace.core.BracePopover
import io.github.joelromanpr.brace.core.braceShortcutEditable

/**
 * A controlled, filterable single selection in an anchored Compose popup.
 *
 * [selectedKey] is owned by the caller and must identify a unique [options] entry. The caller
 * also owns [expanded] so the popup can be restored alongside screen state. [state] retains the
 * query and active option through activity recreation. [onSelect] receives the entire typed
 * option; selection then requests popup closure. Disabled options stay visible but cannot be
 * selected. The search field keeps keyboard focus while Up/Down, Enter, Home, End, and Escape
 * operate the list; pointer and TalkBack can activate individual options. [label], [queryLabel],
 * [placeholder], and [emptyLabel] should be localized for the app's domain.
 */
@Composable
public fun <T> BraceSelect(
    options: List<BraceSelectOption<T>>,
    selectedKey: String?,
    onSelect: (BraceSelectOption<T>) -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    state: BraceQueryListState = rememberBraceQueryListState(),
    enabled: Boolean = true,
    filterable: Boolean = true,
    resetQueryOnSelect: Boolean = true,
    predicate: ((String, BraceSelectOption<T>) -> Boolean)? = null,
    optionContent: (@Composable (BraceSelectOption<T>) -> Unit)? = null,
    placeholder: String? = null,
    queryLabel: String? = null,
    emptyLabel: String? = null,
) {
    require(label.isNotBlank()) { "Select label must not be blank" }
    require(queryLabel == null || queryLabel.isNotBlank()) { "Select query label must not be blank" }
    require(placeholder == null || placeholder.isNotBlank()) { "Select placeholder must not be blank" }
    require(emptyLabel == null || emptyLabel.isNotBlank()) { "Select empty label must not be blank" }
    require(options.map { it.key }.toSet().size == options.size) { "BraceSelect option keys must be unique" }
    val colors = BraceTheme.colors.components.select
    val metrics = BraceTheme.componentMetrics.select
    val activeOptions = if (filterable) state.filter(options, predicate) else options
    val enabledOptions = activeOptions.filter { it.enabled }
    val enabledKeys = enabledOptions.map { it.key }
    val chosen = options.firstOrNull { it.key == selectedKey }
    val placeholderText = placeholder ?: stringResource(R.string.brace_select_choose)
    val queryText = queryLabel ?: stringResource(R.string.brace_select_search)
    val noResultsText = emptyLabel ?: stringResource(R.string.brace_select_empty)
    val expansionText = stringResource(if (expanded && enabled) R.string.brace_select_expanded
        else R.string.brace_select_collapsed)
    val queryFocus = remember { FocusRequester() }
    val triggerFocus = remember { FocusRequester() }
    var queryField by remember(state) {
        mutableStateOf(TextFieldValue(state.query, TextRange(state.query.length)))
    }
    SideEffect {
        if (queryField.text != state.query) {
            queryField = TextFieldValue(state.query, TextRange(state.query.length))
        }
    }
    val optionFocus = remember(activeOptions.map { it.key }) {
        activeOptions.associate { it.key to FocusRequester() }
    }
    val listState = rememberLazyListState()
    val effectiveExpanded = expanded && enabled
    LaunchedEffect(enabled, expanded) {
        if (!enabled && expanded) onExpandedChange(false)
    }

    fun selectOption(option: BraceSelectOption<T>) {
        state.activeKey = option.key
        onSelect(option)
        if (resetQueryOnSelect) state.query = ""
        onExpandedChange(false)
    }

    fun chooseActive() {
        val option = enabledOptions.firstOrNull { it.key == state.activeKey } ?: enabledOptions.firstOrNull()
        if (option != null) selectOption(option)
    }

    LaunchedEffect(effectiveExpanded, selectedKey, enabledKeys) {
        if (effectiveExpanded) {
            if (state.activeKey !in enabledKeys) {
                state.activeKey = if (selectedKey in enabledKeys) selectedKey else enabledKeys.firstOrNull()
            }
        }
    }

    BracePopover(
        expanded = effectiveExpanded,
        onDismissRequest = { onExpandedChange(false) },
        title = label,
        modifier = modifier,
        surfaceModifier = Modifier.widthIn(min = metrics.minWidth),
        target = {
            BraceButton(
                label = "$label: ${chosen?.label ?: placeholderText}",
                onClick = { onExpandedChange(!expanded) },
                enabled = enabled,
                intent = BraceButtonIntent.Secondary,
                variant = BraceButtonVariant.Outline,
                modifier = Modifier.focusRequester(triggerFocus).clearAndSetSemantics {
                    testTag = "brace-select-trigger"
                    contentDescription = "$label: ${chosen?.label ?: placeholderText}"
                    role = Role.Button
                    stateDescription = expansionText
                    if (!enabled) disabled() else {
                        onClick { onExpandedChange(!expanded); true }
                        requestFocus { triggerFocus.requestFocus() }
                    }
                },
            )
        },
    ) {
        val activeIndex = enabledKeys.indexOf(state.activeKey)
        val positionDescription = if (activeIndex >= 0) {
            stringResource(R.string.brace_select_active_position, activeIndex + 1,
                enabledKeys.size, enabledOptions[activeIndex].label)
        } else null
        Column(
            modifier = Modifier
                .braceQueryNavigation(state, enabledKeys,
                    onActivate = { key -> state.activeKey = key; chooseActive() },
                    onDismiss = { onExpandedChange(false) },
                    isTextComposing = { filterable && queryField.composition != null },
                    activateOnSpace = !filterable),
            verticalArrangement = Arrangement.spacedBy(BraceTheme.spacing.xs),
        ) {
            if (filterable) {
                val searchInteraction = remember { MutableInteractionSource() }
                val focused by searchInteraction.collectIsFocusedAsState()
                val searchShape = RoundedCornerShape(metrics.cornerRadius)
                val searchColor = if (focused) colors.focusRing else colors.searchBorder
                BasicTextField(
                    value = queryField,
                    onValueChange = { next ->
                        queryField = next
                        if (state.query != next.text) state.query = next.text
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = BraceTheme.sizing.touchTarget)
                        .background(colors.searchContainer, searchShape)
                        .border(if (focused) BraceTheme.sizing.focusRingWidth else BraceTheme.sizing.borderWidth,
                            searchColor, searchShape)
                        .focusRequester(queryFocus)
                        .braceShortcutEditable()
                        .semantics {
                            contentDescription = queryText
                            if (positionDescription != null) stateDescription = positionDescription
                        }
                        .testTag("brace-select-query"),
                    singleLine = true,
                    textStyle = BraceTheme.typography.body.copy(color = colors.content),
                    cursorBrush = SolidColor(colors.focusRing),
                    interactionSource = searchInteraction,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        if (queryField.composition == null) chooseActive()
                    }),
                    decorationBox = { inner ->
                        Box(Modifier.padding(horizontal = metrics.searchHorizontalPadding,
                            vertical = BraceTheme.spacing.sm), contentAlignment = Alignment.CenterStart) {
                            if (state.query.isEmpty()) Text(queryText, color = colors.mutedContent,
                                style = BraceTheme.typography.body)
                            inner()
                        }
                    },
                )
                LaunchedEffect(Unit) { queryFocus.requestFocus() }
            }
            if (activeOptions.isEmpty()) {
                Text(noResultsText, modifier = Modifier.padding(BraceTheme.spacing.md),
                    color = colors.mutedContent, style = BraceTheme.typography.body)
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.heightIn(max = metrics.maxListHeight).testTag("brace-select-list"),
                ) {
                    items(activeOptions, key = { it.key }) { option ->
                        val selected = option.key == selectedKey
                        val active = option.key == state.activeKey
                        val interaction = remember { MutableInteractionSource() }
                        val focused by interaction.collectIsFocusedAsState()
                        val hovered by interaction.collectIsHoveredAsState()
                        val shape = RoundedCornerShape(metrics.cornerRadius)
                        val background = when {
                            selected -> colors.selectedContainer
                            active || focused || hovered -> colors.activeContainer
                            else -> colors.container
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = BraceTheme.sizing.touchTarget)
                                .background(background, shape)
                                .then(if (focused || active) Modifier.border(
                                    if (focused) BraceTheme.sizing.focusRingWidth else BraceTheme.sizing.borderWidth,
                                    colors.focusRing, shape) else Modifier)
                                .focusRequester(optionFocus.getValue(option.key))
                                .clearAndSetSemantics {
                                    testTag = "brace-select-option-${option.key}"
                                    contentDescription = listOfNotNull(option.label, option.description)
                                        .joinToString(", ")
                                    role = Role.RadioButton
                                    this.selected = selected
                                    if (!option.enabled) disabled() else {
                                        this.focused = focused
                                        onClick { selectOption(option); true }
                                        requestFocus { optionFocus.getValue(option.key).requestFocus() }
                                    }
                                }
                                .selectable(
                                    selected = selected,
                                    enabled = option.enabled,
                                    role = Role.RadioButton,
                                    interactionSource = interaction,
                                    indication = null,
                                ) { selectOption(option) }
                                .onFocusChanged { if (it.isFocused) state.activeKey = option.key }
                                .padding(horizontal = metrics.optionHorizontalPadding,
                                    vertical = BraceTheme.spacing.sm),
                            verticalArrangement = Arrangement.spacedBy(BraceTheme.spacing.xxs),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(BraceTheme.spacing.sm)) {
                                Column(Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(BraceTheme.spacing.xxs)) {
                                    if (optionContent != null) optionContent(option) else {
                                        Text(option.label,
                                            color = if (!option.enabled) colors.disabledContent
                                            else if (selected) colors.selectedContent else colors.content,
                                            style = BraceTheme.typography.body)
                                        if (option.description != null) Text(option.description,
                                            color = if (option.enabled) colors.mutedContent else colors.disabledContent,
                                            style = BraceTheme.typography.label)
                                    }
                                }
                                if (selected) {
                                    Canvas(Modifier.size(BraceTheme.sizing.iconMd)
                                        .clearAndSetSemantics { }) {
                                        drawLine(colors.selectedContent,
                                            Offset(size.width * 0.18f, size.height * 0.51f),
                                            Offset(size.width * 0.43f, size.height * 0.73f),
                                            strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
                                        drawLine(colors.selectedContent,
                                            Offset(size.width * 0.43f, size.height * 0.73f),
                                            Offset(size.width * 0.84f, size.height * 0.24f),
                                            strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
                                    }
                                }
                            }
                        }
                    }
                }
                // Keep the keyboard-active option visible. In the no-search form, keep actual
                // focus on that row so Space and Enter activate the same choice.
                LaunchedEffect(effectiveExpanded, filterable, state.activeKey, activeOptions) {
                    if (effectiveExpanded) {
                        val active = state.activeKey?.takeIf { it in enabledKeys }
                            ?: enabledKeys.firstOrNull()
                        val index = activeOptions.indexOfFirst { it.key == active }
                        if (index >= 0) {
                            val visible = listState.layoutInfo.visibleItemsInfo
                            if (visible.none { it.index == index }) listState.scrollToItem(index)
                            if (!filterable) {
                                withFrameNanos { }
                                optionFocus[active]?.requestFocus()
                            }
                        }
                    }
                }
            }
        }
    }
}
