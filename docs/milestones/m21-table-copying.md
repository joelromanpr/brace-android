# M21: table copying

**Status:** pre-report source head `107f36a` was pushed to draft PR #26, stacked on M18 selection/resizing PR #25, M14 viewport PR #24, and M13 DatePicker; this report-only update changes no production code or tests. These APIs are **in progress**, `firstRelease` is null, and generated released coverage remains **0/121** applicable rows. This branch checkout still needs its own post-restack Gradle/device and hosted gates.

## Source delivered in this branch

- `BraceTableClipboard.formatSelection` exports a controlled cell, row, column, or rectangular range as quoted tab-separated text in logical table order. Stale selections return `null`.
- The `BraceDataTable` keyboard focus stop handles Ctrl/Cmd+C, and its TalkBack node provides a localized copy action. The Android system clipboard receives plain text. The catalog has a copy action and preview, and the separate Maven consumer calls the formatter API.
- The pinned Copying capability row is **in progress**. Eight of 23 main-track table rows are now in progress; 15 remain planned. Released applicable coverage remains **0/121** until a verified Maven release. The guide records large-selection and format limits.
- Rebase onto M18 retained M14's cached row and column indexes, binary-search viewport, clipped fixed row headers, and M18's controlled selection, resize grips, and native accessibility semantics. The earlier M18 restack resolved one conflict in the table's root selection semantics to retain both the indexed collection size and the copy action. The formatter uses M14's `TableRowIndex.byKey` and cached column indexes for in-table copying. This DatePicker-parent rebase retained the same source behavior and merged the TopBar and table Pages links.

## Verification

- On this rebased source tree, token generation and coverage `--check`, JavaScript syntax, Pages build (**147 inventory rows, 39 guides**), and Git whitespace checks pass. Generated coverage reports **0/121** stable applicable rows and 0/94 stable components.
- Before this rebase, the integrated table instrumentation suite passed **29/29** on an API 36 emulator, with zero failures or skips. Its five copying tests covered TSV quoting and order, controlled updates, offscreen range copying with Meta+C, stale clipboard preservation and hidden stale accessibility action, and the TalkBack copy action. The pre-rebase broad `build lint checkTokenGeneration checkInventory apiCheck` passed **653 tasks**. All six source artifacts published to Maven Local, and the separate coordinate-only consumer assembled (**37 tasks**). These results do not validate the new rebased head.
- Before this rebase, the 320 × 640 catalog's selection, copy button, and copied-value preview were inspected. Copying the first visible cell displayed `Copied: Case 1000` without horizontal overflow. Post-rebase visual review remains.
- This branch checkout still needs its own Gradle build/lint/API, full table instrumentation, Maven Local publication, and independent consumer. The pushed source-head [run 36249696386](https://github.com/joelromanpr/brace-android/actions/runs/36249696386) on `107f36a` marked `verify` and API 34 `instrumented` failed with **zero steps run**; both job annotations cite failed recent payments or an Actions spending limit. This is an external account gate, not a test result. Required checks on this report-only head and API 34 remain pending.

- **Descendant integration evidence:** M26 head `e615553` contains this table source and passed a local broad build/lint/token/inventory/API/catalog gate (**653 tasks**), API 36 full table instrumentation (**58/58**, zero failures or skips), six-artifact Maven Local publication (**230 tasks**), and a separate coordinate-only consumer assemble (**37 tasks**). This validates the combined descendant checkout; it does not replace this branch checkout's own current-head gates or hosted API 34 verification.

## Remaining and next branch

The next stacked table slice, M22, adds controlled cell editing and the pinned EditableCell behavior. Disjoint selection, sorting, reordering, freezing, and advanced formatting remain planned. No Maven Central publication or release tag is part of this branch.
