# Date range picker and fields

`brace-datetime` has source implementations for the pinned Blueprint 6.18.0 [DateRangePicker](https://blueprintjs.com/docs/#datetime/date-range-picker) and [DateRangeInput](https://blueprintjs.com/docs/#datetime/date-range-input) rows. Both remain **in progress**. Both APIs are available in `1.0.0`; [coverage](coverage.md) still reports zero stable rows.

## Choose a range

```kotlin
var start by rememberSaveable { mutableStateOf<String?>(null) }
var end by rememberSaveable { mutableStateOf<String?>(null) }
BraceDateRangePicker(
    value = BraceDateRange(start?.let(LocalDate::parse), end?.let(LocalDate::parse)),
    onValueChange = { start = it.start?.toString(); end = it.end?.toString() },
    locale = Locale.US,
    minDate = LocalDate.of(2026, 1, 1),
    maxDate = LocalDate.of(2026, 12, 31),
    shortcuts = listOf(BraceDateRangeShortcut("Audit week",
        BraceDateRange(LocalDate.of(2026, 9, 14), LocalDate.of(2026, 9, 18)))),
)
```

The first day starts a partial range. The second day completes it in chronological order even when the user taps an earlier date. After a complete range, tapping an unrelated date starts a new range. Tapping an endpoint clears that endpoint. `allowSingleDayRange` lets a second tap on the starting day complete a one-day range. Shortcut labels are supplied by the app; shortcuts are disabled if any day in the span is unavailable or a one-day shortcut is disallowed. `isDateEnabled` filters every day of a complete range, including interior days. Calendar targets that would cross an unavailable day are disabled; typed ranges retain their drafts with an announced error. The caller still owns the controlled value and should validate any range supplied from outside the component.

The first visible month and focused day survive activity recreation. A wide viewport shows two sequential months; narrow Android windows show one month. Seven 48 dp day targets remain full size and the calendar scrolls horizontally when needed. Navigation exposes previous/next month and year actions. Arrow keys move by day or week, Page Up/Down by month, Ctrl+Page Up/Down by year, and Enter/Space activates a day. RTL reverses the horizontal arrow direction. Full localized dates, selected endpoints, in-range days, Today, unavailable dates, and the grid structure are exposed to accessibility services. Colors and dimensions use versioned `dateRangePicker` tokens across light, dark, high contrast, and density modes.

## Enter two dates

```kotlin
var start by rememberSaveable { mutableStateOf<String?>(null) }
var end by rememberSaveable { mutableStateOf<String?>(null) }
BraceDateRangeField(
    value = BraceDateRange(start?.let(LocalDate::parse), end?.let(LocalDate::parse)),
    onValueChange = { start = it.start?.toString(); end = it.end?.toString() },
    label = "Travel dates",
    locale = Locale.US,
    onInvalidInput = { boundary, draft -> logInvalidDate(boundary, draft) },
)
```

Each field accepts the locale's short date format. A valid draft updates its controlled endpoint immediately. Invalid, unavailable, and reversed drafts remain visible with announced errors, and `onInvalidInput` fires when editing finishes. Clearing one field emits a partial range. The calendar button opens a shared Compose popover for ordinary two-click selection; Alt+Down from a field opens the calendar to edit that endpoint. A complete selection closes the popover by default. Escape, Back, and outside click dismiss it through `BracePopover`. Draft text, active endpoint, and open state are saved; callers should save the controlled ISO endpoint strings as in the example. `readOnly` keeps the calendar available; `enabled = false` disables input and selection.

`BraceDateRange` uses `LocalDate` calendar days, avoiding implicit timestamp or timezone conversion. `Clock` controls Today. This is an Android adaptation of Blueprint's JavaScript `Date` tuples and two-input DOM popover. The web `boundaryToModify` and `singleMonthOnly` concepts become an explicit endpoint enum and responsive month count. Blueprint formatter functions, DayPicker pass-through props, browser focus behavior, and prebuilt shortcut presets are not copied. Hover range preview, independent noncontiguous month navigation, shortcut selection callbacks, and time-of-day controls from the pinned range props are still open for stable acceptance. A visible time-zone selector has its own inventory row. All assets here are original Brace source; the pinned Blueprint docs provide the comparison scope.

Manual TalkBack listening, physical keyboard and mouse checks, large-text and RTL visual review, and multiple-device QA remain in the acceptance review. Automated tests cover large-text targets and RTL keyboard behavior. The [component list](coverage.md) links to the tests and current status.
