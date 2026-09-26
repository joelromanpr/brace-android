# Menus and overlays

This `brace-core` slice covers Blueprint's Menu, MenuItem, MenuDivider, Overlay2, Dialog, DialogBody, DialogFooter, and Alert rows, plus explicit Android mappings for the older Overlay, Portal, and overlay provider/hooks. Every row remains **in progress** until a release and broader device review. The catalog includes interactive examples; the generated coverage ledger remains the authority for availability.

## Menus

```kotlin
BraceMenu {
    BraceMenuItem("Open report", onClick = ::openReport)
    BraceMenuDivider(title = "Workspace")
    BraceMenuItem("Archived", onClick = ::toggleArchived, selected = archived)
}
```

`BraceMenu` is a static Compose menu for screens and dialogs. `BraceMenuPopup` adds native anchored positioning, outside-click and Back dismissal when a popup is needed. Items have 48 dp targets, disabled and selected semantics, intent colors, optional decorative icons, an end label, and multiline text. The active state represents hover or focus separately from selected state. Up and Down move keyboard focus between items. A titled divider is a heading; an untitled rule is excluded from TalkBack traversal. Apps own menu state and route navigation in callbacks. Blueprint HTML links and React rendering hooks map to callbacks, slots, and `Modifier`. Nested submenus, rich right-label elements, and Blueprint listbox role variants remain follow-up parity work. The platform popup animation is outside Brace's reduced-motion token control.

## Overlay windows and web mechanisms

```kotlin
val overlays = rememberBraceOverlayState()
BraceOverlayHost(overlays) {
    BraceOverlay(open = detailsOpen, onDismissRequest = { detailsOpen = false }, title = "Details") {
        // Accessible content with its own visible heading and actions.
    }
}
```

`BraceOverlay` is a controlled Android modal window. The caller changes `open` after `onDismissRequest`; configurable Back/Escape and outside-touch requests are sent only for the topmost layer in a shared `BraceOverlayHost`. The state holder exposes the number of open layers. A title supplies a TalkBack pane name; content provides the visible heading. Brace tokens style the surface, border, and maximum width, while Compose's dialog window handles placement, modal focus isolation, and the platform scrim. A host scopes nested overlays; use one near a screen root when multiple dialogs can coexist.

Blueprint's deprecated `Overlay` and `Overlay2` use one Compose API. DOM `Portal` and `PortalProvider` have no Android container equivalent: Compose `Dialog` and, for anchored menus, `DropdownMenu` create their own windows. `OverlaysProvider` maps to `BraceOverlayHost`, and `useOverlayStack` maps to `rememberBraceOverlayState`. DOM node targets, CSS portal classes, React refs, and browser scroll locks have no separate Android components. Nonmodal free-positioned overlays and custom transition hooks remain follow-up work.

## Dialogs and alerts

```kotlin
BraceDialog(
    open = editorOpen,
    onDismissRequest = { editorOpen = false },
    title = "Edit project",
    actions = {
        BraceButton("Cancel", onClick = { editorOpen = false },
            intent = BraceButtonIntent.Secondary)
        BraceButton("Save", onClick = ::save)
    },
) {
    BraceTextField(name, { name = it }, label = "Name")
}

BraceAlertDialog(
    open = deleteOpen,
    title = "Delete report?",
    message = "This cannot be undone.",
    onConfirm = ::deleteReport,
    onCancel = { deleteOpen = false },
    confirmLabel = "Delete",
    confirmIntent = BraceButtonIntent.Danger,
)
```

`BraceDialog` uses the controlled overlay window, a visible heading, a `BraceDialogBody` that scrolls under large text or short screens, and `BraceDialogActions` outside the body scroll. The footer wraps actions in logical reading order. The body and actions are also public layout building blocks. The close control is a 48 dp target; pass a localized `closeContentDescription`. A titleless dialog needs an accessible heading supplied in its body.

`BraceAlertDialog` requires an explicit confirmation action and a separate optional cancel callback. Back and outside-touch cancellation are disabled by default; callers can opt in only when a cancel callback exists. Loading suppresses repeated confirmation and cancellation. Label defaults are English, so applications should supply localized button text. The confirm button currently supports Brace primary, secondary, and danger intents; Blueprint success and warning intent treatments remain follow-up work.

## Acceptance status

This slice is not released. Core Android tests cover focus return, nested outside-touch order, dismissal flags, menu key movement, large text scrolling, and 48 dp targets. Back dismissal is wired through `DialogProperties` and was verified manually by opening the installed catalog Overlay2 sample and sending `adb shell input keyevent 4`; the instrumentation Activity still cannot deliver this key to the dialog, so its automated Back case is explicitly skipped. Manual TalkBack, mouse, and representative light/dark/high-contrast review remain. See the [coverage ledger](coverage.md) for each row and its current evidence.
