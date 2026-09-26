# M21: table copying

**Status:** draft PR [#26](https://github.com/joelromanpr/brace-android/pull/26), replayed onto protected main `0fbe2c2` after M18 merged. This source is **in progress**, `firstRelease` is null, and generated released coverage is **0/122** applicable rows (0/94 components). It is not a Maven Central release or a Blueprint parity claim.

## Source in this slice

- `BraceTableClipboard.formatSelection` exports a controlled cell, row, column, or rectangular range as quoted tab-separated text in logical table order. Stale selections return `null`. It validates standalone row keys; the table reuses its cached row and column indexes.
- `BraceDataTable` handles Ctrl/Cmd+C from its keyboard focus stop and exposes a localized TalkBack copy action on the same named table node. The Android clipboard receives plain text. The catalog's 120-row sample offers a touch copy button and preview; the independent Maven consumer compiles the public formatter API.
- The pinned Copying capability row is **in progress** with implementation, sample, guide, and tests linked in the inventory. Eight of 23 main-track table rows are in progress and 15 remain planned. The [guide](../table-copying.md) documents output format, stale selections, and the cost of very large selections.
- The replay retains M14 viewport virtualization and measured headers, M18 controlled selection and resize handles, row-major traversal, finite-size checks, and focused resize-handle identity.

## Verification

- On this M18-main replay, token and pinned icon generation, coverage `--check`, JavaScript syntax, whitespace checks, and the Pages build pass: **148 inventory rows, 14 real Android captures, 52 guides**, and **0/122** stable applicable rows.
- The earlier M21 stacked source passed **29/29** table instrumentation tests on API 36, a **653-task** build/lint/token/inventory/API gate, six-artifact Maven Local publication, and a **37-task** independent consumer. Its five copying tests covered quoted output and order, controlled updates, offscreen range copying, stale clipboard preservation, and the accessibility action. These are historical results from before the replay; integrated Gradle, device, consumer, and hosted PR gates remain pending on this head.
- The earlier 320 × 640 catalog review showed selection and the copy preview without horizontal overflow. The replay has not yet had a new device appearance review.

## Limits and next table slice

This copies one contiguous selection as plain text. Disjoint regions, HTML clipboard formats, editing, sorting, reordering, freezing, and advanced formatting remain separate slices. Formatting a very large whole-column selection is synchronous and may allocate a large string; callers should use an app-specific background export for such data. Manual TalkBack spoken-order QA remains open. M22 is the next controlled cell-editing slice.
