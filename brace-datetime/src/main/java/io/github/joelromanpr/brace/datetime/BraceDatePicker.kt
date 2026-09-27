package io.github.joelromanpr.brace.datetime

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.CollectionItemInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.collectionItemInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.focused
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.requestFocus
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.LayoutDirection
import io.github.braceandroid.foundation.BraceTheme
import io.github.joelromanpr.brace.core.BraceButton
import io.github.joelromanpr.brace.core.BraceButtonVariant
import io.github.joelromanpr.brace.core.BracePopover
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.DecimalStyle
import java.time.format.FormatStyle
import java.time.temporal.WeekFields
import java.util.Locale

private data class DateLabels(
    val previousMonth: String, val nextMonth: String, val previousYear: String,
    val nextYear: String, val chooseMonth: String, val today: String,
    val clear: String, val selected: String, val currentDay: String,
)

@Composable
private fun dateLabels(locale: Locale): DateLabels {
    val context = LocalContext.current
    val hostConfiguration = LocalConfiguration.current
    val localized = remember(context, hostConfiguration, locale) {
        val configuration = Configuration(hostConfiguration)
        configuration.setLocale(locale)
        context.createConfigurationContext(configuration)
    }
    return remember(localized) {
        DateLabels(
            localized.getString(R.string.brace_date_previous_month),
            localized.getString(R.string.brace_date_next_month),
            localized.getString(R.string.brace_date_previous_year),
            localized.getString(R.string.brace_date_next_year),
            localized.getString(R.string.brace_date_choose_month),
            localized.getString(R.string.brace_date_today),
            localized.getString(R.string.brace_date_clear),
            localized.getString(R.string.brace_date_selected),
            localized.getString(R.string.brace_date_current_day),
        )
    }
}

/**
 * Controlled, locale-aware single-day calendar. The caller hoists [value] and restores it.
 * The visible month is saveable internally; [initialMonth] applies only when first composed.
 * Initial and externally changed months outside [minDate]/[maxDate] display the nearest allowed month.
 * Dates outside [minDate]/[maxDate], or rejected by [isDateEnabled], cannot be selected.
 *
 * Touch, mouse, TalkBack, and Enter/Space select a day. Arrow keys move day focus, Page Up/Down
 * moves a month, and Ctrl+Page Up/Down moves a year. The first weekday follows [locale].
 * A 48 dp day target is retained even in compact density; narrow containers can scroll the
 * calendar horizontally. [clock] determines the Today action, including its time zone.
 */
