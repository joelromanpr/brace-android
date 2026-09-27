# Tag input

`BraceTagInput` is an Android adaptation of [Blueprint TagInput](https://blueprintjs.com/docs/#core/components/tag-input) in the pinned `@blueprintjs/core@6.18.0` inventory. It is implemented in `brace-core` and is **in progress**. No Maven Central release includes it yet. The generated [coverage page](coverage.md) remains the authority for shipped status.

```kotlin
var tags by rememberSaveable { mutableStateOf(listOf("Compose", "Android")) }
var draft by rememberSaveable { mutableStateOf("") }

BraceTagInput(
    values = tags,
    onValuesChange = { tags = it },
    draft = draft,
    onDraftChange = { draft = it },
    label = "Skills",
    placeholder = "Add a skill",
    duplicatePolicy = BraceTagDuplicatePolicy.RejectIgnoreCase,
    validator = { it.length >= 2 },
    onRejected = { value, reason -> showValidation(value, reason) },
)
```

Both tags and the draft are controlled. Hoist them into saveable screen state for rotation and process restoration. The field splits at commas and line breaks by default; `separators` sets individual delimiter characters, or `""` disables splitting. Enter and IME Done submit the whole draft. A typed delimiter submits complete values and leaves an unfinished tail in the editor. Bulk insertion with a delimiter, including Android system paste, submits all values. The placeholder stays visible in an empty editor even when tags are present, so a wrapped line remains understandable. A single pasted word remains in the editor for correction; `addOnPaste = false` keeps even separated bulk text in the draft until Enter/IME. `addOnBlur = true` optionally submits the draft when focus leaves the field. The optional `onTagsAdded` callback reports `Separator`, `Paste`, `Keyboard`, `Ime`, or `Blur`; `onTagRemoved` reports value and index.

Active IME composition is kept in the draft until it commits, so Enter and delimiters do not interrupt composing text. The default duplicate policy allows repeated values like Blueprint. `RejectExact` and `RejectIgnoreCase` are available. A `validator` checks each trimmed value. A batch with any rejected value adds nothing, preserves the full draft, calls `onRejected`, and exposes a localized error to TalkBack. Apps can also supply `isError` and `supportingText` for server-side validation. All added values are strings; Blueprint's arbitrary React children and `tagProps` map to Compose text and Brace tag intent/size. Compose controls layout as tags wrap, without DOM auto-resize or HTML input props.

An empty focused draft supports Left/Right tag selection; physical direction mirrors in RTL. Backspace selects the last tag, then removes the selected tag on the next press. Delete removes only a selected tag. Each tag has its own remove button with a 48 dp target and a localized spoken label. The draft exposes tag count and helper/error text through semantics. Disabled and read-only states suppress mutation. The visual label can be tapped to focus the input without becoming an extra keyboard stop. Keyboard focus, compact and comfortable density, large text, light/dark/high-contrast themes, and pointer access use Brace tokens.

The catalog entry shows editable, read-only, disabled, duplicate, validation, and method feedback states. Device tests cover delimiter entry, bulk insertion, IME, blur, keyboard tag removal, RTL arrows, touch target sizing, mouse removal, and API 34+ Compose accessibility checks. Manual TalkBack and hardware keyboard review across form factors remains before stable status.
