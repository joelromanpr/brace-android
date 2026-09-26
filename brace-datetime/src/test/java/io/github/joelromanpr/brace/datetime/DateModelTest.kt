package io.github.joelromanpr.brace.datetime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale

class DateModelTest {
    @Test fun monthGridUsesLocaleFirstDayAndRealLength() {
        val us = monthDates(YearMonth.of(2026, 2), Locale.US)
        val fr = monthDates(YearMonth.of(2026, 2), Locale.FRANCE)
        assertEquals(LocalDate.of(2026, 2, 1), us.first())
        assertNull(fr.first())
        assertEquals(LocalDate.of(2026, 2, 1), fr[6])
        assertEquals(28, us.filterNotNull().size)
    }

    @Test fun localeParserRejectsRolloverAndTrailingInput() {
        val formatter = localizedDateFormatter(Locale.US)
        val shown = formatter.format(LocalDate.of(2026, 2, 14))
        assertEquals(LocalDate.of(2026, 2, 14), parseLocalizedDate(shown, Locale.US))
        assertNull(parseLocalizedDate("2/30/26", Locale.US))
        assertNull(parseLocalizedDate("2/14/26 extra", Locale.US))
        assertNull(parseLocalizedDate("", Locale.US))
        val french = localizedDateFormatter(Locale.FRANCE).format(LocalDate.of(2026, 2, 14))
        assertEquals(LocalDate.of(2026, 2, 14), parseLocalizedDate(french, Locale.FRANCE))
        val arabic = Locale.forLanguageTag("ar-EG")
        val arabicText = localizedDateFormatter(arabic).format(LocalDate.of(2026, 2, 14))
        assertEquals(LocalDate.of(2026, 2, 14), parseLocalizedDate(arabicText, arabic))
    }

    @Test fun boundsAndPredicateApplyTogether() {
        val date = LocalDate.of(2026, 2, 14)
        assertTrue(canSelectDate(date, date, date) { true })
        assertFalse(canSelectDate(date, date.plusDays(1), null) { true })
        assertFalse(canSelectDate(date, null, date.minusDays(1)) { true })
        assertFalse(canSelectDate(date, null, null) { false })
        assertFalse(monthHasBoundedDay(YearMonth.of(2026, 1), date, null))
        assertEquals(YearMonth.of(2026, 2),
            monthWithinBounds(YearMonth.of(1900, 1), date, date))
        assertEquals(YearMonth.of(2026, 2),
            monthWithinBounds(YearMonth.of(2100, 1), date, date))
    }
}
