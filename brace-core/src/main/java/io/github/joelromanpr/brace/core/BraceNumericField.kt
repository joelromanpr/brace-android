package io.github.joelromanpr.brace.core

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.isShiftPressed as isPointerShiftPressed
import androidx.compose.ui.input.pointer.isAltPressed as isPointerAltPressed
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import io.github.braceandroid.foundation.BraceTheme
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.util.Locale

/** Logical placement of the 48dp increment and decrement actions. */
public enum class BraceNumericButtonPosition { Start, End, None }

/** Per-control type and padding scale; theme density still applies to every size. */
public enum class BraceNumericFieldSize { Small, Medium, Large }

/** Numeric border intent. A validation error takes precedence over this intent. */
public enum class BraceNumericIntent { Default, Primary, Success, Warning, Danger }

private enum class NumericStepKind { Normal, Major, Minor }

/**
 * A controlled numeric field with a **string** [value] so partial drafts such as `-`, `0.`,
 * and a locale-specific decimal separator survive typing. Hoist [value] with `rememberSaveable`
 * to restore a draft after activity recreation. [onValueChange] receives the exact draft and is
 * called only when its text changes; parse it when a complete number is required.
 *
 * Up/Down applies [stepSize], Shift applies [majorStepSize], and Alt applies [minorStepSize].
 * Passing null for a modified step disables that shortcut. Steps use decimal arithmetic,
 * retain the highest fractional precision of [stepSize], [majorStepSize], [minorStepSize],
 * [min], and [max], then clamp to those bounds.
 * Direct typing may temporarily exceed bounds. [clampValueOnBlur] normalizes a numeric draft
 * to the bounds and clears a nonempty invalid draft on blur or IME Done.
 * [locale] defaults to the current Android configuration and governs decimal input, localized
 * digits, and step output. A caller holding a draft across a runtime locale switch should
 * translate that string when changing locale.
 *
 * [buttonPosition] uses logical start/end for RTL. The step actions sit side by side so each
 * is at least 48dp, adapting Blueprint's compact stacked web buttons to Android touch. The
 * visual label has a 48dp pointer target that focuses the field without a second focus stop. Mouse
 * Shift/Alt click chooses major/minor steps. [onButtonClick] runs only for a changed value from
 * a step button. Long-press auto-repeat and the pinned documentation's separate arithmetic,
 * abbreviation, and expression evaluation example are outside this core field; callers can
 * implement custom parsing through [onValueChange], [onImeDone], and [onBlur].
 *
 * The input suppresses unrelated [BraceShortcutRegistry] shortcuts while writable and focused.
 * [errorText], [supportingText], [incrementActionLabel], and [decrementActionLabel] should be
 * localized by callers when supplied. The default step action labels use Android resources.
 */
