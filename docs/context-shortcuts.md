# Context menus and keyboard shortcuts

This `brace-core` source slice maps six pinned Blueprint rows to Android Compose. All six remain **in progress**. The [coverage ledger](coverage.md) records implementation, sample, tests, and release status per row; all are available in `1.0.0` but remain in progress.

## Context menu

```kotlin
var rowMenuOpen by rememberSaveable { mutableStateOf(false) }
BraceContextMenu(
    expanded = rowMenuOpen,
    onExpandedChange = { rowMenuOpen = it },
    title = "Report actions",
    targetIsFocusable = true,
    target = { targetModifier ->
        BraceButton("Quarterly report", onClick = ::openReport, modifier = targetModifier)
    },
) { dismiss ->
    BraceMenuItem("Copy link", onClick = { copyLink(); dismiss() })
    BraceMenuItem("Archive", onClick = { archiveReport(); dismiss() })
}
```

`BraceContextMenu` wraps a target without changing its normal tap action. Set `targetIsFocusable = true` when the target already has a focus stop, as in the `BraceButton` example, so Tab visits it once; leave the default for noninteractive targets. Apply the modifier provided to `target` to the target control so its primary click and context action share one TalkBack node. Secondary mouse click opens at the pointer, touch or stylus long press opens at the held point, and Shift+F10 or the hardware Menu key opens at the target's logical start. TalkBack gets a localized long-click action through `openActionLabel`. The caller owns `expanded`; set it to false when the menu closes. If an open state is restored, Brace waits for target layout and anchors at its logical start; while open, the point follows target movement and is clamped inside its bounds after size changes. The content receives a `dismiss` callback so an item can close the menu after its action or keep it open intentionally. The popup is focusable, uses the existing Brace menu and overlay tokens, clamps to the window in LTR and RTL, and handles Escape, Back, and outside click. `title` names the TalkBack pane. A device regression checks that nested targets give the inner menu the gesture; manual TalkBack review remains before stable status.

Blueprint's [ContextMenu](https://github.com/palantir/blueprint/blob/a60d4c92257612808fbfac81cfeee4fcba91a8b4/packages/core/src/components/context-menu/context-menu.mdx) uses React target render props and browser `contextmenu` events. Brace uses a Compose target slot and Android input events. An open Brace context menu does not automatically disable a nested tooltip; coordinate their `enabled` and `expanded` state when combining them.

### Controlled point-anchored surface

```kotlin
var menuOpen by rememberSaveable { mutableStateOf(false) }
val pointTrigger = remember { FocusRequester() }
var hadPointMenuOpen by remember { mutableStateOf(false) }
LaunchedEffect(menuOpen) {
    if (menuOpen) hadPointMenuOpen = true
    else if (hadPointMenuOpen) {
        pointTrigger.requestFocus()
        hadPointMenuOpen = false
    }
}
BraceButton("More actions", onClick = { menuOpen = true }, modifier = Modifier.focusRequester(pointTrigger))
BraceContextMenuPopup(
    expanded = menuOpen,
    onDismissRequest = { menuOpen = false },
    targetOffset = IntOffset(80, 220), // window pixels; derive from layout coordinates in an app
    title = "More actions",
) { dismiss ->
    BraceMenuItem("Refresh", onClick = { refresh(); dismiss() })
}
```

`BraceContextMenuPopup` is the independent mapping for Blueprint's [ContextMenuPopover](https://github.com/palantir/blueprint/blob/a60d4c92257612808fbfac81cfeee4fcba91a8b4/packages/core/src/components/context-menu/context-menu-popover.mdx). It accepts caller-owned state and a window-pixel `IntOffset`, supplies no trigger, and participates in `BraceOverlayHost` stacking when present. The caller restores focus to its own trigger after dismissal; the example uses a `FocusRequester` after the popup leaves composition, including when an open menu was restored. Blueprint's virtual DOM target, portal, and Floating UI positioning are replaced by a Compose `Popup` position provider. Use this API for programmatic point-based menus; use `BraceContextMenu` when the target should own mouse, touch, and keyboard gestures.

## Shortcut scopes and discovery

