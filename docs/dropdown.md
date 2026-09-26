# Single-choice dropdown

`BraceDropdown` is the `brace-core` Android adaptation of the pinned Blueprint 6.18.0 [HTMLSelect](https://blueprintjs.com/docs/#core/components/html-select). Its inventory row is **in progress**; there is no Maven Central release or stable-coverage claim.

```kotlin
var region by rememberSaveable { mutableStateOf<String?>(null) }
BraceDropdown(
    options = listOf(
        BraceDropdownOption("east", "East"),
        BraceDropdownOption("west", "West"),
        BraceDropdownOption("central", "Central", enabled = false),
    ),
    selectedValue = region,
    onValueChange = { region = it },
    label = "Region",
    placeholder = "Choose a region",
)
```

The caller owns `selectedValue`. Save it in screen state or a persisted model for restoration. The popup is transient and closes after a choice, Back, Escape, or an outside tap. Selecting the already selected value closes the popup without repeating the callback. Values must be unique; the optional blank value can represent a domain choice when it has a nonblank label. A selected value absent from `options` shows the placeholder until the data source supplies it.

`label`, `placeholder`, and `supportingText` are app-localized strings. A 48 dp or larger trigger announces the label and current choice to TalkBack, plus expanded or collapsed state. Disabled options remain visible but cannot be activated. Keyboard users can focus the trigger, press Enter, Space, or an arrow to open it, move through enabled options with Up and Down, choose with Enter or Space, and close with Escape. The menu returns focus to the trigger. Touch and mouse use the same action; `Row` and the anchored popup follow RTL layout. At large text sizes the choice can wrap to two lines. The menu, field, focus outline, error, and disabled visuals use Brace input, menu, semantic, typography, spacing, sizing, and density tokens. The site and catalog theme controls show light, dark, high contrast, and compact or comfortable density.

Use `size = BraceDropdownSize.Small` or `Large` for typography and spacing, `minimal = true` for a transparent field, `fill = false` for intrinsic width, and `enabled = false` for a disabled field. Set `isError = true` with nonblank, localized `supportingText` to announce validation; the API requires a message for an error state. Keep this component to short, static option lists. For filtering, custom item rendering, or many results, use `BraceSelect` in `brace-select`; use its multiple-selection API when that later inventory row is complete.

Blueprint's native HTML `<select>`, `<option>` children, `value`/`defaultValue`, DOM refs, HTML attributes, CSS classes, wrapper, and caret icon props map to controlled Compose state, typed `BraceDropdownOption`s, a tokenized anchored menu, and Android focus and accessibility semantics. They have no separate DOM-style public API. Blueprint HTMLSelect explicitly disallows `multiple`, so this component is single choice. The [pinned source](https://github.com/palantir/blueprint/blob/a60d4c92257612808fbfac81cfeee4fcba91a8b4/packages/core/src/components/html-select/htmlSelect.tsx) records that boundary.

## Verification

`BraceDropdownTest` passed 7/7 API 36 cases covering controlled selection, disabled options, touch, keyboard, mouse, native accessibility click, focus return, error semantics, RTL, 2x text, compact high contrast dark mode, state restoration, and an automated Compose accessibility audit where supported. The catalog was visually reviewed at 320 dp in light and dark high contrast plus 200% Android text. Manual human TalkBack and real hardware review remain before stable status. Use the generated [coverage ledger](coverage.md) as the authoritative availability record.
