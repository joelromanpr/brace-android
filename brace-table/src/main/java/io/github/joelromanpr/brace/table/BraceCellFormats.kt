package io.github.joelromanpr.brace.table

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Dialog
import io.github.braceandroid.foundation.BraceTheme
import org.json.JSONArray
import org.json.JSONObject
import java.util.IdentityHashMap

/** When a [BraceTruncatedCell] offers its full-value reveal action. */
public enum class BraceRevealMode { WhenTruncated, Always, Never }

/**
 * A compact formatter for long text, usable independently or as [BraceTableColumn.cellContent].
 *
 * Supply a bounded width through [modifier] so the preview can measure visual overflow.
 * [maxCharacters] limits Unicode code points before adding [suffix]; zero disables this limit.
 * [maxLines] and Compose text layout detect actual visual overflow at the current width, font
 * scale and theme. This replaces Blueprint's DOM measurement and approximate-size modes.
 * [revealMode] controls the dedicated 48 dp action. Revealing opens a Brace-token-styled dialog
 * with selectable full text; Back, outside touch, Escape or Close dismiss it. An uncontrolled
 * dialog state is saveable. If [expanded] is passed, [onExpandedChange] must update it and save
 * it in the host as needed. The preview always exposes the complete [value] to TalkBack.
 *
 * In [BraceDataTable], pass the full string as [BraceTableColumn.cellText] so the table's single
 * accessibility cell node can speak it. Set [revealMode] to [BraceRevealMode.Never] inside
 * `cellContent`: the data table owns pointer selection and suppresses nested semantics. Place
 * this formatter outside the table when a separate reveal action is required.
 */
