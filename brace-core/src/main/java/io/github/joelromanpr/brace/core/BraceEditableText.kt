package io.github.joelromanpr.brace.core

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import io.github.braceandroid.foundation.BraceTheme

/** Visual intent for an inline editable text value. */
public enum class BraceEditableTextIntent { Default, Primary, Success, Warning, Danger }

/**
 * Inline text which becomes an input when tapped or reached by keyboard focus.
 *
 * [value] and [onValueChange] own the live text. A saveable draft and the value at edit entry
 * are retained so Escape can restore the last confirmed text. Confirm runs on Enter, IME Done,
 * or blur. In multiline mode Enter inserts a line and Control/Meta+Enter confirms; setting
 * [confirmOnEnterKey] reverses these two shortcuts. Confirm and cancel callbacks run only when
 * the draft changed, matching the pinned Blueprint 6.18.0 user-facing contract.
 *
 * [isEditing] optionally controls display/edit mode. When non-null, update it from
 * [onEditingChange]; when null the component saves mode itself. Explicit confirm/cancel returns
 * focus to the displayed value. Blur leaves focus on the newly focused control.
 * [editActionLabel] is the TalkBack action name and should be localized by the caller.
 *
 * Android shows an input-token affordance without relying on web hover. A Compose text field
 * replaces Blueprint's DOM input and textarea. The experimental web alwaysRenderInput option
 * maps to using BraceTextField or BraceTextArea instead. DOM attributes and HTML input types
 * have no separate Compose component.
 */
