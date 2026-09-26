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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.CollectionItemInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.collectionItemInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.focused
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.requestFocus
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.LayoutDirection
import io.github.braceandroid.foundation.BraceTheme
import io.github.joelromanpr.brace.core.BraceButton
import io.github.joelromanpr.brace.core.BraceButtonVariant
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.DecimalStyle
import java.time.format.FormatStyle
import java.time.temporal.WeekFields
import java.util.Locale

internal data class RangeLabels(
    val start: String, val end: String, val inRange: String, val today: String,
    val previousMonth: String, val nextMonth: String, val previousYear: String,
    val nextYear: String, val clear: String, val open: String, val title: String,
    val invalid: String, val unavailable: String, val overlapping: String,
)

@Composable
internal fun rangeLabels(locale: Locale): RangeLabels {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val localized = remember(context, configuration, locale) {
        val copy = Configuration(configuration)
        copy.setLocale(locale)
        context.createConfigurationContext(copy)
    }
    return remember(localized) {
        RangeLabels(
            localized.getString(R.string.brace_range_start),
            localized.getString(R.string.brace_range_end),
            localized.getString(R.string.brace_range_in),
            localized.getString(R.string.brace_range_today),
            localized.getString(R.string.brace_range_previous_month),
            localized.getString(R.string.brace_range_next_month),
            localized.getString(R.string.brace_range_previous_year),
            localized.getString(R.string.brace_range_next_year),
            localized.getString(R.string.brace_range_clear),
            localized.getString(R.string.brace_range_open),
            localized.getString(R.string.brace_range_title),
            localized.getString(R.string.brace_range_invalid),
            localized.getString(R.string.brace_range_unavailable),
            localized.getString(R.string.brace_range_overlapping),
        )
    }
}

/**
 * Controlled date-only range picker. [value] may be empty, have one endpoint, or be complete.
 * The caller owns and restores its endpoints; the visible month and keyboard day are saveable.
 * On wide surfaces two sequential months appear; on a narrow device one month appears with
 * horizontal scrolling so the seven 48 dp day targets never shrink.
 *
 * A first day starts a selection, and a second day completes it in chronological order.
 * [allowSingleDayRange] controls whether tapping the same first day again completes or clears.
 * [boundaryToModify] lets a focused range field replace a particular endpoint, swapping
 * endpoints if needed to preserve order. Disabled and bounded days cannot be endpoints.
 * Interior disabled days may still appear inside a range; applications requiring fully
 * contiguous availability should validate the returned range before accepting it.
 * Arrow keys move by day/week, Page Up/Down by month, and Ctrl+Page Up/Down by year.
 */
