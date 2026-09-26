# M17: Spinner and Skeleton loading feedback

**Status:** source slice [merged as PR #29](https://github.com/joelromanpr/brace-android/pull/29) into `main` at `8fffe5d` after integration with M13 date picker/date field and the visual showcase. The pinned Blueprint Spinner and Skeleton rows remain **in progress**, `firstRelease` is null, and generated stable coverage is **0/121** applicable rows. No Maven Central release is claimed.

## Included in this branch

- `BraceSpinner` with controlled determinate or indeterminate progress, three token sizes, four intents, accessible range semantics, and reduced-motion drawing.
- `BraceSkeleton` with a scalable block size, optional loading semantics, token colors, shimmer or static mode, and no hidden interactive descendants.
- Versioned, platform-neutral spinner and skeleton color/metric tokens with generated Kotlin types.
- Inventory, interactive catalog, guide, public API baseline, meaningful device tests, and separate Maven consumer example.

## Verification

| Gate | Result |
| --- | --- |
| Current post-M13 static checks | Passed on the current merged tree: token generation and coverage `--check`, JavaScript syntax, documentation build (**147 inventory rows, 11 real catalog captures, 38 guides**), and Git whitespace/conflict checks. M12 Select/QueryList, M13 DatePicker/DateField, M19 Radio/SegmentedControl, M25 TopBar, and M17 Spinner/Skeleton remain in tokens, inventory, catalog, docs, and the independent consumer. |
| Current build, lint, API, and catalog | `build lint checkTokenGeneration checkInventory apiCheck :catalog:assembleDebug` passed on the post-M13 merged tree: **563 tasks**, no failures. |
| Current API 36 device tests | `BraceLoadingTest` passed **5/5** after the M13 integration with no failures or skips (71 tasks, 24s). This covers progress semantics, reduced-motion pixels, sizing/intents, large text, RTL, high contrast, and automated accessibility checks. |
| Current Maven consumer | Five aligned snapshot artifacts (foundation, core, icons, select, datetime) published into a fresh isolated Maven Local directory with AAR, sources, Javadoc, POM, and Gradle metadata (**193 tasks**). The independent coordinate-only consumer assembled from that directory (**37 tasks**). |
| Final hosted checks | On post-M13 PR head `35f5dc8`, [verify](https://github.com/joelromanpr/brace-android/actions/runs/36278511900/job/108505765018) passed in 2m59s, [instrumented API 34](https://github.com/joelromanpr/brace-android/actions/runs/36278511900/job/108505764906) passed in 6m38s, and [CodeQL analysis](https://github.com/joelromanpr/brace-android/actions/runs/36278511904/job/108505764461) passed in 3m14s. That head was squash merged to `main` as `8fffe5d`. |
| Current visual review | On API 36 at 400 × 800, Spinner showed determinate progress in dark high contrast and Skeleton showed a loading placeholder stack in light mode. Both fresh emulator captures are linked to the exact source commit in the Pages manifest. Chrome verified all 11 cards, the new source links, the high-contrast filter, and no horizontal overflow at 375 px. After merge, [Pages run 36278891151](https://github.com/joelromanpr/brace-android/actions/runs/36278891151) deployed the gallery; the live site showed all 11 captures and the two new PNGs returned HTTP 200 on 2026-09-26. |
| Prior post-M11 static checks | Passed on the earlier M11 rebased tree: token generation and coverage `--check`, JavaScript syntax, documentation build (147 inventory rows, 29 guides), and Git whitespace/conflict checks. |
| Prior post-M11 build, lint, API, and catalog | `build lint checkTokenGeneration checkInventory apiCheck :catalog:assembleDebug` passed with **377 tasks**. Generated API baselines and icon/loading catalog samples compiled together. |
| Prior post-M11 API 36 device tests | `BraceLoadingTest` passed **5/5** with no failures or skips (**71 tasks**): determinate and indeterminate range semantics, reduced-motion pixels, sizing/intent tokens, large text, RTL, high contrast, and automated accessibility checks. |
| Prior post-M11 Maven Local and independent consumer | Foundation, core, and icons each published an AAR, sources JAR, Javadoc JAR, POM, and Gradle module metadata (**115 tasks**). The separate coordinate-only consumer assembled (**37 tasks**). |
| Prior 320dp visual and interaction check | Passed before the M11 rebase on API 36 at 320 × 640: Spinner determinate toggle and dark high-contrast rendering; Skeleton shimmer, content swap, and light/dark high-contrast layout. No clipping or overlap was seen in these examples. |
| Prior hosted CI and review | On pre-M12/M25 source head `61c01ec`, [verify](https://github.com/joelromanpr/brace-android/actions/runs/36244963138/job/108412353217) passed in 5m7s and [instrumented](https://github.com/joelromanpr/brace-android/actions/runs/36244963138/job/108412353069) passed in 9m33s. Code scanning reported a skipped job; review remains. |

## Limits and next branch

The loading placeholders are noninteractive. An app controls when to show a loader and when to replace it with content. Manual TalkBack, pointer, large-text, theme-matrix, and additional form-factor review remain before these rows can become stable. The remaining acceptance step is manual accessibility assessment before stable status. Other core, table, and select families remain tracked by their focused inventory rows.
