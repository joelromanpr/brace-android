package io.github.joelromanpr.brace.table

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.CollectionItemInfo
import androidx.compose.ui.semantics.collectionItemInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import io.github.braceandroid.foundation.BraceTheme

/**
 * An active, single-line table cell editor. [value] is the confirmed value; the draft is
 * saveable until [onCommit] or [onCancel] closes the editor. [validate] returns a user-facing
 * error, or null to accept a draft. A rejected draft stays in the field for correction.
 *
 * The editor requests keyboard focus when composed. Enter and IME Done commit; Escape cancels.
 * An unfinished IME composition is allowed to consume Enter before Brace commits. The input
 * keeps a 48dp minimum touch target. Android exposes the [label] as a nearby context
 * node and retains the native editable text node with validation and save/cancel actions.
 * Screen readers encounter the row/column context before the active text field.
 * Optional zero-based [rowIndex] and [columnIndex] preserve grid collection coordinates.
 * Callers own data updates and should remove this editor after either callback. In a
 * [BraceDataTable], pass the same stable cell key to [BraceDataTable.editingCell].
 */
@Composable
fun BraceEditableCell(
    value: String,
    label: String,
    onCommit: (String) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    validate: (String) -> String? = { null },
    rowIndex: Int? = null,
    columnIndex: Int? = null,
) {
    require((rowIndex == null && columnIndex == null) ||
        (rowIndex != null && rowIndex >= 0 && columnIndex != null && columnIndex >= 0)) {
        "Row and column indices must both be nonnegative or absent"
    }
    require(label.isNotBlank()) { "Editable cell label must not be blank" }
    var draft by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(value, TextRange(value.length)))
    }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var finishing by remember { mutableStateOf(false) }
    val requester = remember { FocusRequester() }
    val colors = BraceTheme.colors.components.table
    val semantic = BraceTheme.colors.semantic
    val metrics = BraceTheme.componentMetrics.table
    val saveLabel = stringResource(R.string.brace_table_save_edit)
    val cancelLabel = stringResource(R.string.brace_table_cancel_edit)
    val genericError = stringResource(R.string.brace_table_invalid_value)

    fun commit(): Boolean {
        if (finishing) return false
        // Input methods can send Enter while a marked candidate is still being composed.
        if (draft.composition != null) return false
        val problem = validate(draft.text)
        if (problem != null) {
            errorMessage = problem.ifBlank { genericError }
            return false
        }
        errorMessage = null
        finishing = true
        onCommit(draft.text)
        return true
    }
    fun cancel(): Boolean {
        if (finishing) return false
        finishing = true
        onCancel()
        return true
    }

    LaunchedEffect(Unit) { requester.requestFocus() }
    Column(
        modifier.background(colors.editorBackground)
            .border(BraceTheme.sizing.focusRingWidth,
                if (errorMessage == null) colors.editorBorder else colors.editorErrorBorder)
            .padding(horizontal = metrics.cellHorizontalPadding, vertical = BraceTheme.spacing.xxs),
        verticalArrangement = Arrangement.Center,
    ) {
        BasicTextField(
            value = draft,
            onValueChange = { draft = it; errorMessage = null },
            modifier = Modifier.fillMaxWidth().heightIn(min = BraceTheme.sizing.touchTarget)
                .focusRequester(requester).testTag("brace-editable-cell-input")
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when (event.key) {
                        Key.Enter, Key.NumPadEnter -> if (draft.composition == null) {
                            commit(); true
                        } else false
                        Key.Escape -> cancel()
                        else -> false
                    }
                }
                .semantics(mergeDescendants = true) {
                    contentDescription = label
                    if (rowIndex != null && columnIndex != null) {
                        collectionItemInfo = CollectionItemInfo(rowIndex + 1, 1, columnIndex + 1, 1)
                    }
                    errorMessage?.let { error(it) }
                    customActions = listOf(
                        CustomAccessibilityAction(saveLabel) { commit() },
                        CustomAccessibilityAction(cancelLabel) { cancel() },
                    )
                },
            singleLine = true,
            textStyle = BraceTheme.typography.body.copy(color = colors.editorText),
            cursorBrush = SolidColor(semantic.focusRing),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { commit() }),
            decorationBox = { field ->
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) { field() }
            },
        )
        errorMessage?.let { message ->
            Text(message, modifier = Modifier.clearAndSetSemantics {},
                color = colors.editorErrorBorder, style = BraceTheme.typography.caption,
                maxLines = 1)
        }
    }
}
