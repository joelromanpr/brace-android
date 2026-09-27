# M37: Numeric sliders

[PR #47](https://github.com/joelromanpr/brace-android/pull/47) adds controlled `BraceSlider` and `BraceRangeSlider` to `brace-core`. Both inventory rows remain **in progress**, with no released version. Generated stable coverage stays at **0/122 applicable rows** and **0/94 components**.

## Included

- Numeric bounds and equal steps, locale-aware value labels, release callbacks, disabled states, and token-driven colors and sizes.
- Touch and mouse adjustment, keyboard arrows with RTL direction, and separate start/end progress actions for range sliders.
- Two focusable, separately named range controls with spoken values and 48 dp targets; the visual range track uses Material 3.
- Interactive catalog examples, copyable usage, a [guide](../sliders.md), versioned tokens, an independent consumer example, and seven device tests.
- Android adaptation of the [pinned Blueprint 6.18.0 sliders](https://github.com/palantir/blueprint/blob/a60d4c92257612808fbfac81cfeee4fcba91a8b4/packages/core/src/components/slider/sliders.mdx).

## Verification

On the table-sorting integration base, API dump/check, foundation unit tests, core AndroidTest Kotlin compile, catalog assembly, token and coverage checks, the 80-guide Pages build, JavaScript syntax, and `git diff --check` passed. The focused API 36 `BraceSliderTest` passed **7/7**, with no failures or skips. The preceding PR head passed all hosted checks, including API 34 instrumentation and Temurin 21 lint; the final restack needs its own hosted run. A local lint run crashed the host Corretto 17.0.7 JVM while compiling UAST code. The earlier slice passed Maven Local publication for eight aligned snapshot artifacts and a coordinate-only consumer compile; those are pre-restack results and do not represent Maven Central publication.

## Still needed

Manual TalkBack, large-text, and tablet review remain. Blueprint's vertical orientation, arbitrary fill origin, and custom axis label placement/renderers are not implemented. Equal steps currently support 1–1000 intervals. Keep the rows in progress until these gaps and release acceptance are resolved. M38 MultiSlider remains a separate draft slice.
