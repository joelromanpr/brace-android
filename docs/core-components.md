# Core components

The `1.0.0` release includes buttons, checkboxes, switches, and a single-line text field. [Add `brace-core`](installation.md), wrap your screen in `BraceTheme`, and try the examples below. Their APIs are still in progress; the [component status](coverage.md) lists tests and remaining work.

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

The API accepts optional `leadingIcon` and `trailingIcon` composable slots. The text label is mandatory and announced to accessibility services; icon slots are decorative. Optional `accessibilityLabel` and `onClickLabel` supply a more specific spoken label and click action, for example a navigation destination. Filled and outlined visual variants use token colors for default, hover, pressed, focused, disabled, and loading states. Compact density draws a shorter visual control inside a 48 dp minimum hit region; large text may grow the control. Compose `clickable` provides touch, mouse, Enter, and Space activation. A focused button gets a visible token-colored border. `loading` suppresses activation and announces a loading state.

For navigation, call your app's navigator from `onClick`. For a URI or app destination, use [`BraceLinkButton`](links.md) or the text [`BraceLink`](links.md).

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

`BraceTextField` is a single-line field with hoisted string state, keyboard options, visual transformation, read-only and disabled modes, and an accessibility error message. Icons, clear buttons, suggestion popups, multiline input, and format-specific controls remain follow-up work. `KeyboardOptions` configures the Android keyboard; the cursor and border use input tokens. Restore the value with `rememberSaveable` or a persisted model. Inside `BraceShortcutRegistry`, this field marks its editable focus automatically so screen shortcuts do not interrupt typing. Custom Compose text inputs can use `Modifier.braceShortcutEditable()`.

## Verification and acceptance

`BraceCoreInteractionTest` checks enabled/disabled/loading button behavior and 48 dp targeting, controlled checkbox and switch toggling under RTL and compact high-contrast dark mode, field input and its announced label, operation at 2x font scale, and an automated accessibility audit on API 34 or later. Compile it with `./gradlew :brace-core:compileDebugAndroidTestKotlin`; run it on a connected device with `./gradlew :brace-core:connectedDebugAndroidTest`. The theme's unit tests check default contrast pairs. Before marking these rows stable, inspect at 1.3x and 2.0x font scales, and manually check TalkBack, keyboard focus order, mouse hover, and all four color schemes.
