# M18: table selection and resizing

## Source delivered in this branch

- `BraceTableSelection` now represents a controlled cell, row, column, or inclusive rectangular range using stable keys. A touch long press followed by an endpoint tap, Shift plus keyboard navigation, Shift plus mouse click, and a TalkBack custom action can extend a range. The focused table announces cell, row, column, or range selection, and marks visible selected cells.
- `BraceDataTable` accepts controlled `columnWidths` and `rowHeights` maps with change callbacks and optional maximums. Visible column and row grips expose at least 48 dp touch and focus targets, pointer drag, keyboard adjustment, and localized TalkBack actions. Row-height minimums react to density and font scale; the handle and cell colors come from Brace tokens.
- The inventory now marks the pinned resizing and cell-selection rows **in progress**, with code, sample, documentation, and test links. The Region row remains planned because multiple disjoint regions and whole-table selection are not implemented. Of 23 pinned main-track table rows, 7 are in progress and 16 remain planned. No row is marked stable or released; generated released coverage remains **0/121**.
- The [catalog](../../catalog/src/main/java/io/github/joelromanpr/brace/catalog/CatalogActivity.kt) shows the 120-row table, live controlled resizing, selection state, and reset actions. The [usage guide](../table-selection-resize.md) and independent Maven consumer exercise the public API.

## Verification

- `:brace-table:compileDebugKotlin`, `:brace-table:compileDebugAndroidTestKotlin`, and `:catalog:compileDebugKotlin` passed after correcting test opt-in annotations. `:brace-table:apiDump` and `:brace-table:apiCheck` passed, and the new API baseline records `Column`, `Range`, and the controlled size parameters.
- The focused `BraceTableSelectionResizeTest` passed **13/13** on the API 36 emulator. The combined M14 and M18 `:brace-table:connectedDebugAndroidTest` suite passed **24/24, 0 failed, 0 skipped**. It covers touch, Shift plus mouse and keyboard range creation, column/row resizing by keyboard, pointer and accessibility action, RTL drag direction, bounded dimensions, resized-row Page Up/Down, selection announcements, deleted-anchor recovery, virtualization, theme and large-text changes, and automated accessibility checks.
- `build lint checkTokenGeneration checkInventory apiCheck` passed with 653 tasks on the integrated M18 branch. The generated inventory still reports **0/121 released applicable rows** and 0/94 released components. `node scripts/build-docs.mjs`, `node --check docs/site/app.js`, and `git diff --check` passed on this branch.
- Foundation, core, icons, select, datetime, and table published to Maven Local with the aligned `0.1.0-SNAPSHOT` coordinate. The table artifact contains its AAR, sources JAR, KDoc JAR, POM, and Gradle metadata. The separate `verification/consumer-smoke` app assembled using the locally published artifacts. This machine's temporary Gradle mirror init script needed `mavenLocal()` ahead of the mirror for the consumer; repository settings already include Maven Local.
- On the 320 × 640 emulator catalog, light and dark high-contrast layouts were inspected. Tapping a column and row header updated controlled selection, tapping a row resize grip increased height by the Brace spacing step, row headers stayed visibly fixed during horizontal scroll, and a long-press/tap selected a rectangular range. The catalog range summary was shortened after a visual check, and the rebuilt APK was inspected again. These are local checks; hosted CI remains to run after integration.

No Maven Central deployment or release tag was attempted. These source APIs remain **in progress**.

## Limits and next table branch

Only one contiguous range is supported. Disjoint regions, mouse drag selection, touch auto-scroll while extending a range, whole-table selection, copying, editing, frozen data regions, sorting, and row/column reordering remain planned. A later table branch should add copying and editing with controlled cell values, preserving keyboard and TalkBack behavior. Blueprint parity remains governed by the pinned inventory.
