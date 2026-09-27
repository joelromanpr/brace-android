# Command palette

`brace-select` includes a source implementation of `BraceCommandPalette`, an Android adaptation of the pinned Blueprint [Omnibar](https://blueprintjs.com/docs/#select/omnibar). The inventory row is **in progress**. The comparison uses the pinned [Omnibar documentation](https://github.com/palantir/blueprint/blob/a60d4c92257612808fbfac81cfeee4fcba91a8b4/packages/select/src/components/omnibar/omnibar.mdx) and [source](https://github.com/palantir/blueprint/blob/a60d4c92257612808fbfac81cfeee4fcba91a8b4/packages/select/src/components/omnibar/omnibar.tsx). No Maven Central version has shipped.

```kotlin
val commands = listOf(
    BraceCommand("open", RecordAction.Open, "Open record", group = "Records", shortcut = "Ctrl+O"),
    BraceCommand("export", RecordAction.Export, "Export CSV", group = "Reports",
        description = "Download the current report"),
    BraceCommand("delete", RecordAction.Delete, "Delete record", group = "Records", enabled = false),
)
var paletteOpen by rememberSaveable { mutableStateOf(false) }
val queryState = rememberBraceQueryListState()
val triggerFocus = remember { FocusRequester() }

BraceButton("Commands", onClick = { paletteOpen = true }, modifier = Modifier.focusRequester(triggerFocus))
BraceCommandPalette(
    commands = commands,
    open = paletteOpen,
    onOpenChange = { paletteOpen = it },
    onExecute = { command -> runRecordAction(command.value) },
    title = "Commands",
    state = queryState,
    restoreFocusTo = triggerFocus,
)
```

The caller controls opening and closing. `BraceCommand` has a stable key, typed value, visible label, optional group, description, shortcut, and enabled state. Groups appear in first-seen order; ungrouped actions are in their own section. Labels and descriptions match a query by default, and `predicate` can search other fields. The catalog shows grouping, a disabled action, a loading state, current query, selected command, runtime theme controls, and copyable usage. Supply localized labels and shortcuts relevant to the host app, including `queryLabel`, `emptyLabel`, `loadingLabel`, and `closeLabel` when overriding defaults.

The modal search field takes focus when opened. Arrow Up/Down, Home/End, and Enter operate enabled commands; disabled commands stay visible and are skipped. The software keyboard Search action activates the current command. During IME composition, Enter stays with the input method and does not run a command. The visible Close action, Escape, system Back, or an outside touch requests closure; the caller must update `open`. Touch, mouse, keyboard, and TalkBack command actions use the same `onExecute` callback. `selectedKey` optionally marks a checked command. Search announces the active command's position; group headings and disabled state are exposed to accessibility. `loading` replaces results with an indeterminate spoken status, and an empty result has a polite announcement. Each action and the search field meet the 48 dp touch minimum.

`rememberBraceQueryListState` saves the query and active key through activity recreation. The caller saves `open` with `rememberSaveable` when it should restore. The editor uses `TextFieldValue` internally to preserve cursor and IME composition during typing; an external query replacement moves the cursor to the end of the new text. `resetQueryOnExecute` defaults to true. Pass `restoreFocusTo` to return keyboard focus to a caller-owned trigger after closure. The Android dialog window confines modal focus and exposes a TalkBack pane title. Brace uses semantic and component tokens for light, dark, high contrast, brand overrides, and compact or comfortable density. The palette does not animate, so reduced-motion mode requires no special transition.

Blueprint's `Omnibar<T>` composes `Overlay2` and `QueryList<T>`, requires caller-owned `isOpen`, has no built-in trigger, and defaults to no initial result content. Brace replaces the React overlay/provider and DOM input with a native Android dialog and Compose text field. Brace shows commands before typing to support discovery and adds optional grouping and a loading status. The optional focus requester provides deterministic focus return for a caller's trigger. The inventory records this as an **Android adaptation**. These source APIs still require hosted CI, release review, and broader device accessibility checks before the row can become stable; see the [M23 report](milestones/m23-command-palette.md).