@Composable
public fun BraceTruncatedCell(
    value: String,
    modifier: Modifier = Modifier,
    maxCharacters: Int = 2000,
    maxLines: Int = 1,
    suffix: String = "...",
    revealMode: BraceRevealMode = BraceRevealMode.WhenTruncated,
    expanded: Boolean? = null,
    onExpandedChange: ((Boolean) -> Unit)? = null,
    preformatted: Boolean = false,
    textStyle: TextStyle? = null,
    textColor: Color? = null,
) {
    require(maxCharacters >= 0) { "maxCharacters must be nonnegative" }
    require(maxLines > 0) { "maxLines must be positive" }
    require(expanded == null || onExpandedChange != null) {
        "Controlled BraceTruncatedCell needs onExpandedChange"
    }
    val semantic = BraceTheme.colors.semantic
    val popover = BraceTheme.colors.components.popover
    val metrics = BraceTheme.componentMetrics.popover
    val shape = RoundedCornerShape(metrics.cornerRadius)
    val style = textStyle ?: BraceTheme.typography.body
    val resolvedStyle = if (preformatted) style.copy(textDirection = TextDirection.Ltr) else style
    val codePointCount = value.codePointCount(0, value.length)
    val charTruncated = maxCharacters > 0 && codePointCount > maxCharacters
    val visible = BraceTruncatedFormatter.preview(value, maxCharacters, suffix)
    var layoutOverflow by remember(value, maxCharacters, maxLines, textStyle, preformatted) { mutableStateOf(false) }
    var internalExpanded by rememberSaveable(value) { mutableStateOf(false) }
    val isExpanded = (expanded ?: internalExpanded) && revealMode != BraceRevealMode.Never
    val showReveal = when (revealMode) {
        BraceRevealMode.Always -> true
        BraceRevealMode.Never -> false
        BraceRevealMode.WhenTruncated -> charTruncated || layoutOverflow
    }
    val trigger = remember { FocusRequester() }
    var hadExpanded by remember { mutableStateOf(false) }
    LaunchedEffect(isExpanded, showReveal) {
        if (isExpanded) hadExpanded = true
        else if (hadExpanded) {
            if (showReveal) trigger.requestFocus()
            hadExpanded = false
        }
    }
    fun requestExpanded(next: Boolean) {
        if (expanded == null) internalExpanded = next
        onExpandedChange?.invoke(next)
    }
    val revealLabel = stringResource(R.string.brace_table_show_full_value)
    val closeLabel = stringResource(R.string.brace_table_close_full_value)
    val revealTitle = stringResource(R.string.brace_table_full_value)
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val pressed by interaction.collectIsPressedAsState()
    var focused by remember { mutableStateOf(false) }
    val actionBackground = when {
        pressed -> semantic.primaryPressed
        hovered -> semantic.primaryHover
        else -> semantic.primarySubtle
    }
    val actionContent = if (pressed || hovered) semantic.onPrimary else semantic.onPrimarySubtle

    Row(modifier.semantics { isTraversalGroup = true },
        horizontalArrangement = Arrangement.spacedBy(BraceTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically) {
        Text(visible, modifier = Modifier.weight(1f).clearAndSetSemantics {
            contentDescription = value
        }.testTag("brace-truncated-preview"),
            color = textColor ?: semantic.onSurface, style = resolvedStyle, maxLines = maxLines,
            softWrap = !preformatted, overflow = TextOverflow.Ellipsis,
            onTextLayout = { result ->
                if (layoutOverflow != result.hasVisualOverflow) layoutOverflow = result.hasVisualOverflow
            })
        if (showReveal) {
            Box(Modifier.defaultMinSize(minWidth = BraceTheme.sizing.touchTarget,
                minHeight = BraceTheme.sizing.touchTarget)
                .background(actionBackground, shape)
                .then(if (focused) Modifier.border(BraceTheme.sizing.focusRingWidth,
                    semantic.focusRing, shape) else Modifier)
                .focusRequester(trigger)
                .onFocusChanged { focused = it.isFocused }
                .clickable(role = Role.Button, interactionSource = interaction, indication = null,
                    onClickLabel = revealLabel) { requestExpanded(true) }
                .semantics { contentDescription = revealLabel }
                .testTag("brace-truncated-reveal"),
                contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.brace_table_more), color = actionContent,
                    style = BraceTheme.typography.label,
                    modifier = Modifier.padding(horizontal = BraceTheme.spacing.sm)
                        .clearAndSetSemantics { })
            }
        }
    }

    if (isExpanded) {
        Dialog(onDismissRequest = { requestExpanded(false) }) {
            val closeRequester = remember { FocusRequester() }
            var closeFocused by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { closeRequester.requestFocus() }
            Column(Modifier.fillMaxWidth().widthIn(max = metrics.maxWidth)
                .heightIn(max = BraceTheme.sizing.overlayMaxWidth)
                .background(popover.container, shape)
                .border(BraceTheme.sizing.borderWidth, popover.border, shape)
                .padding(metrics.contentPadding)
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown && event.key == Key.Escape) {
                        requestExpanded(false)
                        true
                    } else false
                }
                .semantics { isTraversalGroup = true }
                .testTag("brace-truncated-dialog"),
                verticalArrangement = Arrangement.spacedBy(BraceTheme.spacing.sm)) {
                Text(revealTitle, color = popover.content, style = BraceTheme.typography.subtitle,
                    modifier = Modifier.semantics { heading() })
                Box(Modifier.weight(1f, fill = false).fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .horizontalScroll(rememberScrollState())) {
                    SelectionContainer {
                        Text(value,
                            modifier = Modifier.padding(vertical = BraceTheme.spacing.xs)
                                .testTag("brace-truncated-full-value"),
                            color = popover.content, style = resolvedStyle,
                            softWrap = !preformatted)
                    }
                }
                Box(Modifier.align(Alignment.End)
                    .defaultMinSize(minWidth = BraceTheme.sizing.touchTarget,
                        minHeight = BraceTheme.sizing.touchTarget)
                    .background(semantic.primarySubtle, shape)
                    .then(if (closeFocused) Modifier.border(BraceTheme.sizing.focusRingWidth,
                        semantic.focusRing, shape) else Modifier)
                    .focusRequester(closeRequester)
                    .onFocusChanged { closeFocused = it.isFocused }
                    .clickable(role = Role.Button, onClickLabel = closeLabel) {
                        requestExpanded(false)
                    }
                    .semantics { contentDescription = closeLabel }
                    .testTag("brace-truncated-close"),
                    contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.brace_table_close), color = semantic.onPrimarySubtle,
                        style = BraceTheme.typography.label,
                        modifier = Modifier.padding(horizontal = BraceTheme.spacing.md)
                            .clearAndSetSemantics { })
                }
            }
        }
    }
}

/** Exact Unicode-code-point preview used by [BraceTruncatedCell]. */
public object BraceTruncatedFormatter {
    /** [maxCharacters] zero disables clipping; [suffix] is appended only when clipped. */
    public fun preview(value: String, maxCharacters: Int = 2000, suffix: String = "..."): String {
        require(maxCharacters >= 0) { "maxCharacters must be nonnegative" }
        if (maxCharacters == 0 || value.codePointCount(0, value.length) <= maxCharacters) return value
        return value.substring(0, value.offsetByCodePoints(0, maxCharacters)) + suffix
    }
}

/**
 * A JSON value rendered with two-space indentation by default, then passed through
 * [BraceTruncatedCell]. JSON strings omit quotes by default like Blueprint `JSONFormat`.
 * Null is shown as `null` and never opens a reveal dialog. Use [BraceJsonFormatter.format]
 * for the full value passed to [BraceTableColumn.cellText].
 */
