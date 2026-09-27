# Numeric input

`BraceNumericField` maps the pinned [Blueprint 6.18.0 NumericInput](https://github.com/palantir/blueprint/blob/a60d4c92257612808fbfac81cfeee4fcba91a8b4/packages/core/src/components/forms/numeric-input.mdx) to a controlled Compose field in `brace-core`. This row remains **in progress**. The [generated coverage ledger](coverage.md) records the implementation and release status; no Maven Central release includes it yet.

## Controlled string value

```kotlin
var amount by rememberSaveable { mutableStateOf("") }
BraceNumericField(
    value = amount,
    onValueChange = { amount = it },
    label = "Amount",
    min = 0.0,
    max = 100.0,
    stepSize = 1.0,
    majorStepSize = 10.0,
    minorStepSize = 0.1,
    clampValueOnBlur = true,
    onImeDone = { draft -> saveAmount(draft) },
)
```

Keep `value` as a **string**. The field sends exact typed drafts such as `-`, `0.`, `1e`, or `0,` to `onValueChange` without coercing them into a number. `onValueChange` only fires when text changes; selection movement does not fire it. Hoist the value with `rememberSaveable` when an unfinished draft must survive activity recreation. The default numeric-character filter permits digits, a leading sign, one decimal separator, and an exponent draft. Set `allowNumericCharactersOnly = false` if application code needs a custom parser. Direct text entry may temporarily exceed `min` and `max` so users can edit a value naturally.

Up/Down steps by `stepSize`; Shift+Up/Down uses `majorStepSize`; Alt+Up/Down uses `minorStepSize`. A null major or minor step disables that modified gesture; the shortcut is ignored so a host can handle it. Buttons also step by normal increments; mouse Shift/Alt click chooses major/minor increments. Stepping starts at zero when the value is empty, clamps to bounds, uses decimal arithmetic, and formats to the decimal precision of the minor step (or the normal step when minor is null), retaining any extra fractional precision required by a bound. For example, `0.2 + 0.1` yields `0.3`. `onButtonClick` runs after a button produces a changed string. `selectAllOnFocus` and `selectAllOnIncrement` support rapid replacement.

The `locale` parameter defaults to the current Android configuration, so a runtime language change changes parsing and step output. Comma-decimal locales accept `0,5` and format stepped values with a comma; localized decimal digits are parsed. A caller that retains a draft across a runtime locale switch should translate that string when it changes locale. Group separators and currency symbols are not accepted by the built-in numeric filter. `clampValueOnBlur = true` normalizes a complete value to the bounds on blur or IME Done, and clears an invalid nonempty draft. It is off by default, matching Blueprint. `onBlur` and `onImeDone` receive the normalized string when clamping is enabled.

## States and Android controls

```kotlin
BraceNumericField(
    value = draft,
    onValueChange = { draft = it },
    label = "Seats",
    supportingText = "Choose 1 through 12 seats",
    min = 1.0,
    max = 12.0,
    buttonPosition = BraceNumericButtonPosition.Start,
    size = BraceNumericFieldSize.Large,
    intent = BraceNumericIntent.Primary,
)
```

`buttonPosition` is logical `Start`, `End`, or `None` and follows RTL layout. Each visible `−` and `+` action has a 48dp or larger touch target and a localized default TalkBack label. They sit beside each other to meet Android touch guidance; Blueprint's narrow stacked HTML buttons are a web layout detail. The visible label provides a 48dp pointer target that focuses the input without creating another keyboard or TalkBack stop. The input exposes its label, value, bounds, helper text, and error to TalkBack. `enabled = false` disables both text entry and steps. `readOnly = true` keeps selection and copying available while preventing changes. Use `isError = true` with a localized `errorText` to announce the validation error and show its message. `supportingText`, intent, size, placeholder, focus ring, selection, dark/high-contrast color, and compact/comfortable density use Brace tokens. `showLabel = false` hides the visual label when a surrounding `BraceFormField` supplies one, while retaining the required accessible label.

The default step action labels come from English and Spanish Android resources. Override `incrementActionLabel` and `decrementActionLabel` for other languages or domain-specific speech. Keyboard entry uses a decimal IME and Done action; `onImeDone` lets a screen submit or validate its string. An enabled writable field marks itself as editable for `BraceShortcutRegistry`, so ordinary screen shortcuts do not interrupt typing.

A callback can reject an edit by leaving `value` unchanged; the visible field then returns to that value on the next composition while preserving accepted IME drafts. Blueprint's `defaultValue` and async React controlled mode map to caller-owned Compose state; DOM input refs, HTML attributes, CSS class names, and browser button placement have no separate Android API. The pinned documentation's number-abbreviation, scientific-expression, and arithmetic-expression **extended example is explicitly non-core** and has no built-in evaluator. Applications can build that behavior with callbacks and a custom parser. Long-press button auto-repeat is not yet implemented in this in-progress slice.

## Verification and limits

The source includes device tests for partial drafts, keyboard and touch/mouse stepping, precision, locale, bounds and blur clamping, disabled/read-only semantics, state restoration, RTL, large text, high contrast, and automated Compose accessibility where supported. Manual TalkBack and representative phone/tablet checks remain before stable status. See the [component list](coverage.md) for current availability.
