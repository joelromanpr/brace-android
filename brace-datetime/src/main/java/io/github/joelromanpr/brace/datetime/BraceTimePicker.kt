package io.github.joelromanpr.brace.datetime

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.focused
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.requestFocus
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.braceandroid.foundation.BraceTheme
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

internal data class TimeLabels(
    val hour12: String, val hour24: String, val minute: String, val second: String,
    val millisecond: String, val hourShort: String, val minuteShort: String,
    val secondShort: String, val millisecondShort: String, val period: String, val increase: String, val decrease: String,
    val switchPeriod: String, val open: String, val invalid: String, val unavailable: String,
    val title: String, val done: String, val clear: String,
)

@Composable
internal fun timeLabels(locale: Locale): TimeLabels {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val localized = remember(context, configuration, locale) {
        val copy = Configuration(configuration)
        copy.setLocale(locale)
        context.createConfigurationContext(copy)
    }
    return remember(localized) {
        TimeLabels(
            localized.getString(R.string.brace_time_hour_12),
            localized.getString(R.string.brace_time_hour_24),
            localized.getString(R.string.brace_time_minute),
            localized.getString(R.string.brace_time_second),
            localized.getString(R.string.brace_time_millisecond),
            localized.getString(R.string.brace_time_hour_short),
            localized.getString(R.string.brace_time_minute_short),
            localized.getString(R.string.brace_time_second_short),
            localized.getString(R.string.brace_time_millisecond_short),
            localized.getString(R.string.brace_time_period),
            localized.getString(R.string.brace_time_increase),
            localized.getString(R.string.brace_time_decrease),
            localized.getString(R.string.brace_time_switch_period),
            localized.getString(R.string.brace_time_open),
            localized.getString(R.string.brace_time_invalid),
            localized.getString(R.string.brace_time_unavailable),
            localized.getString(R.string.brace_time_title),
            localized.getString(R.string.brace_time_done),
            localized.getString(R.string.brace_time_clear),
        )
    }
}

/**
 * Controlled, time-only picker. Hoist [value] and save it as a [LocalTime] ISO string for
 * restoration. [locale] chooses localized labels, digits, day period, and the default hour cycle;
 * [use24Hour] overrides the cycle. [precision] exposes minutes, seconds, or milliseconds.
 *
 * Each numeric unit supports typing, IME Done, keyboard Up/Down, and 48 dp increment/decrement
 * buttons. Entered numbers commit on focus loss or Done. A step is rejected when its resulting
 * time falls outside the inclusive [minTime]/[maxTime] range. A later [minTime] than [maxTime]
 * describes an overnight range. Both bounds equal allow just that exact time. Hidden smaller
 * units are cleared when a value is emitted at the selected precision. The picker does not
 * silently change a caller-supplied value when bounds change.
 */
