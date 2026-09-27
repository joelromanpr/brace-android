# Panel stack

`BracePanelStack` displays the last destination in a root-first list. The first panel cannot be removed. The API is an Android adaptation of [Blueprint PanelStack 6.18.0](https://github.com/palantir/blueprint/blob/a60d4c92257612808fbfac81cfeee4fcba91a8b4/packages/core/src/components/panel-stack/panel-stack.mdx) and its [pinned implementation](https://github.com/palantir/blueprint/blob/a60d4c92257612808fbfac81cfeee4fcba91a8b4/packages/core/src/components/panel-stack/panelStack.tsx). Brace uses its own visual tokens and Compose navigation behavior.

## Saveable stack

```kotlin
val stack = rememberBracePanelStackState(BracePanel("root", "Workspace"))
BracePanelStack(stack, modifier = Modifier.height(320.dp)) {
    when (panel.id) {
        "root" -> BraceButton("Open filters", onClick = {
            openPanel(BracePanel("filters", "Filters"))
        })
        "filters" -> FiltersScreen(onDone = ::closePanel)
    }
}
```

`BracePanel.id` is a stable instance key, and IDs must be unique within a stack. The stack and its titles survive Activity recreation. `rememberSaveable` state inside a covered panel survives when `preservePanelState` is true (the default). Hoist unsaveable work such as streams or long-running tasks above the panel when it must continue while covered. A popped panel is removed from the stack. Use a fresh ID for a new visit when its saveable content state must start over.

## Controlled stack

```kotlin
var panels by remember { mutableStateOf(listOf(BracePanel("root", "Workspace"))) }
BracePanelStack(
    stack = panels,
    onOpenPanel = { panels = panels + it },
    onClosePanel = { panels = panels.dropLast(1) },
    modifier = Modifier.height(320.dp),
) {
    // Render the route identified by panel.id.
}
```

The caller owns controlled state and must supply a Saver if the stack should survive Activity recreation. Append a new destination to open it and remove the last destination to pop. The library ignores close requests at the root. Callbacks on the stateful overload report completed opens and closes. Give a panel content a bounded height when it contains a scrolling list.

## Interaction and accessibility

- A header shows the current panel title as a heading and a labeled back action naming the previous panel. Set `showHeader = false` when a host supplies an equivalent heading and navigation control.
- The header back action, Android system Back, Escape, and `BracePanelScope.closePanel()` pop the same active entry. At the root, Brace leaves Android Back to the host.
- On a push or pop, keyboard focus moves to the new header back action or root title. With no header, it moves to the first focusable control in the active pane. The host supplies a heading for panes without one. The inactive panel leaves the focus and accessibility trees after the transition.
- Push and pop slide in logical start/end directions, including RTL. `BraceMotion.Reduced` and the system animator scale turn the transition off through `BraceTheme.motionTokens`.
- The back action is at least 48 dp, exposes one labeled button action to TalkBack, and has a visible keyboard focus boundary. The pane title is announced separately.

The [Android catalog capture](site/showcase/panel-stack-400.png) shows the Filters pane open above Workspace, with its back action visible. The capture is from a draft branch and does not change the inventory status.

The catalog app includes nested panels and a header toggle. Android tests cover root protection, controlled and saveable state, keyboard and system Back, RTL, large text, high contrast, reduced motion, native accessibility nodes, and automated accessibility checks where supported.

## Blueprint mapping and limits

Blueprint injects `openPanel` and `closePanel` into React panel renderers and accepts arbitrary renderer props and HTML titles. Brace supplies typed destination IDs and `BracePanelScope` actions; application data belongs to the caller. Blueprint's `renderActivePanelOnly = false` keeps every React tree mounted in the DOM. Brace composes the active panel and preserves its saveable state by default. Arbitrary `remember` state and effects are not retained while a panel is covered. The host can keep shared work above the stack. Blueprint's CSS transitions map to Compose transitions controlled by Brace motion tokens. There is no DOM `className` or HTML title API.

This source slice is **in progress** and has no public release. The [inventory](coverage.md) tracks its implementation, sample, documentation, tests, and eventual first release. See the [M32 report](milestones/m32-panel-stack.md) for current verification.
