# Collapse and text

This source slice covers the pinned Blueprint [Collapse](https://github.com/palantir/blueprint/blob/a60d4c92257612808fbfac81cfeee4fcba91a8b4/packages/core/src/components/collapse/collapse.mdx) and [Text](https://github.com/palantir/blueprint/blob/a60d4c92257612808fbfac81cfeee4fcba91a8b4/packages/core/src/components/text/text.mdx) rows. Both Brace APIs remain **in progress** and have no first release version.

## Controlled disclosure body

```kotlin
var detailsOpen by rememberSaveable { mutableStateOf(false) }

BraceTheme {
    Column {
        BraceButton(
            label = if (detailsOpen) "Hide details" else "Show details",
            onClick = { detailsOpen = !detailsOpen },
            modifier = Modifier.semantics {
                stateDescription = if (detailsOpen) "Expanded" else "Collapsed"
            },
        )
        BraceCollapse(expanded = detailsOpen) {
            BraceText("Import details and actions")
        }
    }
}
```

`BraceCollapse` animates height in normal motion and snaps under reduced motion using `BraceTheme.motionTokens.normal`. The caller owns `expanded` and supplies a separate trigger. Put expanded/collapsed semantics and the action on that trigger; the body has no click behavior of its own. Closed content leaves the accessibility tree and does not receive pointer or keyboard input during the transition. Its content follows normal Compose reading order, including RTL and large text.

By default, children leave composition after the closing animation. `keepContentMounted = true` retains child composition while the body is fully closed, for non-saveable internal state. The default also preserves `rememberSaveable` child state across close and reopen. Prefer the default for large data sets or expensive content. A fixed height supplied by a caller may prevent a collapsed body from measuring to zero.

Blueprint's `component` HTML root tag becomes Compose `Modifier` and layout composition. Its custom `transitionDuration` prop maps to the scoped Brace motion tokens, so a screen uses one coherent motion scale and follows the system reduced-motion setting.

## Text overflow

```kotlin
BraceText("A long project title", modifier = Modifier.width(140.dp), ellipsize = true)
BraceText("Status", title = "Current report status")
BraceText("A description that wraps naturally across lines.")
```

`BraceText` uses the theme's body typography and semantic foreground color. With `ellipsize = true`, it draws a one-line ellipsis only when constrained; the full original string remains available to TalkBack. A tooltip appears on hover or touch long press only when overflow is measured. `title` supplies an explicit tooltip even without overflow. A tooltip target keeps the theme's minimum touch height. Text wraps normally when `ellipsize` is false. Font scaling, RTL, light/dark, high contrast, and brand color scopes come from Compose and `BraceTheme`.

Blueprint's HTML `tagName`, DOM ref, and title attribute do not produce separate Android components. Compose uses a `Modifier` for layout and a token-styled tooltip for title help. Interactive links belong in a separate link API rather than hidden in a text wrapper.

The catalog has live expansion, mounted-content, width, and tooltip examples. Focused Android tests cover state retention, hidden semantics, motion, overflow help, large text, RTL, and automated accessibility checks. Manual TalkBack speech, hardware keyboard, mouse, and additional screen-size review remain before either row can be stable.

The pinned [EntityTitle](https://github.com/palantir/blueprint/blob/a60d4c92257612808fbfac81cfeee4fcba91a8b4/packages/core/src/components/entity-title/entity-title.mdx) remains planned. Its complete mapping needs icon and loading-placeholder APIs, heading and tag composition, and native navigation behavior for Blueprint's title URL.
