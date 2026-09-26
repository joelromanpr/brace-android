# M17: Spinner and Skeleton loading feedback

**Status:** source slice rebased onto merged M11 main `e03bde9` in `joelromanpr/m17-loading-feedback`. The pinned Blueprint Spinner and Skeleton rows remain **in progress**, `firstRelease` is null, and generated stable coverage is **0/121** applicable rows. Post-M11 Gradle, device, Maven consumer, and hosted checks passed; no Maven Central release is claimed.

## Included in this branch

- `BraceSpinner` with controlled determinate or indeterminate progress, three token sizes, four intents, accessible range semantics, and reduced-motion drawing.
- `BraceSkeleton` with a scalable block size, optional loading semantics, token colors, shimmer or static mode, and no hidden interactive descendants.
- Versioned, platform-neutral spinner and skeleton color/metric tokens with generated Kotlin types.
- Inventory, interactive catalog, guide, public API baseline, meaningful device tests, and separate Maven consumer example.

## Verification

| Gate | Result |
| --- | --- |
| Post-rebase static checks | Passed on the rebased tree: token generation and coverage `--check`, JavaScript syntax checks, documentation build (147 inventory rows, 29 guides), and Git whitespace/conflict checks. The rebase retained the M11 icon catalog, guide, sample, and inventory alongside loading feedback. |
| Post-rebase build, lint, API, and catalog | `build lint checkTokenGeneration checkInventory apiCheck :catalog:assembleDebug` passed with **377 tasks**. Generated API baselines and icon/loading catalog samples compiled together. |
| Post-rebase API 36 device tests | `BraceLoadingTest` passed **5/5** with no failures or skips (**71 tasks**): determinate and indeterminate range semantics, reduced-motion pixels, sizing/intent tokens, large text, RTL, high contrast, and automated accessibility checks. |
| Post-rebase Maven Local and independent consumer | Foundation, core, and icons each published an AAR, sources JAR, Javadoc JAR, POM, and Gradle module metadata (**115 tasks**). The separate coordinate-only consumer assembled (**37 tasks**). |
| Prior 320dp visual and interaction check | Passed before the rebase on API 36 at 320 × 640: Spinner determinate toggle and dark high-contrast rendering; Skeleton shimmer, content swap, and light/dark high-contrast layout. No clipping or overlap was seen in these examples. |
| Hosted CI and review | On rebased source head `61c01ec`, [verify](https://github.com/joelromanpr/brace-android/actions/runs/36244963138/job/108412353217) passed in 5m7s and [instrumented](https://github.com/joelromanpr/brace-android/actions/runs/36244963138/job/108412353069) passed in 9m33s. Code scanning reported a skipped job; review remains. |

## Limits and next branch

The loading placeholders are noninteractive. An app controls when to show a loader and when to replace it with content. Manual TalkBack, pointer, large-text, theme-matrix, and additional form-factor review remain before these rows can become stable. The next integration step for this slice is review and manual accessibility assessment of PR #29. Other core, table, and select families remain tracked by their focused inventory rows.
