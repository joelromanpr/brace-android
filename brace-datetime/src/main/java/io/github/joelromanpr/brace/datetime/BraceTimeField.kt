package io.github.joelromanpr.brace.datetime

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.focused
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.requestFocus
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import io.github.braceandroid.foundation.BraceTheme
import io.github.joelromanpr.brace.core.BraceButton
import io.github.joelromanpr.brace.core.BracePopover
import io.github.joelromanpr.brace.core.braceShortcutEditable
import java.time.Clock
import java.time.LocalTime
import java.util.Locale

/**
 * Controlled localized time entry with an anchored [BraceTimePicker]. The editable text,
 * popup state, and invalid draft survive activity recreation. The caller saves [value] as an
 * ISO time string. Valid drafts emit [onValueChange] immediately; an invalid draft remains
 * visible and [onInvalidInput] fires when editing finishes. Empty text clears the selection.
 *
 * The 48 dp clock action and Alt+Down open the picker. Back, Escape, and outside taps dismiss
 * it. Picker changes stay open by default so all time units can be adjusted; Done closes it.
 * [clock] is used only to initialize the picker when [value] is null. A [LocalTime] has no time
 * zone or date, so callers must handle time-zone conversion separately.
 */
@Composable
public fun BraceTimeField(
    value: LocalTime?,
    onValueChange: (LocalTime?) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    locale: Locale? = null,
    use24Hour: Boolean? = null,
    precision: BraceTimePrecision = BraceTimePrecision.Minute,
    minTime: LocalTime? = null,
    maxTime: LocalTime? = null,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    closeOnSelection: Boolean = false,
    placeholder: String? = null,
    supportingText: String? = null,
    validationError: String? = null,
    onInvalidInput: (String) -> Unit = {},
    clock: Clock = Clock.systemDefaultZone(),
) {
    val resolvedLocale = locale ?: LocalConfiguration.current.locales[0] ?: Locale.getDefault()
    val twentyFour = remember(resolvedLocale, use24Hour) { isTwentyFourHour(resolvedLocale, use24Hour) }
    val formatter = remember(resolvedLocale, twentyFour, precision) {
        timeFormatter(resolvedLocale, twentyFour, precision)
    }
    val labels = timeLabels(resolvedLocale)
    val colors = BraceTheme.colors.components.timeInput
    val metrics = BraceTheme.componentMetrics.timeInput
    val shape = RoundedCornerShape(metrics.cornerRadius)
    val openLabel = remember(labels, label, resolvedLocale) { String.format(resolvedLocale, labels.open, label) }
    val fieldRequester = remember { FocusRequester() }
    val fieldSource = remember { MutableInteractionSource() }
    val fieldFocused by fieldSource.collectIsFocusedAsState()
    val clockRequester = remember { FocusRequester() }
    val clockSource = remember { MutableInteractionSource() }
    val clockFocused by clockSource.collectIsFocusedAsState()
    var expanded by rememberSaveable { mutableStateOf(false) }
    var draft by rememberSaveable { mutableStateOf(value?.let(formatter::format).orEmpty()) }
    var editing by remember { mutableStateOf(false) }
    var lastValue by remember { mutableStateOf(value) }
    var lastLocale by remember { mutableStateOf(resolvedLocale) }
    var lastInvalidNotification by remember { mutableStateOf<String?>(null) }
    val parsed = remember(draft, formatter) { parseTimeDraft(draft, formatter) }
    val invalid = draft.isNotBlank() && parsed == null
    val unavailable = parsed != null && !timeInBounds(parsed, minTime, maxTime)
    val errorMessage = when {
        validationError != null -> validationError
        invalid -> labels.invalid
        unavailable -> labels.unavailable
        else -> null
    }
    LaunchedEffect(value, resolvedLocale, formatter) {
        if (resolvedLocale != lastLocale || (value != lastValue && (!editing || value != parsed))) {
            draft = value?.let(formatter::format).orEmpty()
        }
        lastValue = value
        lastLocale = resolvedLocale
    }
    LaunchedEffect(enabled) { if (!enabled) expanded = false }
    fun finishDraft() {
        val candidate = parseTimeDraft(draft, formatter)
        when {
            draft.isBlank() -> { if (value != null) onValueChange(null); draft = "" }
            candidate == null || !timeInBounds(candidate, minTime, maxTime) -> {
                if (draft != lastInvalidNotification) {
                    onInvalidInput(draft)
                    lastInvalidNotification = draft
                }
            }
            else -> {
                lastInvalidNotification = null
                if (candidate != value) onValueChange(candidate)
                draft = formatter.format(candidate)
            }
        }
    }
    val initialTime = remember(value, minTime, maxTime, precision, clock) {
        val now = atPrecision(LocalTime.now(clock), precision)
        when {
            value != null -> value
            timeInBounds(now, minTime, maxTime) -> now
            minTime != null -> minTime
            else -> maxTime ?: now
        }
    }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(BraceTheme.spacing.xs)) {
        Box(Modifier.fillMaxWidth().heightIn(min = BraceTheme.sizing.touchTarget)
            .then(if (enabled) Modifier.pointerInput(fieldRequester) {
                detectTapGestures(onTap = { fieldRequester.requestFocus() })
            } else Modifier)
            .clearAndSetSemantics {}, contentAlignment = Alignment.CenterStart) {
            Text(label, color = if (enabled) colors.content else colors.disabledContent,
                style = BraceTheme.typography.label)
        }
        BracePopover(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            title = labels.title,
            target = {
                Row(Modifier.fillMaxWidth()
                    .background(if (enabled) colors.container else colors.disabledContainer, shape)
                    .border(if (fieldFocused || clockFocused) BraceTheme.sizing.focusRingWidth else metrics.borderWidth,
                        when {
                            errorMessage != null -> colors.errorBorder
                            fieldFocused || clockFocused -> colors.focusedBorder
                            else -> colors.border
                        }, shape),
                    verticalAlignment = Alignment.CenterVertically) {
                    BasicTextField(
                        value = draft,
                        onValueChange = { next ->
                            draft = next
                            lastInvalidNotification = null
                            if (next.isBlank()) {
                                if (value != null) onValueChange(null)
                            } else {
                                val candidate = parseTimeDraft(next, formatter)
                                if (candidate != null && timeInBounds(candidate, minTime, maxTime) && candidate != value) {
                                    onValueChange(candidate)
                                }
                            }
                        },
                        modifier = Modifier.weight(1f)
                            .heightIn(min = BraceTheme.sizing.touchTarget)
                            .focusRequester(fieldRequester)
                            .onFocusChanged { state ->
                                if (editing && !state.isFocused) finishDraft()
                                editing = state.isFocused
                            }
                            .onPreviewKeyEvent { event ->
                                if (enabled && event.type == KeyEventType.KeyDown &&
                                    event.key == Key.DirectionDown && event.isAltPressed
                                ) { expanded = true; true } else false
                            }
                            .then(if (enabled && !readOnly) Modifier.braceShortcutEditable() else Modifier)
                            .semantics {
                                contentDescription = label
                                if (errorMessage != null) error(errorMessage)
                                else if (supportingText != null) stateDescription = supportingText
                            },
                        enabled = enabled,
                        readOnly = readOnly,
                        singleLine = true,
                        textStyle = BraceTheme.typography.body.copy(
                            color = if (enabled) colors.content else colors.disabledContent),
                        cursorBrush = SolidColor(BraceTheme.colors.semantic.primary),
                        interactionSource = fieldSource,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { finishDraft() }),
                        decorationBox = { inner ->
                            Box(Modifier.padding(horizontal = metrics.horizontalPadding),
                                contentAlignment = Alignment.CenterStart) {
                                if (draft.isEmpty()) Text(
                                    placeholder ?: formatter.format(LocalTime.of(14, 30)),
                                    color = colors.placeholder, style = BraceTheme.typography.body)
                                inner()
                            }
                        },
                    )
                    Box(Modifier.size(BraceTheme.sizing.touchTarget)
                        .focusRequester(clockRequester)
                        .clearAndSetSemantics {
                            contentDescription = openLabel
                            role = Role.Button
                            if (!enabled) disabled() else {
                                focused = clockFocused
                                onClick { expanded = true; true }
                                requestFocus { clockRequester.requestFocus() }
                            }
                        }
                        .clickable(enabled = enabled, role = Role.Button,
                            interactionSource = clockSource, indication = null) { expanded = true }
                        .border(if (clockFocused) BraceTheme.sizing.focusRingWidth else BraceTheme.sizing.borderWidth,
                            if (clockFocused) colors.focusRing else androidx.compose.ui.graphics.Color.Transparent, shape),
                        contentAlignment = Alignment.Center) {
                        Canvas(Modifier.size(BraceTheme.sizing.iconMd)) {
                            val stroke = 1.8.dp.toPx()
                            val tint = if (enabled) colors.clockIcon else colors.disabledContent
                            drawCircle(tint, style = Stroke(stroke))
                            drawLine(tint, center, Offset(center.x, center.y - size.height * 0.25f), stroke)
                            drawLine(tint, center, Offset(center.x + size.width * 0.22f, center.y), stroke)
                        }
                    }
                }
            },
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(BraceTheme.spacing.sm)) {
                BraceTimePicker(initialTime, { selected ->
                    onValueChange(selected)
                    draft = formatter.format(selected)
                    if (closeOnSelection) expanded = false
                }, locale = resolvedLocale, use24Hour = use24Hour, precision = precision,
                    minTime = minTime, maxTime = maxTime, enabled = enabled)
                Row(horizontalArrangement = Arrangement.spacedBy(BraceTheme.spacing.sm)) {
                    BraceButton(labels.done, onClick = { expanded = false })
                    if (value != null) BraceButton(labels.clear, onClick = {
                        onValueChange(null)
                        draft = ""
                        expanded = false
                    })
                }
            }
        }
        val helper = errorMessage ?: supportingText
        if (helper != null) Text(helper,
            color = if (errorMessage != null) BraceTheme.colors.semantic.danger
                else BraceTheme.colors.semantic.onSurfaceMuted,
            style = BraceTheme.typography.label)
    }
}
