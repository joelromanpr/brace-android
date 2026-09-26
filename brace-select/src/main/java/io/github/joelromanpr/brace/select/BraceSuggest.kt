package io.github.joelromanpr.brace.select

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import io.github.braceandroid.foundation.BraceTheme
import io.github.joelromanpr.brace.core.BraceButton
import io.github.joelromanpr.brace.core.BraceButtonIntent
import io.github.joelromanpr.brace.core.BraceButtonVariant
import io.github.joelromanpr.brace.core.BraceMenuItem
import io.github.joelromanpr.brace.core.BracePopover
import io.github.joelromanpr.brace.core.braceShortcutEditable

/**
 * A suggestion editor that preserves free text and IME composition.
 *
 * [value] is a controlled [TextFieldValue], including cursor selection and composition. Tapping
 * the displayed value opens an anchored Android editor with a filterable list; closing the popup
 * or using Done preserves the free text. [onSelect] is called only for an explicit suggestion,
 * and the caller decides whether to replace the text. [expanded] and the selected key are also
 * caller-controlled and can be saved with screen state. Arrow keys navigate enabled suggestions,
 * Enter selects one when available and the IME is not composing, or keeps unmatched free text.
 * During composition Enter passes to the IME and can close the editor without selecting.
 * Escape/Back dismisses, and touch/TalkBack can
 * activate each option. The popup uses 48 dp targets and returns focus to its trigger on close.
 */
@Composable
public fun <T> BraceSuggest(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    options: List<BraceSelectOption<T>>,
    selectedKey: String?,
    onSelect: (BraceSelectOption<T>) -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    state: BraceQueryListState = rememberBraceQueryListState(),
    enabled: Boolean = true,
    placeholder: String? = null,
    queryLabel: String? = null,
    emptyLabel: String? = null,
    predicate: ((String, BraceSelectOption<T>) -> Boolean)? = null,
) {
    require(options.map { it.key }.toSet().size == options.size) { "BraceSuggest option keys must be unique" }
    val colors = BraceTheme.colors.components.select
    val metrics = BraceTheme.componentMetrics.select
    val query = value.text.trim()
    val visible = if (query.isEmpty()) options else options.filter { option ->
        predicate?.invoke(query, option)
            ?: (option.label.contains(query, ignoreCase = true) ||
                option.description?.contains(query, ignoreCase = true) == true)
    }
    val enabledOptions = visible.filter { it.enabled }
    val enabledKeys = enabledOptions.map { it.key }
    val effectiveExpanded = expanded && enabled
    val searchFocus = remember { FocusRequester() }
    val searchLabel = queryLabel ?: stringResource(R.string.brace_select_search)
    val placeholderText = placeholder ?: stringResource(R.string.brace_select_choose)
    val noResultsText = emptyLabel ?: stringResource(R.string.brace_select_empty)
    val doneLabel = stringResource(R.string.brace_suggest_done)

    LaunchedEffect(enabled, expanded) {
        if (!enabled && expanded) onExpandedChange(false)
    }
    LaunchedEffect(effectiveExpanded, enabledKeys, selectedKey) {
        if (effectiveExpanded && state.activeKey !in enabledKeys) {
            state.activeKey = if (selectedKey in enabledKeys) selectedKey else enabledKeys.firstOrNull()
        }
    }
    fun chooseActive() {
        val chosen = enabledOptions.firstOrNull { it.key == state.activeKey }
            ?: enabledOptions.firstOrNull()
        if (chosen != null) {
            onSelect(chosen)
            onExpandedChange(false)
        }
    }

    BracePopover(
        expanded = effectiveExpanded,
        onDismissRequest = { onExpandedChange(false) },
        title = label,
        modifier = modifier.fillMaxWidth(),
        surfaceModifier = Modifier.widthIn(min = metrics.minWidth),
        target = {
            BraceSelectTrigger(
                label = "$label: ${value.text.ifBlank { placeholderText }}",
                onClick = { onExpandedChange(!expanded) },
                enabled = enabled,
                expanded = effectiveExpanded,
                modifier = Modifier.testTag("brace-suggest-trigger"),
            )
        },
    ) {
        val position = enabledKeys.indexOf(state.activeKey)
        val activeAnnouncement = if (position >= 0) stringResource(
            R.string.brace_select_active_position, position + 1, enabledKeys.size,
            enabledOptions[position].label,
        ) else null
        Column(
            verticalArrangement = Arrangement.spacedBy(BraceTheme.spacing.xs),
        ) {
            val interaction = remember { MutableInteractionSource() }
            val focused by interaction.collectIsFocusedAsState()
            val shape = RoundedCornerShape(metrics.cornerRadius)
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth()
                    .onPreviewKeyEvent { event ->
                        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                        when (event.key) {
                            Key.DirectionDown -> { state.moveActive(enabledKeys, 1); true }
                            Key.DirectionUp -> { state.moveActive(enabledKeys, -1); true }
                            Key.Enter, Key.NumPadEnter -> if (value.composition == null) {
                                if (enabledOptions.isEmpty()) onExpandedChange(false) else chooseActive()
                                true
                            } else false
                            else -> false
                        }
                    }
                    .defaultMinSize(minHeight = BraceTheme.sizing.touchTarget)
                    .background(colors.searchContainer, shape)
                    .border(if (focused) BraceTheme.sizing.focusRingWidth else BraceTheme.sizing.borderWidth,
                        if (focused) colors.focusRing else colors.searchBorder, shape)
                    .focusRequester(searchFocus)
                    .braceShortcutEditable()
                    .semantics {
                        contentDescription = searchLabel
                        if (activeAnnouncement != null) stateDescription = activeAnnouncement
                    }
                    .testTag("brace-suggest-query"),
                singleLine = true,
                textStyle = BraceTheme.typography.body.copy(color = colors.content),
                cursorBrush = SolidColor(colors.focusRing),
                interactionSource = interaction,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onExpandedChange(false) }),
                decorationBox = { inner ->
                    Box(Modifier.padding(horizontal = metrics.searchHorizontalPadding,
                        vertical = BraceTheme.spacing.sm)) {
                        if (value.text.isEmpty()) Text(searchLabel, color = colors.mutedContent,
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
                    .testTag("brace-suggest-list")) {
                    items(visible, key = { it.key }) { option ->
                        BraceMenuItem(
                            label = option.label,
                            endLabel = option.description,
                            enabled = option.enabled,
                            selected = option.key == selectedKey,
                            active = option.key == state.activeKey,
                            modifier = Modifier
                                .onFocusChanged { if (it.hasFocus) state.activeKey = option.key }
                                .testTag("brace-suggest-option-${option.key}"),
                            onClick = {
                                state.activeKey = option.key
                                onSelect(option)
                                onExpandedChange(false)
                            },
                        )
                    }
                }
            }
            BraceButton(doneLabel, onClick = { onExpandedChange(false) },
                intent = BraceButtonIntent.Secondary, variant = BraceButtonVariant.Outline,
                modifier = Modifier.testTag("brace-suggest-done"))
        }
    }
}