@Composable
public fun BraceNumericField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    supportingText: String? = null,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    isError: Boolean = false,
    errorText: String? = null,
    min: Double? = null,
    max: Double? = null,
    stepSize: Double = 1.0,
    majorStepSize: Double? = 10.0,
    minorStepSize: Double? = 0.1,
    locale: Locale? = null,
    clampValueOnBlur: Boolean = false,
    allowNumericCharactersOnly: Boolean = true,
    selectAllOnFocus: Boolean = false,
    selectAllOnIncrement: Boolean = false,
    buttonPosition: BraceNumericButtonPosition = BraceNumericButtonPosition.End,
    size: BraceNumericFieldSize = BraceNumericFieldSize.Medium,
    intent: BraceNumericIntent = BraceNumericIntent.Default,
    fill: Boolean = true,
    showLabel: Boolean = true,
    incrementActionLabel: String? = null,
    decrementActionLabel: String? = null,
    onButtonClick: (String) -> Unit = {},
    onImeDone: (String) -> Unit = {},
    onBlur: (String) -> Unit = {},
) {
    require(label.isNotBlank()) { "Numeric field label must not be blank" }
    require(!isError || !errorText.isNullOrBlank()) { "A localized errorText is required for an error" }
    require(incrementActionLabel == null || incrementActionLabel.isNotBlank()) { "Increment action label must not be blank" }
    require(decrementActionLabel == null || decrementActionLabel.isNotBlank()) { "Decrement action label must not be blank" }
    require(min == null || min.isFinite()) { "min must be finite" }
    require(max == null || max.isFinite()) { "max must be finite" }
    require(min == null || max == null || min <= max) { "min must not exceed max" }
    require(stepSize.isFinite() && stepSize > 0.0) { "stepSize must be finite and positive" }
    require(majorStepSize == null || majorStepSize.isFinite() && majorStepSize > 0.0) {
        "majorStepSize must be finite and positive when present"
    }
    require(minorStepSize == null || minorStepSize.isFinite() && minorStepSize > 0.0) {
        "minorStepSize must be finite and positive when present"
    }
    val configuration = LocalConfiguration.current
    val resolvedLocale = locale ?: configuration.locales[0]
    val symbols = remember(resolvedLocale) { DecimalFormatSymbols.getInstance(resolvedLocale) }
    val minDecimal = remember(min) { min?.let(BigDecimal::valueOf) }
    val maxDecimal = remember(max) { max?.let(BigDecimal::valueOf) }
    val precision = remember(stepSize, majorStepSize, minorStepSize, min, max) {
        listOfNotNull(
            BigDecimal.valueOf(stepSize),
            majorStepSize?.let(BigDecimal::valueOf),
            minorStepSize?.let(BigDecimal::valueOf),
            minDecimal,
            maxDecimal,
        ).maxOf { it.stripTrailingZeros().scale().coerceAtLeast(0) }
    }
    val semantic = BraceTheme.colors.semantic
    val input = BraceTheme.colors.components.input
    val metrics = BraceTheme.componentMetrics.input
    val shape = RoundedCornerShape(metrics.cornerRadius)
    val textStyle = when (size) {
        BraceNumericFieldSize.Small -> BraceTheme.typography.label
        BraceNumericFieldSize.Medium -> BraceTheme.typography.body
        BraceNumericFieldSize.Large -> BraceTheme.typography.subtitle
    }
    val horizontalPadding = when (size) {
        BraceNumericFieldSize.Small -> BraceTheme.spacing.sm
        BraceNumericFieldSize.Medium -> metrics.horizontalPadding
        BraceNumericFieldSize.Large -> metrics.horizontalPadding + BraceTheme.spacing.sm
    }
    val verticalPadding = when (size) {
        BraceNumericFieldSize.Small -> BraceTheme.spacing.xs
        BraceNumericFieldSize.Medium -> BraceTheme.spacing.sm
        BraceNumericFieldSize.Large -> BraceTheme.spacing.md
    }
    val interaction = remember { MutableInteractionSource() }
    val focusRequester = remember { FocusRequester() }
    val focused by interaction.collectIsFocusedAsState()
    var hadFocus by remember { mutableStateOf(false) }
    var field by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(value, TextRange(value.length)))
    }
    val focusManager = LocalFocusManager.current
    // Keep selection and IME composition locally, but let the caller reject a text change.
    // A value-keyed effect would never run when the caller deliberately keeps `value` unchanged.
    SideEffect {
        if (field.text != value) field = TextFieldValue(value, TextRange(value.length))
    }
    val textColor = if (enabled) input.content else input.disabledContent
    val intentBorder = when (intent) {
        BraceNumericIntent.Default -> input.border
        BraceNumericIntent.Primary -> semantic.primary
        BraceNumericIntent.Success -> semantic.success
        BraceNumericIntent.Warning -> semantic.warning
        BraceNumericIntent.Danger -> semantic.danger
    }
    val borderColor = when {
        isError -> input.errorBorder
        !enabled -> input.border
        intent != BraceNumericIntent.Default -> intentBorder
        focused -> input.focusedBorder
        else -> input.border
    }
    val incrementLabel = incrementActionLabel ?: stringResource(R.string.brace_numeric_increment, label)
    val decrementLabel = decrementActionLabel ?: stringResource(R.string.brace_numeric_decrement, label)
    val boundDescription = when {
        min != null && max != null -> stringResource(
            R.string.brace_numeric_range, formatNumeric(minDecimal!!, resolvedLocale, precision),
            formatNumeric(maxDecimal!!, resolvedLocale, precision),
        )
        min != null -> stringResource(R.string.brace_numeric_minimum, formatNumeric(minDecimal!!, resolvedLocale, precision))
        max != null -> stringResource(R.string.brace_numeric_maximum, formatNumeric(maxDecimal!!, resolvedLocale, precision))
        else -> null
    }

    fun emit(next: String, selection: TextRange = TextRange(next.length)): Boolean {
        if (next == field.text) return false
        field = TextFieldValue(next, selection)
        onValueChange(next)
        return true
    }

    fun normalizedOnBlur(): String {
        if (!clampValueOnBlur || field.text.isEmpty()) return field.text
        val parsed = parseNumeric(field.text, symbols)
        val next = if (parsed == null) "" else formatNumeric(
            clampNumeric(parsed, minDecimal, maxDecimal).setScale(precision, RoundingMode.HALF_UP),
            resolvedLocale,
            precision,
        )
        emit(next)
        return next
    }

    fun atDirectionalBound(current: BigDecimal, direction: Int): Boolean =
        if (direction > 0) maxDecimal != null && current >= maxDecimal
        else minDecimal != null && current <= minDecimal

    fun stepValue(direction: Int, kind: NumericStepKind, fromButton: Boolean): Boolean {
        if (!enabled || readOnly) return false
        val amount = when (kind) {
            NumericStepKind.Normal -> stepSize
            NumericStepKind.Major -> majorStepSize ?: return false
            NumericStepKind.Minor -> minorStepSize ?: return false
        }
        val original = if (field.text.isEmpty()) BigDecimal.ZERO else parseNumeric(field.text, symbols) ?: return false
        if (atDirectionalBound(original, direction)) return false
        val changed = clampNumeric(
            original.add(BigDecimal.valueOf(amount).multiply(BigDecimal.valueOf(direction.toLong()))),
            minDecimal,
            maxDecimal,
        ).setScale(precision, RoundingMode.HALF_UP)
        val next = formatNumeric(changed, resolvedLocale, precision)
        val selection = if (selectAllOnIncrement) TextRange(0, next.length) else TextRange(next.length)
        if (emit(next, selection)) {
            if (fromButton) onButtonClick(next)
            return true
        }
        return false
    }

    fun canStep(direction: Int): Boolean {
        if (!enabled || readOnly) return false
        val current = if (field.text.isEmpty()) BigDecimal.ZERO else parseNumeric(field.text, symbols) ?: return false
        if (atDirectionalBound(current, direction)) return false
        val changed = clampNumeric(
            current.add(BigDecimal.valueOf(stepSize).multiply(BigDecimal.valueOf(direction.toLong()))),
            minDecimal,
            maxDecimal,
        ).setScale(precision, RoundingMode.HALF_UP)
        return formatNumeric(changed, resolvedLocale, precision) != field.text
    }

    LaunchedEffect(focused, clampValueOnBlur) {
        if (focused) {
            hadFocus = true
            if (selectAllOnFocus) field = field.copy(selection = TextRange(0, field.text.length))
        } else if (hadFocus) {
            // A disappearing composition cancels this effect before a restoration can commit.
            withFrameNanos { }
            if (!focused) {
                hadFocus = false
                onBlur(normalizedOnBlur())
            }
        }
    }

    val fieldMinHeight = maxOf(
        BraceTheme.sizing.touchTarget + if (size == BraceNumericFieldSize.Large) BraceTheme.spacing.md else 0.dp,
        BraceTheme.densityTokens.controlHeightDp,
    )
    val detail = if (isError) errorText else supportingText
    Column(modifier, verticalArrangement = Arrangement.spacedBy(BraceTheme.spacing.sm)) {
        if (showLabel) {
            Text(
                label,
                modifier = Modifier
                    .heightIn(min = BraceTheme.sizing.touchTarget)
                    .widthIn(min = BraceTheme.sizing.touchTarget)
                    .then(if (enabled) Modifier.pointerInput(focusRequester) {
                        detectTapGestures(onTap = { focusRequester.requestFocus() })
                    } else Modifier)
                    .clearAndSetSemantics { },
                color = if (enabled) semantic.onSurface else semantic.disabledContent,
                style = BraceTheme.typography.label,
            )
        }
        Row(
            modifier = if (fill) Modifier.fillMaxWidth() else Modifier,
            horizontalArrangement = Arrangement.spacedBy(BraceTheme.spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            @Composable fun Buttons() {
                BraceNumericStepButton("−", decrementLabel, enabled && !readOnly && canStep(-1),
                    onClick = { kind -> stepValue(-1, kind, fromButton = true) })
                BraceNumericStepButton("+", incrementLabel, enabled && !readOnly && canStep(1),
                    onClick = { kind -> stepValue(1, kind, fromButton = true) })
            }
            if (buttonPosition == BraceNumericButtonPosition.Start) Buttons()
            val inputWidthModifier = if (fill) Modifier.weight(1f) else Modifier.widthIn(min = BraceTheme.sizing.touchTarget * 2)
            CompositionLocalProvider(
                LocalTextSelectionColors provides TextSelectionColors(
                    handleColor = semantic.primary,
                    backgroundColor = input.selection,
                ),
            ) {
                BasicTextField(
                    value = field,
                    onValueChange = { next ->
                        if (!allowNumericCharactersOnly || isAllowedNumericDraft(next.text, symbols)) {
                            val changed = next.text != field.text
                            field = next
                            if (changed) onValueChange(next.text)
                        }
                    },
                    modifier = Modifier
                        .then(inputWidthModifier)
                        .heightIn(min = fieldMinHeight)
                        .clip(shape)
                        .background(if (enabled) input.container else input.disabledContainer)
                        .border(
                            if (focused) BraceTheme.sizing.focusRingWidth else metrics.borderWidth,
                            borderColor,
                            shape,
                        )
                        .then(if (enabled && !readOnly) Modifier.braceShortcutEditable() else Modifier)
                        .focusRequester(focusRequester)
                        .onFocusChanged { hadFocus = hadFocus || it.isFocused }
                        .onPreviewKeyEvent { event ->
                            if (event.type != KeyEventType.KeyDown || field.composition != null || !enabled || readOnly) {
                                false
                            } else when (event.key) {
                                Key.DirectionUp, Key.DirectionDown -> {
                                    val kind = when {
                                        event.isShiftPressed -> NumericStepKind.Major
                                        event.isAltPressed -> NumericStepKind.Minor
                                        else -> NumericStepKind.Normal
                                    }
                                    val available = when (kind) {
                                        NumericStepKind.Major -> majorStepSize != null
                                        NumericStepKind.Minor -> minorStepSize != null
                                        NumericStepKind.Normal -> true
                                    }
                                    if (available) {
                                        stepValue(if (event.key == Key.DirectionUp) 1 else -1, kind, fromButton = false)
                                        true
                                    } else false
                                }
                                else -> false
                            }
                        }
                        .semantics {
                            contentDescription = label
                            listOfNotNull(boundDescription, detail).takeIf { it.isNotEmpty() }
                                ?.let { stateDescription = it.joinToString(". ") }
                            if (isError) error(errorText!!)
                        },
                    enabled = enabled,
                    readOnly = readOnly,
                    interactionSource = interaction,
                    singleLine = true,
                    textStyle = textStyle.copy(color = textColor),
                    cursorBrush = SolidColor(semantic.primary),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = {
                        val submitted = normalizedOnBlur()
                        onImeDone(submitted)
                        focusManager.clearFocus()
                    }),
                    decorationBox = { inner ->
                        Box(
                            Modifier.padding(horizontal = horizontalPadding, vertical = verticalPadding),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            if (field.text.isEmpty() && placeholder.isNotEmpty()) {
                                Text(placeholder, color = input.placeholder, style = textStyle)
                            }
                            inner()
                        }
                    },
                )
            }
            if (buttonPosition == BraceNumericButtonPosition.End) Buttons()
        }
        if (detail != null) {
            Text(
                detail,
                modifier = Modifier.clearAndSetSemantics { },
                color = if (isError) semantic.danger else semantic.onSurfaceMuted,
                style = BraceTheme.typography.label,
            )
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun BraceNumericStepButton(
    glyph: String,
    actionLabel: String,
    enabled: Boolean,
    onClick: (NumericStepKind) -> Unit,
) {
    val input = BraceTheme.colors.components.input
    val semantic = BraceTheme.colors.semantic
    val metrics = BraceTheme.componentMetrics.input
    val shape = RoundedCornerShape(metrics.cornerRadius)
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    var pointerKind by remember { mutableStateOf(NumericStepKind.Normal) }
    Box(
        modifier = Modifier
            .size(BraceTheme.sizing.touchTarget)
            .clip(shape)
            .background(if (enabled) input.container else input.disabledContainer)
            .border(
                if (focused) BraceTheme.sizing.focusRingWidth else metrics.borderWidth,
                if (focused) input.focusedBorder else input.border,
                shape,
            )
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        if (event.type == PointerEventType.Press) {
                            pointerKind = when {
                                event.keyboardModifiers.isPointerShiftPressed -> NumericStepKind.Major
                                event.keyboardModifiers.isPointerAltPressed -> NumericStepKind.Minor
                                else -> NumericStepKind.Normal
                            }
                        }
                    }
                }
            }
            .clickable(
                enabled = enabled,
                role = Role.Button,
                onClickLabel = actionLabel,
                interactionSource = interaction,
                indication = null,
            ) {
                val kind = pointerKind
                pointerKind = NumericStepKind.Normal
                onClick(kind)
            }
            .semantics { contentDescription = actionLabel },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            glyph,
            color = if (enabled) semantic.onSurface else semantic.disabledContent,
            style = BraceTheme.typography.subtitle,
        )
    }
}

