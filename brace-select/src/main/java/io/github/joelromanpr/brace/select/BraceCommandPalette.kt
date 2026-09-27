package io.github.joelromanpr.brace.select

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import io.github.braceandroid.foundation.BraceTheme
import io.github.joelromanpr.brace.core.BraceButton
import io.github.joelromanpr.brace.core.BraceButtonIntent
import io.github.joelromanpr.brace.core.BraceButtonVariant
import io.github.joelromanpr.brace.core.BraceMenuItem
import io.github.joelromanpr.brace.core.BraceOverlay
import io.github.joelromanpr.brace.core.braceShortcutEditable

/** A keyed action in [BraceCommandPalette]. Keys must be unique and stable across restoration. */
public data class BraceCommand<T>(
    val key: String,
    val value: T,
    val label: String,
    val group: String? = null,
    val description: String? = null,
    val shortcut: String? = null,
    val enabled: Boolean = true,
) {
    init {
        require(key.isNotBlank()) { "Command key must not be blank" }
        require(label.isNotBlank()) { "Command label must not be blank" }
        require(group == null || group.isNotBlank()) { "Command group must not be blank" }
    }
}

/**
 * A controlled Android command dialog adapted from Blueprint's Omnibar.
 *
 * The caller owns [open] and closes it in [onOpenChange]. [state] saves the query and active
 * command across activity recreation; supply stable command keys. Commands are grouped in the
 * order their first group appears, with ungrouped commands in their own section. The query
 * searches labels and descriptions by default; [predicate] can search domain fields. Disabled
 * commands remain visible but cannot execute. [selectedKey] optionally marks a checked command;
 * [loading] replaces the result list with a spoken
 * progress state. The query keeps focus while arrows/Home/End move the active command and Enter
 * executes it. IME composition retains Enter for candidate confirmation; Escape, Back, and an
 * outside tap request dismissal. Touch, mouse, and TalkBack activate the same command action.
 *
 * Supply [restoreFocusTo] for a caller-owned trigger when deterministic focus restoration is
 * required. The search field and every command use token colors and at least 48 dp targets.
 * There is no animation, so reduced-motion settings need no separate transition override.
 * Localize the title and optional labels in the host app.
 *
 * @see <a href="https://blueprintjs.com/docs/#select/omnibar">Blueprint Omnibar documentation</a>
 */
