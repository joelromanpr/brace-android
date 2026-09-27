# Time picker and time field

`brace-datetime` implements the pinned Blueprint [TimePicker](https://blueprintjs.com/docs/#datetime/timepicker) row with two native Compose APIs: `BraceTimePicker` for direct unit adjustment and `BraceTimeField` for localized text entry with an anchored picker. Blueprint 6.18.0 has **no separate documented TimeInput component**, so the field is an adjunct to the single inventory row. This slice is available in the `0.1.0-alpha01` preview and remains **in progress**; it does not raise stable Blueprint coverage.

## Add the datetime artifact

Add the artifact from Maven Central:

```kotlin
implementation("io.github.joelromanpr.brace:brace-datetime:0.1.0-alpha01")
```

The artifact depends on Brace foundation and core. See [installation](installation.md) for the local Maven repository and supported toolchain.

## Choose a time

```kotlin
var selected by rememberSaveable { mutableStateOf("23:30") }
BraceTimePicker(
    value = LocalTime.parse(selected),
    onValueChange = { selected = it.toString() },
    locale = Locale.US,
    use24Hour = true,
    precision = BraceTimePrecision.Minute,
    minTime = LocalTime.of(22, 0),
    maxTime = LocalTime.of(2, 0), // Overnight window.
)
```

The caller owns and restores the selected `LocalTime`. The picker displays hour and minute by default; `Second` and `Millisecond` precision add corresponding segments. A locale chooses the default 12 or 24 hour cycle, and `use24Hour` can override it. In 12 hour mode, an explicit day-period action switches AM/PM. Each segment accepts digits and commits on Done or focus loss. Up/Down keys and 48 dp increment/decrement controls adjust one unit. Values outside inclusive bounds are rejected, including when a step would cross a range edge. A later minimum than maximum expresses a window spanning midnight; equal bounds allow only that time. Bounds are checked against the full `LocalTime`, and smaller units are cleared when a value is emitted at the selected precision. A caller-supplied out-of-range value is displayed without a hidden callback; the owner should provide a valid initial value.

## Enter a time

```kotlin
var dueTime by rememberSaveable { mutableStateOf<String?>(null) }
BraceTimeField(
    value = dueTime?.let(LocalTime::parse),
    onValueChange = { dueTime = it?.toString() },
    label = "Due time",
    locale = Locale.US,
    minTime = LocalTime.of(9, 0),
    maxTime = LocalTime.of(17, 0),
    onInvalidInput = { draft -> logInvalidTime(draft) },
)
```

The field uses Android's locale time pattern and numerals for its chosen precision. Valid complete drafts emit immediately. Impossible or unavailable times remain visible, show an error, and call `onInvalidInput` when editing finishes. Empty text clears the selection. The clock button and Alt+Down open the picker. Done, Back, Escape, and an outside click close it; the popup normally stays open after each unit change so a user can adjust several units. The 48 dp label target focuses text entry without an extra TalkBack stop. Draft text and popup state are saveable, while the caller saves the selected time. `clock` only sets the first picker value when no time is selected.

The field and picker inherit light, dark, high-contrast, brand, density, and reduced-motion modes from `BraceTheme`, using `timeInput` and `timePicker` component tokens. In compact mode, 48 dp controls are retained and the picker scrolls horizontally on narrow screens. Period names and control labels are localized in English, Spanish, and French. No animation is required to use these controls. Keyboard focus is drawn using Brace focus tokens.

`LocalTime` is a wall-clock time without a date or time zone. Applications must associate it with a date and zone to create an instant; the planned Blueprint `TimezoneSelect` row owns visible zone selection. Browser `Date` day/month/year fields, DOM number inputs, and React blur handlers do not become Android APIs. There is no separate Blueprint TimeInput row in the pinned documentation. Manual TalkBack, mouse, hardware keyboard, RTL, large-text, and multi-device review remain part of acceptance before a stable release.
