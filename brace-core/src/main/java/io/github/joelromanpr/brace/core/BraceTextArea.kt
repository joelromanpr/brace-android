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

/**
 * A controlled multiline input with a visible token-driven focus and validation state.
 *
 * [value] belongs to the caller and can be saved with `rememberSaveable`. [accessibilityLabel]
 * names the field in TalkBack even when a visible [BraceFormField] label is used. Supply a
 * localized [errorText] whenever [isError] is true. With [autoResize], the field grows from
 * [minLines] to [maxLines] as text wraps; its height is also capped at 60% of the window, after
 * which the text scrolls inside the field. Without [autoResize], the viewport stays at [minLines]
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
    val borderColor = when {
        isError -> input.errorBorder
        focused -> input.focusedBorder
        else -> input.border
    }
    val viewportMin = maxOf(BraceTheme.sizing.touchTarget, BraceTheme.densityTokens.controlHeightDp)
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
                .semantics {
                    contentDescription = accessibilityLabel
                    if (isError) error(errorText!!)
                },
            enabled = enabled,
            readOnly = readOnly,
            singleLine = false,
            minLines = minLines,
            maxLines = if (autoResize) maxLines else minLines,
            textStyle = BraceTheme.typography.body.copy(color = textColor),
            cursorBrush = SolidColor(semantic.primary),
            interactionSource = interactionSource,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier.padding(
                        horizontal = metrics.horizontalPadding,
                        vertical = BraceTheme.spacing.sm,
                    ),
                    contentAlignment = Alignment.TopStart,
                ) {
                    if (value.isEmpty() && !placeholder.isNullOrEmpty()) {
                        Text(
                            text = placeholder,
                            color = input.placeholder,
                            style = BraceTheme.typography.body,
                        )
                    }
                    innerTextField()
                }
            },
        )
    }
}