@Composable
public fun BraceDateRangePicker(
    value: BraceDateRange,
    onValueChange: (BraceDateRange) -> Unit,
    modifier: Modifier = Modifier,
    locale: Locale? = null,
    minDate: LocalDate? = null,
    maxDate: LocalDate? = null,
    isDateEnabled: (LocalDate) -> Boolean = { true },
    enabled: Boolean = true,
    allowSingleDayRange: Boolean = false,
    boundaryToModify: BraceRangeBoundary? = null,
    shortcuts: List<BraceDateRangeShortcut> = emptyList(),
    showClear: Boolean = true,
    initialMonth: YearMonth? = null,
    clock: Clock = Clock.systemDefaultZone(),
) {
    require(minDate == null || maxDate == null || !minDate.isAfter(maxDate)) {
        "minDate must be on or before maxDate"
    }
    val resolvedLocale = locale ?: LocalConfiguration.current.locales[0] ?: Locale.getDefault()
    val labels = rangeLabels(resolvedLocale)
    val today = LocalDate.now(clock)
    val colors = BraceTheme.colors.components.dateRangePicker
    val metrics = BraceTheme.componentMetrics.dateRangePicker
    val shape = RoundedCornerShape(metrics.cornerRadius)
    val layoutDirection = LocalLayoutDirection.current
    val fullDateFormatter = remember(resolvedLocale) {
        DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(resolvedLocale)
            .withDecimalStyle(DecimalStyle.of(resolvedLocale))
    }
    val dayFormatter = remember(resolvedLocale) {
        DateTimeFormatter.ofPattern("d", resolvedLocale).withDecimalStyle(DecimalStyle.of(resolvedLocale))
    }
    var firstMonthText by rememberSaveable {
        mutableStateOf(monthWithinBounds(initialMonth ?: value.start?.let(YearMonth::from)
            ?: value.end?.let(YearMonth::from) ?: YearMonth.from(today), minDate, maxDate).toString())
    }
    var focusedDayText by rememberSaveable {
        mutableStateOf((value.start ?: value.end ?: YearMonth.parse(firstMonthText).atDay(1)).toString())
    }
    var requestDayFocus by remember { mutableStateOf(false) }
    var previousValue by remember { mutableStateOf(value) }
    LaunchedEffect(value, minDate, maxDate) {
        val anchor = if (value != previousValue) value.start ?: value.end else null
        val proposed = anchor?.let(YearMonth::from) ?: YearMonth.parse(firstMonthText)
        val bounded = monthWithinBounds(proposed, minDate, maxDate)
        if (firstMonthText != bounded.toString()) firstMonthText = bounded.toString()
        if (anchor != null) focusedDayText = anchor.toString()
        previousValue = value
    }
    val firstMonth = YearMonth.parse(firstMonthText)
    fun moveMonth(delta: Long) {
        val next = runCatching { firstMonth.plusMonths(delta) }.getOrNull() ?: return
        if (monthHasBoundedDay(next, minDate, maxDate)) {
            firstMonthText = next.toString()
            focusedDayText = next.atDay(1).toString()
        }
    }
    fun moveDay(start: LocalDate, delta: Int) {
        var candidate = start
        repeat(366) {
            candidate = runCatching { candidate.plusDays(delta.toLong()) }.getOrNull() ?: return
            if (minDate != null && candidate.isBefore(minDate)) return
            if (maxDate != null && candidate.isAfter(maxDate)) return
            if (canSelectDate(candidate, minDate, maxDate, isDateEnabled)) {
                focusedDayText = candidate.toString()
                val candidateMonth = YearMonth.from(candidate)
                if (candidateMonth != firstMonth && candidateMonth != runCatching { firstMonth.plusMonths(1) }.getOrNull()) {
                    firstMonthText = candidateMonth.toString()
                }
                requestDayFocus = true
                return
            }
        }
    }
    BoxWithConstraints(modifier) {
        val visibleMonths = if (maxWidth >= metrics.twoMonthMinWidth &&
            runCatching { firstMonth.plusMonths(1) }.isSuccess) 2 else 1
        val requesters = remember(firstMonth, visibleMonths) {
            (0 until visibleMonths).flatMap { offset ->
                val month = firstMonth.plusMonths(offset.toLong())
                (1..month.lengthOfMonth()).map { month.atDay(it) to FocusRequester() }
            }.toMap()
        }
        LaunchedEffect(focusedDayText, requestDayFocus, firstMonth, visibleMonths) {
            if (requestDayFocus) {
                val candidate = LocalDate.parse(focusedDayText)
                if (requesters.containsKey(candidate)) {
                    requesters[candidate]?.requestFocus()
                    requestDayFocus = false
                } else firstMonthText = YearMonth.from(candidate).toString()
            }
        }
        Column(
            Modifier.background(colors.container, shape)
                .border(BraceTheme.sizing.borderWidth, colors.border, shape)
                .padding(metrics.contentPadding),
            verticalArrangement = Arrangement.spacedBy(BraceTheme.spacing.sm),
        ) {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(BraceTheme.spacing.xs),
                verticalAlignment = Alignment.CenterVertically) {
                RangeNavButton(if (layoutDirection == LayoutDirection.Rtl) "»" else "«", labels.previousYear,
                    enabled && runCatching { firstMonth.minusYears(1) }.getOrNull()
                        ?.let { monthHasBoundedDay(it, minDate, maxDate) } == true) { moveMonth(-12) }
                RangeNavButton(if (layoutDirection == LayoutDirection.Rtl) "›" else "‹", labels.previousMonth,
                    enabled && runCatching { firstMonth.minusMonths(1) }.getOrNull()
                        ?.let { monthHasBoundedDay(it, minDate, maxDate) } == true) { moveMonth(-1) }
                RangeNavButton(if (layoutDirection == LayoutDirection.Rtl) "‹" else "›", labels.nextMonth,
                    enabled && runCatching { firstMonth.plusMonths(1) }.getOrNull()
                        ?.let { monthHasBoundedDay(it, minDate, maxDate) } == true) { moveMonth(1) }
                RangeNavButton(if (layoutDirection == LayoutDirection.Rtl) "«" else "»", labels.nextYear,
                    enabled && runCatching { firstMonth.plusYears(1) }.getOrNull()
                        ?.let { monthHasBoundedDay(it, minDate, maxDate) } == true) { moveMonth(12) }
            }
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(metrics.monthGap)) {
                repeat(visibleMonths) { offset ->
                    val month = firstMonth.plusMonths(offset.toLong())
                    RangeMonth(month, value, resolvedLocale, today, labels, fullDateFormatter,
                        dayFormatter, enabled, minDate, maxDate, isDateEnabled,
                        requesters, layoutDirection,
                        onSelect = { date ->
                            onValueChange(nextDateRange(value, date, allowSingleDayRange, boundaryToModify))
                            focusedDayText = date.toString()
                        },
                        onNavigate = { date, delta -> moveDay(date, delta) },
                        onNavigateMonth = { date, amount ->
                            val candidate = runCatching { date.plusMonths(amount) }.getOrNull()
                            if (candidate != null && canSelectDate(candidate, minDate, maxDate, isDateEnabled)) {
                                focusedDayText = candidate.toString()
                                firstMonthText = YearMonth.from(candidate).toString()
                                requestDayFocus = true
                            }
                        },
                    )
                }
            }
            if (shortcuts.isNotEmpty()) {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(BraceTheme.spacing.xs)) {
                    shortcuts.forEach { shortcut ->
                        BraceButton(shortcut.label, onClick = {
                            onValueChange(shortcut.range)
                            shortcut.range.start?.let { firstMonthText = YearMonth.from(it).toString() }
                        }, enabled = enabled && canSelectRangeShortcut(shortcut, minDate, maxDate,
                            isDateEnabled, allowSingleDayRange), variant = BraceButtonVariant.Outline)
                    }
                }
            }
            if (showClear) {
                BraceButton(labels.clear, onClick = { onValueChange(BraceDateRange()) },
                    enabled = enabled && value != BraceDateRange(), variant = BraceButtonVariant.Outline)
            }
        }
    }
}

