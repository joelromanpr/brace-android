# Navigation labels and feedback

This `brace-core` slice adds Breadcrumbs, BreadcrumbItem, Tag, CompoundTag, Callout, and EmptyState. The pinned inventory marks them **in progress** until a release and broader manual review. They consume Brace semantic and component tokens under `BraceTheme`; the catalog offers interactive states and copyable examples.

## Breadcrumb path

```kotlin
BraceBreadcrumbs(
    items = listOf(
        BraceBreadcrumb("Home", onClick = navigator::home),
        BraceBreadcrumb("Projects", onClick = navigator::projects),
        BraceBreadcrumb("Current project"),
    ),
    minVisibleItems = 1,
)

BraceBreadcrumbItem(label = "Projects", onClick = navigator::projects)
```

The final item is the current destination. Clickable items have a 48 dp target, visible keyboard focus, and native callback navigation; current and disabled items do not activate. The current item announces its location state. The trail measures labels with Compose text metrics at the active font scale and moves items into a keyboard and TalkBack reachable menu when space is limited. `BraceBreadcrumbCollapseFrom.Start` or `.End` chooses which edge collapses first; logical separators mirror in RTL. `minVisibleItems` keeps a requested number of steps visible, even if an unusually narrow parent must clip a long label. Apps should allow enough horizontal width for at least one item and the overflow control.

Blueprint's `href`, DOM `OverflowList`, popover props, and custom renderer hooks are web mechanisms. Brace uses action callbacks, Compose width measurement, and a native dropdown. The current API takes a typed item model and an optional decorative icon slot; arbitrary custom breadcrumb rendering remains a follow-up for strict parity. The standalone `BraceBreadcrumbItem` lets apps build a custom trail.

## Tags

```kotlin
BraceTag(
    label = "Finance",
    intent = BraceTagIntent.Primary,
    selected = selected,
    onClick = { selected = !selected },
    onRemove = { removeFilter() },
    removeContentDescription = "Remove Finance filter",
)

BraceCompoundTag(label = "Status", value = "Active")
```

Tag and CompoundTag support semantic intents, medium/large sizes, minimal and rounded variants, fill, selected, disabled, and action states. Tag can wrap with `multiline = true`. A removable tag exposes separate main and remove focus stops, each at least 48 dp when actionable. The text label (or `label: value` pair) remains the accessibility name even when optional composable visual content and icon slots are supplied. Supply a localized `removeContentDescription` in production. Blueprint's arbitrary React children, HTML attributes, and icon names map to Compose content/icon slots, `Modifier`, and Android semantics. Static tags add no click action.

## Messages and empty states

```kotlin
BraceCallout(title = "Changes saved", intent = BraceCalloutIntent.Success,
    action = { BraceButton("Dismiss", onClick = onDismiss) }) {
    Text("The workspace is up to date.")
}

BraceEmptyState(title = "No results", description = "Try a different query.",
    action = { BraceButton("Retry", onClick = onRetry) })
```

Callout supports neutral/primary/success/warning/danger intents, compact and minimal treatments, an optional custom icon, and an action slot. Its default intent marks are original Brace Canvas drawings; no third-party icon asset is bundled. EmptyState supports vertical/horizontal arrangements, icon sizes, optional muted icon treatment, description, action, and extra content. Decorative icons are removed from the accessibility tree, while titles are headings and actions remain separate focus targets. Dynamic text is quiet by default; set `announceChanges = true` for a polite live-region announcement when appropriate. A custom icon or action slot is drawn and labeled by its caller; use an accessible control such as `BraceButton`.

## Verification and limits

The dedicated Android tests cover breadcrumb overflow and navigation, current/disabled semantics, Tag action and removal independence, rich visual slots with stable spoken labels, Callout/EmptyState action and heading behavior, keyboard/RTL/2x text/high contrast, and API 34+ automated accessibility checks. Run `./gradlew :brace-core:connectedDebugAndroidTest`. Manual TalkBack, mouse, focus order across representative screens, and all light/dark/high-contrast variants remain acceptance work. The first release field is empty until Maven Central publication. Remaining Blueprint navigation, feedback, and content rows stay planned in the coverage ledger.
