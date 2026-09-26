package io.github.joelromanpr.brace.core

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import io.github.braceandroid.foundation.BraceTheme

/**
 * A labeled, single-line editable field. Its value is hoisted for state restoration.
 * [supportingText] also supplies the accessibility error message when [isError] is true.
 */
@Composable
public fun BraceTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    supportingText: String? = null,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    isError: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val semantic = BraceTheme.colors.semantic
    val input = BraceTheme.colors.components.input
    val metrics = BraceTheme.componentMetrics.input
    val textColor = if (enabled) input.content else input.disabledContent
    val borderColor = when {
        isError -> input.errorBorder
        focused -> input.focusedBorder
        else -> input.border
    }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(BraceTheme.spacing.sm)) {
        Text(label, color = textColor, style = BraceTheme.typography.label)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = maxOf(BraceTheme.sizing.touchTarget, BraceTheme.densityTokens.controlHeightDp))
                .background(if (enabled) input.container else input.disabledContainer, RoundedCornerShape(metrics.cornerRadius))
                .border(if (focused) BraceTheme.sizing.focusRingWidth else metrics.borderWidth, borderColor, RoundedCornerShape(metrics.cornerRadius))
                .semantics {
                    contentDescription = label
                    if (isError) error(supportingText ?: "Invalid value")
                },
            enabled = enabled,
            readOnly = readOnly,
            singleLine = true,
            textStyle = BraceTheme.typography.body.copy(color = textColor),
            cursorBrush = SolidColor(semantic.primary),
            interactionSource = interactionSource,
            keyboardOptions = keyboardOptions,
            visualTransformation = visualTransformation,
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier.padding(horizontal = metrics.horizontalPadding, vertical = BraceTheme.spacing.sm),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (value.isEmpty() && placeholder.isNotEmpty()) {
                        Text(placeholder, color = input.placeholder, style = BraceTheme.typography.body)
                    }
                    innerTextField()
                }
            },
        )
        if (supportingText != null) {
            Text(
                text = supportingText,
                color = if (isError) semantic.danger else semantic.onSurface,
                style = BraceTheme.typography.label,
            )
        }
    }
}

/** A labeled binary choice with a 48dp or larger touch target and checkbox semantics. */
@Composable
public fun BraceCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val semantic = BraceTheme.colors.semantic
    val checkbox = BraceTheme.colors.components.checkbox
    val metrics = BraceTheme.componentMetrics.checkbox
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    Row(
        modifier = modifier
            .defaultMinSize(minHeight = BraceTheme.sizing.touchTarget)
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Checkbox,
                interactionSource = interactionSource,
                indication = null,
                onValueChange = onCheckedChange,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(BraceTheme.sizing.touchTarget)
                .then(if (focused) Modifier.border(BraceTheme.sizing.focusRingWidth, checkbox.focusRing, RoundedCornerShape(metrics.focusCornerRadius)) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.size(metrics.size)) {
                val stroke = metrics.strokeWidth.toPx()
                drawRoundRect(
                    color = if (!enabled) checkbox.disabledContainer else if (checked) checkbox.checkedContainer else checkbox.uncheckedContainer,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(metrics.cornerRadius.toPx()),
                )
                if (checked) {
                    val path = Path().apply {
                        moveTo(size.width * 0.2f, size.height * 0.5f)
                        lineTo(size.width * 0.43f, size.height * 0.72f)
                        lineTo(size.width * 0.8f, size.height * 0.28f)
                    }
                    drawPath(path, if (enabled) checkbox.checkmark else checkbox.disabledContent, style = Stroke(width = stroke))
                } else {
                    drawRoundRect(
                        color = if (enabled) checkbox.uncheckedBorder else checkbox.disabledContent,
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(metrics.cornerRadius.toPx()),
                        style = Stroke(width = stroke),
                    )
                }
            }
        }
        Spacer(Modifier.width(BraceTheme.spacing.sm))
        Text(label, color = if (enabled) semantic.onSurface else semantic.disabledContent, style = BraceTheme.typography.body)
    }
}

/** A labeled on/off setting with switch semantics and a minimum accessible target. */
@Composable
public fun BraceSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val semantic = BraceTheme.colors.semantic
    val switch = BraceTheme.colors.components.switch
    val metrics = BraceTheme.componentMetrics.switch
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    Row(
        modifier = modifier
            .defaultMinSize(minHeight = BraceTheme.sizing.touchTarget)
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                interactionSource = interactionSource,
                indication = null,
                onValueChange = onCheckedChange,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(width = BraceTheme.sizing.touchTarget, height = BraceTheme.sizing.touchTarget)
                .then(if (focused) Modifier.border(BraceTheme.sizing.focusRingWidth, switch.focusRing, RoundedCornerShape(metrics.focusCornerRadius)) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.size(width = metrics.trackWidth, height = metrics.trackHeight)) {
                drawRoundRect(
                    color = if (!enabled) switch.disabledTrack else if (checked) switch.checkedTrack else switch.uncheckedTrack,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2),
                )
                val radius = metrics.thumbRadius.toPx()
                drawCircle(
                    color = if (enabled) switch.thumb else switch.disabledThumb,
                    radius = radius,
                    center = Offset(if (checked) size.width - radius - metrics.thumbInset.toPx() else radius + metrics.thumbInset.toPx(), size.height / 2),
                )
            }
        }
        Spacer(Modifier.width(BraceTheme.spacing.sm))
        Text(label, color = if (enabled) semantic.onSurface else semantic.disabledContent, style = BraceTheme.typography.body)
    }
}
