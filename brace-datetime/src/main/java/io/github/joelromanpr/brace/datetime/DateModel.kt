package io.github.joelromanpr.brace.datetime

import java.text.ParsePosition
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.DecimalStyle
import java.time.format.FormatStyle
import java.time.format.ResolverStyle
import java.time.temporal.ChronoField
import java.time.temporal.WeekFields
import java.util.Locale

/** A labeled date shortcut supplied to [BraceDatePicker]. */
public data class BraceDateShortcut(val label: String, val date: LocalDate)

internal fun localizedDateFormatter(locale: Locale): DateTimeFormatter =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).withLocale(locale)
        .withDecimalStyle(DecimalStyle.of(locale)).withResolverStyle(ResolverStyle.SMART)

internal fun parseLocalizedDate(text: String, locale: Locale): LocalDate? {
    val raw = text.trim()
    if (raw.isEmpty()) return null
    return runCatching {
        val formatter = localizedDateFormatter(locale)
        val cursor = ParsePosition(0)
        val fields = formatter.parseUnresolved(raw, cursor)
        if (fields == null || cursor.errorIndex >= 0 || cursor.index != raw.length) return null
        val date = LocalDate.parse(raw, formatter)
        // SMART parsing can silently turn February 30 into February 28. Reject that.
        if (fields.isSupported(ChronoField.DAY_OF_MONTH) &&
            fields.get(ChronoField.DAY_OF_MONTH) != date.dayOfMonth) return null
        if (fields.isSupported(ChronoField.MONTH_OF_YEAR) &&
            fields.get(ChronoField.MONTH_OF_YEAR) != date.monthValue) return null
        date
    }.getOrNull()
}

internal fun canSelectDate(
    date: LocalDate,
    minDate: LocalDate?,
    maxDate: LocalDate?,
    isDateEnabled: (LocalDate) -> Boolean,
): Boolean = (minDate == null || !date.isBefore(minDate)) &&
    (maxDate == null || !date.isAfter(maxDate)) && isDateEnabled(date)

internal fun monthDates(month: YearMonth, locale: Locale): List<LocalDate?> {
    val firstDay = WeekFields.of(locale).firstDayOfWeek
    val leading = (month.atDay(1).dayOfWeek.value - firstDay.value + 7) % 7
    val count = ((leading + month.lengthOfMonth() + 6) / 7) * 7
    return List(count) { index ->
        val day = index - leading + 1
        if (day in 1..month.lengthOfMonth()) month.atDay(day) else null
    }
}

internal fun monthHasBoundedDay(month: YearMonth, minDate: LocalDate?, maxDate: LocalDate?): Boolean =
    (minDate == null || !month.atEndOfMonth().isBefore(minDate)) &&
    (maxDate == null || !month.atDay(1).isAfter(maxDate))

internal fun monthWithinBounds(month: YearMonth, minDate: LocalDate?, maxDate: LocalDate?): YearMonth =
    when {
        minDate != null && month.isBefore(YearMonth.from(minDate)) -> YearMonth.from(minDate)
        maxDate != null && month.isAfter(YearMonth.from(maxDate)) -> YearMonth.from(maxDate)
        else -> month
    }
