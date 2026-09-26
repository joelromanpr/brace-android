# Radio choices and segmented control

**Status:** In progress, unreleased. These APIs map the pinned Blueprint 6.18.0 [Radio and RadioGroup](https://blueprintjs.com/docs/#core/components/radio) and [SegmentedControl](https://blueprintjs.com/docs/#core/components/segmented-control) rows. The comparison source is commit `a60d4c92257612808fbfac81cfeee4fcba91a8b4`. The [coverage inventory](coverage.md) remains the authority for release status. No Maven Central artifact or full Blueprint parity is claimed.

The APIs live in `io.github.joelromanpr.brace:brace-core`. For local snapshot consumption, follow [installation](installation.md).

## Standalone radio

```kotlin
var meal by rememberSaveable { mutableStateOf("soup") }
BraceRadio(
    selected = meal == "soup",
    onSelect = { meal = "soup" },
    label = "Soup",
    description = "Vegetarian",
)
```

Use `BraceRadioGroup` when the options form one choice. A standalone radio offers a full-row 48 dp target, selected and disabled states, keyboard Enter/Space activation, a focus ring, and a radio role for TalkBack. The optional indicator position uses logical start/end, so it follows RTL. The visible description is included in the spoken label. The caller owns the selected state.

## RadioGroup

```kotlin
val meals = listOf(
    BraceRadioOption("soup", "Soup", description = "Vegetarian"),
    BraceRadioOption("salad", "Salad", enabled = false),
    BraceRadioOption("sandwich", "Sandwich"),
)
var meal by rememberSaveable { mutableStateOf("soup") }
BraceRadioGroup(
    options = meals,
    selectedValue = meal,
    onValueChange = { meal = it },
    label = "Lunch special",
)
```

The label names the selection group. Options must have unique values. Touch and mouse selection update through `onValueChange`. The selected option is the group's Tab stop (or the first enabled option when no value is selected). Arrow keys select, focus, skip disabled options, and wrap; inline layout scrolls horizontally on narrow screens. Left/right direction follows RTL. Options retain 48 dp targets at compact density and large font sizes. Persist the hoisted value with `rememberSaveable` or a view model.

## SegmentedControl

```kotlin
val layouts = listOf(
    BraceSegmentedOption("list", "List"),
    BraceSegmentedOption("grid", "Grid", enabled = false),
    BraceSegmentedOption("gallery", "Gallery"),
)
var layout by rememberSaveable { mutableStateOf("list") }
BraceSegmentedControl(
    options = layouts,
    value = layout,
    onValueChange = { layout = it },
    label = "Layout",
    fill = true,
    intent = BraceSegmentedIntent.Primary,
    size = BraceSegmentedSize.Medium,
)
```

The selection strip has `Neutral` and `Primary` visual intents, `Small`, `Medium`, and `Large` sizes, optional decorative icons, a full-width `fill` mode, and horizontal scrolling in intrinsic-width mode. Every option keeps a 48 dp minimum hit region. The chosen option is announced as selected with a radio role. The selected segment is the group's Tab stop (or the first enabled segment when unselected). Arrow keys skip disabled options, wrap, and mirror horizontally in RTL. Enter and Space activate a focused option. The neutral selected segment has a strong token border so its state remains visible even where adjacent surfaces are similar. All colors, focus rings, sizes, and gaps come from Brace semantic and component tokens; global light, dark, high-contrast, brand, and density changes flow through `BraceTheme`.

Blueprint also exposes DOM refs, CSS `inline`, uncontrolled `defaultValue`, and alternate ARIA group, toolbar, and menu roles. Android callers use a layout `Modifier`, hoisted state, and a radio-style accessibility group. Menu and toolbar interactions belong in their native Brace components. Blueprint's deprecated `small`/`large` flags are represented by `size`.

## Verification

`BraceRadioSegmentedTest` covers controlled selection, disabled states, spoken labels and roles, 48 dp targets, arrow navigation with skip/wrap and RTL, Enter/Space and mouse activation, state restoration, large text, dark high-contrast compact mode, and an automated accessibility audit on API 34+. The catalog exposes interactive samples for all three rows and switches themes at runtime. Manual TalkBack, hardware keyboard, and mouse checks remain part of release acceptance; the inventory stays **in progress** until a published and verified release.