@Composable
public fun BraceJsonCell(
    value: Any?,
    modifier: Modifier = Modifier,
    omitQuotesOnStrings: Boolean = true,
    indent: Int = 2,
    maxCharacters: Int = 2000,
    maxLines: Int = 1,
    revealMode: BraceRevealMode = BraceRevealMode.WhenTruncated,
    expanded: Boolean? = null,
    onExpandedChange: ((Boolean) -> Unit)? = null,
) {
    val formatted = remember(value, omitQuotesOnStrings, indent) {
        BraceJsonFormatter.format(value, omitQuotesOnStrings, indent)
    }
    BraceTruncatedCell(formatted, modifier, maxCharacters, maxLines,
        revealMode = if (value == null || value === JSONObject.NULL) BraceRevealMode.Never else revealMode,
        expanded = expanded, onExpandedChange = onExpandedChange,
        preformatted = true, textStyle = BraceTheme.typography.code,
        textColor = if (value == null || value === JSONObject.NULL)
            BraceTheme.colors.semantic.onSurfaceMuted else null)
}

/** Deterministic JSON formatting for null, strings, numbers, booleans, maps, lists and Android JSON values. */
public object BraceJsonFormatter {
    /** Format a JSON-compatible [value]; reject cycles, non-string map keys and non-finite numbers. */
    public fun format(value: Any?, omitQuotesOnStrings: Boolean = true, indent: Int = 2): String {
        require(indent in 0..8) { "indent must be between 0 and 8" }
        if (omitQuotesOnStrings && value is String) return value
        val output = StringBuilder()
        appendJson(value, output, 0, indent, IdentityHashMap())
        return output.toString()
    }
}

private fun appendJson(value: Any?, out: StringBuilder, depth: Int, indent: Int,
    ancestors: IdentityHashMap<Any, Boolean>) {
    require(depth <= 128) { "JSON nesting exceeds 128 levels" }
    when {
        value == null || value === JSONObject.NULL -> out.append("null")
        value is String -> appendJsonString(value, out)
        value is Boolean -> out.append(value)
        value is Number -> {
            require((value !is Double || value.isFinite()) && (value !is Float || value.isFinite())) {
                "JSON numbers must be finite"
            }
            out.append(value)
        }
        value is JSONObject -> appendObject(value.keys().asSequence().map { it to value.opt(it) }.toList(),
            value, out, depth, indent, ancestors)
        value is Map<*, *> -> appendObject(value.entries.map { entry ->
            require(entry.key is String) { "JSON object keys must be strings" }
            entry.key as String to entry.value
        }, value, out, depth, indent, ancestors)
        value is JSONArray -> appendArray((0 until value.length()).map { value.opt(it) },
            value, out, depth, indent, ancestors)
        value is List<*> -> appendArray(value, value, out, depth, indent, ancestors)
        value is Array<*> -> appendArray(value.asList(), value, out, depth, indent, ancestors)
        else -> throw IllegalArgumentException("Unsupported JSON value: ${value?.javaClass?.name ?: "null"}")
    }
}

private fun appendObject(entries: List<Pair<String, Any?>>, owner: Any, out: StringBuilder,
    depth: Int, indent: Int, ancestors: IdentityHashMap<Any, Boolean>) {
    require(ancestors.put(owner, true) == null) { "Cyclic JSON object" }
    out.append('{')
    entries.forEachIndexed { index, (key, value) ->
        if (index > 0) out.append(',')
        newline(out, depth + 1, indent)
        appendJsonString(key, out)
        out.append(if (indent == 0) ":" else ": ")
        appendJson(value, out, depth + 1, indent, ancestors)
    }
    if (entries.isNotEmpty()) newline(out, depth, indent)
    out.append('}')
    ancestors.remove(owner)
}

private fun appendArray(values: List<Any?>, owner: Any, out: StringBuilder,
    depth: Int, indent: Int, ancestors: IdentityHashMap<Any, Boolean>) {
    require(ancestors.put(owner, true) == null) { "Cyclic JSON array" }
    out.append('[')
    values.forEachIndexed { index, value ->
        if (index > 0) out.append(',')
        newline(out, depth + 1, indent)
        appendJson(value, out, depth + 1, indent, ancestors)
    }
    if (values.isNotEmpty()) newline(out, depth, indent)
    out.append(']')
    ancestors.remove(owner)
}

private fun newline(out: StringBuilder, depth: Int, indent: Int) {
    if (indent > 0) {
        out.append('\n')
        repeat(depth * indent) { out.append(' ') }
    }
}

private fun appendJsonString(value: String, out: StringBuilder) {
    out.append('"')
    var index = 0
    while (index < value.length) {
        val character = value[index]
        when {
            character == '"' -> out.append("\\\"")
            character == '\\' -> out.append("\\\\")
            character == '\b' -> out.append("\\b")
            character == '\u000c' -> out.append("\\f")
            character == '\n' -> out.append("\\n")
            character == '\r' -> out.append("\\r")
            character == '\t' -> out.append("\\t")
            character.isHighSurrogate() && index + 1 < value.length &&
                value[index + 1].isLowSurrogate() -> {
                out.append(character).append(value[index + 1])
                index++
            }
            character.code < 0x20 || character.isSurrogate() -> {
                out.append("\\u")
                out.append(character.code.toString(16).padStart(4, '0'))
            }
            else -> out.append(character)
        }
        index++
    }
    out.append('"')
}
