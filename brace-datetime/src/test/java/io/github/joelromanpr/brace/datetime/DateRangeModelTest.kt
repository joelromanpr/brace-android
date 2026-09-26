package io.github.joelromanpr.brace.datetime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DateRangeModelTest {
    private val early = LocalDate.of(2026, 2, 10)
    private val late = LocalDate.of(2026, 2, 20)

    @Test fun reversedRangeCannotBeConstructed() {
        assertThrows(IllegalArgumentException::class.java) { BraceDateRange(late, early) }
    }

    @Test fun defaultSelectionOrdersEndpointsAndRestartsAfterCompleteRange() {
        assertEquals(BraceDateRange(early, null), nextDateRange(BraceDateRange(), early, false))
        assertEquals(BraceDateRange(early, late),
            nextDateRange(BraceDateRange(late, null), early, false))
        assertEquals(BraceDateRange(late.plusDays(1), null),
            nextDateRange(BraceDateRange(early, late), late.plusDays(1), false))
    }

    @Test fun equalDayPolicyAndEndpointClearAreExplicit() {
        assertEquals(BraceDateRange(), nextDateRange(BraceDateRange(early, null), early, false))
        assertEquals(BraceDateRange(early, early), nextDateRange(BraceDateRange(early, null), early, true))
        assertEquals(BraceDateRange(null, late), nextDateRange(BraceDateRange(early, late), early, false))
        assertEquals(BraceDateRange(early, null), nextDateRange(BraceDateRange(early, late), late, false))
    }

    @Test fun focusedBoundarySwapPreservesChronologicalOrder() {
        assertEquals(BraceDateRange(early, late),
            nextDateRange(BraceDateRange(late, null), early, false, BraceRangeBoundary.End))
        assertEquals(BraceDateRange(early, late),
            nextDateRange(BraceDateRange(null, early), late, false, BraceRangeBoundary.Start))
    }

    @Test fun editingStartAtEndRespectsSingleDayPolicy() {
        assertEquals(BraceDateRange(null, late),
            nextDateRange(BraceDateRange(early, late), late, false, BraceRangeBoundary.Start))
        assertEquals(BraceDateRange(late, late),
            nextDateRange(BraceDateRange(early, late), late, true, BraceRangeBoundary.Start))
    }

    @Test fun shortcutsRespectBoundsAndSingleDayPolicy() {
        val shortcut = BraceDateRangeShortcut("Window", BraceDateRange(early, late))
        assertTrue(canSelectRangeShortcut(shortcut, early, late, { true }, false))
        assertFalse(canSelectRangeShortcut(shortcut, early.plusDays(1), late, { true }, false))
        assertFalse(canSelectRangeShortcut(shortcut, early, late, { it != late }, false))
        val oneDay = BraceDateRangeShortcut("Today", BraceDateRange(early, early))
        assertFalse(canSelectRangeShortcut(oneDay, null, null, { true }, false))
        assertTrue(canSelectRangeShortcut(oneDay, null, null, { true }, true))
    }
}
