# Web mechanisms in Compose

Brace tracks Blueprint's public web mechanisms in the [pinned inventory](coverage.md) even when Android has no separate component. This guide covers the pinned [Classes](https://github.com/palantir/blueprint/blob/a60d4c92257612808fbfac81cfeee4fcba91a8b4/packages/core/src/docs/classes.mdx), [ResizeSensor](https://github.com/palantir/blueprint/blob/a60d4c92257612808fbfac81cfeee4fcba91a8b4/packages/core/src/components/resize-sensor/resize-sensor.mdx), and [BlueprintProvider](https://github.com/palantir/blueprint/blob/a60d4c92257612808fbfac81cfeee4fcba91a8b4/packages/core/src/context/blueprint-provider.mdx) in the `@blueprintjs/core@6.18.0` baseline. Each is classified **web-specific**. These mappings explain how to accomplish the user-facing job in Compose; they do not add Android APIs with those Blueprint names. They remain **in progress** in the inventory until review and validation. No Maven Central version has shipped.

## CSS classes and modifiers

Blueprint's `Classes` constants name versioned CSS selectors and its modifier classes apply visual variants to DOM elements. Android has neither DOM selectors nor a CSS cascade. Use a Brace component's typed parameters for its state, `BraceTheme` semantic or component tokens for custom content, and ordinary Compose `Modifier` chains for layout and drawing:

```kotlin
BraceTheme {
    val semantic = BraceTheme.colors.semantic
    Column(Modifier.background(semantic.surface).padding(BraceTheme.spacing.md)) {
        Text("Queue", color = semantic.onSurface, style = BraceTheme.typography.subtitle)
        BraceButton(
            label = "Retry",
            onClick = ::retry,
            intent = BraceButtonIntent.Primary,
            variant = BraceButtonVariant.Outline,
        )
    }
}
```

Import `androidx.compose.foundation.background`, `androidx.compose.foundation.layout.Column`, `androidx.compose.foundation.layout.padding`, `androidx.compose.material3.Text`, and `androidx.compose.ui.Modifier` along with the Brace types. The sample catalog toggles the button variant and the app-wide mode, contrast, brand, and density. A custom `Modifier` chain affects only the element where it is applied; it does not implicitly recolor descendants. For a scoped style change, nest `BraceTheme(overrides = …)` as shown in [Theming](theming.md). There is no `Classes` enum, `className`, Sass namespace, selector, or CSS utility artifact in Brace.

Use `Modifier.braceMinimumTouchTarget()` for custom interactive elements and `Modifier.braceFocusOutline(focused)` for a visible token-colored keyboard focus boundary. Add Compose semantics to custom controls; visual modifiers alone do not provide a role, click action, or focus order. Prefer the component's own state parameters and semantics whenever a Brace component exists.

## ResizeSensor

Blueprint wraps browser `ResizeObserver` around one DOM child. Compose exposes the measured size directly with `Modifier.onSizeChanged` from `androidx.compose.ui:ui`. It supplies an `IntSize` in **pixels** on first measure and when size changes. No ref forwarding, DOM observer, wrapper node, or `brace-core` resize component is needed:

```kotlin
var measured by remember { mutableStateOf(IntSize.Zero) }
Column {
    Box(
        Modifier
            .fillMaxWidth()
            .onSizeChanged { size -> measured = size }
            .background(BraceTheme.colors.semantic.surface)
            .padding(BraceTheme.spacing.md),
    ) {
        Text("Measured content", color = BraceTheme.colors.semantic.onSurface)
    }
    Text("Width: ${measured.width} px", color = BraceTheme.colors.semantic.onSurfaceMuted)
}
```

Import `androidx.compose.runtime.remember`, `mutableStateOf`, `getValue`, `setValue`, `androidx.compose.ui.layout.onSizeChanged`, and `androidx.compose.ui.unit.IntSize` in addition to the layout and text types. The catalog changes a preview's width so the reported measurement can be inspected with touch, keyboard, large text, and RTL. Size observation does not create an accessibility node; the child retains its own semantics and reading order.

The callback can repeat the same value, so make downstream effects idempotent. Do not feed the observed size back into the same element's measurement: state read in layout recomposes on a following frame and may introduce lag or a feedback loop. When one child's size must determine another child's layout in the same measure pass, write a `Layout` or `SubcomposeLayout` instead. See the [Compose `onSizeChanged` API reference](https://developer.android.com/reference/kotlin/androidx/compose/ui/Modifier#onSizeChanged(kotlin.Function1)).

## BlueprintProvider

The pinned Blueprint provider is a root singleton that nests `PortalProvider`, `OverlaysProvider`, and `HotkeysProvider`. Android has no DOM portal container. Compose can scope each behavior where it belongs:

```kotlin
BraceTheme {
    val overlays = rememberBraceOverlayState()
    val shortcuts = rememberBraceShortcutRegistryState()
    BraceShortcutRegistry(
        shortcuts = listOf(
            BraceShortcut("ctrl+r", "Refresh", onKeyDown = ::refresh),
        ),
        state = shortcuts,
    ) {
        BraceOverlayHost(overlays) {
            WorkspaceContent()
            BraceOverlay(
                open = detailsOpen,
                onDismissRequest = { detailsOpen = false },
                title = "Details",
            ) {
                DetailsContent()
            }
        }
    }
}
```

`BraceTheme` is in `brace-foundation`; `BraceShortcutRegistry`, `BraceOverlayHost`, and `BraceOverlay` are in `brace-core` (which depends on foundation). The theme uses a scoped CompositionLocal, can be nested for local overrides, and follows caller state for runtime changes. The overlay host shares a stack within its subtree, while the shortcut registry handles focus-tree key events and optional discovery. `BraceOverlay` creates an Android dialog window and handles modal focus; anchored menus use Compose popup behavior. No `BraceBlueprintProvider` singleton or portal target is required. Omit the overlay or shortcut host when a screen does not need that behavior; overlays also create a local stack when no host is supplied. See [Theming](theming.md), [Menus and overlays](overlays.md), and [Context menus and shortcuts](context-shortcuts.md).

The catalog shows a nested contrast scope, a controlled overlay, and a screen shortcut with touch-accessible discovery. Keep shortcuts attached to focused content and out of editable fields unless explicitly safe. Name modal panes and preserve a visible heading. Compose scopes are not app-wide OS keyboard registrations.

## Verification boundary

Existing foundation tests exercise runtime theme scopes, and core tests exercise overlays and shortcut focus behavior. This documentation slice adds catalog examples and links those existing implementations; it does not copy Blueprint CSS or React code. Inventory, Pages generation, Gradle compile, lint, token and API checks passed on this branch. The catalog was inspected on a 320 × 640 API 36 emulator in light and dark high-contrast modes, at 2× text, and in Arabic RTL. Touch controls, a scoped overlay and shortcut guide, resize observation, and Ctrl+R with keyboard focus worked. Manual TalkBack traversal, physical mouse input, reduced-motion behavior, and full focus order remain unverified. The inventory rows remain in progress; no stable coverage or release is claimed.
