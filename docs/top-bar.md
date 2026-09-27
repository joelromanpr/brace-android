# Top app bar

Blueprint 6.18.0 documents Navbar, NavbarGroup, NavbarHeading, and NavbarDivider as four stateless components. Brace maps them to `BraceTopBar`, `BraceTopBarGroup`, `BraceTopBarTitle`, and `BraceTopBarDivider` in `brace-core`. Their inventory rows are **in progress** until release and full acceptance review.

```kotlin
Scaffold(
    topBar = {
        BraceTopBar(
            startContent = {
                BraceTopBarGroup {
                    BraceTopBarTitle("Quarterly report")
                    BraceTopBarDivider()
                }
            },
            endContent = {
                BraceTopBarGroup {
                    BraceButton("Edit", onClick = ::edit)
                }
            },
        )
    },
) { contentPadding ->
    ReportScreen(Modifier.padding(contentPadding))
}
```

The top bar uses Brace component color and dimension tokens. Its surface, title, bottom line, and separator change with light, dark, and high-contrast themes and brand overrides. The optional `raised` parameter applies Brace elevation. The leading and trailing slots follow logical start/end in RTL. `BraceTopBarGroup` preserves composition order for keyboard and TalkBack traversal; its children own labels, actions, and enabled states. `BraceTopBarTitle` is a heading, ellipsizes within the available width, and is not an action. `BraceTopBarDivider` is decorative and has no accessibility stop. Compact density tightens the bar padding and group gap, while action targets remain at least 48 dp.

A top bar has no built-in navigation action. Supply a `BraceButton`, `BraceIconButton` from `brace-icons`, or another labeled action to navigate. When the screen is narrow or text is large, put surplus actions in a menu. Avoid adding more actions than fit alongside the title. Use the `Scaffold(topBar = ...)` slot for top placement. If your screen draws behind the Android status bar, apply `Modifier.statusBarsPadding()` at the host; Brace does not take over window insets. This is the native equivalent of Blueprint's `fixedToTop` CSS behavior. React DOM attributes and nested component aliases map to Compose slots and top-level functions.

The catalog exposes standard and raised variants and an interactive Edit/Done action. The Android test suite checks title heading semantics, action focus and activation by touch, mouse and keyboard, RTL geometry, large text, dark high contrast, 48 dp action bounds, and the separator's absence from the accessibility tree. Automated Compose accessibility checks run where supported. Manual TalkBack review and release validation remain pending, so no first release version is recorded.
