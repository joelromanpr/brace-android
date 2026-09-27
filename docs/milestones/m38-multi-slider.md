# M38: MultiSlider

[PR #48](https://github.com/joelromanpr/brace-android/pull/48) adds a controlled `BraceMultiSlider` and stable-ID `BraceSliderHandle` descriptor to `brace-core`. The pinned comparison is Blueprint `@blueprintjs/core@6.18.0` at `a60d4c92257612808fbfac81cfeee4fcba91a8b4`. Both inventory rows remain **in progress**, with no first release. Generated stable coverage stays at **0/122 applicable rows** and **0/94 components**.

## Included

- Any number of handles, deterministic equal-value ordering, lock/push collision rules, and invisible marker stops for colored track segments.
- Touch, mouse, keyboard and RTL movement, distinct accessible handle names and actions, 48 dp targets, disabled behavior, focus restoration, and caller-owned state.
- Versioned token colors and sizes, an interactive catalog sample, copyable usage, a [guide](../multi-slider.md), tests, and an independent consumer example.

## Verification

On the M37 and table-sorting integration base, API dump/check, foundation unit tests, core AndroidTest Kotlin compile, and catalog assembly passed (**233 Gradle tasks**). The focused API 36 `BraceMultiSliderTest` passed **14/14**, with no failures or skips (**71 tasks**). Token and coverage checks, the Pages build with **148 rows, 25 real Android captures, and 82 guides**, JavaScript syntax, and diff checks also pass. The earlier M38 branch passed Maven Local publication and separate Maven-coordinate consumer compilation; those results predate this replay. Current-head consumer, hosted CI, and review are pending. Push this follow-up after M37 merges.

## Still needed

Manual TalkBack, physical keyboard, large-text, and tablet review remain. The control is horizontal; arbitrary axis label placement and custom renderers are not implemented. Equal steps support 1–1000 intervals. No Maven Central release or full pinned comparison coverage is claimed.
