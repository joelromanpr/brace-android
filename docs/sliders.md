# Numeric sliders

`brace-core` provides controlled single and range sliders for numerical settings. These inventory rows are **in progress**. They use Material 3 for pointer movement and Brace tokens for color and size. The single slider keeps native progress semantics. The range slider places two separately named, focusable Android progress controls over its visual handles so keyboard and TalkBack users can adjust the start and end independently.

```kotlin
var volume by rememberSaveable { mutableFloatStateOf(3f) }
BraceSlider(
    value = volume,
    onValueChange = { volume = it },
    label = "Volume",
    min = 0f,
    max = 10f,
    stepSize = 1f,
    onRelease = { saveVolume(it) },
)

var start by rememberSaveable { mutableFloatStateOf(2f) }
var end by rememberSaveable { mutableFloatStateOf(7f) }
BraceRangeSlider(
    value = start..end,
    onValueChange = { start = it.start; end = it.endInclusive },
    label = "Hours",
)
```

A bounded positive `stepSize` must divide `max - min` into 1–1000 intervals. The component validates finite ordered bounds and values within them instead of silently drawing an invalid track. The caller owns value state and should save it across configuration and process recreation. `onValueChange` runs while moving a handle; `onRelease` receives the last proposed value at the end of a touch or mouse gesture. A label remains visible, and the current value and axis limits use token-driven text colors. The default display formatter follows the Android app locale and the step precision. `formatValue` lets callers add units or customize currencies and decimals. The token-sized thumb slots expose at least 48 dp native touch bounds in both dimensions on API 36, including compact density.

The inventory tracks Blueprint's `MultiSlider` and `MultiSliderHandle` separately. The single and range sliders do not implement those APIs; the [MultiSlider guide](multi-slider.md) documents their separate in-progress implementation. The pinned Blueprint Slider API also supports an arbitrary initial fill origin, vertical orientation, and custom axis label positions/renderers. These are not yet in this Brace slice; they must be addressed before the Slider/RangeSlider rows can be stable. Brace uses the Material 3 track with token-sized, rounded Brace thumbs and visible pressed and disabled states. Further geometry review remains before the first stable release.

Tests cover numeric progress actions, discrete keyboard movement, two-handle semantics, disabled state, dark high-contrast and compact density, 48 dp targets, and API 34+ automated Compose accessibility checks. Automated pointer tests cover touch and mouse input, and keyboard tests cover both left-to-right and right-to-left direction. API 36 compact-width visual review covers light and dark high-contrast modes. Manual TalkBack, large-text review, and full theme matrix review remain acceptance work. The catalog shows both controls with live enable/disable and theme switches, sourced from the coverage inventory.

Step ticks use explicit active and inactive Brace color roles so they remain visible on both track colors in every built-in theme. The foundation contrast test checks at least 3:1 for each pair.

The range slider exposes “Start of Hours” and “End of Hours” as separate progress controls with current values, bounds, and actions. Automated API 36 inspection found both controls at least 48 dp wide and high. Manual TalkBack review is still needed before calling this stable.
