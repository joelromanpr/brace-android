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

The default layout stacks label and control. `inline = true` uses a side-by-side layout only at 480dp or wider with font scale at most 1.3; narrow and large-text layouts stack. `fill` controls the wrapper width. The label has a 48dp or larger pointer target and does not add a separate keyboard or TalkBack focus stop. `BraceFormIntent` colors supporting text for default, primary, success, warning, and danger states. `disabled` dims the wrapper; also set the child control's `enabled = false`. An error message in the wrapper does not silently change an arbitrary child's border, so pass the child's error state too.

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
    intent = BraceFormIntent.Primary,
    size = BraceTextAreaSize.Medium,
    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
    keyboardActions = KeyboardActions(onDone = { submit(details) }),
)
```

`BraceTextArea` is controlled and multiline. With `autoResize = true`, its viewport grows from `minLines` through `maxLines` and is capped at 60% of the screen height reported by Android configuration; longer content scrolls inside. With `autoResize = false`, it stays at `minLines` and scrolls. A nonblank `accessibilityLabel` is required. `isError = true` also requires a localized `errorText`. The field announces it to TalkBack; show the same message visibly through `BraceFormField` or a sibling `Text` in standalone use so the error is not communicated by border color alone. `intent` selects a semantic border color; `BraceTextAreaSize.Small`, `Medium`, and `Large` use Brace typography, padding, and minimum-height tokens. Focus, disabled, placeholder, error border, cursor, and selection styling use Brace semantic and input tokens. `keyboardOptions` and `keyboardActions` expose Android IME behavior; `readOnly` preserves selection without accepting edits. Inside `BraceShortcutRegistry`, an enabled writable text area automatically suppresses ordinary shortcuts while focused; commands explicitly marked `allowInEditable` may run. Hoist `value` with `rememberSaveable` when a draft must survive recreation.

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

`BraceEditableText` displays a value until touch or keyboard focus opens an inline editor. The caller owns the live value. Enter or IME Done confirms a single-line edit; Escape cancels and restores the last confirmed value. Blur confirms without stealing focus from the next control. `onConfirm` and `onCancel` run only when the draft changed. An optional controlled `isEditing` pairs with `onEditingChange`; otherwise edit mode and the draft are saved by the component. Supply a localized `label` for TalkBack. The edit action uses a localized built-in label by default and accepts a localized `editActionLabel` override. Disabled, placeholder, intent, supporting text, error, maximum length, and select-all-on-focus states are exposed.

For `multiline = true`, Enter inserts a line and Control/Command+Enter confirms. `confirmOnEnterKey = true` reverses those shortcuts. Use `minLines` and `maxLines` to bound the editor. Android exposes the edit affordance without relying on browser hover; Blueprint's DOM element ref, CSS hover styling, HTML input attributes, and experimental `alwaysRenderInput` mechanism have no separate API. Use `BraceTextField` or `BraceTextArea` for an always-editable field.

## Verification and limits

Focused API 36 FormField and TextArea tests pass 14/14 (6 FormField, 8 TextArea) with no failures, errors, or skips. They cover label-tap focus and minimum target, required/helper/error semantics, a FormField/TextArea combination, narrow and large-text layout fallback, IME entry, state restoration through a caller-owned value, size/intent states, bounded versus fixed resizing, touch scrolling inside the 60%-screen cap, writable shortcut suppression, read-only/disabled semantics, RTL, dark high contrast, and an automated Compose accessibility check. EditableText's earlier focused suite passed 11/11 with no failures, errors, or skips, including localized default edit-action behavior, controlled mode, and focus/restoration regressions. A new disable/re-enable regression is pending final-diff device verification. The catalog examples were visually checked on an Android API 36 device at 320 × 640 dp without clipping or overlap. Integrated review and manual TalkBack listening remain before any stable status.
