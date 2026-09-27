# Time-zone selection

`brace-datetime` contains the in-progress Android adaptation of the pinned Blueprint 6.18.0 [TimezoneSelect](https://blueprintjs.com/docs/#datetime/timezone-select) row. The catalog app has an interactive search, local-zone ordering, winter/summer reference dates, all five trigger display modes, and a disabled state. This API has not been released to Maven Central or marked stable.

```kotlin
var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
BraceTimeZoneSelect(
    value = selectedId?.let(ZoneId::of),
    onValueChange = { selectedId = it.id },
    label = "Reporting time zone",
    locale = Locale.US,
    referenceInstant = Instant.parse("2026-07-15T12:00:00Z"),
    showLocalTimeZone = true,
)
```

The caller controls the selected `ZoneId` and should save its `id` string for activity restoration. Brace obtains the list from Android's time-zone database and searches IANA identifiers, localized long names, city words, and UTC offsets. `showLocalTimeZone` places `systemZone` first; the full list stays searchable and lazy rendered. `referenceInstant` determines the offset and standard or daylight name. Changing it updates the displayed offset without replacing the selected zone. Without an explicit reference, `clock` is sampled once at first composition. `BraceTimeZoneDisplay.Abbreviation`, `Identifier`, `LongName`, `Offset`, and `Composite` change the trigger's visible text. `placeholder`, `enabled`, and `fill` adapt the trigger to a form.

A 48 dp button opens a focusable Compose popover. Search retains focus while Up/Down, Home/End, Enter, and IME Search operate results; Tab can move to a row. Touch, mouse, and TalkBack activate rows. Escape, Back, and outside click dismiss the popover. The open state and query survive activity recreation; closing resets the query. Search and row labels expose the active result, selected state, IANA identity, localized name, and reference offset. Component colors and dimensions come from `timeZoneSelect` tokens, including high contrast and compact modes.

Blueprint's JavaScript string and `Date` props become a typed `ZoneId` and `Instant`. Android's time-zone database replaces Blueprint's bundled web list and browser local-zone detection; its version may differ by device and OS update. The Compose popover replaces DOM portal and Select wrappers. `buttonProps`, `inputProps`, `popoverProps`, and custom React children have no direct one-to-one API. Custom Compose trigger and filter slots, plus manual TalkBack, large-text, RTL, physical-keyboard, and device QA, remain open before this row can be stable. Automated RTL, 2× text, mouse, target-size, and supported accessibility checks pass in the current test suite. No Blueprint assets or source were copied.

See the [component list](coverage.md) for current status, code, examples, and tests.