@Composable
public fun BraceEditableText(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    enabled: Boolean = true,
    multiline: Boolean = false,
    minLines: Int = 1,
    maxLines: Int = Int.MAX_VALUE,
    maxLength: Int? = null,
    confirmOnEnterKey: Boolean = false,
    selectAllOnFocus: Boolean = false,
    isEditing: Boolean? = null,
    onEditingChange: (Boolean) -> Unit = {},
    onConfirm: (String) -> Unit = {},
    onCancel: (String) -> Unit = {},
    intent: BraceEditableTextIntent = BraceEditableTextIntent.Default,
    supportingText: String? = null,
    errorMessage: String? = null,
    editActionLabel: String = "Edit",
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    require(label.isNotBlank()) { "Editable text label must not be blank" }
    require(editActionLabel.isNotBlank()) { "Edit action label must not be blank" }
    require(minLines >= 1 && maxLines >= minLines) { "Line bounds must satisfy 1 <= minLines <= maxLines" }
    require(maxLength == null || maxLength >= 0) { "maxLength must be nonnegative" }

    var internalEditing by rememberSaveable { mutableStateOf(false) }
    val editing = enabled && (isEditing ?: internalEditing)
    var draft by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(value))
    }
    var confirmedAtEntry by rememberSaveable { mutableStateOf(value) }
    var priorExternalValue by remember { mutableStateOf(value) }
    var priorEditing by remember { mutableStateOf(false) }
    var editSnapshotInitialized by rememberSaveable { mutableStateOf(false) }
    var openingRequested by remember { mutableStateOf(false) }
    var finishing by remember { mutableStateOf(false) }
    var restoreFocusOnClose by remember { mutableStateOf(false) }
    var suppressDisplayFocusOpen by remember { mutableStateOf(false) }
    var editorHadFocus by remember { mutableStateOf(false) }
    var displayFocused by remember { mutableStateOf(false) }
    var editorFocused by remember { mutableStateOf(false) }
    val displayRequester = remember { FocusRequester() }
    val editorRequester = remember { FocusRequester() }

    fun beginEditing() {
        if (!enabled || editing || finishing || openingRequested) return
        openingRequested = true
        editSnapshotInitialized = true
        confirmedAtEntry = value
        draft = TextFieldValue(
            text = value,
            selection = if (selectAllOnFocus) TextRange(0, value.length) else TextRange(value.length),
        )
        editorHadFocus = false
        restoreFocusOnClose = false
        if (isEditing == null) internalEditing = true
        onEditingChange(true)
    }

    fun confirm(restoreFocus: Boolean) {
        if (!editing || finishing) return
        finishing = true
        restoreFocusOnClose = restoreFocus
        val finalValue = draft.text
        if (finalValue != confirmedAtEntry) onConfirm(finalValue)
        confirmedAtEntry = finalValue
        if (isEditing == null) internalEditing = false
        onEditingChange(false)
    }

    fun cancel() {
        if (!editing || finishing) return
        finishing = true
        restoreFocusOnClose = true
        if (draft.text != confirmedAtEntry) {
            draft = TextFieldValue(confirmedAtEntry, TextRange(confirmedAtEntry.length))
            onValueChange(confirmedAtEntry)
            onCancel(confirmedAtEntry)
        }
        if (isEditing == null) internalEditing = false
        onEditingChange(false)
    }

    fun insertNewline() {
        val start = draft.selection.min
        val end = draft.selection.max
        val newText = draft.text.replaceRange(start, end, "\n")
        if (maxLength != null && newText.length > maxLength) return
        draft = TextFieldValue(newText, TextRange(start + 1))
        onValueChange(newText)
    }

    LaunchedEffect(value, editing) {
        if (value != priorExternalValue) {
            priorExternalValue = value
            if (!editing || value != draft.text) {
                draft = TextFieldValue(value, TextRange(value.length))
            }
        } else if (!editing && draft.text != value) {
            draft = TextFieldValue(value, TextRange(value.length))
        }
    }
    LaunchedEffect(editing) {
        if (editing) {
            openingRequested = false
            if (isEditing != null && !editSnapshotInitialized) {
                confirmedAtEntry = value
                draft = TextFieldValue(
                    value,
                    if (selectAllOnFocus) TextRange(0, value.length) else TextRange(value.length),
                )
                editSnapshotInitialized = true
            }
            editorRequester.requestFocus()
        } else if (priorEditing) {
            val shouldRestoreFocus = restoreFocusOnClose
            restoreFocusOnClose = false
            finishing = false
            editSnapshotInitialized = false
            openingRequested = false
            if (shouldRestoreFocus) {
                withFrameNanos { }
                suppressDisplayFocusOpen = true
                displayRequester.requestFocus()
            }
        }
        priorEditing = editing
    }
    LaunchedEffect(editorFocused, editing) {
        if (!editorFocused && editorHadFocus && editing && !finishing) {
            // Wait a frame so a disappearing composition does not commit a draft on rotation.
            withFrameNanos { }
            if (!editorFocused && editing && !finishing) confirm(restoreFocus = false)
        }
    }

    val semantic = BraceTheme.colors.semantic
    val input = BraceTheme.colors.components.input
    val metrics = BraceTheme.componentMetrics.input
    val shape = RoundedCornerShape(metrics.cornerRadius)
    val intentBorder = when (intent) {
        BraceEditableTextIntent.Default -> input.border
        BraceEditableTextIntent.Primary -> semantic.primary
        BraceEditableTextIntent.Success -> semantic.success
        BraceEditableTextIntent.Warning -> semantic.warning
        BraceEditableTextIntent.Danger -> semantic.danger
    }
    val focusedBorder = if (intent == BraceEditableTextIntent.Default) input.focusedBorder else intentBorder
    val textColor = if (enabled) input.content else input.disabledContent
    val detail = errorMessage ?: supportingText
    val hasError = errorMessage != null

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(BraceTheme.spacing.sm)) {
        if (editing) {
            val imeAction = if (keyboardOptions.imeAction != ImeAction.Unspecified) {
                keyboardOptions.imeAction
            } else if (multiline && !confirmOnEnterKey) {
                ImeAction.Default
            } else {
                ImeAction.Done
            }
            CompositionLocalProvider(
                LocalTextSelectionColors provides TextSelectionColors(
                    handleColor = semantic.primary,
                    backgroundColor = input.selection,
                ),
            ) {
                BasicTextField(
                    value = draft,
                    onValueChange = { next ->
                        if (maxLength == null || next.text.length <= maxLength) {
                            draft = next
                            onValueChange(next.text)
                        }
                    },
                    modifier = Modifier
                        .focusRequester(editorRequester)
                        .onFocusChanged { focus ->
                            editorFocused = focus.isFocused
                            if (focus.isFocused) {
                                editorHadFocus = true
                            }
                        }
                        .onPreviewKeyEvent { event ->
                            if (event.type != KeyEventType.KeyDown || draft.composition != null) {
                                false
                            } else when (event.key) {
                                Key.Escape -> {
                                    cancel()
                                    true
                                }
                                Key.Enter -> {
                                    val mod = event.isCtrlPressed || event.isMetaPressed
                                    when {
                                        !multiline -> { confirm(restoreFocus = true); true }
                                        confirmOnEnterKey && mod -> { insertNewline(); true }
                                        confirmOnEnterKey || mod -> { confirm(restoreFocus = true); true }
                                        else -> false
                                    }
                                }
                                else -> false
                            }
                        }
                        .widthIn(min = BraceTheme.sizing.touchTarget)
                        .heightIn(min = maxOf(BraceTheme.sizing.touchTarget, BraceTheme.densityTokens.controlHeightDp))
                        .background(input.container, shape)
                        .border(
                            if (editorFocused) BraceTheme.sizing.focusRingWidth else metrics.borderWidth,
                            if (editorFocused) focusedBorder else intentBorder,
                            shape,
                        )
                        .semantics {
                            contentDescription = label
                            if (hasError) error(errorMessage)
                        },
                    enabled = enabled,
                    singleLine = !multiline,
                    minLines = if (multiline) minLines else 1,
                    maxLines = if (multiline) maxLines else 1,
                    textStyle = BraceTheme.typography.body.copy(color = textColor),
                    cursorBrush = SolidColor(semantic.primary),
                    keyboardOptions = keyboardOptions.copy(imeAction = imeAction),
                    keyboardActions = KeyboardActions(onDone = { confirm(restoreFocus = true) }),
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier.padding(
                                horizontal = metrics.horizontalPadding,
                                vertical = BraceTheme.spacing.sm,
                            ),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            if (draft.text.isEmpty() && placeholder.isNotEmpty()) {
                                Text(placeholder, color = input.placeholder, style = BraceTheme.typography.body)
                            }
                            innerTextField()
                        }
                    },
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .focusRequester(displayRequester)
                    .onFocusChanged { focus ->
                        displayFocused = focus.isFocused
                        if (!focus.isFocused) {
                            suppressDisplayFocusOpen = false
                        } else if (suppressDisplayFocusOpen) {
                            suppressDisplayFocusOpen = false
                        } else if (enabled) {
                            beginEditing()
                        }
                    }
                    .widthIn(min = BraceTheme.sizing.touchTarget)
                    .heightIn(min = maxOf(BraceTheme.sizing.touchTarget, BraceTheme.densityTokens.controlHeightDp))
                    .background(if (enabled) input.container else input.disabledContainer, shape)
                    .border(
                        if (displayFocused) BraceTheme.sizing.focusRingWidth else metrics.borderWidth,
                        if (displayFocused) focusedBorder else intentBorder,
                        shape,
                    )
                    .clickable(enabled = enabled, onClickLabel = editActionLabel, onClick = ::beginEditing)
                    .semantics(mergeDescendants = true) {
                        contentDescription = label
                        if (hasError) error(errorMessage)
                    }
                    .padding(horizontal = metrics.horizontalPadding, vertical = BraceTheme.spacing.sm),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(
                    text = value.ifEmpty { placeholder.ifEmpty { label } },
                    color = if (value.isEmpty()) input.placeholder else textColor,
                    style = BraceTheme.typography.body,
                    maxLines = if (multiline) maxLines else 1,
                )
            }
        }
        if (detail != null) {
            Text(
                text = detail,
                color = if (hasError) semantic.danger else semantic.onSurfaceMuted,
                style = BraceTheme.typography.label,
            )
        }
    }
}

