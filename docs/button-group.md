# Button group

`BraceButtonGroup` in `brace-core` maps the pinned Blueprint 6.18.0 [ButtonGroup documentation](https://blueprintjs.com/docs/#core/components/button-group) to a connected group of native Compose actions. The inventory row is **in progress**; there is no first public release.

```kotlin
var view by rememberSaveable { mutableStateOf("list") }
BraceButtonGroup(
    actions = listOf(
        BraceButtonGroupAction(
            key = "list", label = "List", onClick = { view = "list" },
            selected = view == "list",
        ),
        BraceButtonGroupAction(
            key = "grid", label = "Grid", onClick = { view = "grid" },
            selected = view == "grid",
        ),
    ),
    fill = true,
    accessibilityLabel = "Display mode",
)
```

Each action needs a unique stable key and a localized, nonblank label. `onClick`, `enabled`, `loading`, and optional `selected` are owned by the screen. A null `selected` means an ordinary command. A Boolean `selected` exposes selected or unselected semantics; the group does not impose single or multiple selection. Persist screen state with `rememberSaveable` or a view model. Pass a localized `loadingDescription` when actions can load.

`vertical` changes a row to a stack. `fill` gives horizontal actions equal widths or stretches a vertical group across its parent; a vertical group without `fill` takes the width of its widest action. This Android adaptation avoids Blueprint CSS `height: 100%` behavior in Compose scrolling parents. `variant` offers solid, outlined, or minimal treatment; `size` sets visual height while every action keeps at least a 48 dp target. `alignment` uses logical start or end so RTL mirrors correctly. Individual action `intent` can emphasize primary or danger operations. Mixed form controls belong in `BraceControlGroup`.

```kotlin
BraceButtonGroup(
    actions = listOf(
        BraceButtonGroupAction(
            key = "search", label = "Search records", onClick = onSearch,
            showLabel = false,
            leadingIcon = { BraceIcon(BraceIcons.Search, contentDescription = null) },
        ),
    ),
    variant = BraceButtonGroupVariant.Outline,
    accessibilityLabel = "Search commands",
)
```

An icon-only action still announces its `label`. Icons are decorative. Every child is a separate button semantics node and focus stop; the group label describes the traversal region without merging actions. Compose `clickable` handles touch, mouse, Enter, and Space. Hover, press, focus, disabled, loading, selected, and intent visuals use semantic and ButtonGroup tokens from `tokens/v1/brace.tokens.json`. The focused action has a visible ring. Compact density does not shrink the 48 dp target. Large text can increase width; put a long horizontal group in a horizontally scrollable host or use `vertical = true` on narrow screens.

Blueprint's cascading CSS classes, z-index border stacking, HTML attributes, and `div` wrappers are web-only mechanisms. The Android component uses Compose layout, clipping, focus grouping, and semantics. Popovers and tooltips remain separate Compose components; an app may place a grouped action beside one or open one from its `onClick`, but arbitrary React child wrappers are not copied into this API.

## Verification and limitations

The focused device suite covers separate click targets, selected and disabled semantics, horizontal equal fill, vertical order, RTL high-contrast at 2x text scale, keyboard Enter/Space, mouse activation, loading, icon-only naming, saved-state restoration, and an automated accessibility check on supported API levels. Manual TalkBack, hardware keyboard/pointer, and light/dark/high-contrast visual review remain required before stable status. The catalog has interactive variant, size, orientation, and enabled-state controls. The [coverage ledger](coverage.md) is generated from the inventory and remains the authority for release status.
