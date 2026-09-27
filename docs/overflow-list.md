# Overflow list

`BraceOverflowList<T>` is the generic, width-adaptive Compose equivalent of Blueprint 6.18.0's [OverflowList](https://blueprintjs.com/docs/#core/components/overflow-list). It is in progress on M29; the inventory does not count it as stable or released. Add `io.github.joelromanpr.brace:brace-core` from the aligned Brace version when a release exists. The catalog's OverflowList page demonstrates the current source API.

## Navigation example

```kotlin
var expanded by rememberSaveable { mutableStateOf(false) }
var selected by rememberSaveable { mutableStateOf("Overview") }
val sections = listOf("Overview", "Analysis", "Forecast", "Exports")

BraceOverflowList(
    items = sections,
    itemKey = { it },
    modifier = Modifier.fillMaxWidth(),
    collapseFrom = BraceOverflowCollapseFrom.Start,
    minVisibleItems = 1,
    navigationLabel = "Report sections",
    onOverflow = { hidden -> if (hidden.isEmpty()) expanded = false },
    visibleItem = { section, _ ->
        BraceButton(section, onClick = { selected = section })
    },
    overflowContent = { hidden ->
        BraceMenuPopup(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            anchor = { BraceButton("More ${hidden.size}", onClick = { expanded = true }) },
        ) {
            hidden.forEach { section ->
                BraceMenuItem(section, onClick = { selected = section })
            }
        }
    },
    overflowMeasureContent = { hidden ->
        BraceButton("More ${hidden.size}", onClick = {})
    },
)
```

The list measures actual composable slots at the width offered by its parent. Width changes, font scaling, density changes, and content changes trigger Compose remeasurement. The maximum fitting visible count is selected, even when the overflow label changes width at a number boundary. Hidden items retain source order. `Start` hides a prefix and places the trigger at logical start; `End` hides a suffix and places it at logical end. Logical placement mirrors in RTL. Use stable, unique, Android Bundle-saveable `itemKey` values so item saveable state follows an item as it moves in and out of overflow. If a visible item slot starts effects or owns transient UI state, provide a width-matched `visibleMeasureContent` that draws only its measured form. `minVisibleItems` can force content wider than a very narrow parent; choose the minimum for the smallest supported viewport.

`onOverflow` runs after layout only when the hidden item list changes, including when it becomes empty. When `alwaysRenderOverflow = true`, the final overflow slot remains composed even when nothing is hidden, which keeps a caller-owned menu state. Set `overflowMeasureContent` to a visually matching trigger without the popup if `overflowContent` includes a popup or side effects: the layout briefly composes measurement candidates to determine their widths. The displayed final slot alone owns the menu.

## Accessibility and native behavior

The list groups actions in the semantics tree; `navigationLabel` names navigation use. The generic layout does not assume that every item navigates. Supply an accessible visible item and an overflow trigger with a clear label, at least 48dp target, visible focus, and a keyboard and TalkBack reachable menu. `BraceButton`, `BraceMenuPopup`, and `BraceMenuItem` provide those behaviors in the example. Place the component in a bounded-width parent, such as `fillMaxWidth()` in a screen column. Slots own their visual colors and states through `BraceTheme`; the row gap uses Brace spacing tokens and follows compact/comfortable theme choices.

Blueprint's `ResizeObserver` and `observeParents` become normal Compose parent constraint propagation. `visibleItemRenderer` and `overflowRenderer` become composable slots; `className`, `style`, and `tagName` become `Modifier` and surrounding Compose layout. `navigable` and `navigationAriaLabel` become `navigationLabel` semantics. No DOM observer or React wrapper is needed. The API intentionally leaves routing and popup content to callers because generic items have no inherent Android action.

## Verification and limits

The focused device suite covers start/end partitioning, parent resize, content changes at unchanged overflow count, callback deduplication, count-dependent trigger widths, required minimum, RTL placement, trigger state retention, keyboard menu actions, and an API 34+ automated accessibility check under large text and high contrast. Manual TalkBack, tablet, pointer behavior with the catalog, and hosted CI remain release acceptance work. The row stays in progress with no first release version until those gates and publication are complete.