@Composable
public fun BraceTimePicker(
    value: LocalTime,
    onValueChange: (LocalTime) -> Unit,
    modifier: Modifier = Modifier,
    locale: Locale? = null,
    use24Hour: Boolean? = null,
    precision: BraceTimePrecision = BraceTimePrecision.Minute,
    minTime: LocalTime? = null,
    maxTime: LocalTime? = null,
    enabled: Boolean = true,
) {
    val resolvedLocale = locale ?: LocalConfiguration.current.locales[0] ?: Locale.getDefault()
    val labels = timeLabels(resolvedLocale)
    val twentyFour = remember(resolvedLocale, use24Hour) { isTwentyFourHour(resolvedLocale, use24Hour) }
    val colors = BraceTheme.colors.components.timePicker
    val metrics = BraceTheme.componentMetrics.timePicker
    val shape = RoundedCornerShape(metrics.cornerRadius)
    val periodFormatter = remember(resolvedLocale) { DateTimeFormatter.ofPattern("a", resolvedLocale) }
    fun emit(candidate: LocalTime) {
        val precise = atPrecision(candidate, precision)
        if (enabled && timeInBounds(precise, minTime, maxTime) && precise != value) onValueChange(precise)
    }
    val units = buildList {
        add(TimeUnit.Hour)
        add(TimeUnit.Minute)
        if (precision != BraceTimePrecision.Minute) add(TimeUnit.Second)
        if (precision == BraceTimePrecision.Millisecond) add(TimeUnit.Millisecond)
    }
    Row(
        modifier.background(colors.container, shape)
            .border(metrics.borderWidth, colors.border, shape)
            .horizontalScroll(rememberScrollState())
            .padding(metrics.contentPadding),
        horizontalArrangement = Arrangement.spacedBy(metrics.columnGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        units.forEach { unit ->
            val label = when (unit) {
                TimeUnit.Hour -> if (twentyFour) labels.hour24 else labels.hour12
                TimeUnit.Minute -> labels.minute
                TimeUnit.Second -> labels.second
                TimeUnit.Millisecond -> labels.millisecond
            }
            val numericValue = when (unit) {
                TimeUnit.Hour -> if (twentyFour) value.hour else (value.hour % 12).let { if (it == 0) 12 else it }
                TimeUnit.Minute -> value.minute
                TimeUnit.Second -> value.second
                TimeUnit.Millisecond -> value.nano / 1_000_000
            }
            val display = localizedUnit(numericValue, when (unit) {
                TimeUnit.Hour -> 1
                TimeUnit.Millisecond -> 3
                else -> 2
            }, resolvedLocale)
            val shortLabel = when (unit) {
                TimeUnit.Hour -> labels.hourShort
                TimeUnit.Minute -> labels.minuteShort
                TimeUnit.Second -> labels.secondShort
                TimeUnit.Millisecond -> labels.millisecondShort
            }
            TimeUnitEditor(
                unit = unit, label = label, shortLabel = shortLabel, display = display, value = value,
                minTime = minTime, maxTime = maxTime, precision = precision,
                twelveHour = !twentyFour, enabled = enabled, labels = labels,
                onCommit = { entered ->
                    setUnit(value, unit, entered, !twentyFour)?.let { candidate ->
                        val precise = atPrecision(candidate, precision)
                        if (timeInBounds(precise, minTime, maxTime)) {
                            emit(candidate)
                            true
                        } else false
                    } ?: false
                },
                onStep = { amount -> emit(changeUnit(value, unit, amount)) },
            )
        }
        if (!twentyFour) {
            val period = periodFormatter.format(value)
            val next = periodFormatter.format(value.plusHours(12))
            val nextValue = atPrecision(value.plusHours(12), precision)
            val canSwitch = enabled && timeInBounds(nextValue, minTime, maxTime)
            TimeAction(
                label = String.format(resolvedLocale, labels.switchPeriod, next), symbol = period, enabled = canSwitch,
                modifier = Modifier.width(metrics.unitWidth),
                state = "${labels.period}: $period",
                onClick = { emit(value.plusHours(12)) },
            )
        }
    }
}

@Composable
private fun TimeUnitEditor(
    unit: TimeUnit,
    label: String,
    shortLabel: String,
    display: String,
    value: LocalTime,
    minTime: LocalTime?,
    maxTime: LocalTime?,
    precision: BraceTimePrecision,
    twelveHour: Boolean,
    enabled: Boolean,
    labels: TimeLabels,
    onCommit: (Int) -> Boolean,
    onStep: (Int) -> Unit,
) {
    val colors = BraceTheme.colors.components.timePicker
    val metrics = BraceTheme.componentMetrics.timePicker
    val shape = RoundedCornerShape(metrics.cornerRadius)
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    var draft by rememberSaveable(unit.name) { mutableStateOf(display) }
    var lastDisplay by rememberSaveable(unit.name) { mutableStateOf(display) }
    var wasFocused by remember { mutableStateOf(false) }
    LaunchedEffect(display) {
        if (display != lastDisplay) {
            draft = display
            lastDisplay = display
        }
    }
    val parsed = parseLocalizedUnit(draft)
    val validUnit = parsed != null && setUnit(value, unit, parsed, twelveHour) != null
    val candidate = if (validUnit) atPrecision(setUnit(value, unit, parsed!!, twelveHour)!!, precision) else null
    val invalid = draft.isNotBlank() && !validUnit
    val unavailable = candidate != null && !timeInBounds(candidate, minTime, maxTime)
    fun finishDraft() {
        if (!enabled) return
        val accepted = parsed != null && onCommit(parsed)
        if (accepted || draft.isBlank() || invalid || unavailable) draft = display
    }
    fun canStep(delta: Int): Boolean = enabled && timeInBounds(
        atPrecision(changeUnit(value, unit, delta), precision), minTime, maxTime,
    )
    Column(Modifier.width(metrics.unitWidth), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(BraceTheme.spacing.xs)) {
        Text(shortLabel, color = if (enabled) colors.mutedContent else colors.disabledContent,
            style = BraceTheme.typography.label, maxLines = 1)
        TimeAction(String.format(labels.increase, label), "+", canStep(1),
            Modifier.fillMaxWidth(), onClick = { onStep(1) })
        BasicTextField(
            value = draft,
            onValueChange = { next -> if (next.length <= (if (unit == TimeUnit.Millisecond) 3 else 2) && next.all(Char::isDigit)) draft = next },
            modifier = Modifier.fillMaxWidth().heightIn(min = BraceTheme.sizing.touchTarget)
                .onFocusChanged { state ->
                    if (state.isFocused) wasFocused = true
                    else if (wasFocused) { finishDraft(); wasFocused = false }
                }
                .onPreviewKeyEvent { event ->
                    if (!enabled || event.type != KeyEventType.KeyDown) false else when (event.key) {
                        Key.DirectionUp -> { onStep(1); true }
                        Key.DirectionDown -> { onStep(-1); true }
                        Key.Enter, Key.NumPadEnter -> { finishDraft(); true }
                        else -> false
                    }
                }
                .background(colors.container, shape)
                .border(if (focused) BraceTheme.sizing.focusRingWidth else metrics.borderWidth,
                    when {
                        invalid || unavailable -> colors.errorBorder
                        focused -> colors.focusRing
                        else -> colors.border
                    }, shape)
                .padding(horizontal = BraceTheme.spacing.xs)
                .semantics {
                    contentDescription = label
                    stateDescription = "$label, $draft"
                    if (invalid || unavailable) error(if (invalid) labels.invalid else labels.unavailable)
                },
            enabled = enabled,
            singleLine = true,
            textStyle = BraceTheme.typography.body.copy(
                color = if (enabled) colors.content else colors.disabledContent,
                textAlign = TextAlign.Center),
            cursorBrush = SolidColor(BraceTheme.colors.semantic.primary),
            interactionSource = source,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { finishDraft() }),
            decorationBox = { inner -> Box(Modifier.fillMaxWidth().heightIn(min = BraceTheme.sizing.touchTarget),
                contentAlignment = Alignment.Center) { inner() } },
        )
        TimeAction(String.format(labels.decrease, label), "−", canStep(-1),
            Modifier.fillMaxWidth(), onClick = { onStep(-1) })
    }
}

