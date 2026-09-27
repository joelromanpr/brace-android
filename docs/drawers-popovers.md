# Drawers and popovers

This `brace-core` source slice adds controlled Android surfaces for Blueprint Drawer and PopoverNext, and maps the deprecated Popover to the same Compose API. All three pinned rows remain **in progress**. The [coverage ledger](coverage.md) is authoritative for release status.

## Drawer

```kotlin
var filtersOpen by rememberSaveable { mutableStateOf(false) }
BraceButton("Filters", onClick = { filtersOpen = true })
BraceDrawer(
    open = filtersOpen,
    onDismissRequest = { filtersOpen = false },
    title = "Filter results",
    position = BraceDrawerPosition.End,
    footer = { BraceButton("Apply", onClick = { filtersOpen = false }) },
) {
    BraceCheckbox(activeOnly, { activeOnly = it }, "Active records")
}
```

`BraceDrawer` is a modal edge sheet in an Android dialog window. `Start` and `End` follow RTL; `Top` and `Bottom` use the short window axis. `Small`, `Standard`, and `Large` choose adaptive extents, while `customExtent` allows a dp size clamped to a content-safe minimum and the current window. The body scrolls under large text while an optional footer remains visible. The title is both a visible heading and TalkBack pane name. If the visible title is omitted, provide `accessibilityTitle` and a visible heading in the body. Localize the close button description.

The caller owns `open` and closes the drawer in `onDismissRequest`. Back and outside tap can be disabled separately. A `BraceOverlayHost` includes open drawers in its stack and routes dismissal only to the topmost surface. Drawer surface colors and dimensions come from the versioned token source; the Android dialog window supplies background dimming. Blueprint's CSS transform hooks and DOM focus selectors have no separate Android API. A persistent split-pane drawer remains planned.

## Popover

```kotlin
var open by rememberSaveable { mutableStateOf(false) }
BracePopover(
    expanded = open,
    onDismissRequest = { open = false },
    title = "Filter options",
    placement = BracePopoverPlacement.BottomStart,
    target = { BraceButton("Filters", onClick = { open = !open }) },
) {
    BraceButton("Apply", onClick = { open = false })
}
```

`BracePopover` is a focusable Compose popup anchored to its target. It supports automatic placement, top/bottom with start/center/end alignment, and logical start/end sides with top/center/bottom alignment. Placement flips when the opposite side has more room and clamps to the window. The source reads host and popup-window IME insets, caps the surface height above the keyboard, and repositions it as the keyboard opens. Logical sides and alignments mirror in RTL. Outside tap, Back, and keyboard Escape request dismissal; closing requests focus back to the target. Provide a localized `title` as a TalkBack pane name and label the controls in `content`. Long content supplies its own scrolling layout.

The popup participates in `BraceOverlayHost` when one is present. Blueprint's Floating UI middleware, React target render props, arrows, DOM wrappers, and deprecated Popper machinery map to the Compose anchor, position provider, content slot, and `Modifier`; there is no Android DOM equivalent. Brief hover, long-press, and keyboard-focus help is covered by [BraceTooltip](tooltip-toast.md). Nonmodal free-positioned overlays, custom transitions, and exact web arrow geometry remain follow-up parity work.

## Verification and limitations

API 36 device interaction tests cover controlled state, keyboard Escape, focus return, stack order, edge geometry, IME-aware low-anchor positioning, and RTL placement. A 320dp Gboard-open Suggest and MultiSelect catalog check confirms the shared popover surface stays above the keyboard. The catalog exposes both surfaces in runtime light, dark, high-contrast, and density modes. Manual TalkBack, pointer, and visual checks across form factors remain before stable status. No Maven Central version has shipped.
