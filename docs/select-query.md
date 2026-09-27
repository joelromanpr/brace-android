# Single selection and query state

Brace's `brace-select` artifact introduces the source implementation of `BraceSelect` and `rememberBraceQueryListState`. They correspond to the pinned Blueprint 6.18.0 [Select](https://blueprintjs.com/docs/#select/select-component) and [QueryList](https://blueprintjs.com/docs/#select/query-list) rows. Both are available in the `1.0.0` release and remain **in progress**.

```kotlin
val choices = listOf(
    BraceSelectOption("east", Region.East, "East"),
    BraceSelectOption("west", Region.West, "West"),
)
var selectedKey by rememberSaveable { mutableStateOf<String?>(null) }
var expanded by rememberSaveable { mutableStateOf(false) }
val query = rememberBraceQueryListState()

BraceSelect(
    options = choices,
    selectedKey = selectedKey,
    onSelect = { selectedKey = it.key; openRegion(it.value) },
    expanded = expanded,
    onExpandedChange = { expanded = it },
    label = "Region",
    state = query,
)
```

Options need unique, stable, nonblank keys and nonblank localized spoken labels; the select and its optional search/empty labels must also be nonblank. Selection and popup expansion are controlled by the caller, so save those in screen state. `rememberBraceQueryListState` saves query text and the active key across recreation. The default filter searches labels and descriptions without case sensitivity; pass a `predicate` for domain search. The headless state can power another Compose layout directly:

```kotlin
val query = rememberBraceQueryListState()
val visible = query.filter(choices)
BraceTextField(query.query, { query.query = it }, label = "Find a region")
Column(Modifier.braceQueryNavigation(query, visible.filter { it.enabled }.map { it.key },
    onActivate = { key -> openRegion(visible.first { it.key == key }.value) },
    onDismiss = { closeSearch() })) {
    visible.forEach { choice ->
        BraceButton(choice.label, onClick = { query.activeKey = choice.key; openRegion(choice.value) },
            enabled = choice.enabled)
    }
}
```

The select trigger announces its selected label and localized expanded/collapsed state as one TalkBack action. The popup uses an Android focusable anchored surface. It keeps the search field focused while Up/Down wraps through enabled choices, Home/End jumps to boundaries, Enter selects, and Escape/Back or outside touch dismisses. A software keyboard Search action accepts the active choice after composition ends. Hardware navigation passes through while the IME is composing a candidate. A custom headless query UI should pass its `TextFieldValue.composition != null` through `isTextComposing`; set `activateOnSpace = true` only for a non-editable list. Disabled options remain visible and are skipped. Each option has a 48dp minimum target, selection semantics, a visible focus ring, and an accessible label. The search field announces the active option position and label. Touch, mouse, and TalkBack activate an option directly. The popup returns focus to its trigger after dismissal through `BracePopover`.

`filterable = false` removes the search field and focuses the active enabled option (or first enabled option when none is active). Arrow keys move both highlight and actual row focus; Space and Enter act on the current active key even when focus has not moved to the next row yet. Keyboard movement scrolls the lazy list to keep the active option visible. A custom `optionContent` slot changes only visual rendering; supply localized option labels and descriptions for accessibility. `queryLabel`, `placeholder`, and `emptyLabel` can be overridden with localized text; English and Spanish defaults are included. Light, dark, high contrast, brand, and compact/comfortable density resolve through the versioned select tokens, while targets remain 48dp.

Blueprint's React child wrapper, render prop, and DOM event objects are represented by controlled Compose state, data options, and a composable option slot. The pinned Select row also documents create-new-item-from-query callbacks/rendering, initial custom popup content, whole-list predicate reordering, and replacement list rendering. Those within-row behaviors are not yet exposed here; `predicate` only filters individual options and `optionContent` customizes one option. `BraceSelect` currently handles single selection; [Suggest and MultiSelect](suggest-multiselect.md) have separate in-progress source implementations; Omnibar remains planned. The interactive catalog offers select, suggestion, multiple selection, and headless query examples.
