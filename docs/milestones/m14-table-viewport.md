# M14: first table viewport slice

**Status:** implementation in progress on draft PR #24, stacked on M13 after the M11 icon and M12 Select restack. No first release or stable coverage is claimed.

## Shipped in this branch

- New `brace-table` Android library and aligned `0.1.0-SNAPSHOT` Maven metadata with AAR, source JAR, and KDoc JAR. CI, API baseline, release staging list, and the separate Maven consumer include the artifact.
- `BraceDataTable`, typed `BraceTableColumn`, controlled `BraceTableSelection`, and saveable `BraceTableViewport`. Rows are lazy; columns outside the horizontal viewport are not composed except for one adjacent column. Fixed column and row headers, single-cell/whole-row selection, keyboard navigation, RTL, and localized TalkBack grid semantics are present.
- Token-driven light, dark, high-contrast, compact, and comfortable styling; cell and row selection targets are at least 48 dp high. The inventory-backed catalog has a 120-row interactive sample, and the [usage guide](../table-viewport.md) documents the public API and limits.
- Five pinned table rows are **in progress**. The remaining 18 table rows are planned. Nothing is marked stable or released.

## Verification on this branch

- The earlier M14 slice passed `build lint checkTokenGeneration checkInventory apiCheck` (653 Gradle tasks) and separate Maven consumption before the M11–M13 restack. This review patch reran `:brace-table:apiCheck` successfully before the restack; the full integrated gate and consumer still need rerun on the current stack. A local temporary Gradle repository init script was needed for this machine's dependency mirror; it is not part of the project.
- Focused `BraceDataTableTest` on an API 36 emulator: **14/14 passed, 0 skipped, 0 failed** on the final review diff before the M11–M13 restack; a current-stack rerun is pending. The suite covers duplicate and blank row keys, both-axis virtualization, fixed headers, partial vertical-scroll row-header alignment, RTL, touch and mouse selection, controlled state, keyboard reveal/navigation, viewport restoration, density and 48 dp targets, 3× text, native accessibility nodes, an automated accessibility check, and a bounded 5,000-row × 400-column composition/key-index regression. This is a composition and lookup check, not a frame-time or memory benchmark.
- The 320 × 640 catalog was inspected in light and dark high-contrast modes. Horizontal scrolling visibly retained the row numbers; tapping a row number selected the row. This found and fixed an earlier paint bug: row-header semantics had stayed pinned while cells covered the row-header pixels. A touch-after-scroll regression now guards the visible fixed overlay.
- Before the M11–M13 restack, `publishToMavenLocal` passed for foundation, core, icons, select, datetime, and table. The table snapshot has its AAR, sources, KDoc JAR, POM, and Gradle metadata; the POM names Apache-2.0, Joel Roman, and the foundation dependency. The independent `verification/consumer-smoke` app compiled against these locally published artifacts (`:app:assembleDebug`, 37 tasks).
- `node scripts/build-docs.mjs` (33 guides), `node --check docs/site/app.js`, `python3 scripts/generate_coverage.py --check`, and `python3 scripts/generate_tokens.py --check` passed on the current stack. Generated coverage remains **0/121 released applicable rows**; M14 source work is not a release.

Hosted pull-request `verify` and API 34 `instrumented` jobs must pass after this branch is integrated. No Maven Central deployment or signed release was attempted.

## Remaining and next table branch

The proposed next table branch is `joelromanpr/m18-table-selection-resize`: add range/cell/column selection and accessible column resize handles as a focused slice. Copying, editing, frozen data regions, reordering, loading and formatting helpers, and header actions require later branches. Sorting and persistence of controlled selection currently belong to the caller. Blueprint parity remains a long-term goal governed by the pinned inventory.
