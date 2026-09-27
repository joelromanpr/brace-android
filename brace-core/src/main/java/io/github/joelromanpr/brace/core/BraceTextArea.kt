package io.github.joelromanpr.brace.core

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.braceandroid.foundation.BraceTheme

/** Size of a text-area viewport and its type scale. */
public enum class BraceTextAreaSize { Small, Medium, Large }

/**
 * A controlled multiline input with a visible token-driven focus and validation state.
 *
 * [value] belongs to the caller and can be saved with `rememberSaveable`. [accessibilityLabel]
 * names the field in TalkBack even when a visible [BraceFormField] label is used. An enabled,
 * writable field automatically suppresses unrelated [BraceShortcutRegistry] commands. [intent]
 * selects a visual border role; [size] selects token-driven type, padding, and minimum height.
 * Supply a localized [errorText] whenever [isError] is true. It is announced through field
 * semantics; show the same message visibly in a surrounding [BraceFormField] or sibling text.
 * With [autoResize], the field grows
 * from [minLines] to [maxLines] as text wraps, then stops at 60% of Android's reported screen
 * height. Longer text scrolls inside the field. Without [autoResize], the viewport stays at [minLines]
 * while the text scrolls. The caller can set [keyboardOptions] and [keyboardActions] for an IME.
 *
 * Blueprint's HTML textarea ref, manual browser resize handle, and async React control mode
 * have no separate Compose API. Compose state hoisting and a bounded scrolling viewport replace
 * those mechanisms.
 */
@Composable
public fun BraceTextArea(
    value: String,
    onValueChange: (String) -> Unit,
    accessibilityLabel: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    isError: Boolean = false,
    errorText: String? = null,
    minLines: Int = 3,
    maxLines: Int = 8,
    autoResize: Boolean = false,
    intent: BraceFormIntent = BraceFormIntent.Default,
    size: BraceTextAreaSize = BraceTextAreaSize.Medium,
    fill: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    require(accessibilityLabel.isNotBlank()) { "Text area accessibility label must not be blank" }
    require(minLines >= 1) { "minLines must be at least 1" }
    require(maxLines >= minLines) { "maxLines must not be smaller than minLines" }
    require(!isError || !errorText.isNullOrBlank()) {
        "A localized errorText is needed when isError is true"
    }
    val input = BraceTheme.colors.components.input
    val semantic = BraceTheme.colors.semantic
    val metrics = BraceTheme.componentMetrics.input
    val shape = RoundedCornerShape(metrics.cornerRadius)
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val textColor = if (enabled) input.content else input.disabledContent
    val intentBorder = when (intent) {
        BraceFormIntent.Default -> input.border
        BraceFormIntent.Primary -> semantic.primary
        BraceFormIntent.Success -> semantic.success
        BraceFormIntent.Warning -> semantic.warning
        BraceFormIntent.Danger -> semantic.danger
    }
    val borderColor = when {
        isError -> input.errorBorder
        !enabled -> input.border
        focused -> input.focusedBorder
        intent != BraceFormIntent.Default -> intentBorder
        else -> input.border
    }
    val textStyle = when (size) {
        BraceTextAreaSize.Small -> BraceTheme.typography.label
        BraceTextAreaSize.Medium -> BraceTheme.typography.body
        BraceTextAreaSize.Large -> BraceTheme.typography.subtitle
    }
    val horizontalPadding = when (size) {
        BraceTextAreaSize.Small -> BraceTheme.spacing.sm
        BraceTextAreaSize.Medium -> metrics.horizontalPadding
        BraceTextAreaSize.Large -> metrics.horizontalPadding + BraceTheme.spacing.sm
    }
    val verticalPadding = when (size) {
        BraceTextAreaSize.Small -> BraceTheme.spacing.xs
        BraceTextAreaSize.Medium -> BraceTheme.spacing.sm
        BraceTextAreaSize.Large -> BraceTheme.spacing.md
    }
    val viewportMin = maxOf(
        BraceTheme.sizing.touchTarget + if (size == BraceTextAreaSize.Large) BraceTheme.spacing.md else 0.dp,
        BraceTheme.densityTokens.controlHeightDp,
    )
    val viewportMax = maxOf(viewportMin, LocalConfiguration.current.screenHeightDp.dp * 0.6f)
    val selectionColors = remember(input.selection, semantic.primary) {
        TextSelectionColors(handleColor = semantic.primary, backgroundColor = input.selection)
    }

    CompositionLocalProvider(LocalTextSelectionColors provides selectionColors) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier
                .then(if (fill) Modifier.fillMaxWidth() else Modifier)
                .heightIn(min = viewportMin, max = viewportMax)
                .clip(shape)
                .background(if (enabled) input.container else input.disabledContainer)
                .border(
                    if (focused) BraceTheme.sizing.focusRingWidth else metrics.borderWidth,
                    borderColor,
                    shape,
                )
                .then(if (enabled && !readOnly) Modifier.braceShortcutEditable() else Modifier)
                .semantics {
                    contentDescription = accessibilityLabel
                    if (isError) error(errorText!!)
                },
            enabled = enabled,
            readOnly = readOnly,
            singleLine = false,
            minLines = minLines,
            maxLines = if (autoResize) maxLines else minLines,
            textStyle = textStyle.copy(color = textColor),
            cursorBrush = SolidColor(semantic.primary),
            interactionSource = interactionSource,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier.padding(
                        horizontal = horizontalPadding,
                        vertical = verticalPadding,
                    ),
                    contentAlignment = Alignment.TopStart,
                ) {
                    if (value.isEmpty() && !placeholder.isNullOrEmpty()) {
                        Text(
                            text = placeholder,
                            color = input.placeholder,
                            style = textStyle,
                        )
                    }
                    innerTextField()
                }
            },
        )
    }
}
