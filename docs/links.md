# Links and button-shaped navigation

The pinned [Blueprint Link](https://blueprintjs.com/docs/#core/components/link) and [AnchorButton](https://blueprintjs.com/docs/#core/components/buttons) rows have Android source APIs in `brace-core`. They remain **in progress** in the [coverage ledger](coverage.md); no Maven Central version has shipped.

## Explicit destinations

```kotlin
BraceLink(
    label = "Read the guide",
    destination = BraceLinkDestination.Uri(
        uri = "https://example.org/guide",
        label = "Guide",
    ),
)

BraceLinkButton(
    label = "Open reports",
    destination = BraceLinkDestination.Action("Reports") {
        navigator.openReports()
    },
)
```

`Uri` uses Compose's Android `LocalUriHandler` by default. Pass `onOpenUri = { uri -> ... }` to use an app-owned opener such as Custom Tabs or to route a deep link. The callback receives the URI string exactly as supplied. The caller chooses trusted URI values and handles platform failures or missing handlers. `Action` invokes the supplied callback without launching an Android intent. Both forms require a nonblank, localized destination label. The visible label is separate from the destination name so TalkBack can announce both.

`BraceLink` is text navigation. It has primary, success, warning, danger, and inherited text colors through `BraceLinkColor` (pass `inheritedColor` to mirror an enclosing text color; otherwise it uses on-surface), plus always, hover/focus, and no underline via `BraceLinkUnderline`. Disabled links suppress activation and use disabled tokens. Hover, press, focus, 48 dp target, theme, brand, high contrast, density, large text, and RTL use Brace/Compose behavior. High-contrast press adds a strong token outline so touch feedback stays visible. Optional icon slots are decorative; include their meaning in the visible label. The default token contrast check covers rest, hover, and pressed text on common Brace surfaces: minimum 4.60:1 in light, 6.10:1 in dark, 7.20:1 in high-contrast light, and 7.40:1 in high-contrast dark. Run `python3 scripts/check_link_contrast.py`; `checkTokenGeneration` also runs it. Caller-provided inherited colors, brand palettes, and theme overrides need their own contrast review.

`BraceLinkButton` uses `BraceButton` component tokens and offers its current primary, secondary, danger, solid, and outline styles, plus loading and disabled states. Loading and disabled navigation are suppressed. Both controls accept a standard Compose `Modifier` for layout and focus order. `BraceLinkButton` has the Compose button role. Compose UI 1.9 has no public `Role.Link`, so `BraceLink` announces itself and the destination as a link through localized content and click-action labels without claiming a native link role. Manual TalkBack review remains part of acceptance.

Blueprint's `href`, `target`, `rel`, download attribute, arbitrary HTML anchor attributes, and browser history behavior do not map one-for-one. Android URI handling and app navigation callbacks replace them. Android decides the appropriate URI handler; no new browser window or download is implied. Blueprint's built-in external-link icon is an optional Compose icon slot here, so apps can choose licensed icons suited to their navigation context. The current `BraceLink` is a standalone 48 dp control; embedding a link span inside a paragraph or block of `Text` remains a follow-up.

## Verification and limits

The Android tests cover URI routing, caller opener precedence, action callbacks, disabled/loading behavior, keyboard activation, destination semantics, 48 dp targets, RTL, high contrast, and accessibility checks. The [catalog capture](site/index.html#showcase) shows a link action result. Manual TalkBack review remains, and the button API still lacks some variants needed for full AnchorButton coverage. Check the [component list](coverage.md) for the current status and test links.
