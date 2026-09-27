package io.github.joelromanpr.brace.select

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import io.github.braceandroid.foundation.BraceTheme
import io.github.joelromanpr.brace.core.BraceMenuItem
import io.github.joelromanpr.brace.core.BracePopover
import io.github.joelromanpr.brace.core.BraceTag
import io.github.joelromanpr.brace.core.braceShortcutEditable

/**
 * A controlled multiple-choice field with removable tags and a filterable anchored list.
 *
 * [selectedKeys] is an ordered, unique list owned by the caller. Selecting an option toggles it
 * without closing the popup; removing a tag invokes [onSelectedKeysChange] independently. The
 * caller also controls [expanded]. [state] restores query and active option through activity
 * recreation. Up/Down/Home/End and Enter navigate/toggle enabled choices, Escape/Back and outside
 * touch dismiss, and each option and tag remove control supports touch, mouse, and TalkBack.
 * IME composition stays in the text editor; Enter and Search do not toggle a choice while a
 * candidate is composing. External changes to [state] query reset the editor to the new text.
 * All actions retain 48 dp targets, including compact density. Keys and labels must be stable and
 * localized. Options omitted after data refresh show their key as a fallback selected-tag label.
 */
@Composable
public fun <T> BraceMultiSelect(
    options: List<BraceSelectOption<T>>,
    selectedKeys: List<String>,
    onSelectedKeysChange: (List<String>) -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    state: BraceQueryListState = rememberBraceQueryListState(),
    enabled: Boolean = true,
    resetQueryOnSelect: Boolean = true,
    predicate: ((String, BraceSelectOption<T>) -> Boolean)? = null,
    queryLabel: String? = null,
    emptyLabel: String? = null,
) {
    val optionsByKey = options.associateBy { it.key }
    require(optionsByKey.size == options.size) { "BraceMultiSelect option keys must be unique" }
    require(selectedKeys.toSet().size == selectedKeys.size) { "BraceMultiSelect selected keys must be unique" }
    val colors = BraceTheme.colors.components.select
    val metrics = BraceTheme.componentMetrics.select
    val visible = state.filter(options, predicate)
    val enabledOptions = visible.filter { it.enabled }
    val enabledKeys = enabledOptions.map { it.key }
    val effectiveExpanded = expanded && enabled
    val searchFocus = remember { FocusRequester() }
    var queryField by remember(state) {
        mutableStateOf(TextFieldValue(state.query, TextRange(state.query.length)))
    }
    SideEffect {
        if (queryField.text != state.query) {
            queryField = TextFieldValue(state.query, TextRange(state.query.length))
        }
    }
    val queryText = queryLabel ?: stringResource(R.string.brace_select_search)
    val noResultsText = emptyLabel ?: stringResource(R.string.brace_select_empty)
    val countText = stringResource(R.string.brace_multi_selected_count, selectedKeys.size)

    LaunchedEffect(enabled, expanded) {
        if (!enabled && expanded) onExpandedChange(false)
    }
    LaunchedEffect(effectiveExpanded, enabledKeys) {
        if (effectiveExpanded && state.activeKey !in enabledKeys) state.activeKey = enabledKeys.firstOrNull()
    }
    fun toggle(key: String) {
        val updated = if (key in selectedKeys) selectedKeys.filterNot { it == key }
            else selectedKeys + key
        onSelectedKeysChange(updated)
        if (resetQueryOnSelect) state.query = ""
    }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(BraceTheme.spacing.xs)) {
        if (selectedKeys.isNotEmpty()) {
            FlowRow(Modifier.fillMaxWidth().testTag("brace-multi-tags"),
                horizontalArrangement = Arrangement.spacedBy(BraceTheme.spacing.xs),
                verticalArrangement = Arrangement.spacedBy(BraceTheme.spacing.xs)) {
                selectedKeys.forEach { key ->
                    val option = optionsByKey[key]
                    val tagLabel = option?.label ?: key
                    BraceTag(tagLabel,
                        enabled = enabled,
                        onRemove = if (enabled) ({ onSelectedKeysChange(selectedKeys.filterNot { it == key }) })
                            else null,
                        removeContentDescription = stringResource(R.string.brace_multi_remove, tagLabel),
                        modifier = Modifier.testTag("brace-multi-tag-$key"))
                }
            }
        }
        BracePopover(
            expanded = effectiveExpanded,
            onDismissRequest = { onExpandedChange(false) },
            title = label,
            modifier = Modifier.fillMaxWidth(),
            surfaceModifier = Modifier.widthIn(min = metrics.minWidth),
            target = {
                BraceSelectTrigger(
                    label = "$label: $countText",
                    onClick = { onExpandedChange(!expanded) },
                    enabled = enabled,
                    expanded = effectiveExpanded,
                    modifier = Modifier.testTag("brace-multi-trigger"),
                )
            },
        ) {
            val position = enabledKeys.indexOf(state.activeKey)
            val activeAnnouncement = if (position >= 0) stringResource(
                R.string.brace_select_active_position, position + 1, enabledKeys.size,
                enabledOptions[position].label,
            ) else null
            Column(
                modifier = Modifier.braceQueryNavigation(state, enabledKeys,
                    onActivate = { toggle(it) }, onDismiss = { onExpandedChange(false) },
                    isTextComposing = { queryField.composition != null }),
                verticalArrangement = Arrangement.spacedBy(BraceTheme.spacing.xs),
            ) {
                val interaction = remember { MutableInteractionSource() }
                val focused by interaction.collectIsFocusedAsState()
                val shape = RoundedCornerShape(metrics.cornerRadius)
                BasicTextField(
                    value = queryField,
                    onValueChange = { next ->
                        queryField = next
                        if (state.query != next.text) state.query = next.text
                    },
                    modifier = Modifier.fillMaxWidth()
                        .defaultMinSize(minHeight = BraceTheme.sizing.touchTarget)
                        .background(colors.searchContainer, shape)
                        .border(if (focused) BraceTheme.sizing.focusRingWidth else BraceTheme.sizing.borderWidth,
                            if (focused) colors.focusRing else colors.searchBorder, shape)
                        .focusRequester(searchFocus)
                        .braceShortcutEditable()
                        .semantics {
                            contentDescription = queryText
                            if (activeAnnouncement != null) stateDescription = activeAnnouncement
                        }
                        .testTag("brace-multi-query"),
                    singleLine = true,
                    textStyle = BraceTheme.typography.body.copy(color = colors.content),
                    cursorBrush = SolidColor(colors.focusRing),
                    interactionSource = interaction,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        if (queryField.composition == null) {
                            val key = state.activeKey?.takeIf { it in enabledKeys }
                                ?: enabledKeys.firstOrNull()
                            if (key != null) toggle(key)
                        }
                    }),
                    decorationBox = { inner ->
                        Box(Modifier.padding(horizontal = metrics.searchHorizontalPadding,
                            vertical = BraceTheme.spacing.sm)) {
                            if (state.query.isEmpty()) Text(queryText, color = colors.mutedContent,
                                style = BraceTheme.typography.body)
                            inner()
                        }
                    },
                )
                LaunchedEffect(Unit) { searchFocus.requestFocus() }
                if (visible.isEmpty()) {
                    Text(noResultsText, Modifier.padding(BraceTheme.spacing.md),
                        color = colors.mutedContent, style = BraceTheme.typography.body)
                } else {
                    LazyColumn(Modifier.weight(1f, fill = false).heightIn(max = metrics.maxListHeight)
                        .testTag("brace-multi-list")) {
                        items(visible, key = { it.key }) { option ->
                            BraceMenuItem(
                                label = option.label,
                                endLabel = option.description,
                                enabled = option.enabled,
                                selected = option.key in selectedKeys,
                                active = option.key == state.activeKey,
                                modifier = Modifier
                                    .onFocusChanged { if (it.hasFocus) state.activeKey = option.key }
                                    .testTag("brace-multi-option-${option.key}"),
                                onClick = { state.activeKey = option.key; toggle(option.key) },
                            )
                        }
                    }
                }
            }
        }
    }
}
