# M26: editable table column names

**Status:** local source branch rebased onto M22 cell editing head `4dda5bf`, stacked on M21, M18, M14, and M13 DatePicker `ac121b3`. These APIs are **in progress**, `firstRelease` is null, and released applicable coverage remains **0/121**. The stacked source has passed local build, device, Maven Local, independent consumer, and 320dp catalog checks. The later report-only parent restack left production code, tokens, inventory, tests, and API byte-identical; hosted CI and review remain pending.

## Source delivered in this branch

- `BraceEditableColumnName` provides a token-driven, saveable single-line header draft with select-all-on-focus, required-name and caller validation, IME Done/Enter commit, Escape cancel, and localized TalkBack actions.
- `BraceDataTable` has a separate controlled header edit session identified by stable column key. Opted-in headers support selected-column Enter/F2, double-tap/double-click, and an accessibility edit action. Offscreen headers are revealed, the active resize grip yields to the editor, unavailable cell Edit accessibility actions are hidden, switching to another header cannot discard an active draft, and commit/cancel return table focus.
- The inventory's pinned `EditableName` row is **in progress**. The catalog and independent Maven consumer include source examples. Released applicable coverage remains **0/121** until a verified Maven release.
- The rebase retained M14's cached row and column indexes, fixed header clipping and same-node accessibility semantics; M18 selection and resize; M21 clipboard copying; and M22 cell editing. Header reveal now uses the cached column boundaries. Its generated coverage conflict kept M21's Copying milestone, M22's EditableCell mapping, and M26's EditableName row. The two later accessibility and active-draft fixes were replayed. The DatePicker-parent restack retained byte-identical table production source while adding the TopBar and updated DatePicker parent changes; the Pages sidebar and guide grid include the full table sequence.

## Verification

- On this rebased source tree, token and inventory generation `--check` pass (**147 pinned rows, 0/121 released**), as do the Pages build (**43 guides**), JavaScript syntax, Android string XML parsing, Git whitespace check, and conflict-marker check.
- `:brace-table:apiDump` generated the exact current public API after the source changes; `apiCheck` passed. The snapshot now contains `BraceEditableColumnName`, `BraceTableColumn.editableName`, and the controlled header-edit parameters. The compiler-generated Compose lambda signature was refreshed after the final semantics fix.
- The current-head repository `build lint checkTokenGeneration checkInventory apiCheck :catalog:assembleDebug` passed (**653 tasks**). API 36 `:brace-table:connectedDebugAndroidTest` passed **58/58**, zero skips or failures, covering viewport, fixed headers, selection, resizing, copying, cell editing, and editable names. The initial run exposed Compose test API chaining mistakes and two header-name assertions. The fixture now explicitly opts into cell editing, and the selected header context is a separate native node with a localized spoken state while the input retains native editable actions. The focused native test and final full suite pass.
- All six aligned artifacts published to Maven Local with AAR, sources, KDoc/Javadoc, POM, and Gradle metadata (**230 tasks**). The independent coordinate-only consumer assembled (**37 tasks**).
- The 320 × 640 API 36 catalog showed the table viewport without layout overlap; horizontal clipping follows the scroll viewport. Native accessibility inspection found row, column, cell, and resize-handle labels and actions. The current static checks pass with **147 pinned rows, 43 Pages guides, 0/121 released**, plus JavaScript, XML, and whitespace checks. Manual TalkBack and hosted API 34 remain.
- The M14 parent [Actions run](https://github.com/joelromanpr/brace-android/actions/runs/36247229045) was blocked before jobs started because GitHub reported failed account payments or an Actions spending limit. This is an external gate, not a test result. API 34 needs a rerun after M14's density-corrected test. All table stack PRs remain draft until required CI can run.
- No Maven Central publication or release is claimed.

## Remaining and next branch

This slice edits one plain-text column title at a time. Blueprint's per-keystroke `onChange`, custom header renderer, interaction bar, and menus; typed or multiline editing; row transactions; reordering and freezing remain planned. The next focused table branch should take a separate pinned inventory row. No Maven Central publication or release tag is part of this branch.
