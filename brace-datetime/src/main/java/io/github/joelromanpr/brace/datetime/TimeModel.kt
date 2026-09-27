package io.github.joelromanpr.brace.datetime

import android.text.format.DateFormat
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.DecimalStyle
import java.time.format.ResolverStyle
import java.util.Locale

/** The smallest editable unit displayed by [BraceTimePicker] and [BraceTimeField]. */
public enum class BraceTimePrecision { Minute, Second, Millisecond }

internal enum class TimeUnit { Hour, Minute, Second, Millisecond }

internal fun isTwentyFourHour(locale: Locale, overrideValue: Boolean?): Boolean =
    overrideValue ?: DateFormat.getBestDateTimePattern(locale, "jm").any { it == 'H' || it == 'k' }

internal fun timeFormatter(locale: Locale, twentyFourHour: Boolean, precision: BraceTimePrecision): DateTimeFormatter {
    val skeleton = (if (twentyFourHour) "H" else "h") + when (precision) {
        BraceTimePrecision.Minute -> "m"
        BraceTimePrecision.Second -> "ms"
        BraceTimePrecision.Millisecond -> "msSSS"
    }
    val localizedPattern = DateFormat.getBestDateTimePattern(locale, skeleton)
    // ICU may return one fractional digit for a skeleton; the API always shows millisecond precision.
    val pattern = if (precision == BraceTimePrecision.Millisecond)
        localizedPattern.replace(Regex("S+"), "SSS") else localizedPattern
    return DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern(pattern).toFormatter(locale)
        .withDecimalStyle(DecimalStyle.of(locale)).withResolverStyle(ResolverStyle.STRICT)
}

internal fun parseTimeDraft(draft: String, formatter: DateTimeFormatter): LocalTime? =
    runCatching { LocalTime.parse(draft.trim(), formatter) }.getOrNull()

internal fun parseLocalizedUnit(draft: String): Int? {
    if (draft.isEmpty()) return null
    var result = 0
    for (char in draft) {
        val digit = Character.digit(char, 10)
        if (digit !in 0..9) return null
        result = result * 10 + digit
    }
    return result
}

internal fun localizedUnit(value: Int, width: Int, locale: Locale): String {
    val zero = DecimalStyle.of(locale).zeroDigit
    return value.toString().padStart(width, '0').map { (zero.code + (it - '0')).toChar() }.joinToString("")
}

internal fun timeInBounds(value: LocalTime, minTime: LocalTime?, maxTime: LocalTime?): Boolean {
    if (minTime == null) return maxTime == null || !value.isAfter(maxTime)
    if (maxTime == null) return !value.isBefore(minTime)
    if (minTime == maxTime) return value == minTime
    return if (minTime.isBefore(maxTime))
        !value.isBefore(minTime) && !value.isAfter(maxTime)
    else !value.isBefore(minTime) || !value.isAfter(maxTime)
}

internal fun atPrecision(value: LocalTime, precision: BraceTimePrecision): LocalTime = when (precision) {
    BraceTimePrecision.Minute -> value.withSecond(0).withNano(0)
    BraceTimePrecision.Second -> value.withNano(0)
    BraceTimePrecision.Millisecond -> value.withNano((value.nano / 1_000_000) * 1_000_000)
}

internal fun changeUnit(value: LocalTime, unit: TimeUnit, amount: Int): LocalTime = when (unit) {
    TimeUnit.Hour -> value.withHour(Math.floorMod(value.hour + amount, 24))
    TimeUnit.Minute -> value.withMinute(Math.floorMod(value.minute + amount, 60))
    TimeUnit.Second -> value.withSecond(Math.floorMod(value.second + amount, 60))
    TimeUnit.Millisecond -> value.withNano(Math.floorMod(value.nano / 1_000_000 + amount, 1000) * 1_000_000)
}

internal fun setUnit(value: LocalTime, unit: TimeUnit, entered: Int, twelveHour: Boolean): LocalTime? = when (unit) {
    TimeUnit.Hour -> if (twelveHour && entered in 1..12)
        value.withHour((entered % 12) + if (value.hour >= 12) 12 else 0)
    else if (!twelveHour && entered in 0..23) value.withHour(entered) else null
    TimeUnit.Minute -> if (entered in 0..59) value.withMinute(entered) else null
    TimeUnit.Second -> if (entered in 0..59) value.withSecond(entered) else null
    TimeUnit.Millisecond -> if (entered in 0..999) value.withNano(entered * 1_000_000) else null
}
