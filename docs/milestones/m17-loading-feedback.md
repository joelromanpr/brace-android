# M17: Spinner and Skeleton loading feedback

**Status:** source slice locally rebased onto merged main `47d2d38` (M12 Select/QueryList and M25 TopBar included) in `joelromanpr/m17-loading-feedback`. The pinned Blueprint Spinner and Skeleton rows remain **in progress**, `firstRelease` is null, and generated stable coverage is **0/121** applicable rows. This rebased head has not been pushed or run through Gradle, device, Maven consumer, or hosted checks. No Maven Central release is claimed.

## Included in this branch

- `BraceSpinner` with controlled determinate or indeterminate progress, three token sizes, four intents, accessible range semantics, and reduced-motion drawing.
- `BraceSkeleton` with a scalable block size, optional loading semantics, token colors, shimmer or static mode, and no hidden interactive descendants.
- Versioned, platform-neutral spinner and skeleton color/metric tokens with generated Kotlin types.
- Inventory, interactive catalog, guide, public API baseline, meaningful device tests, and separate Maven consumer example.

## Verification

| Gate | Result |
| --- | --- |
| Current post-M12/M25 static checks | Passed on the locally rebased tree: token generation and coverage `--check`, JavaScript syntax, documentation build (**147 inventory rows, 33 guides**), and Git whitespace/conflict checks. M12 Select/QueryList, M25 TopBar, and M17 Spinner/Skeleton remain in tokens, inventory, catalog, docs, and the independent consumer. |
| Current Gradle, API, device, Maven consumer, and hosted checks | **Pending** on this local rebase. The evidence below belongs to earlier commits on the M11 base and does not validate the integrated M12/M25 tree. The branch is intentionally unpushed while new GitHub Actions jobs are blocked by account billing. |
| Prior post-M11 static checks | Passed on the earlier M11 rebased tree: token generation and coverage `--check`, JavaScript syntax, documentation build (147 inventory rows, 29 guides), and Git whitespace/conflict checks. |
| Prior post-M11 build, lint, API, and catalog | `build lint checkTokenGeneration checkInventory apiCheck :catalog:assembleDebug` passed with **377 tasks**. Generated API baselines and icon/loading catalog samples compiled together. |
| Prior post-M11 API 36 device tests | `BraceLoadingTest` passed **5/5** with no failures or skips (**71 tasks**): determinate and indeterminate range semantics, reduced-motion pixels, sizing/intent tokens, large text, RTL, high contrast, and automated accessibility checks. |
| Prior post-M11 Maven Local and independent consumer | Foundation, core, and icons each published an AAR, sources JAR, Javadoc JAR, POM, and Gradle module metadata (**115 tasks**). The separate coordinate-only consumer assembled (**37 tasks**). |
| Prior 320dp visual and interaction check | Passed before the M11 rebase on API 36 at 320 × 640: Spinner determinate toggle and dark high-contrast rendering; Skeleton shimmer, content swap, and light/dark high-contrast layout. No clipping or overlap was seen in these examples. |
| Prior hosted CI and review | On pre-M12/M25 source head `61c01ec`, [verify](https://github.com/joelromanpr/brace-android/actions/runs/36244963138/job/108412353217) passed in 5m7s and [instrumented](https://github.com/joelromanpr/brace-android/actions/runs/36244963138/job/108412353069) passed in 9m33s. Code scanning reported a skipped job; review remains. |

## Limits and next branch

The loading placeholders are noninteractive. An app controls when to show a loader and when to replace it with content. Manual TalkBack, pointer, large-text, theme-matrix, and additional form-factor review remain before these rows can become stable. The next integration step is to run the broad Gradle/API gate, focused device tests, Maven consumer, and hosted checks on the post-M12/M25 head before review and manual accessibility assessment of PR #29. Other core, table, and select families remain tracked by their focused inventory rows.