private fun isAllowedNumericDraft(text: String, symbols: DecimalFormatSymbols): Boolean {
    var digits = 0
    var decimal = false
    var exponent = false
    text.forEachIndexed { index, character ->
        when {
            Character.digit(character, 10) >= 0 -> digits++
            (character == '+' || character == '-') &&
                (index == 0 || text[index - 1] == 'e' || text[index - 1] == 'E') -> Unit
            character == symbols.decimalSeparator && !decimal && !exponent -> decimal = true
            (character == 'e' || character == 'E') && !exponent && digits > 0 -> exponent = true
            else -> return false
        }
    }
    return true
}

private fun parseNumeric(text: String, symbols: DecimalFormatSymbols): BigDecimal? {
    if (!isAllowedNumericDraft(text, symbols)) return null
    val normalized = buildString {
        text.forEach { character ->
            when {
                Character.digit(character, 10) >= 0 -> append(Character.digit(character, 10))
                character == symbols.decimalSeparator -> append('.')
                else -> append(character)
            }
        }
    }
    return runCatching { BigDecimal(normalized) }.getOrNull()
}

private fun clampNumeric(value: BigDecimal, min: BigDecimal?, max: BigDecimal?): BigDecimal =
    when {
        min != null && value < min -> min
        max != null && value > max -> max
        else -> value
    }

private fun formatNumeric(value: BigDecimal, locale: Locale, precision: Int): String {
    val formatter = NumberFormat.getNumberInstance(locale) as DecimalFormat
    formatter.isGroupingUsed = false
    formatter.roundingMode = RoundingMode.HALF_UP
    formatter.minimumFractionDigits = 0
    formatter.maximumFractionDigits = precision
    return formatter.format(value)
}
