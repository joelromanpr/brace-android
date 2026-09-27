# Semantic content and text wrappers

Blueprint's H1–H6, blockquote, code, preformatted text, and ordered/unordered list pages describe HTML elements. Brace provides Compose text equivalents in `brace-core` for common data and documentation views. These rows are **in progress**. They are separate from Blueprint's `Text` utility, which has its own inventory row.

```kotlin
BraceHeading("Quarterly operations", level = BraceHeadingLevel.One)
BraceBlockquote("A decision needs evidence.", citation = "Design review")
BraceCode("val ready = true")
BraceCodeBlock("val rows = listOf(1, 2, 3)\nrows.forEach(::println)")
BraceOrderedList(listOf("Open record", "Review fields", "Save changes"), start = 1)
BraceUnorderedList(listOf("Keyboard navigation", "TalkBack labels"))
```

`BraceHeading1` through `BraceHeading6` are convenience entry points. Heading levels select existing Brace typography tokens, and each text node receives Compose heading semantics. Android accessibility exposes a heading role for heading navigation, but has no HTML heading-level property, so TalkBack cannot announce H1 versus H6. Use levels in document order. Text scales with Android font settings and follows the surrounding RTL direction.

Quote, inline code, and code block colors use semantic surface/text tokens in all four light/dark/contrast combinations. Code is selectable. A code block preserves line breaks and scrolls horizontally for long lines. It does not shrink the font or automatically syntax highlight. Lists expose collection and item position semantics; the marker and text occupy one spoken node. Their `List<String>` API is for short plain-text lists. Use a lazy list with custom composables for rich or very large content.

These are static content elements; they add no touch target or keyboard focus stop. The Android catalog reads their names and in-progress status from the generated inventory. It offers a live sample under each row and a copyable Compose example. Automated device checks cover heading navigation semantics across theme/density changes, list order and collection semantics in RTL, quote/code reading, and API 34+ Compose accessibility checks. Manual TalkBack reading order, large text, and representative layouts remain acceptance work before the rows become stable.

Blueprint's HTML wrappers do not create Android DOM nodes. `ResizeSensor` maps to Compose's `Modifier.onSizeChanged` on the measured composable; it needs no separate Brace component. CSS utility classes map to explicit Compose modifiers and `BraceTheme` tokens. Those two inventory rows remain planned until their mapping examples and validation are complete.
