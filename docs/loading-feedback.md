# Circular progress and loading placeholders

This source slice maps the pinned Blueprint 6.18.0 [Spinner](https://blueprintjs.com/docs/#core/components/spinner) and [Skeleton](https://blueprintjs.com/docs/#core/components/skeleton) rows to native Compose. Both remain **in progress**. No Maven Central version has been released.

```kotlin
BraceTheme {
    Column(verticalArrangement = Arrangement.spacedBy(BraceTheme.spacing.sm)) {
        BraceSpinner(label = "Loading reports")
        BraceSpinner(label = "Export progress", value = 0.65f,
            size = BraceSpinnerSize.Large, intent = BraceProgressIntent.Success)
        BraceSkeleton(label = "Loading report title")
        BraceSkeleton(width = 180.dp, animated = false)
    }
}
```

`BraceSpinner` accepts null or non-finite progress as indeterminate and clamps finite values to `valueRange`. The accessible progress value reflects the caller's value, never an animation frame. Its label should name the operation. Small, medium, and large sizes, stroke width, track, and four intent colors come from the versioned spinner tokens. A positive `customSize` is available for unusual layouts. The indicator has no click or focus action.

`BraceSkeleton` is a display-only placeholder block; build several blocks that resemble the upcoming layout. It has no child slot, so an invisible button or field cannot remain in the focus order. Give one representative block a localized loading `label` when the region needs an announcement; leave repeated blocks decorative. Width can be a positive dp value or fill the available space. The default height follows both the skeleton metric and the current scaled body line height. When content arrives, the app should replace the placeholder with its real content and set an appropriate live region on that content if an announcement is needed.

The theme supplies light, dark, high-contrast, brand, and density variants. In reduced-motion mode, the spinner's indeterminate arc is stationary, determinate progress snaps, and the skeleton shimmer stops. `animated = false` also disables shimmer per block. Neither indicator requires a 48 dp touch target because neither is interactive. The [component list](coverage.md) links to their semantics, visual-state, and focus tests.

Blueprint's SVG-only spinner props translate to Compose Canvas drawing, `Modifier`, semantic tokens, and Android progress semantics. Blueprint's Skeleton CSS class is a web-specific mechanism: Brace exposes a standalone Compose placeholder block with no HTML wrapper or class name. Apps supply the block shape explicitly, so they can choose placeholders that match their actual screen without masking interactive descendants.