@Composable
public fun <T> BraceCommandPalette(
    commands: List<BraceCommand<T>>,
    open: Boolean,
    onOpenChange: (Boolean) -> Unit,
    onExecute: (BraceCommand<T>) -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    state: BraceQueryListState = rememberBraceQueryListState(),
    loading: Boolean = false,
    selectedKey: String? = null,
    predicate: ((String, BraceCommand<T>) -> Boolean)? = null,
    queryLabel: String? = null,
    emptyLabel: String? = null,
    loadingLabel: String? = null,
    closeLabel: String? = null,
    restoreFocusTo: FocusRequester? = null,
    resetQueryOnExecute: Boolean = true,
) {
    require(title.isNotBlank()) { "Command palette title must not be blank" }
    require(queryLabel == null || queryLabel.isNotBlank()) { "Query label must not be blank" }
    require(emptyLabel == null || emptyLabel.isNotBlank()) { "Empty label must not be blank" }
    require(loadingLabel == null || loadingLabel.isNotBlank()) { "Loading label must not be blank" }
    require(closeLabel == null || closeLabel.isNotBlank()) { "Close label must not be blank" }
    require(commands.map { it.key }.toSet().size == commands.size) { "Command keys must be unique" }
    val searchText = queryLabel ?: stringResource(R.string.brace_command_search)
    val emptyText = emptyLabel ?: stringResource(R.string.brace_command_empty)
    val loadingText = loadingLabel ?: stringResource(R.string.brace_command_loading)
    val closeText = closeLabel ?: stringResource(R.string.brace_command_close)
    val activeText = stringResource(R.string.brace_command_active)
    val colors = BraceTheme.colors.components.select
    val metrics = BraceTheme.componentMetrics.select
    val queryFocus = remember { FocusRequester() }
    val hostView = LocalView.current
    var wasOpen by remember { mutableStateOf(open) }
    var queryField by remember(state) {
        mutableStateOf(TextFieldValue(state.query, TextRange(state.query.length)))
    }
    SideEffect {
        if (queryField.text != state.query) queryField = TextFieldValue(state.query, TextRange(state.query.length))
    }
    val term = state.query.trim()
    val filtered = if (term.isEmpty()) commands else commands.filter { command ->
        predicate?.invoke(term, command)
            ?: (command.label.contains(term, ignoreCase = true) ||
                command.description?.contains(term, ignoreCase = true) == true)
    }
    val grouped = filtered.groupBy { it.group }
    val displayOrder = grouped.values.flatten()
    val enabledKeys = if (loading) emptyList() else displayOrder.filter { it.enabled }.map { it.key }
    val listState = rememberLazyListState()
    val indexByKey = remember(grouped) {
        buildMap<String, Int> {
            var index = 0
            grouped.forEach { (group, groupCommands) ->
                if (group != null) index++
                groupCommands.forEach { command -> put(command.key, index++) }
            }
        }
    }

    LaunchedEffect(open) {
        if (open) {
            wasOpen = true
        } else if (wasOpen) {
            wasOpen = false
            if (restoreFocusTo != null) {
                // A native Dialog window detaches asynchronously. Wait until the host
                // window owns keyboard focus before returning it to the caller's trigger.
                for (attempt in 0 until 60) {
                    withFrameNanos { }
                    if (hostView.hasWindowFocus()) break
                }
                restoreFocusTo.requestFocus()
            }
        }
    }
    LaunchedEffect(open, loading, enabledKeys) {
        if (open && state.activeKey !in enabledKeys) state.activeKey = enabledKeys.firstOrNull()
    }
    LaunchedEffect(open, state.activeKey, indexByKey) {
        if (open) state.activeKey?.let(indexByKey::get)?.let { listState.scrollToItem(it) }
    }
    fun execute(key: String) {
        val command = displayOrder.firstOrNull { it.key == key && it.enabled } ?: return
        state.activeKey = command.key
        onExecute(command)
        if (resetQueryOnExecute) state.query = ""
        onOpenChange(false)
    }

    BraceOverlay(
        open = open,
        onDismissRequest = { onOpenChange(false) },
        title = title,
        modifier = modifier,
    ) {
        val dialogFocusManager = LocalFocusManager.current
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = LocalConfiguration.current.screenHeightDp.dp * 0.82f)
                .padding(BraceTheme.spacing.md)
                .braceQueryNavigation(
                    state = state,
                    enabledKeys = enabledKeys,
                    onActivate = ::execute,
                    onDismiss = {
                        dialogFocusManager.clearFocus(force = true)
                        onOpenChange(false)
                    },
                    isTextComposing = { queryField.composition != null },
                )
                .testTag("brace-command-palette"),
            verticalArrangement = Arrangement.spacedBy(BraceTheme.densityTokens.itemGapDp),
        ) {
            Text(title, Modifier.fillMaxWidth().semantics { heading() },
                color = colors.content, style = BraceTheme.typography.title)
            val focusManager = dialogFocusManager
            val interaction = remember { MutableInteractionSource() }
            val focused by interaction.collectIsFocusedAsState()
            val shape = RoundedCornerShape(metrics.cornerRadius)
            val activePosition = enabledKeys.indexOf(state.activeKey)
            val activeAnnouncement = if (activePosition >= 0) stringResource(
                R.string.brace_select_active_position, activePosition + 1, enabledKeys.size,
                displayOrder.first { it.key == state.activeKey }.label,
            ) else null
            BasicTextField(
                value = queryField,
                onValueChange = { next ->
                    queryField = next
                    if (state.query != next.text) state.query = next.text
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = BraceTheme.sizing.touchTarget)
                    .background(colors.searchContainer, shape)
                    .border(
                        if (focused) BraceTheme.sizing.focusRingWidth else BraceTheme.sizing.borderWidth,
                        if (focused) colors.focusRing else colors.searchBorder,
                        shape,
                    )
                    .focusRequester(queryFocus)
                    .onPreviewKeyEvent { event ->
                        if (event.type == KeyEventType.KeyDown && event.key == Key.Tab &&
                            queryField.composition == null) {
                            focusManager.moveFocus(
                                if (event.isShiftPressed) FocusDirection.Previous else FocusDirection.Next,
                            )
                            true
                        } else false
                    }
                    .braceShortcutEditable()
                    .semantics {
                        contentDescription = searchText
                        if (activeAnnouncement != null) stateDescription = activeAnnouncement
                    }
                    .testTag("brace-command-query"),
                singleLine = true,
                textStyle = BraceTheme.typography.body.copy(color = colors.content),
                cursorBrush = SolidColor(colors.focusRing),
                interactionSource = interaction,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    if (queryField.composition == null) {
                        (state.activeKey?.takeIf { it in enabledKeys } ?: enabledKeys.firstOrNull())
                            ?.let(::execute)
                    }
                }),
                decorationBox = { inner ->
                    Box(Modifier.padding(horizontal = metrics.searchHorizontalPadding,
                        vertical = BraceTheme.spacing.sm)) {
                        if (state.query.isEmpty()) Text(searchText, color = colors.mutedContent,
                            style = BraceTheme.typography.body)
                        inner()
                    }
                },
            )
            LaunchedEffect(Unit) { queryFocus.requestFocus() }
            when {
                loading -> Text(loadingText, Modifier.fillMaxWidth()
                    .defaultMinSize(minHeight = BraceTheme.sizing.touchTarget)
                    .semantics {
                        liveRegion = LiveRegionMode.Polite
                        progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
                    }
                    .testTag("brace-command-loading"),
                    color = colors.mutedContent, style = BraceTheme.typography.body)
                displayOrder.isEmpty() -> Text(emptyText, Modifier.fillMaxWidth()
                    .defaultMinSize(minHeight = BraceTheme.sizing.touchTarget)
                    .semantics { liveRegion = LiveRegionMode.Polite }
                    .testTag("brace-command-empty"),
                    color = colors.mutedContent, style = BraceTheme.typography.body)
                else -> LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f, fill = false)
                        .heightIn(max = metrics.maxListHeight)
                        .testTag("brace-command-list"),
                ) {
                    grouped.forEach { (group, itemsInGroup) ->
                        if (group != null) item(key = "group:$group") {
                            Text(group, Modifier.fillMaxWidth()
                                .padding(horizontal = BraceTheme.spacing.sm,
                                    vertical = BraceTheme.spacing.xs)
                                .semantics { heading() }
                                .testTag("brace-command-group-$group"),
                                color = colors.mutedContent, style = BraceTheme.typography.label)
                        }
                        items(itemsInGroup, key = { "command:${it.key}" }) { command ->
                            val details = listOfNotNull(command.description, command.shortcut)
                            BraceMenuItem(
                                label = command.label,
                                endLabel = details.joinToString(" · ").ifEmpty { null },
                                accessibilityLabel = listOfNotNull(
                                    command.label, command.description, command.group,
                                    command.shortcut,
                                ).joinToString(", "),
                                enabled = command.enabled,
                                selected = if (selectedKey == null) null else command.key == selectedKey,
                                active = command.key == state.activeKey,
                                multiline = true,
                                modifier = Modifier
                                    .onFocusChanged { if (it.hasFocus && command.enabled) state.activeKey = command.key }
                                    .semantics {
                                        if (command.key == state.activeKey) stateDescription = activeText
                                    }
                                    .testTag("brace-command-${command.key}"),
                                onClick = { execute(command.key) },
                            )
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                BraceButton(closeText, onClick = { onOpenChange(false) },
                    intent = BraceButtonIntent.Secondary, variant = BraceButtonVariant.Outline,
                    modifier = Modifier.testTag("brace-command-close"))
            }
        }
    }
}
