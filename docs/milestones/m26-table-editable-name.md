# M26: editable table column names

**Status:** local source branch rebased onto M22 cell editing head `714a470`, stacked on M21, M18, and M14. These APIs are **in progress**, `firstRelease` is null, and released applicable coverage remains **0/121**. The rebased head has not been pushed or run through Gradle and device gates.

## Source delivered in this branch

- `BraceEditableColumnName` provides a token-driven, saveable single-line header draft with select-all-on-focus, required-name and caller validation, IME Done/Enter commit, Escape cancel, and localized TalkBack actions.
- `BraceDataTable` has a separate controlled header edit session identified by stable column key. Opted-in headers support selected-column Enter/F2, double-tap/double-click, and an accessibility edit action. Offscreen headers are revealed, the active resize grip yields to the editor, unavailable cell Edit accessibility actions are hidden, switching to another header cannot discard an active draft, and commit/cancel return table focus.
- The inventory's pinned `EditableName` row is **in progress**. The catalog and independent Maven consumer include source examples. Released applicable coverage remains **0/121** until a verified Maven release.
- The rebase retained M14's cached row and column indexes, fixed header clipping and same-node accessibility semantics; M18 selection and resize; M21 clipboard copying; and M22 cell editing. Header reveal now uses the cached column boundaries. Its generated coverage conflict kept M21's Copying milestone, M22's EditableCell mapping, and M26's EditableName row. The two later accessibility and active-draft fixes were replayed.

## Verification

- On this rebased source tree, token and inventory generation `--check` pass (**147 pinned rows, 0/121 released**), as do the Pages build (**41 guides**), JavaScript syntax, Android string XML parsing, Git whitespace check, and conflict-marker check.
- The public `brace-table` API baseline predates M26 and does not yet contain `BraceEditableColumnName`, `BraceTableColumn.editableName`, or the new table parameters. Exact regeneration with `:brace-table:apiDump` and `apiCheck` is pending the shared Gradle lane; it has not been hand-edited.
- Current-head focused API 36 instrumentation, full table regressions, repository build/lint/token/API gates, Maven Local publication, independent consumer, and 320 × 640 catalog inspection remain pending the shared Gradle/emulator lane. The M14 parent [Actions run](https://github.com/joelromanpr/brace-android/actions/runs/36247229045) was blocked before jobs started because GitHub reported failed account payments or an Actions spending limit. This is an external gate, not a test result. API 34 needs a rerun after M14's density-corrected test.
- No publication is claimed.

## Remaining and next branch

This slice edits one plain-text column title at a time. Blueprint's per-keystroke `onChange`, custom header renderer, interaction bar, and menus; typed or multiline editing; row transactions; reordering and freezing remain planned. The next focused table branch should take a separate pinned inventory row. No Maven Central publication or release tag is part of this branch.
