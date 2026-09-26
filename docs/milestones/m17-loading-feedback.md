# M17: Spinner and Skeleton loading feedback

**Status:** local source slice rebased onto main `b8dc999` in `joelromanpr/m17-loading-feedback`. The pinned Blueprint Spinner and Skeleton rows are in progress; no stable or Maven Central release is claimed. The generated inventory owns coverage counts.

## Included in this branch

- `BraceSpinner` with controlled determinate or indeterminate progress, three token sizes, four intents, accessible range semantics, and reduced-motion drawing.
- `BraceSkeleton` with a scalable block size, optional loading semantics, token colors, shimmer or static mode, and no hidden interactive descendants.
- Versioned, platform-neutral spinner and skeleton color/metric tokens with generated Kotlin types.
- Inventory, interactive catalog, guide, public API baseline, meaningful device tests, and separate Maven consumer example.

## Verification

| Gate | Result |
| --- | --- |
| Post-rebase static checks | Passed on the rebased tree: token generation and coverage `--check`, JavaScript syntax checks, documentation build (147 inventory rows, 27 guides), and Git whitespace/conflict checks. |
| Post-rebase build, lint, API, device tests, and Maven consumer | Pending. The shared Gradle/device lane is occupied; prior results below do not validate the rebased tree. |
| Prior focused API 36 Compose tests | Passed 5/5 on the original `ba4f77e` branch before the rebase, with no skips. Covered progress semantics, reduced-motion pixel stability, redraw, sizes/intents, large text, RTL, high contrast, and automated Compose accessibility. |
| Prior full build/lint/test and independent Maven consumer | Passed on the original `ba4f77e` branch before the rebase: 287 Gradle tasks in the broad gate; foundation/core Maven Local AAR, sources, KDoc, POM, and module metadata published locally; separate consumer assembled from Maven coordinates only (37 tasks). |
| Prior 320dp visual and interaction check | Passed before the rebase on API 36 at 320 × 640: Spinner determinate toggle and dark high-contrast rendering; Skeleton shimmer, content swap, and light/dark high-contrast layout. No clipping or overlap was seen in these examples. |
| Hosted CI and review | Pending PR. |

## Limits and next branch

The loading placeholders are noninteractive. An app controls when to show a loader and when to replace it with content. Manual TalkBack, pointer, large-text, theme-matrix, and additional form-factor review remain before these rows can become stable. The next branch after this slice will address remaining pinned core families, including Collapse and Text, while the table and select work continue on their own focused branches.
