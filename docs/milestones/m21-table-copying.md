# M21: table copying

## Source delivered in this branch

- `BraceTableClipboard.formatSelection` exports a controlled cell, row, column, or rectangular range as quoted tab-separated text in logical table order. Stale selections return `null`.
- The `BraceDataTable` keyboard focus stop handles Ctrl/Cmd+C, and its TalkBack node provides a localized copy action. The Android system clipboard receives plain text. The catalog has a copy action and preview, and the separate Maven consumer compiles the formatter API.
- The pinned Copying capability row is **in progress**. Eight of 23 main-track table rows are now in progress; 15 remain planned. Released applicable coverage remains **0/121** until a verified Maven release. The guide records large-selection and format limits.

## Verification

- The integrated M14, M18, and M21 table instrumentation suite passed **29/29** on an API 36 emulator, with zero failures or skips. The five copying tests cover TSV quoting and order, controlled updates, offscreen range copying with Meta+C, stale clipboard preservation and hidden stale accessibility action, and the TalkBack copy action.
- `build lint checkTokenGeneration checkInventory apiCheck` passed **653 tasks** after the final source and test fixes. Generated inventory remains 0/121 applicable rows stable. The documentation site built 147 rows and 37 guides; JavaScript syntax, token, coverage, and diff checks passed.
- All six source artifacts published to Maven Local with AAR, sources, KDoc, POM, and Gradle metadata. The separate `verification/consumer-smoke` project assembled against those coordinates (37 tasks), including the new formatter API.
- At 320 × 640, the catalog's table row and cell selection, copy button, and copied-value preview were inspected. Copying the first visible cell displayed `Copied: Case 1000` without horizontal overflow. Hosted PR checks remain pending. This is not a release claim.

## Remaining and next branch

The next focused table slice should add controlled cell editing and the pinned EditableCell behavior. Disjoint selection, sorting, reordering, freezing, and advanced formatting remain planned. No Maven Central publication or release tag is part of this branch.
