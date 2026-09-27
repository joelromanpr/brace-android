# Date picker and date input

`brace-datetime` contains two pinned Blueprint datetime rows: [DatePicker](https://blueprintjs.com/docs/#datetime/date-picker) and [DateInput](https://blueprintjs.com/docs/#datetime/date-input). Both are available in the `1.0.0` release and remain **in progress** in the [coverage inventory](coverage.md).

## Add the datetime artifact

Add `implementation("io.github.joelromanpr.brace:brace-datetime:1.0.0")`. The datetime artifact resolves core and foundation transitively. See [installation](installation.md) for the repository block and supported toolchain.

## Choose a day

```kotlin
var selected by rememberSaveable { mutableStateOf<String?>(null) }
BraceDatePicker(
    value = selected?.let(LocalDate::parse),
    onValueChange = { selected = it?.toString() },
    locale = Locale.US,
    minDate = LocalDate.of(2026, 1, 1),
    maxDate = LocalDate.of(2026, 12, 31),
    isDateEnabled = { it.dayOfWeek.value <= 5 },
    shortcuts = listOf(BraceDateShortcut("Quarter end", LocalDate.of(2026, 3, 31))),
)
```

The selected date is controlled by the caller. The visible month is saved by the picker across activity recreation; `initialMonth` only controls its first composition. A month outside `minDate`/`maxDate` is displayed at the nearest allowed month, including after bounds or the controlled value change. Weekday order and full TalkBack date labels follow `locale`. Month navigation works through previous/next buttons or the month/year chooser. Arrow keys move day focus, Page Up/Down moves a month, Ctrl+Page Up/Down moves a year, and Enter/Space selects. Day targets remain 48 dp in compact density; a narrow viewport scrolls horizontally so targets do not shrink. Today uses the provided `Clock` (or the device's default zone), and Clear can be disabled with `canClearSelection`.

## Enter a day

```kotlin
var dueDate by rememberSaveable { mutableStateOf<String?>(null) }
BraceDateField(
    value = dueDate?.let(LocalDate::parse),
    onValueChange = { dueDate = it?.toString() },
    label = "Due date",
    minDate = LocalDate.of(2026, 1, 1),
    maxDate = LocalDate.of(2026, 12, 31),
    onInvalidInput = { invalidDraft -> logInvalidDate(invalidDraft) },
)
```

The field formats and parses Android's localized short date format. It rejects impossible calendar days, dates outside bounds, and dates rejected by `isDateEnabled`. A valid draft calls `onValueChange`; an invalid draft remains visible with an announced error, and `onInvalidInput` runs when editing finishes. Empty text clears selection. The calendar button and Alt+Down open a Compose popover; Escape, outside click, and Back close it. The visible label focuses the text field on touch without adding another keyboard or TalkBack focus stop. The caller saves the selected `LocalDate` as an ISO date string; the field saves unfinished draft text and popup state.

`LocalDate` represents a calendar day rather than an instant. `Clock` controls Today and its zone. Time-of-day selection, a visible time-zone selector, ranges, and natural-language parsing belong to other pinned rows. Blueprint's React DayPicker pass-through props, date-fns formatter functions, DOM popover wrappers, and built-in shortcut presets do not become separate Android APIs here. The current picker accepts explicit shortcut labels and dates. This slice has no arbitrary day-cell styling/modifier API; that and manual TalkBack, mouse, hardware keyboard, large-text, RTL, and multiple-device QA remain in the acceptance review.

If an app changes `locale` at runtime while publishing an Android App Bundle, it must make the requested language resources available (for example by configuring language splits or on-demand language downloads). Without that app configuration, date numbers still follow the requested locale but translated control labels may fall back to the base language.

The colors and dimensions come from `datePicker` and `dateInput` component tokens in the versioned platform-neutral token source. Light, dark, high contrast, brand, compact/comfortable, and reduced-motion modes are inherited from `BraceTheme`; these controls do not animate when reduced motion is requested.
