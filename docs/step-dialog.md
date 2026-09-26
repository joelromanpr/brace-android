# Step dialogs

`BraceStepDialog` covers the pinned Blueprint `MultistepDialog` and `DialogStep` rows with a controlled Compose API. It uses `BraceDialog` for a modal Android window, Back/Escape and outside-touch policy, pane semantics, scrollable content, and fixed actions. The comparison source is [Blueprint core 6.18.0's dialog documentation](https://github.com/palantir/blueprint/blob/a60d4c92257612808fbfac81cfeee4fcba91a8b4/packages/core/src/components/dialog/dialog.mdx).

```kotlin
var open by rememberSaveable { mutableStateOf(false) }
var step by rememberSaveable { mutableStateOf("details") }
var name by rememberSaveable { mutableStateOf("") }
var error by rememberSaveable { mutableStateOf(false) }
val launcher = remember { FocusRequester() }

BraceButton("Create report", onClick = { step = "details"; open = true },
    modifier = Modifier.focusRequester(launcher))
BraceStepDialog(
    open = open,
    selectedStepId = step,
    onStepChange = { next, _ -> step = next },
    onDismissRequest = { open = false },
    onComplete = { saveReport(name); open = false },
    title = "Create report",
    focusReturnRequester = launcher,
    steps = listOf(
        BraceDialogStep("details", "Details", validate = {
            error = name.isBlank(); !error
        }) {
            BraceTextField(name, { name = it; error = false }, label = "Report name",
                isError = error, supportingText = if (error) "Enter a name" else null)
        },
        BraceDialogStep("review", "Review") { Text("Name: $name") },
    ),
)
```

The caller owns `open`, `selectedStepId`, and all business data. `onStepChange` reports both IDs; update the selected ID to show the requested step. The dialog never closes automatically on completion. `canAdvance = false` disables Next or Complete while input is incomplete or work is pending. `validate` runs on forward changes and Complete, so it can reveal field errors before navigation. Back always returns to an earlier step. Previously visited steps are available from the rail, but forward jumps still validate the current step. Give every step a stable, unique, nonblank string ID and a nonblank title. The dialog title must also be nonblank so the TalkBack pane is named. When reordering dynamic steps, keep the selected ID valid. Visited history follows stable IDs rather than positions, so newly inserted steps do not become available by accident.

`rememberSaveable` values inside a step panel survive switching between steps through `SaveableStateHolder`. Keep data needed after removing the dialog from composition in state above it. By default the visible history resets when the dialog is reopened; the caller resets `selectedStepId` to restart. No network or form submission is performed by the component.

The rail is horizontal on narrow windows and scrolls rather than shrinking step targets. At wider widths, `BraceStepNavigation.Auto` uses a side rail. `Start` and `End` follow layout direction; `Top` forces a horizontal rail. Heading focus moves to the active panel after navigation. A launcher with `Modifier.focusRequester(launcher)` and `focusReturnRequester = launcher` restores keyboard focus after dismissal. Keep `BraceStepDialog` composed with `open = false` through that transition; removing it in the same composition skips its focus-return effect. Back dismisses the modal window; the visible Back button changes steps. Escape follows the Back dismissal flag. Outside touch is disabled by default for multi-part forms.

The selected, available, and upcoming states use Brace semantic colors and the dialog's component tokens. Each step target measures at least 48 dp. The rail exposes tab roles, selected/disabled states, a localized positional label, and a named step. The active panel has heading semantics, and the dialog title names the TalkBack pane. Supply localized `BraceStepDialogLabels`, including its `position` formatter, for non-English apps. Long panels and large text scroll within the dialog while actions remain visible. Compact density keeps accessible targets; light, dark, high-contrast, and brand modes use the active Brace theme.

This API is **in progress**. The API 36 interaction/accessibility suite, mouse review, manual TalkBack, 2× text, and hosted CI are required before stable status. A native Compose API replaces React child inspection, HTML classes, and Blueprint's fixed 800 px minimum width. Custom Blueprint button-prop forwarding is represented by caller-owned validation, completion, and localized labels; arbitrary per-step action slots are not included yet.
