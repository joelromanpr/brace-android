# MultiSlider and handle descriptors

`BraceMultiSlider` is a controlled horizontal slider for three or more values, or for a track with semantic color sections. The pinned Blueprint comparison is [`MultiSlider` and `MultiSliderHandle` in core 6.18.0](https://blueprintjs.com/docs/#core/components/sliders) at [commit `a60d4c9`](https://github.com/palantir/blueprint/blob/a60d4c92257612808fbfac81cfeee4fcba91a8b4/packages/core/src/components/slider/sliders.mdx). Both inventory rows are **in progress** and have no first release.

```kotlin
var values by rememberSaveable { mutableStateOf(listOf(2f, 5f, 8f)) }
val handles = listOf(
    BraceSliderHandle("minimum", values[0], "Minimum", intentAfter = BraceSliderTrackIntent.Primary),
    BraceSliderHandle("target", values[1], "Target", BraceSliderHandleInteraction.Push,
        intentAfter = BraceSliderTrackIntent.Success),
    BraceSliderHandle("maximum", values[2], "Maximum", type = BraceSliderHandleType.End),
)
BraceMultiSlider(
    handles = handles,
    onChange = { proposed -> values = proposed.map { it.value } },
    label = "Monthly budget",
    min = 0f,
    max = 10f,
    stepSize = 1f,
    onRelease = { proposed -> persistBudget(proposed.associate { it.id to it.value }) },
)
```

`BraceSliderHandle` is a descriptor, rather than a Composable child or DOM element. Its stable `id` identifies a handle across movements and preserves Compose focus. The `onChange` and `onRelease` lists retain the **input order**, even when handles sort differently along the track; use IDs for business meaning. Handles at equal values are drawn in input order. A track tap chooses the first equally near interactive handle in input order. This makes ties deterministic. `None` handles have no thumb, focus, TalkBack node, or collision behavior; they split the track for coloring. `intentAfter` on a stop takes precedence over the next stop's `intentBefore`, then the slider's `defaultTrackIntent` applies. `showTrackFill = false` draws only the inactive track.

`Lock` handles block other handles from crossing them. `Push` handles move to the same proposed value when crossed, until a `Lock` clamps the moving group. Handles may meet. `onChange` receives the whole proposed list, including values moved by push; the caller decides how to save it. A labeled `BraceSliderHandle` exposes its reachable range and a 48 dp target to TalkBack. Its accessible name combines the group label and handle label, so use a short, distinct handle label such as “Minimum” or “Maximum”. Arrow keys change it by one step; horizontal direction mirrors in RTL. Touch and mouse can drag a handle horizontally or select the nearest handle with a completed track tap; vertical swipes pass to scroll containers. Disabled handles retain their labels and values without an adjustment action. `formatValue` defaults to the app locale and step precision; supply a formatter for units or currencies.

The component validates finite ordered bounds and finite values inside them. `stepSize` must divide the range into 1–1000 equal intervals. The caller owns and saves state; the example saves a list of positions with `rememberSaveable`. Brace colors, typography, density and focus states come from versioned semantic/component tokens. The default neutral track has a dedicated color role so primary, success, warning and danger segments maintain at least 3:1 contrast against it in all four default schemes. A token-driven outline keeps the neutral track at least 3:1 against the surface. High contrast, 2× text, compact density and reduced motion are included in interaction tests; the MultiSlider does not animate value changes.

## Blueprint adaptation and remaining parity

Blueprint's `MultiSliderHandle` is a React child marker with per-handle `onChange`/`onRelease`, HTML aria props, CSS track styles, tooltip labels, vertical orientation, and custom axis label placement/renderers. Brace uses a typed descriptor list and parent list callbacks, meaningful native labels, semantic track intents, and a horizontal Android control. Per-handle callbacks can be recovered by comparing lists by ID; arbitrary CSS/HTML props have no Compose equivalent. Vertical layout, arbitrary custom axis labels and visual tooling remain before stable status. Manual TalkBack/physical keyboard/tablet review and hosted API34 checks are also pending. For one or two values, see the separate [numeric slider guide](sliders.md).
