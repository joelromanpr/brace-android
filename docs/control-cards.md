# Control cards

Brace has three selection cards: `BraceSwitchCard`, `BraceCheckboxCard`, and `BraceRadioCard`. Each card is one touch and focus target, with its indicator drawn inside. All three are in progress and available from source. [Pinned comparison](https://blueprintjs.com/docs/#core/components/control-card).

```kotlin
var notifications by rememberSaveable { mutableStateOf(true) }
BraceSwitchCard(
    checked = notifications,
    onCheckedChange = { notifications = it },
    label = "Notifications",
    description = "Send a daily summary",
    modifier = Modifier.fillMaxWidth(),
)
```

The caller owns `checked`. The full card has a minimum 48 dp touch target, Switch role, visible token focus ring, hover and pressed states, disabled state, and touch, mouse, Enter, Space, and TalkBack activation. `showAsSelectedWhenChecked = false` suppresses selected card color while retaining checked semantics. `compact` reduces padding while keeping the target size. `elevation` uses Brace's five card depths. The switch indicator defaults to logical end; `indicatorPosition` may move it to logical start, which mirrors in RTL.

```kotlin
var archived by rememberSaveable { mutableStateOf(false) }
var mixed by rememberSaveable { mutableStateOf(true) }
BraceCheckboxCard(
    checked = archived,
    indeterminate = mixed,
    onCheckedChange = { archived = it; mixed = false },
    label = "Include archived",
    description = "Across all projects",
    modifier = Modifier.fillMaxWidth(),
)
```

`BraceCheckboxCard` defaults to a logical start indicator. The indeterminate mark is announced as a mixed checkbox state; activating it requests `true` and the caller clears `indeterminate`. Checked, unchecked, and mixed remain controlled values. Disabling the card removes activation but retains its label and state for assistive technology.

```kotlin
var lunch by rememberSaveable { mutableStateOf<String?>("soup") }
BraceRadioCardGroup(
    options = listOf(
        BraceRadioCardOption("soup", "Soup", "Vegetarian"),
        BraceRadioCardOption("salad", "Salad", enabled = false),
        BraceRadioCardOption("sandwich", "Sandwich"),
    ),
    selectedValue = lunch,
    onValueChange = { lunch = it },
    label = "Lunch special",
)
```

A standalone `BraceRadioCard(selected, onSelect, label)` works with app-managed exclusive state. `BraceRadioCardGroup` is a card-specific adjunct: it announces the group label, uses the selected choice as its Tab stop, and moves selection and focus with arrows. Vertical arrows use list order; horizontal arrows mirror in RTL. Disabled choices are skipped and navigation wraps. The general `RadioGroup` inventory row is separate. Blueprint's pinned source and tests default `RadioCard` to an end indicator, although the pinned documentation says start; Brace follows the source and exposes `indicatorPosition` for either layout.

All card color and size values come from versioned `controlCard`, checkbox, and switch tokens. The theme supplies light, dark, high-contrast, brand, compact/comfortable density, and font scaling. Labels are mandatory; optional descriptions are included in the spoken name. Place actions outside the card so each card remains a single focus target. Blueprint's `inputProps`, DOM refs, and arbitrary HTML children map to typed Compose parameters, `Modifier`, and caller-managed state rather than web attributes. Rich Compose content slots, manual TalkBack/physical keyboard/mouse/large-text/RTL review, and CardList composition remain open before stable acceptance.

See the [M41 report](milestones/m41-control-cards.md) and [coverage inventory](coverage.md) for exact row status and verification.
