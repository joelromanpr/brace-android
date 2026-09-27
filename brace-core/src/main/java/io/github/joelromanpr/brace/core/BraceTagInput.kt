package io.github.joelromanpr.brace.core

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.LayoutDirection
import io.github.braceandroid.foundation.BraceTheme
import java.util.Locale

/** The native interaction which submitted one or more tag values. */
public enum class BraceTagInputAddMethod { Separator, Paste, Keyboard, Ime, Blur }

/** Policy for comparing a proposed value with existing and newly submitted tags. */
public enum class BraceTagDuplicatePolicy { Allow, RejectExact, RejectIgnoreCase }

/** Why a proposed tag was rejected. The draft stays available for correction. */
public enum class BraceTagInputRejection { Invalid, Duplicate }

/**
 * A controlled, wrapping list of editable tag values.
 *
 * Hoist both [values] and [draft] with `rememberSaveable` to restore them after process recreation.
 * Enter or the IME Done action submits the draft. Commas and line breaks submit completed values;
 * a bulk insertion containing separators (including Android paste) submits the whole batch when
 * [addOnPaste] is true. A bulk insertion remains draft text when it is false. An
 * insertion without a separator stays editable. [separators] accepts individual delimiter characters.
 * Values are trimmed, empty pieces are discarded, and each batch is atomic: a validator or duplicate
 * rejection keeps the entire draft for correction and calls [onRejected]. [duplicatePolicy] defaults
 * to [BraceTagDuplicatePolicy.Allow], matching Blueprint. The optional [onTagsAdded] and
 * [onTagRemoved] callbacks report actions; [onValuesChange] remains the source of truth.
 *
 * Active IME composition is kept in the draft and is not treated as a delimiter or hardware key
 * submission until composition commits. A focused empty draft lets Left/Right (mirrored in RTL)
 * select tags. Backspace selects the last tag, then removes it on the next press; Delete removes
 * only a selected tag. Each tag also has its
 * own 48 dp remove action for touch, mouse, keyboard, and TalkBack. [readOnly] retains text selection
 * and tag reading while disabling mutation. The visible label is a pointer target to focus the input
 * without adding a redundant keyboard stop. [supportingText] and validation errors are announced.
 *
 * Compose text editing replaces Blueprint's HTML input, DOM auto-resize and React child/props APIs.
 * The field wraps as space or font scale changes and uses Brace input and tag tokens. A paste of a
 * single value without a separator remains draft text so the user can edit it first.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
public fun BraceTagInput(
    values: List<String>,
    onValuesChange: (List<String>) -> Unit,
    draft: String,
    onDraftChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    supportingText: String? = null,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    isError: Boolean = false,
    separators: String = ",\n\r",
    addOnBlur: Boolean = false,
    addOnPaste: Boolean = true,
    duplicatePolicy: BraceTagDuplicatePolicy = BraceTagDuplicatePolicy.Allow,
    validator: (String) -> Boolean = { true },
    onRejected: ((String, BraceTagInputRejection) -> Unit)? = null,
    onTagsAdded: ((List<String>, BraceTagInputAddMethod) -> Unit)? = null,
    onTagRemoved: ((String, Int) -> Unit)? = null,
    tagIntent: BraceTagIntent = BraceTagIntent.Default,
    tagSize: BraceTagSize = BraceTagSize.Medium,
    keyboardOptions: KeyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
) {
    val context = LocalContext.current
    val layoutDirection = LocalLayoutDirection.current
    val input = BraceTheme.colors.components.input
    val semantic = BraceTheme.colors.semantic
    val metrics = BraceTheme.componentMetrics.input
    val focusRequester = remember { FocusRequester() }
    var hasFocus by remember { mutableStateOf(false) }
    var selectedIndex by rememberSaveable { mutableIntStateOf(-1) }
    var localError by rememberSaveable { mutableStateOf<String?>(null) }
    var localErrorDraft by rememberSaveable { mutableStateOf<String?>(null) }
    var field by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(draft, TextRange(draft.length)))
    }
    LaunchedEffect(draft) {
        if (field.text != draft) field = TextFieldValue(draft, TextRange(draft.length))
    }
    val visibleLocalError = localError.takeIf { draft == localErrorDraft }
    val effectiveError = isError || visibleLocalError != null
    val helper = visibleLocalError ?: supportingText
    val tint = if (enabled) input.content else input.disabledContent
    val border = when {
        effectiveError -> input.errorBorder
        hasFocus -> input.focusedBorder
        else -> input.border
    }
    val shape = RoundedCornerShape(metrics.cornerRadius)
    val countDescription = context.resources.getQuantityString(R.plurals.brace_tag_input_count, values.size, values.size)
    val selectionDescription = values.getOrNull(selectedIndex)?.let {
        context.getString(R.string.brace_tag_input_selected, it)
    }

    fun duplicate(candidate: String, previous: List<String>): Boolean = when (duplicatePolicy) {
        BraceTagDuplicatePolicy.Allow -> false
        BraceTagDuplicatePolicy.RejectExact -> previous.any { it == candidate }
        BraceTagDuplicatePolicy.RejectIgnoreCase -> previous.any {
            it.lowercase(Locale.ROOT) == candidate.lowercase(Locale.ROOT)
        }
    }

    fun submit(text: String, complete: Boolean, method: BraceTagInputAddMethod) {
        if (!enabled || readOnly || text.isEmpty()) return
        val pieces = if (separators.isEmpty()) listOf(text) else text.split(*separators.toCharArray())
        val proposed = (if (complete) pieces else pieces.dropLast(1)).map(String::trim).filter(String::isNotEmpty)
        val remainder = if (complete || separators.isEmpty()) "" else pieces.last()
        if (proposed.isEmpty()) {
            if (complete || remainder != text) onDraftChange(remainder)
            return
        }
        val seen = values.toMutableList()
        val rejected = proposed.mapNotNull { candidate ->
            val reason = when {
                !validator(candidate) -> BraceTagInputRejection.Invalid
                duplicate(candidate, seen) -> BraceTagInputRejection.Duplicate
                else -> null
            }
            if (reason == null) {
                seen += candidate
                null
            } else candidate to reason
        }
        if (rejected.isNotEmpty()) {
            rejected.forEach { (value, reason) -> onRejected?.invoke(value, reason) }
            localErrorDraft = text
            localError = context.getString(
                if (rejected.first().second == BraceTagInputRejection.Duplicate)
                    R.string.brace_tag_input_duplicate else R.string.brace_tag_input_invalid,
                rejected.first().first,
            )
            onDraftChange(text)
            return
        }
        selectedIndex = -1
        localError = null
        localErrorDraft = null
        onValuesChange(seen.toList())
        onTagsAdded?.invoke(proposed, method)
        onDraftChange(remainder)
    }

    fun remove(index: Int) {
        if (!enabled || readOnly || index !in values.indices) return
        val removed = values[index]
        onValuesChange(values.filterIndexed { i, _ -> i != index })
        onTagRemoved?.invoke(removed, index)
        selectedIndex = (index - 1).coerceAtLeast(-1)
    }

    LaunchedEffect(values.size) {
        if (selectedIndex >= values.size) selectedIndex = values.lastIndex
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(BraceTheme.spacing.sm)) {
        Text(
            text = label,
            color = tint,
            style = BraceTheme.typography.label,
            modifier = (if (enabled) Modifier
                .heightIn(min = BraceTheme.sizing.touchTarget)
                .pointerInput(focusRequester) { detectTapGestures { focusRequester.requestFocus() } }
            else Modifier)
                .clearAndSetSemantics {},
        )
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = maxOf(BraceTheme.sizing.touchTarget, BraceTheme.densityTokens.controlHeightDp))
                .background(if (enabled) input.container else input.disabledContainer, shape)
                .border(if (hasFocus) BraceTheme.sizing.focusRingWidth else metrics.borderWidth, border, shape)
                .pointerInput(enabled, readOnly, focusRequester) {
                    if (enabled) detectTapGestures { focusRequester.requestFocus() }
                }
                .onFocusChanged { state ->
                    if (addOnBlur && hasFocus && !state.hasFocus && draft.isNotBlank()) {
                        submit(draft, complete = true, method = BraceTagInputAddMethod.Blur)
                    }
                    hasFocus = state.hasFocus
                }
                .padding(horizontal = metrics.horizontalPadding, vertical = BraceTheme.spacing.xxs),
            horizontalArrangement = Arrangement.spacedBy(BraceTheme.spacing.xs),
            verticalArrangement = Arrangement.spacedBy(BraceTheme.spacing.xxs),
        ) {
            values.forEachIndexed { index, value ->
                BraceTag(
                    label = value,
                    intent = tagIntent,
                    size = tagSize,
                    multiline = true,
                    selected = index == selectedIndex,
                    enabled = enabled,
                    onRemove = if (readOnly) null else ({ remove(index) }),
                    removeContentDescription = context.getString(R.string.brace_tag_input_remove, value),
                )
            }
            BasicTextField(
                value = field,
                onValueChange = { next ->
                    val hadComposition = field.composition != null
                    field = next
                    if (!enabled || readOnly) return@BasicTextField
                    selectedIndex = -1
                    localError = null
                    localErrorDraft = null
                    val nextText = next.text
                    val insertion = insertedText(draft, nextText)
                    if (next.composition != null || separators.isEmpty()) {
                        onDraftChange(nextText)
                    } else if (insertion.none { it in separators } &&
                        !(hadComposition && nextText.any { it in separators })) {
                        onDraftChange(nextText)
                    } else {
                        val bulk = insertion.length > 1
                        if (bulk && !addOnPaste) onDraftChange(nextText)
                        else submit(
                            nextText,
                            complete = bulk,
                            method = if (bulk) BraceTagInputAddMethod.Paste else BraceTagInputAddMethod.Separator,
                        )
                    }
                },
                modifier = Modifier
                    .widthIn(min = BraceTheme.sizing.touchTarget * 2)
                    .weight(1f)
                    .heightIn(min = BraceTheme.sizing.touchTarget)
                    .focusRequester(focusRequester)
                    .then(if (enabled && !readOnly) Modifier.braceShortcutEditable() else Modifier)
                    .onPreviewKeyEvent { event ->
                        if (!enabled || readOnly || field.composition != null || event.type != KeyEventType.KeyDown ||
                            event.isAltPressed || event.isCtrlPressed || event.isMetaPressed) return@onPreviewKeyEvent false
                        when (event.key) {
                            Key.Enter -> {
                                if (draft.isNotBlank()) {
                                    submit(draft, complete = true, method = BraceTagInputAddMethod.Keyboard)
                                    true
                                } else false
                            }
                            Key.Backspace -> {
                                if (draft.isNotEmpty() || values.isEmpty()) false
                                else {
                                    if (selectedIndex in values.indices) remove(selectedIndex)
                                    else selectedIndex = values.lastIndex
                                    true
                                }
                            }
                            Key.Delete -> {
                                if (draft.isEmpty() && selectedIndex in values.indices) {
                                    remove(selectedIndex)
                                    true
                                } else false
                            }
                            Key.DirectionLeft, Key.DirectionRight -> {
                                if (draft.isNotEmpty() || values.isEmpty()) false
                                else {
                                    val previous = if (layoutDirection == LayoutDirection.Ltr)
                                        event.key == Key.DirectionLeft else event.key == Key.DirectionRight
                                    if (!previous && selectedIndex < 0) false
                                    else {
                                        selectedIndex = if (previous) {
                                            if (selectedIndex < 0) values.lastIndex else (selectedIndex - 1).coerceAtLeast(0)
                                        } else {
                                            if (selectedIndex >= values.lastIndex) -1 else selectedIndex + 1
                                        }
                                        true
                                    }
                                }
                            }
                            else -> false
                        }
                    }
                    .semantics {
                        contentDescription = label
                        stateDescription = listOfNotNull(
                            countDescription,
                            selectionDescription,
                            helper?.takeIf(String::isNotBlank),
                        ).joinToString(". ")
                        if (effectiveError) error(helper ?: context.getString(R.string.brace_tag_input_error))
                    },
                enabled = enabled,
                readOnly = readOnly,
                singleLine = true,
                textStyle = BraceTheme.typography.body.copy(color = tint),
                cursorBrush = SolidColor(semantic.primary),
                keyboardOptions = keyboardOptions,
                keyboardActions = KeyboardActions(onDone = {
                    if (field.composition == null) {
                        submit(draft, complete = true, method = BraceTagInputAddMethod.Ime)
                    }
                }),
                decorationBox = { inner ->
                    Box(
                        modifier = Modifier.padding(horizontal = BraceTheme.spacing.xs),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        if (draft.isEmpty() && placeholder.isNotEmpty()) {
                            Text(placeholder, color = input.placeholder, style = BraceTheme.typography.body)
                        }
                        inner()
                    }
                },
            )
        }
        if (helper != null) {
            Text(
                text = helper,
                color = if (effectiveError) semantic.danger else semantic.onSurfaceMuted,
                style = BraceTheme.typography.label,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
    }
}

private fun insertedText(before: String, after: String): String {
    val prefix = before.commonPrefixWith(after).length
    val oldTail = before.substring(prefix)
    val newTail = after.substring(prefix)
    val suffix = oldTail.commonSuffixWith(newTail).length
    return newTail.dropLast(suffix)
}