@Composable
internal fun TimeAction(
    label: String,
    symbol: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    state: String? = null,
    onClick: () -> Unit,
) {
    val colors = BraceTheme.colors.components.timePicker
    val metrics = BraceTheme.componentMetrics.timePicker
    val shape = RoundedCornerShape(metrics.cornerRadius)
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val hovered by source.collectIsHoveredAsState()
    val focusRequester = remember { FocusRequester() }
    Box(
        modifier.heightIn(min = BraceTheme.sizing.touchTarget)
            .focusRequester(focusRequester)
            .clearAndSetSemantics {
                contentDescription = label
                role = Role.Button
                if (state != null) stateDescription = state
                if (!enabled) disabled() else {
                    this.focused = focused
                    onClick { onClick(); true }
                    requestFocus { focusRequester.requestFocus() }
                }
            }
            .hoverable(source, enabled = enabled)
            .clickable(enabled = enabled, role = Role.Button, interactionSource = source,
                indication = null, onClick = onClick)
            .background(if (hovered && enabled) colors.hoverContainer else colors.stepContainer, shape)
            .border(if (focused) BraceTheme.sizing.focusRingWidth else metrics.borderWidth,
                if (focused) colors.focusRing else colors.border, shape),
        contentAlignment = Alignment.Center,
    ) {
        Text(symbol, color = if (enabled) colors.stepContent else colors.disabledContent,
            style = BraceTheme.typography.body, maxLines = 1)
    }
}
