package io.github.joelromanpr.brace.datetime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime
import java.util.Locale

class TimeModelTest {
    @Test fun normalAndOvernightBoundsIncludeBothEdges() {
        val morning = LocalTime.of(9, 0)
        val evening = LocalTime.of(17, 0)
        assertTrue(timeInBounds(morning, morning, evening))
        assertTrue(timeInBounds(evening, morning, evening))
        assertFalse(timeInBounds(LocalTime.of(8, 59), morning, evening))
        assertFalse(timeInBounds(LocalTime.of(17, 1), morning, evening))
        val late = LocalTime.of(22, 0)
        val early = LocalTime.of(2, 0)
        assertTrue(timeInBounds(late, late, early))
        assertTrue(timeInBounds(LocalTime.of(0, 30), late, early))
        assertTrue(timeInBounds(early, late, early))
        assertFalse(timeInBounds(LocalTime.NOON, late, early))
        assertTrue(timeInBounds(morning, morning, morning))
        assertFalse(timeInBounds(morning.plusMinutes(1), morning, morning))
    }

    @Test fun unitWrappingPreservesOtherVisibleUnits() {
        val late = LocalTime.of(23, 45, 59, 999_000_000)
        assertEquals(LocalTime.of(0, 45, 59, 999_000_000), changeUnit(late, TimeUnit.Hour, 1))
        assertEquals(LocalTime.of(23, 0, 59, 999_000_000), changeUnit(LocalTime.of(23, 59, 59, 999_000_000), TimeUnit.Minute, 1))
        assertEquals(LocalTime.of(23, 45, 0, 999_000_000), changeUnit(late, TimeUnit.Second, 1))
        assertEquals(LocalTime.of(23, 45, 59, 0), changeUnit(late, TimeUnit.Millisecond, 1))
    }

    @Test fun localizedNumericUnitsParseUnicodeDigits() {
        val arabic = Locale.forLanguageTag("ar-EG")
        assertEquals(42, parseLocalizedUnit(localizedUnit(42, 2, arabic)))
        assertEquals(null, parseLocalizedUnit("4x"))
        assertEquals(null, parseLocalizedUnit(""))
    }

    @Test fun twelveHourEntryUsesCurrentPeriodAndPrecisionClearsHiddenUnits() {
        assertEquals(LocalTime.of(0, 30), setUnit(LocalTime.of(1, 30), TimeUnit.Hour, 12, true))
        assertEquals(LocalTime.of(12, 30), setUnit(LocalTime.of(13, 30), TimeUnit.Hour, 12, true))
        assertEquals(LocalTime.of(23, 30), setUnit(LocalTime.of(13, 30), TimeUnit.Hour, 11, true))
        assertEquals(null, setUnit(LocalTime.NOON, TimeUnit.Hour, 0, true))
        val precise = LocalTime.of(14, 30, 12, 345_678_000)
        assertEquals(LocalTime.of(14, 30), atPrecision(precise, BraceTimePrecision.Minute))
        assertEquals(LocalTime.of(14, 30, 12), atPrecision(precise, BraceTimePrecision.Second))
        assertEquals(LocalTime.of(14, 30, 12, 345_000_000), atPrecision(precise, BraceTimePrecision.Millisecond))
    }
}
