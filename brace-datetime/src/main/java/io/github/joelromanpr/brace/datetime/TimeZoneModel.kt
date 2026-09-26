package io.github.joelromanpr.brace.datetime

import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Locale
import java.util.TimeZone

/** The visible identity and reference-instant offset of one IANA time zone. */
internal data class BraceTimeZoneOption(
    val zone: ZoneId,
    val offsetLabel: String,
    val displayName: String,
    val shortName: String,
    val isLocal: Boolean,
)

/** Display mode for the selected time-zone trigger. */
public enum class BraceTimeZoneDisplay { Abbreviation, Identifier, LongName, Offset, Composite }

internal fun offsetLabel(zone: ZoneId, instant: Instant): String {
    val offset = zone.rules.getOffset(instant)
    val id = if (offset == ZoneOffset.UTC) "+00:00" else offset.id
    return "UTC$id"
}

internal fun timeZoneOption(
    zone: ZoneId,
    instant: Instant,
    locale: Locale,
    systemZone: ZoneId,
): BraceTimeZoneOption {
    val timeZone = TimeZone.getTimeZone(zone)
    val isDaylight = zone.rules.isDaylightSavings(instant)
    return BraceTimeZoneOption(
        zone = zone,
        offsetLabel = offsetLabel(zone, instant),
        displayName = timeZone.getDisplayName(isDaylight, TimeZone.LONG, locale),
        shortName = timeZone.getDisplayName(isDaylight, TimeZone.SHORT, locale),
        isLocal = zone == systemZone,
    )
}

internal fun timeZoneOptions(
    instant: Instant,
    locale: Locale,
    systemZone: ZoneId,
    selectedZone: ZoneId?,
    showLocalTimeZone: Boolean,
    availableIds: Set<String> = ZoneId.getAvailableZoneIds(),
): List<BraceTimeZoneOption> {
    val ids = (availableIds + listOfNotNull(selectedZone?.id, systemZone.id, "UTC"))
        .mapNotNull { runCatching { ZoneId.of(it) }.getOrNull() }
        .distinctBy { it.id }
    val sorted = ids.map { timeZoneOption(it, instant, locale, systemZone) }
        .sortedBy { it.zone.id.lowercase(Locale.ROOT) }
    return if (showLocalTimeZone) sorted.sortedWith(
        compareByDescending<BraceTimeZoneOption> { it.isLocal }
            .thenBy { it.zone.id.lowercase(Locale.ROOT) },
    ) else sorted
}

internal fun filterTimeZones(query: String, options: List<BraceTimeZoneOption>): List<BraceTimeZoneOption> {
    val terms = query.trim().lowercase(Locale.ROOT).split(Regex("\\s+")).filter(String::isNotEmpty)
    if (terms.isEmpty()) return options
    return options.filter { option ->
        val searchable = listOf(option.zone.id, option.zone.id.replace('_', ' '),
            option.displayName, option.shortName, option.offsetLabel).joinToString(" ").lowercase(Locale.ROOT)
        terms.all(searchable::contains)
    }
}

internal fun displayTimeZone(option: BraceTimeZoneOption, display: BraceTimeZoneDisplay): String =
    when (display) {
        BraceTimeZoneDisplay.Abbreviation -> option.shortName
        BraceTimeZoneDisplay.Identifier -> option.zone.id
        BraceTimeZoneDisplay.LongName -> option.displayName
        BraceTimeZoneDisplay.Offset -> option.offsetLabel
        BraceTimeZoneDisplay.Composite -> "${option.zone.id} (${option.offsetLabel})"
    }
