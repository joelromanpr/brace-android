# Form fields and editable text

This `brace-core` source slice covers the pinned Blueprint 6.18.0 FormGroup, TextArea, and EditableText rows. They remain **in progress**. The [coverage ledger](coverage.md) is the source of truth for release status; no Maven Central version has shipped.

## Form field

```kotlin
var notes by rememberSaveable { mutableStateOf("") }
val missing = notes.isBlank()
BraceFormField(
    label = "Incident notes",
    helperText = "Include the event time",
    errorText = if (missing) "Notes are required" else null,
    required = true,
    requiredDescription = "Required",
) { controlModifier ->
    BraceTextArea(
        value = notes,
        onValueChange = { notes = it },
        accessibilityLabel = "Incident notes",
        isError = missing,
        errorText = if (missing) "Notes are required" else null,
        modifier = controlModifier,
    )
}
```

`BraceFormField` takes one primary control in a modifier slot. Apply `controlModifier` to the control's outer node. It associates the visible label, label info, sublabel, helper, required announcement, and validation error with that node for TalkBack. Tapping the visible label requests focus on the control. The visible label and supporting text are announced through the control rather than becoming extra accessibility stops. A localized `requiredDescription` is mandatory when `required = true`; localize labels, help, and errors as well.

The default layout stacks label and control. `inline = true` uses a side-by-side layout only at 480dp or wider with font scale at most 1.3; narrow and large-text layouts stack. `fill` controls the wrapper width. `BraceFormIntent` colors supporting text for default, primary, success, warning, and danger states. `disabled` dims the wrapper; also set the child control's `enabled = false`. An error message in the wrapper does not silently change an arbitrary child's border, so pass the child's error state too.

Blueprint's `labelFor` and DOM IDs become the Compose focus requester and semantics modifier. HTML classes, style objects, and separate child-container class names have no Android component API.

## Text area

```kotlin
var details by rememberSaveable { mutableStateOf("") }
BraceTextArea(
    value = details,
    onValueChange = { details = it },
    accessibilityLabel = "Details",
    placeholder = "Add details",
    minLines = 3,
    maxLines = 8,
    autoResize = true,
    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
    keyboardActions = KeyboardActions(onDone = { submit(details) }),
)
```

`BraceTextArea` is controlled and multiline. With `autoResize = true`, its viewport grows from `minLines` through `maxLines` and is capped at 60% of the window height; longer content scrolls inside. With `autoResize = false`, it stays at `minLines` and scrolls. A nonblank `accessibilityLabel` is required. `isError = true` also requires a localized `errorText`. Focus, disabled, placeholder, error border, cursor, and selection styling use Brace semantic and input tokens. `keyboardOptions` and `keyboardActions` expose Android IME behavior; `readOnly` preserves selection without accepting edits. Hoist `value` with `rememberSaveable` when a draft must survive recreation.

Blueprint's browser resize handle, textarea ref, HTML attributes, and asynchronous React control mode map to bounded Compose layout, `Modifier`, and hoisted state. They have no separate Android component.

## Editable text

```kotlin
var projectName by rememberSaveable { mutableStateOf("Quarterly report") }
BraceEditableText(
    value = projectName,
    onValueChange = { projectName = it },
    label = "Project name",
    editActionLabel = "Edit project name",
    onConfirm = { saveName(it) },
)
```

`BraceEditableText` displays a value until touch or keyboard focus opens an inline editor. The caller owns the live value. Enter or IME Done confirms a single-line edit; Escape cancels and restores the last confirmed value. Blur confirms without stealing focus from the next control. `onConfirm` and `onCancel` run only when the draft changed. An optional controlled `isEditing` pairs with `onEditingChange`; otherwise edit mode and the draft are saved by the component. Supply a localized `label` and `editActionLabel` for TalkBack. Disabled, placeholder, intent, supporting text, error, maximum length, and select-all-on-focus states are exposed.

For `multiline = true`, Enter inserts a line and Control/Command+Enter confirms. `confirmOnEnterKey = true` reverses those shortcuts. Use `minLines` and `maxLines` to bound the editor. Android exposes the edit affordance without relying on browser hover; Blueprint's DOM element ref, CSS hover styling, HTML input attributes, and experimental `alwaysRenderInput` mechanism have no separate API. Use `BraceTextField` or `BraceTextArea` for an always-editable field.

## Verification and limits

Focused API 36 tests pass for FormField and TextArea: label-tap focus, required/helper/error semantics, a FormField/TextArea combination, IME entry, state restoration through a caller-owned value, bounded versus fixed resizing, RTL, large text, dark high contrast, and an automated Compose accessibility check. EditableText device verification and the integrated M8 build are still pending in this source branch. Manual TalkBack and representative visual checks remain before any stable status.