```kotlin
val refresh = BraceShortcut(
    combo = "ctrl+r",
    label = "Refresh reports",
    spokenComboLabel = "Control plus R",
    group = "Reports",
    onKeyDown = ::refreshReports,
)
val shortcutState = rememberBraceShortcutRegistryState()
BraceShortcutRegistry(
    shortcuts = listOf(refresh),
    state = shortcutState,
    discoveryTitle = "Keyboard shortcuts",
) {
    Column {
        BraceButton("Show shortcuts", onClick = { shortcutState.showDiscovery() })
        BraceShortcutScope(
            shortcuts = listOf(
                BraceShortcut("ctrl+shift+e", "Export here", onKeyDown = ::export),
                BraceShortcut("ctrl+g", "Global export", global = true, onKeyDown = ::export),
            ),
        ) {
            BraceButton("Export", onClick = ::export)
        }
        BraceTextField(query, { query = it }, label = "Search")
        BraceShortcutLabel("Ctrl+R", spokenLabel = "Control plus R")
    }
}
```

`BraceShortcut` uses case-insensitive, plus-separated combinations such as `ctrl+s`, `alt+left`, and `mod+enter`; on Android `mod` means Control. A callback can run on key down or key up, and repeats are suppressed unless `repeatable` is true. `BraceShortcutRegistry` is a screen-level Compose host for global-in-screen shortcuts and a grouped discovery dialog opened with `?` (Shift+Slash on common hardware layouts) or a caller-owned `BraceShortcutRegistryState.showDiscovery()` action. `BraceShortcutRegistry(shortcuts = …)` registers commands for its full Compose focus tree. `BraceShortcutScope` and `Modifier.braceShortcuts` register local commands that run only while focus is inside their subtree. Set `global = true` on a shortcut inside a mounted local scope to make it active throughout the nearest registry, including when focus moves outside that subtree; it unregisters when the scope leaves composition. Focused local commands take precedence over the registry's screen commands; root shortcuts take precedence over dynamically registered global shortcuts. Focusable descendants must receive hardware key events; none of these APIs installs an OS-wide registration. The discovery dialog lists enabled screen, mounted global, and local shortcuts. Localize `label` and `group`; set `spokenComboLabel` when a shortcut needs a localized or clearer TalkBack key phrase. Discovery combines each action and combination into one accessible row. The default discovery title is an Android string resource, or supply a localized `discoveryTitle`.

`BraceTextField` marks its enabled, writable input as editable automatically, suppressing ordinary local and screen-global shortcuts while it has focus. Mark custom editable subtrees with `Modifier.braceShortcutEditable()` or set `editable = true` on a scope; unmarked custom text fields may receive shortcuts while typing. `allowInEditable = true` is for commands that are safe during text entry. Avoid overriding Android system key combinations or IME editing keys. `BraceShortcutLabel` is display-only, with Brace key-cap colors, border, shape, spacing, and typography; use its `spokenLabel` for a standalone localized key-cap announcement. It does not register a handler. Keyboard layouts differ, so applications should describe nonobvious combinations in localized text.

Blueprint's [HotkeysTarget](https://github.com/palantir/blueprint/blob/a60d4c92257612808fbfac81cfeee4fcba91a8b4/packages/core/src/components/hotkeys/hotkeys-target.mdx) class wrapper maps to `BraceShortcutScope`; its [HotkeysProvider](https://github.com/palantir/blueprint/blob/a60d4c92257612808fbfac81cfeee4fcba91a8b4/packages/core/src/context/hotkeys/hotkeys-provider.mdx) maps to `BraceShortcutRegistry`; [useHotkeys](https://github.com/palantir/blueprint/blob/a60d4c92257612808fbfac81cfeee4fcba91a8b4/packages/core/src/hooks/hotkeys/use-hotkeys.mdx) maps to `Modifier.braceShortcuts`. The visual `KeyComboTag` maps to `BraceShortcutLabel`. React contexts, document listeners, HTML focus attributes, and browser key strings have no separate Android component.

## Verification and limits

The source includes focused device tests for context gestures, popup dismissal, shortcut precedence, editable suppression, discovery, and labels. Manual TalkBack, hardware keyboard, mouse/stylus, small-window, RTL, large-text, and theme review remain before stable status. Check the [component list](coverage.md) for current availability.
