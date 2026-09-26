# Core components

The first `brace-core` slice implements Button, Checkbox, Switch, and a single-line text field mapped to Blueprint InputGroup. These APIs are **in progress** in the pinned inventory. They compile and have catalog examples and interaction tests, but have no public Maven release or completed device test matrix yet. Do not treat them as full Blueprint parity.

Use `io.github.joelromanpr.brace:brace-core` after the first Maven release, or publish the current snapshot to Maven Local as described in [installation](installation.md). Wrap a screen in `BraceTheme`; every control below reads semantic or component tokens. The catalog's component list and availability labels come from `inventory/blueprint-components.json` through the coverage generator.

## Button

```kotlin
import io.github.joelromanpr.brace.core.BraceButton
import io.github.joelromanpr.brace.core.BraceButtonIntent
import io.github.joelromanpr.brace.core.BraceButtonVariant

BraceButton(label = "Save", onClick = viewModel::save)
BraceButton(label = "Delete", onClick = viewModel::delete, intent = BraceButtonIntent.Danger)
BraceButton(label = "Secondary", onClick = {}, intent = BraceButtonIntent.Secondary)
BraceButton(label = "Outlined", onClick = {}, variant = BraceButtonVariant.Outline)
BraceButton(label = "Pending", onClick = {}, loading = true)
```

The API accepts optional `leadingIcon` and `trailingIcon` composable slots. The text label is mandatory and announced to accessibility services; icon slots are decorative. Filled and outlined visual variants use token colors for default, hover, pressed, focused, disabled, and loading states. Compact density draws a shorter visual control inside a 48 dp minimum hit region; large text may grow the control. Compose `clickable` provides touch, mouse, Enter, and Space activation. A focused button gets a visible token-colored border. `loading` suppresses activation and announces a loading state.

An Android navigation action should call the app's navigator from `onClick`. Blueprint's web anchor behavior has no URL or DOM equivalent inside this button. Apps that need a link role should use an appropriate native text-link or navigation component; the inventory keeps this adaptation explicit.

## Checkbox

```kotlin
var includeArchived by rememberSaveable { mutableStateOf(false) }
BraceCheckbox(
    checked = includeArchived,
    onCheckedChange = { includeArchived = it },
    label = "Include archived",
)
```

The caller owns state. The full row is the 48 dp or larger target. Compose exposes checkbox role and checked state to TalkBack and keyboard users. Checked, unchecked, disabled, and focused visuals read checkbox tokens. The label stays in logical start/end order under RTL.

## Switch

```kotlin
var notifications by rememberSaveable { mutableStateOf(true) }
BraceSwitch(
    checked = notifications,
    onCheckedChange = { notifications = it },
    label = "Notifications",
)
```

Use a switch for an immediate on/off setting. The row is the touch target and exposes switch semantics. The track, thumb, disabled state, and focus outline read switch tokens. Hoisted state lets the app persist and restore the choice.

## Text field

```kotlin
var projectName by rememberSaveable { mutableStateOf("") }
BraceTextField(
    value = projectName,
    onValueChange = { projectName = it },
    label = "Project name",
    placeholder = "Enter a name",
    isError = projectName.isBlank(),
    supportingText = if (projectName.isBlank()) "A name is required" else null,
)
```

`BraceTextField` is a single-line field with hoisted string state, keyboard options, visual transformation, read-only and disabled modes, and an accessibility error message. It maps the first part of Blueprint InputGroup into a labeled Android input. Icon affordances, clear buttons, suggestion popup behavior, multiline input, and format-specific controls are separate follow-up work. On Android, `KeyboardOptions` configures the IME, and the text cursor and border use input tokens. The caller restores value with `rememberSaveable` or a persisted model.

## Verification and acceptance

`BraceCoreInteractionTest` checks enabled/disabled/loading button behavior and 48 dp targeting, controlled checkbox and switch toggling under RTL and compact high-contrast dark mode, field input and its announced label, operation at 2x font scale, and an automated accessibility audit on API 34 or later. Compile it with `./gradlew :brace-core:compileDebugAndroidTestKotlin`; run it on a connected device with `./gradlew :brace-core:connectedDebugAndroidTest`. The theme's unit tests check default contrast pairs. Before marking these inventory rows stable, run the device tests, inspect at 1.3x and 2.0x font scales, and manually check TalkBack, keyboard focus order, mouse hover, and all four color schemes. The inventory's `firstRelease` stays empty until an actual release.
