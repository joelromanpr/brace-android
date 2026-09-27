package io.github.joelromanpr.brace.datetime

import java.time.LocalDate

/** A controlled date-only range. Either endpoint may be absent while selecting or editing. */
public data class BraceDateRange(
    val start: LocalDate? = null,
    val end: LocalDate? = null,
) {
    init { require(start == null || end == null || !start.isAfter(end)) { "start must be on or before end" } }
}

/** Endpoint to edit when [BraceDateRangePicker] is used from a two-field form. */
public enum class BraceRangeBoundary { Start, End }

/** A labeled range shortcut supplied by an application. */
public data class BraceDateRangeShortcut(val label: String, val range: BraceDateRange)

internal fun nextDateRange(
    current: BraceDateRange,
    date: LocalDate,
    allowSingleDayRange: Boolean,
    boundary: BraceRangeBoundary? = null,
): BraceDateRange {
    val start = current.start
    val end = current.end
    if (boundary == BraceRangeBoundary.Start) {
        if (date == start) return if (start == end) BraceDateRange() else BraceDateRange(null, end)
        if (end != null && date == end && !allowSingleDayRange) return BraceDateRange(null, end)
        return if (end != null && date.isAfter(end)) BraceDateRange(end, date)
        else BraceDateRange(date, end)
    }
    if (boundary == BraceRangeBoundary.End) {
        if (date == end) return if (start == end) BraceDateRange() else BraceDateRange(start, null)
        if (start != null && date == start && !allowSingleDayRange) return BraceDateRange(start, null)
        return if (start != null && date.isBefore(start)) BraceDateRange(date, start)
        else BraceDateRange(start, date)
    }
    return when {
        start == null && end == null -> BraceDateRange(date, null)
        start != null && end == null -> when {
            date == start -> if (allowSingleDayRange) BraceDateRange(start, start) else BraceDateRange()
            date.isBefore(start) -> BraceDateRange(date, start)
            else -> BraceDateRange(start, date)
        }
        start == null && end != null -> when {
            date == end -> if (allowSingleDayRange) BraceDateRange(end, end) else BraceDateRange()
            date.isAfter(end) -> BraceDateRange(end, date)
            else -> BraceDateRange(date, end)
        }
        start == end -> if (date == start) BraceDateRange() else BraceDateRange(date, null)
        date == start -> BraceDateRange(null, end)
        date == end -> BraceDateRange(start, null)
        else -> BraceDateRange(date, null)
    }
}

internal fun dateInSelectedRange(date: LocalDate, value: BraceDateRange): Boolean =
    value.start != null && value.end != null &&
        !date.isBefore(value.start) && !date.isAfter(value.end)

/** Validates every calendar day in a complete range, including days between its endpoints. */
internal fun canSelectContinuousRange(
    range: BraceDateRange,
    minDate: LocalDate?,
    maxDate: LocalDate?,
    isDateEnabled: (LocalDate) -> Boolean,
): Boolean {
    val start = range.start ?: return false
    val end = range.end ?: return false
    var day = start
    while (true) {
        if (!canSelectDate(day, minDate, maxDate, isDateEnabled)) return false
        if (day == end) return true
        day = day.plusDays(1)
    }
}

internal fun canSelectRangeCandidate(
    current: BraceDateRange,
    date: LocalDate,
    allowSingleDayRange: Boolean,
    boundary: BraceRangeBoundary?,
    minDate: LocalDate?,
    maxDate: LocalDate?,
    isDateEnabled: (LocalDate) -> Boolean,
): Boolean {
    if (!canSelectDate(date, minDate, maxDate, isDateEnabled)) return false
    val next = nextDateRange(current, date, allowSingleDayRange, boundary)
    return next.start == null || next.end == null ||
        canSelectContinuousRange(next, minDate, maxDate, isDateEnabled)
}

internal fun canSelectRangeShortcut(
    shortcut: BraceDateRangeShortcut,
    minDate: LocalDate?,
    maxDate: LocalDate?,
    isDateEnabled: (LocalDate) -> Boolean,
    allowSingleDayRange: Boolean,
): Boolean {
    val start = shortcut.range.start
    val end = shortcut.range.end
    if (start == null || end == null) return false
    if (!allowSingleDayRange && start == end) return false
    return canSelectContinuousRange(shortcut.range, minDate, maxDate, isDateEnabled)
}
