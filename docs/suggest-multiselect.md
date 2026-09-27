# Suggestions and multiple selection

`brace-select` now contains source implementations of `BraceSuggest` and `BraceMultiSelect`, mapped to the pinned Blueprint 6.18.0 [Suggest](https://blueprintjs.com/docs/#select/suggest) and [MultiSelect](https://blueprintjs.com/docs/#select/multi-select) pages. Both inventory rows are **in progress**. Neither API has a Maven Central release.

## Suggest: free text with explicit suggestions

```kotlin
val choices = listOf(
    BraceSelectOption("east", Region.East, "East"),
    BraceSelectOption("west", Region.West, "West"),
)
var value by rememberSaveable(stateSaver = TextFieldValue.Saver) {
    mutableStateOf(TextFieldValue(""))
}
var selectedKey by rememberSaveable { mutableStateOf<String?>(null) }
var expanded by rememberSaveable { mutableStateOf(false) }
val queryState = rememberBraceQueryListState()

BraceSuggest(
    value = value,
    onValueChange = { value = it },
    options = choices,
    selectedKey = selectedKey,
    onSelect = { option ->
        selectedKey = option.key
        value = TextFieldValue(option.label)
    },
    expanded = expanded,
    onExpandedChange = { expanded = it },
    label = "Region",
    state = queryState,
)
```

`TextFieldValue` retains cursor selection and IME composition. Typing, pressing the IME Done action, or dismissing the popup leaves the text intact. Only tapping an enabled suggestion or pressing Enter outside IME composition invokes `onSelect`; the caller chooses whether selection replaces the text. If typed text should clear an earlier selected key, clear that key inside `onValueChange`. The caller saves `value`, `selectedKey`, and `expanded`; `queryState` saves the active suggestion. A custom `predicate` can search domain fields, while `queryLabel`, `placeholder`, and `emptyLabel` accept localized strings.

Blueprint Suggest places a text input directly in the popover target. Brace displays the current value in a full-width trigger and focuses an anchored Android text editor on open. This keeps the software keyboard and composing text inside the focusable popup. The trigger announces its expanded/collapsed state. It is recorded as an **Android adaptation** in the inventory. Arrow Up/Down skips disabled choices. Enter chooses the active suggestion when the IME is not composing, or closes while preserving unmatched text when no enabled choice remains. During composition Enter passes to Android's text editor and may commit and close without selecting. Escape, Back, outside touch, or Done closes. The popup returns focus to the trigger.

## MultiSelect: chips and filterable choices

```kotlin
var selectedKeys by rememberSaveable { mutableStateOf(listOf("east")) }
var expanded by rememberSaveable { mutableStateOf(false) }
val queryState = rememberBraceQueryListState()

BraceMultiSelect(
    options = choices,
    selectedKeys = selectedKeys,
    onSelectedKeysChange = { selectedKeys = it },
    expanded = expanded,
    onExpandedChange = { expanded = it },
    label = "Regions",
    state = queryState,
)
```

`selectedKeys` is an ordered, unique list owned by the caller. Enabled options toggle without closing the picker. Selected tags can be removed separately; absent options keep their key as a fallback label until the app refreshes its data. The search query and active option restore through `queryState`. The default predicate matches labels and descriptions without case sensitivity. `resetQueryOnSelect` controls whether a choice clears the filter. The field announces its selected count and expanded/collapsed state, and each choice exposes selected/disabled semantics. Arrow Up/Down and Home/End navigate enabled options. Enter and the software keyboard Search action toggle the active one after IME composition ends; while a candidate is composing, those actions stay with the text editor. Escape or Back dismisses. The editor retains cursor and composition during normal typing, while an external `queryState.query` change moves the cursor to the end of the new text.

Blueprint uses a TagInput as the popover target. Brace wraps removable chips before a full-width trigger, then edits the filter inside the popup. This is an **Android adaptation** that keeps tags accessible at narrow widths and gives the IME its own anchored surface. Selected tags and options have separate touch, mouse, keyboard, and TalkBack actions. Tokens supply colors, focus borders, spacing, and 48dp targets in compact and comfortable density.

Both APIs require stable option keys and localized labels. The interactive catalog includes editable examples, disabled states, selection and query readouts, runtime theme controls, and copyable usage. The included automated tests exercise controlled state, free text, disabled options, keyboard, touch, mouse, RTL, large text, compact high contrast, minimum targets, and available Compose accessibility checks; see the M16 report for pass evidence. The shared `BracePopover` reads host and popup-window IME insets, caps its height, and repositions above the keyboard. API 36 real-keyboard tests and 320dp catalog inspection verify that Suggest Done and MultiSelect options remain visible at a low anchor. One-line supporting descriptions may truncate visually at 320dp; their full text remains in TalkBack semantics. The source build, lint, token, inventory, API, and Maven Local consumer gates pass; the complete select API 36 suite passes 32/32. Manual TalkBack and physical keyboard review across device sizes remain before either row can be stable.
