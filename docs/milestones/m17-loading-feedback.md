# M17: Spinner and Skeleton loading feedback

**Status:** source slice rebased onto merged M11 main `e03bde9` in `joelromanpr/m17-loading-feedback`. The pinned Blueprint Spinner and Skeleton rows remain **in progress**, `firstRelease` is null, and generated stable coverage is **0/121** applicable rows. Post-M11 Gradle, device, Maven consumer, and hosted checks remain pending; no Maven Central release is claimed.

## Included in this branch

- `BraceSpinner` with controlled determinate or indeterminate progress, three token sizes, four intents, accessible range semantics, and reduced-motion drawing.
- `BraceSkeleton` with a scalable block size, optional loading semantics, token colors, shimmer or static mode, and no hidden interactive descendants.
- Versioned, platform-neutral spinner and skeleton color/metric tokens with generated Kotlin types.
- Inventory, interactive catalog, guide, public API baseline, meaningful device tests, and separate Maven consumer example.

## Verification

| Gate | Result |
| --- | --- |
| Post-rebase static checks | Passed on the rebased tree: token generation and coverage `--check`, JavaScript syntax checks, documentation build (147 inventory rows, 29 guides), and Git whitespace/conflict checks. The rebase retained the M11 icon catalog, guide, sample, and inventory alongside loading feedback. |
| Post-rebase build, lint, API, device tests, and Maven consumer | Pending. The shared Gradle/device lane is with M19 and M12; prior results below do not validate the M11 rebased tree. |
| Prior focused API 36 Compose tests | Passed 5/5 on the original `ba4f77e` branch before the rebase, with no skips. Covered progress semantics, reduced-motion pixel stability, redraw, sizes/intents, large text, RTL, high contrast, and automated Compose accessibility. |
| Prior full build/lint/test and independent Maven consumer | Passed on the original `ba4f77e` branch before the rebase: 287 Gradle tasks in the broad gate; foundation/core Maven Local AAR, sources, KDoc, POM, and module metadata published locally; separate consumer assembled from Maven coordinates only (37 tasks). |
| Prior 320dp visual and interaction check | Passed before the rebase on API 36 at 320 × 640: Spinner determinate toggle and dark high-contrast rendering; Skeleton shimmer, content swap, and light/dark high-contrast layout. No clipping or overlap was seen in these examples. |
| Hosted CI and review | Draft PR #29 previously passed verify and instrumented checks on the pre-M11 head. The rebased head needs a fresh hosted run and review. |

## Limits and next branch

The loading placeholders are noninteractive. An app controls when to show a loader and when to replace it with content. Manual TalkBack, pointer, large-text, theme-matrix, and additional form-factor review remain before these rows can become stable. The next integration step for this slice is post-M11 build, API, device, Maven consumer, and hosted verification. Other core, table, and select families remain tracked by their focused inventory rows.