@Composable
public fun BraceDatePicker(
    value: LocalDate?,
    onValueChange: (LocalDate?) -> Unit,
    modifier: Modifier = Modifier,
    locale: Locale? = null,
    minDate: LocalDate? = null,
    maxDate: LocalDate? = null,
    isDateEnabled: (LocalDate) -> Boolean = { true },
    enabled: Boolean = true,
    canClearSelection: Boolean = true,
    showActions: Boolean = true,
    shortcuts: List<BraceDateShortcut> = emptyList(),
    initialMonth: YearMonth? = null,
    clock: Clock = Clock.systemDefaultZone(),
) {
    require(minDate == null || maxDate == null || !minDate.isAfter(maxDate)) {
        "minDate must be on or before maxDate"
    }
    val resolvedLocale = locale ?: LocalConfiguration.current.locales[0] ?: Locale.getDefault()
    val labels = dateLabels(resolvedLocale)
    val today = LocalDate.now(clock)
    val colors = BraceTheme.colors.components.datePicker
    val metrics = BraceTheme.componentMetrics.datePicker
    val shape = RoundedCornerShape(metrics.cornerRadius)
    val layoutDirection = LocalLayoutDirection.current
    var visibleMonthText by rememberSaveable {
        mutableStateOf(monthWithinBounds(
            initialMonth ?: value?.let(YearMonth::from) ?: YearMonth.from(today), minDate, maxDate,
        ).toString())
    }
    val visibleMonth = YearMonth.parse(visibleMonthText)
    var focusedDayText by rememberSaveable {
        mutableStateOf((value?.takeIf { YearMonth.from(it) == visibleMonth }
            ?: visibleMonth.atDay(1)).toString())
    }
    var requestDayFocus by remember { mutableStateOf(false) }
    val requesters = remember(visibleMonth) {
        (1..visibleMonth.lengthOfMonth()).associateWith { FocusRequester() }
    }
    var previousValue by remember { mutableStateOf(value) }
    LaunchedEffect(value, minDate, maxDate) {
        val requestedMonth = if (value != previousValue && value != null) YearMonth.from(value)
            else YearMonth.parse(visibleMonthText)
        val boundedMonth = monthWithinBounds(requestedMonth, minDate, maxDate)
        if (boundedMonth.toString() != visibleMonthText) {
            visibleMonthText = boundedMonth.toString()
            focusedDayText = boundedMonth.atDay(1).toString()
        } else if (value != previousValue && value != null) {
            focusedDayText = (value.takeIf { YearMonth.from(it) == boundedMonth }
                ?: boundedMonth.atDay(1)).toString()
        }
        previousValue = value
    }
    fun moveMonth(delta: Long) {
        val next = runCatching { visibleMonth.plusMonths(delta) }.getOrNull() ?: return
        if (monthHasBoundedDay(next, minDate, maxDate)) {
            visibleMonthText = next.toString()
            focusedDayText = next.atDay(1).toString()
        }
    }
    fun navigateDay(start: LocalDate, delta: Int) {
        var candidate = start
        repeat(366) {
            candidate = runCatching { candidate.plusDays(delta.toLong()) }.getOrNull() ?: return
            if (minDate != null && candidate.isBefore(minDate)) return
            if (maxDate != null && candidate.isAfter(maxDate)) return
            if (canSelectDate(candidate, minDate, maxDate, isDateEnabled)) {
                focusedDayText = candidate.toString()
                visibleMonthText = YearMonth.from(candidate).toString()
                requestDayFocus = true
                return
            }
        }
    }
    val previousMonth = remember(visibleMonth) { runCatching { visibleMonth.minusMonths(1) }.getOrNull() }
    val nextMonth = remember(visibleMonth) { runCatching { visibleMonth.plusMonths(1) }.getOrNull() }
    val cells = remember(visibleMonth, resolvedLocale) { monthDates(visibleMonth, resolvedLocale) }
    val firstWeekday = WeekFields.of(resolvedLocale).firstDayOfWeek
    val horizontalScroll = rememberScrollState()
    val density = LocalDensity.current
    LaunchedEffect(visibleMonthText, focusedDayText, requestDayFocus, value, horizontalScroll.maxValue) {
        val focused = LocalDate.parse(focusedDayText)
        val dayToReveal = when {
            requestDayFocus && YearMonth.from(focused) == visibleMonth -> focused
            value != null && YearMonth.from(value) == visibleMonth -> value
            YearMonth.from(today) == visibleMonth -> today
            YearMonth.from(focused) == visibleMonth -> focused
            else -> visibleMonth.atDay(1)
        }
        val daySizePx = with(density) { metrics.daySize.roundToPx() }
        val overflow = horizontalScroll.maxValue
        if (overflow in 1..(daySizePx * 7)) {
            val leading = (visibleMonth.atDay(1).dayOfWeek.value - firstWeekday.value + 7) % 7
            val column = (leading + dayToReveal.dayOfMonth - 1) % 7
            val viewport = daySizePx * 7 - overflow
            horizontalScroll.scrollTo(((column + 1) * daySizePx - viewport).coerceIn(0, overflow))
        }
        if (requestDayFocus && YearMonth.from(focused) == visibleMonth) {
            requesters[focused.dayOfMonth]?.requestFocus()
            requestDayFocus = false
        }
    }
    val monthFormatter = remember(resolvedLocale) {
        DateTimeFormatter.ofPattern("LLLL yyyy", resolvedLocale)
            .withDecimalStyle(DecimalStyle.of(resolvedLocale))
    }
    val fullDateFormatter = remember(resolvedLocale) {
        DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(resolvedLocale)
            .withDecimalStyle(DecimalStyle.of(resolvedLocale))
    }
    val dayFormatter = remember(resolvedLocale) {
        DateTimeFormatter.ofPattern("d", resolvedLocale).withDecimalStyle(DecimalStyle.of(resolvedLocale))
    }
    val yearFormatter = remember(resolvedLocale) {
        DateTimeFormatter.ofPattern("uuuu", resolvedLocale).withDecimalStyle(DecimalStyle.of(resolvedLocale))
    }
    var monthChooserOpen by rememberSaveable { mutableStateOf(false) }
    var chooserYear by rememberSaveable { mutableStateOf(visibleMonth.year) }
    Column(
        modifier = modifier.background(colors.container, shape)
            .border(BraceTheme.sizing.borderWidth, colors.border, shape)
            .padding(metrics.contentPadding),
        verticalArrangement = Arrangement.spacedBy(BraceTheme.spacing.sm),
    ) {
        // The viewport may be narrower than seven 48 dp targets. Keep the targets intact.
        Column(Modifier.fillMaxWidth().horizontalScroll(horizontalScroll)) {
            Column(Modifier.widthIn(min = metrics.daySize * 7)) {
                Row(
                    Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    DateNavButton(
                        if (layoutDirection == LayoutDirection.Rtl) "›" else "‹",
                        labels.previousMonth,
                        enabled && previousMonth != null && monthHasBoundedDay(previousMonth, minDate, maxDate),
                    ) { moveMonth(-1) }
                    BracePopover(
                        expanded = monthChooserOpen,
                        onDismissRequest = { monthChooserOpen = false },
                        title = labels.chooseMonth,
                        target = {
                            BraceButton(
                                monthFormatter.format(visibleMonth.atDay(1)),
                                onClick = { chooserYear = visibleMonth.year; monthChooserOpen = true },
                                enabled = enabled,
                                variant = BraceButtonVariant.Outline,
                            )
                        },
                    ) {
                        Column(
                            Modifier.widthIn(min = metrics.daySize * 5),
                            verticalArrangement = Arrangement.spacedBy(BraceTheme.spacing.xs),
                        ) {
                            Row(
                                Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                DateNavButton(if (layoutDirection == LayoutDirection.Rtl) "›" else "‹", labels.previousYear,
                                    enabled && chooserYear > 1 &&
                                        monthHasBoundedDay(YearMonth.of(chooserYear - 1, 12), minDate, maxDate)) {
                                    chooserYear--
                                }
                                Text(yearFormatter.format(YearMonth.of(chooserYear, 1).atDay(1)),
                                    color = colors.content, style = BraceTheme.typography.subtitle)
                                DateNavButton(if (layoutDirection == LayoutDirection.Rtl) "‹" else "›", labels.nextYear,
                                    enabled && chooserYear < 999_999_999 &&
                                        monthHasBoundedDay(YearMonth.of(chooserYear + 1, 1), minDate, maxDate)) {
                                    chooserYear++
                                }
                            }
                            repeat(4) { row ->
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(BraceTheme.spacing.xxs)) {
                                    repeat(3) { column ->
                                        val monthNumber = row * 3 + column + 1
                                        val candidate = YearMonth.of(chooserYear, monthNumber)
                                        Box(Modifier.weight(1f)) {
                                            BraceButton(
                                                candidate.month.getDisplayName(java.time.format.TextStyle.SHORT, resolvedLocale),
                                                onClick = {
                                                    visibleMonthText = candidate.toString()
                                                    focusedDayText = candidate.atDay(1).toString()
                                                    monthChooserOpen = false
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                                enabled = enabled && monthHasBoundedDay(candidate, minDate, maxDate),
                                                variant = if (candidate == visibleMonth) BraceButtonVariant.Solid else BraceButtonVariant.Outline,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    DateNavButton(
                        if (layoutDirection == LayoutDirection.Rtl) "‹" else "›",
                        labels.nextMonth,
                        enabled && nextMonth != null && monthHasBoundedDay(nextMonth, minDate, maxDate),
                    ) { moveMonth(1) }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(metrics.columnGap)) {
                    repeat(7) { index ->
                        val weekday = firstWeekday.plus(index.toLong())
                        Box(Modifier.size(metrics.daySize), contentAlignment = Alignment.Center) {
                            Text(
                                weekday.getDisplayName(java.time.format.TextStyle.NARROW, resolvedLocale),
                                modifier = Modifier.semantics {
                                    contentDescription = weekday.getDisplayName(
                                        java.time.format.TextStyle.FULL, resolvedLocale,
                                    )
                                },
                                color = colors.mutedContent, style = BraceTheme.typography.label,
                            )
                        }
                    }
                }
                Column(Modifier.semantics { collectionInfo = CollectionInfo(cells.size / 7, 7) }) {
                    cells.chunked(7).forEachIndexed { rowIndex, week ->
                        Row(horizontalArrangement = Arrangement.spacedBy(metrics.columnGap)) {
                            week.forEachIndexed { columnIndex, date ->
                                if (date == null) {
                                    Spacer(Modifier.size(metrics.daySize))
                                } else {
                                    val selectable = enabled && canSelectDate(date, minDate, maxDate, isDateEnabled)
                                    val selected = date == value
                                    val source = remember(date) { MutableInteractionSource() }
                                    val focused by source.collectIsFocusedAsState()
                                    val hovered by source.collectIsHoveredAsState()
                                    val state = listOfNotNull(
                                        labels.selected.takeIf { selected },
                                        labels.currentDay.takeIf { date == today },
                                    ).joinToString(", ")
                                    Box(
                                        Modifier.size(metrics.daySize)
                                            .focusRequester(requesters.getValue(date.dayOfMonth))
                                            .onPreviewKeyEvent { event ->
                                                if (event.type != KeyEventType.KeyDown || !enabled) false
                                                else when (event.key) {
                                                    Key.DirectionLeft -> {
                                                        navigateDay(date, if (layoutDirection == LayoutDirection.Rtl) 1 else -1); true
                                                    }
                                                    Key.DirectionRight -> {
                                                        navigateDay(date, if (layoutDirection == LayoutDirection.Rtl) -1 else 1); true
                                                    }
                                                    Key.DirectionUp -> { navigateDay(date, -7); true }
                                                    Key.DirectionDown -> { navigateDay(date, 7); true }
                                                    Key.PageUp, Key.PageDown -> {
                                                        val delta = if (event.key == Key.PageUp) -1L else 1L
                                                        val candidate = runCatching {
                                                            date.plusMonths(if (event.isCtrlPressed) delta * 12 else delta)
                                                        }.getOrNull()
                                                        if (candidate != null && canSelectDate(candidate, minDate, maxDate, isDateEnabled)) {
                                                            focusedDayText = candidate.toString()
                                                            visibleMonthText = YearMonth.from(candidate).toString()
                                                            requestDayFocus = true
                                                        }
                                                        true
                                                    }
                                                    else -> false
                                                }
                                            }
                                            .clearAndSetSemantics {
                                                contentDescription = fullDateFormatter.format(date)
                                                role = Role.Button
                                                this.selected = selected
                                                if (state.isNotEmpty()) stateDescription = state
                                                collectionItemInfo = CollectionItemInfo(rowIndex, 1, columnIndex, 1)
                                                if (!selectable) disabled() else {
                                                    this.focused = focused
                                                    onClick {
                                                        onValueChange(if (selected && canClearSelection) null else date)
                                                        true
                                                    }
                                                    requestFocus { requesters.getValue(date.dayOfMonth).requestFocus() }
                                                }
                                            }
                                            .clickable(
                                                enabled = selectable,
                                                role = Role.Button,
                                                interactionSource = source,
                                                indication = null,
                                            ) { onValueChange(if (selected && canClearSelection) null else date) }
                                            .background(
                                                when {
                                                    selected -> colors.selectedContainer
                                                    hovered && selectable -> colors.hoverContainer
                                                    else -> colors.container
                                                }, shape,
                                            )
                                            .border(
                                                BorderStroke(
                                                    if (focused) BraceTheme.sizing.focusRingWidth
                                                    else BraceTheme.sizing.borderWidth,
                                                    when {
                                                        focused -> colors.focusRing
                                                        date == today -> colors.todayBorder
                                                        else -> androidx.compose.ui.graphics.Color.Transparent
                                                    },
                                                ), shape,
                                            ),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            dayFormatter.format(date),
                                            color = when {
                                                !selectable -> colors.disabledContent
                                                selected -> colors.selectedContent
                                                else -> colors.content
                                            },
                                            style = BraceTheme.typography.body,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        if (shortcuts.isNotEmpty()) {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(BraceTheme.spacing.xs),
            ) {
                shortcuts.forEach { shortcut ->
                    BraceButton(
                        shortcut.label,
                        onClick = { onValueChange(shortcut.date); visibleMonthText = YearMonth.from(shortcut.date).toString() },
                        enabled = enabled && canSelectDate(shortcut.date, minDate, maxDate, isDateEnabled),
                        variant = BraceButtonVariant.Outline,
                    )
                }
            }
        }
        if (showActions) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(BraceTheme.spacing.sm)) {
                BraceButton(labels.today, onClick = {
                    onValueChange(today)
                    visibleMonthText = YearMonth.from(today).toString()
                }, enabled = enabled && canSelectDate(today, minDate, maxDate, isDateEnabled))
                BraceButton(labels.clear, onClick = { onValueChange(null) },
                    enabled = enabled && canClearSelection && value != null,
                    variant = BraceButtonVariant.Outline)
            }
        }
    }
}

@Composable
private fun DateNavButton(glyph: String, label: String, enabled: Boolean, onClick: () -> Unit) {
    val colors = BraceTheme.colors.components.datePicker
    val metrics = BraceTheme.componentMetrics.datePicker
    val source = remember { MutableInteractionSource() }
    val requester = remember { FocusRequester() }
    val focused by source.collectIsFocusedAsState()
    val hovered by source.collectIsHoveredAsState()
    Box(
        Modifier.size(metrics.daySize)
            .focusRequester(requester)
            .clearAndSetSemantics {
                contentDescription = label
                role = Role.Button
                if (!enabled) disabled() else {
                    this.focused = focused
                    onClick { onClick(); true }
                    requestFocus { requester.requestFocus() }
                }
            }
            .clickable(enabled = enabled, role = Role.Button, interactionSource = source,
                indication = null, onClick = onClick)
            .background(if (hovered && enabled) colors.hoverContainer else colors.container,
                RoundedCornerShape(metrics.cornerRadius))
            .border(if (focused) BraceTheme.sizing.focusRingWidth else BraceTheme.sizing.borderWidth,
                if (focused) colors.focusRing else androidx.compose.ui.graphics.Color.Transparent,
                RoundedCornerShape(metrics.cornerRadius)),
        contentAlignment = Alignment.Center,
    ) {
        Text(glyph, color = if (enabled) colors.content else colors.disabledContent,
            style = BraceTheme.typography.subtitle)
    }
}
