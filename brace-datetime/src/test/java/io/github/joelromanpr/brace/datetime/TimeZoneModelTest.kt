package io.github.joelromanpr.brace.datetime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

class TimeZoneModelTest {
    private val winter = Instant.parse("2026-01-15T12:00:00Z")
    private val summer = Instant.parse("2026-07-15T12:00:00Z")
    private val losAngeles = ZoneId.of("America/Los_Angeles")

    @Test fun offsetsAndNamesUseReferenceInstantWithoutChangingIdentity() {
        val january = timeZoneOption(losAngeles, winter, Locale.US, ZoneId.of("UTC"))
        val july = timeZoneOption(losAngeles, summer, Locale.US, ZoneId.of("UTC"))
        assertEquals(losAngeles, january.zone)
        assertEquals(january.zone, july.zone)
        assertEquals("UTC-08:00", january.offsetLabel)
        assertEquals("UTC-07:00", july.offsetLabel)
        assertTrue(january.displayName.contains("Standard"))
        assertTrue(july.displayName.contains("Daylight"))
    }

    @Test fun localZonePriorityAndSelectedZoneArePreserved() {
        val ids = setOf("Europe/Paris", "America/New_York")
        val local = ZoneId.of("Europe/Paris")
        val selected = ZoneId.of("Asia/Tokyo")
        val options = timeZoneOptions(winter, Locale.US, local, selected, true, ids)
        assertEquals(local, options.first().zone)
        assertTrue(options.first().isLocal)
        assertTrue(options.any { it.zone == selected })
        assertTrue(options.any { it.zone.id == "UTC" })
        assertEquals(options.size, options.map { it.zone.id }.toSet().size)
    }

    @Test fun queryMatchesIdentifierWordsNameAndOffsetLiterally() {
        val options = timeZoneOptions(winter, Locale.US, ZoneId.of("UTC"), null, false,
            setOf("America/New_York", "Pacific/Honolulu"))
        assertEquals("America/New_York", filterTimeZones("new york", options).single().zone.id)
        assertEquals("Pacific/Honolulu", filterTimeZones("honolulu", options).single().zone.id)
        assertTrue(filterTimeZones("UTC-05:00", options).any { it.zone.id == "America/New_York" })
        assertTrue(filterTimeZones("[not a zone]", options).isEmpty())
    }

    @Test fun displayModesDoNotDiscardZoneIdentity() {
        val option = timeZoneOption(losAngeles, summer, Locale.US, ZoneId.of("UTC"))
        assertEquals("America/Los_Angeles", displayTimeZone(option, BraceTimeZoneDisplay.Identifier))
        assertEquals("UTC-07:00", displayTimeZone(option, BraceTimeZoneDisplay.Offset))
        assertEquals(option.displayName, displayTimeZone(option, BraceTimeZoneDisplay.LongName))
        assertEquals(option.shortName, displayTimeZone(option, BraceTimeZoneDisplay.Abbreviation))
        assertTrue(displayTimeZone(option, BraceTimeZoneDisplay.Composite).contains("America/Los_Angeles"))
        assertFalse(option.isLocal)
    }
}
