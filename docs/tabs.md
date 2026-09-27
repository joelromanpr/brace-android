# Tabs and panels

`brace-core` contains a native Compose interpretation of Blueprint 6.18.0 Tabs, Tab, TabPanel, and TabsExpander. These four inventory rows are **in progress**. No Maven Central release or stable coverage is claimed.

## Install and choose an API

Use the aligned `io.github.joelromanpr.brace:brace-core` coordinate from [installation](installation.md). [BraceTheme](theming.md) supplies the colors, typography, density, and focus dimensions.

For caller-owned selection that survives recreation, save the stable tab ID:

```kotlin
var selected by rememberSaveable { mutableStateOf("overview") }
val tabs = listOf(
    BraceTab("overview", "Overview"),
    BraceTab("activity", "Activity", badge = "3",
        accessibilityLabel = "Activity, 3 updates"),
    BraceTab("locked", "Locked", enabled = false),
)
BraceTabs(
    tabs = tabs,
    selectedTabId = selected,
    onTabSelected = { selected = it },
    trailingContent = { BraceButton("Refresh", onClick = ::refresh) },
) { tab ->
    when (tab.id) {
        "overview" -> OverviewPanel()
        "activity" -> ActivityPanel()
    }
}
```

Omit `selectedTabId` for a saveable internal selection. `initialSelectedTabId` chooses its first tab; otherwise Brace chooses the first enabled tab. An unknown controlled ID leaves the strip and panel unselected. IDs are strings so they can be saved with Android state; they must be unique and stable. `onTabSelected` receives the ID on touch, mouse, keyboard activation, and TalkBack action. An unchanged controlled ID remains unchanged until the caller updates it.

A panel can live elsewhere in the layout:

```kotlin
BraceTabs(tabs = tabs, selectedTabId = selected, onTabSelected = { selected = it })
BraceTabPanel(tab = tabs[0], selectedTabId = selected) {
    OverviewPanel()
}
```

Only the selected panel is composed and exposed to accessibility. Its title is a pane label; `rememberSaveable` values inside the panel survive switches while `BraceTabPanel` remains in composition. Use `rememberSaveable` or a screen state holder for data that must also survive leaving the entire screen. A programmatic selection change while keyboard focus is inside the old panel does not restore a previously focused descendant; apps with that requirement should own focus explicitly.

## Layout and interaction

`BraceTabsOrientation.Horizontal` places a horizontally scrollable tab strip above the panel. `Vertical` places the strip at the logical start side on wide screens and stacks it above the panel below the tokenized 420 dp breakpoint. A side rail scrolls vertically when the parent provides a bounded height; a stacked rail follows the screen scroll. Tabs grow for large font settings, keep a 48 dp minimum interactive target, and bring focused or selected titles into view. `BraceTabSize.Large` uses larger typography. The trailing action slot remains visible outside horizontal scrolling; provide a compact action on narrow phones.

Arrow keys move focus among enabled tabs and wrap at the ends without selecting. Enter or Space selects the focused tab. Left and Right follow physical directions in RTL; Up and Down apply to a vertical strip. Disabled tabs remain announced as unavailable and are skipped by keyboard arrows. Focus has a visible semantic-color outline, selected state uses the selection colors and primary indicator, and decorative icons are silent. Tab badge text enters the spoken label unless `accessibilityLabel` supplies better context. Compose Role.Tab and selected semantics are exposed to TalkBack.

`BraceTabSpacer()` is the silent `RowScope` or `ColumnScope` weight spacer for a custom arrangement:

```kotlin
Row(Modifier.fillMaxWidth()) {
    Text("Workspace")
    BraceTabSpacer()
    BraceButton("Search", onClick = ::search)
}
```

Inside `BraceTabs`, use `trailingContent` for the same placement. The CSS flex expander, React child wrapper, ARIA ID references, HTML classes, and DOM indicator animation have no direct Android meaning. The native selection callback receives the new ID; apps can compare saved state for the previous ID, while DOM event arguments are absent. Brace uses a typed item model, Compose semantics, an active pane, and a fixed trailing slot. Arbitrary React title children, custom Tag props on badges, interactive elements between tab titles, Blueprint `fill`, and a moving indicator animation are not in this slice. Tabs whose labels are wider than the viewport remain reachable by scrolling; labels can still truncate within a tab at extreme constrained widths.

## Verification and release gate

`BraceTabsTest` covers touch, mouse, keyboard, RTL, 2× text, controlled selection, panel state, disabled semantics, restoration, narrow vertical layout, and API 34+ automated accessibility checks. See the [M27 milestone report](milestones/m27-tabs.md) for actual local and hosted results. Manual TalkBack, tablet, and physical mouse review remain before these rows can be stable. The inventory retains `firstRelease: null` until publication.