@Composable
private fun RangeMonth(
    month: YearMonth,
    value: BraceDateRange,
    locale: Locale,
    today: LocalDate,
    labels: RangeLabels,
    fullDateFormatter: DateTimeFormatter,
    dayFormatter: DateTimeFormatter,
    enabled: Boolean,
    minDate: LocalDate?,
    maxDate: LocalDate?,
    isDateEnabled: (LocalDate) -> Boolean,
    requesters: Map<LocalDate, FocusRequester>,
    layoutDirection: LayoutDirection,
    onSelect: (LocalDate) -> Unit,
    onNavigate: (LocalDate, Int) -> Unit,
    onNavigateMonth: (LocalDate, Long) -> Unit,
) {
    val colors = BraceTheme.colors.components.dateRangePicker
    val metrics = BraceTheme.componentMetrics.dateRangePicker
    val shape = RoundedCornerShape(metrics.cornerRadius)
    val cells = remember(month, locale) { monthDates(month, locale) }
    val firstWeekday = remember(locale) { WeekFields.of(locale).firstDayOfWeek }
    Column(Modifier.width(metrics.daySize * 7)) {
        Text(DateTimeFormatter.ofPattern("LLLL yyyy", locale)
            .withDecimalStyle(DecimalStyle.of(locale)).format(month.atDay(1)),
            color = colors.content, style = BraceTheme.typography.label)
        Row(horizontalArrangement = Arrangement.spacedBy(metrics.columnGap)) {
            repeat(7) { index ->
                val weekday = firstWeekday.plus(index.toLong())
                Box(Modifier.size(metrics.daySize), contentAlignment = Alignment.Center) {
                    Text(weekday.getDisplayName(java.time.format.TextStyle.NARROW, locale),
                        color = colors.mutedContent, style = BraceTheme.typography.label,
                        modifier = Modifier.clearAndSetSemantics {
                            contentDescription = weekday.getDisplayName(java.time.format.TextStyle.FULL, locale)
                        })
                }
            }
        }
        Column(Modifier.semantics { collectionInfo = CollectionInfo(cells.size / 7, 7) }) {
            cells.chunked(7).forEachIndexed { rowIndex, week ->
                Row(horizontalArrangement = Arrangement.spacedBy(metrics.columnGap)) {
                    week.forEachIndexed { columnIndex, date ->
                        if (date == null) Spacer(Modifier.size(metrics.daySize))
                        else {
                            val selectable = enabled && canSelectDate(date, minDate, maxDate, isDateEnabled)
                            val endpoint = date == value.start || date == value.end
                            val inRange = dateInSelectedRange(date, value)
                            val source = remember(date) { MutableInteractionSource() }
                            val focused by source.collectIsFocusedAsState()
                            val hovered by source.collectIsHoveredAsState()
                            val state = listOfNotNull(
                                labels.start.takeIf { date == value.start },
                                labels.end.takeIf { date == value.end },
                                labels.inRange.takeIf { inRange && !endpoint },
                                labels.today.takeIf { date == today },
                            ).joinToString(", ")
                            Box(Modifier.size(metrics.daySize)
                                .focusRequester(requesters.getValue(date))
                                .onPreviewKeyEvent { event ->
                                    if (event.type != KeyEventType.KeyDown || !enabled) false
                                    else when (event.key) {
                                        Key.DirectionLeft -> { onNavigate(date,
                                            if (layoutDirection == LayoutDirection.Rtl) 1 else -1); true }
                                        Key.DirectionRight -> { onNavigate(date,
                                            if (layoutDirection == LayoutDirection.Rtl) -1 else 1); true }
                                        Key.DirectionUp -> { onNavigate(date, -7); true }
                                        Key.DirectionDown -> { onNavigate(date, 7); true }
                                        Key.PageUp, Key.PageDown -> {
                                            val amount = if (event.key == Key.PageUp) -1L else 1L
                                            onNavigateMonth(date, if (event.isCtrlPressed) amount * 12 else amount)
                                            true
                                        }
                                        else -> false
                                    }
                                }
                                .clearAndSetSemantics {
                                    contentDescription = fullDateFormatter.format(date)
                                    role = Role.Button
                                    selected = endpoint
                                    if (state.isNotEmpty()) stateDescription = state
                                    collectionItemInfo = CollectionItemInfo(rowIndex, 1, columnIndex, 1)
                                    if (!selectable) disabled() else {
                                        this.focused = focused
                                        onClick { onSelect(date); true }
                                        requestFocus { requesters.getValue(date).requestFocus() }
                                    }
                                }
                                .clickable(enabled = selectable, role = Role.Button,
                                    interactionSource = source, indication = null) { onSelect(date) }
                                .background(when {
                                    endpoint -> colors.selectedContainer
                                    inRange -> colors.rangeContainer
                                    hovered && selectable -> colors.hoverContainer
                                    else -> colors.container
                                }, shape)
                                .border(BorderStroke(
                                    if (focused) BraceTheme.sizing.focusRingWidth else BraceTheme.sizing.borderWidth,
                                    when {
                                        focused -> colors.focusRing
                                        date == today -> colors.todayBorder
                                        else -> Color.Transparent
                                    }), shape), contentAlignment = Alignment.Center) {
                                Text(dayFormatter.format(date), style = BraceTheme.typography.body,
                                    color = when {
                                        !selectable -> colors.disabledContent
                                        endpoint -> colors.selectedContent
                                        inRange -> colors.rangeContent
                                        else -> colors.content
                                    })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RangeNavButton(glyph: String, label: String, enabled: Boolean, onClick: () -> Unit) {
    val colors = BraceTheme.colors.components.dateRangePicker
    val metrics = BraceTheme.componentMetrics.dateRangePicker
    val shape = RoundedCornerShape(metrics.cornerRadius)
    val source = remember { MutableInteractionSource() }
    val requester = remember { FocusRequester() }
    val focused by source.collectIsFocusedAsState()
    val hovered by source.collectIsHoveredAsState()
    Box(Modifier.size(metrics.daySize).focusRequester(requester)
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
        .background(if (hovered && enabled) colors.hoverContainer else colors.container, shape)
        .border(if (focused) BraceTheme.sizing.focusRingWidth else BraceTheme.sizing.borderWidth,
            if (focused) colors.focusRing else Color.Transparent, shape),
        contentAlignment = Alignment.Center) {
        Text(glyph, color = if (enabled) colors.content else colors.disabledContent,
            style = BraceTheme.typography.subtitle)
    }
}
