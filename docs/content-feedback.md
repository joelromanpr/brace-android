# Content and feedback components

This `brace-core` milestone adds `BraceCard`, `BraceCardList`, `BraceDivider`, `BraceSection`, `BraceSectionCard`, and `BraceProgressBar`. Their inventory rows are **in progress** until a public release and broader device review. They use Brace semantic and component tokens, so `BraceTheme` controls light, dark, high contrast, brand, density, and reduced motion. The Android catalog reads their names and availability from the generated coverage inventory and shows interactive states and copyable examples.

## Cards and dividers

```kotlin
BraceCard(
    elevation = BraceCardElevation.One,
    selected = selected,
    onClick = { selected = !selected },
) {
    Text("Project overview")
    Text("Open details")
}

BraceCardList(
    items = projects,
    itemKey = { it.id },
    isSelected = { it.id == selectedId },
    onItemClick = { selectedId = it.id },
) { project ->
    Text(project.name)
}

BraceDivider()
BraceDivider(orientation = BraceDividerOrientation.Vertical)
```

A static card does not create an accessibility focus stop. A clickable card uses button semantics, a 48 dp minimum target, visible keyboard focus, and controlled selection. Put independently interactive controls in a static card so TalkBack and keyboard users can reach each control separately. `BraceCardElevation.Zero` through `Four` map to the five documented card depths; compact padding responds to the theme density. `BraceCardList` groups a **short, bounded** list and exposes collection and item semantics. Its typed item API adapts Blueprint's React children to a Compose list; provide `itemKey` if item identity must survive reordering. For large lists, place `BraceCard` items in `LazyColumn` instead. `BraceDivider` is decorative and can be horizontal or vertical; give a vertical divider a height-constrained parent or explicit height.

## Sections

```kotlin
BraceSection(
    title = "Projects",
    subtitle = "Three workspaces",
    collapsible = true,
) {
    BraceSectionCard { Text("Workspace content") }
}
```

The title is a heading. An optional `icon` is decorative; `trailingAction` remains a separate focus target. A collapsible title supports touch, pointer, Enter/Space, TalkBack expand/collapse actions, and a visible focus ring. Uncontrolled expansion uses saveable state; pass `expanded` and `onExpandedChange` together for controlled state and save it in your own model. The body leaves the focus and accessibility tree while collapsed, while `rememberSaveable` body state survives re-expansion. `BraceSectionCard` supplies token-driven padding and border; its default inset responds to comfortable or compact density, and `padded = false` supports edge-to-edge content.

## Progress

```kotlin
BraceProgressBar(label = "Uploading files", value = progress)
BraceProgressBar(label = "Waiting for response", value = null)
BraceProgressBar(label = "Validating", value = 36f, valueRange = 0f..100f,
    intent = BraceProgressIntent.Success)
```

`value = null` (or a non-finite value) is indeterminate; finite values clamp to the supplied range. The indicator exposes progress range and label semantics, has primary/success/warning/danger intents, and supports a disabled state. The visual fill follows RTL. Reduced motion snaps determinate updates and holds an indeterminate segment still. The progress bar is informational; put cancellation or retry in a separate action.

## Verification and limits

The Android tests cover card actions and list selection, section state and keyboard control, progress semantics and clamping, RTL, 2x text, compact density, high contrast, and API 34+ automated Compose accessibility checks. Run `./gradlew :brace-core:connectedDebugAndroidTest` on an emulator or device. Manual TalkBack, keyboard, mouse, all theme variants, and representative app layouts remain acceptance work before the rows can become stable. The first release version stays empty until Maven publication. These components provide the documented base behaviors; the remaining pinned Blueprint core rows remain in the coverage inventory for later milestones.
