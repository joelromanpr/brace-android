# M22: table cell editing

**Status:** pre-report source head `f5882da` was pushed to draft PR #31, stacked on M21 copying PR #26, M18, M14, and M13 DatePicker; this report-only update changes no production code or tests. These APIs are **in progress**, `firstRelease` is null, and released applicable coverage remains **0/121**. This branch checkout still needs its own post-restack Gradle/device and hosted gates.

## Source delivered in this branch

- `BraceEditableCell` provides a saveable single-line draft, token-driven focus and error states, synchronous validation, IME Done and hardware Enter commit, Escape cancel, and localized accessibility save/cancel actions.
- `BraceDataTable` accepts controlled edit state and commit/validation callbacks. Editable columns opt in. Enter/F2, double-tap or double-click, and a TalkBack cell action open the editor. Commit and cancel restore table focus; removal of the edited row or column closes the stale session.
- The catalog and separate Maven consumer contain interactive controlled examples. The pinned EditableCell and Editing rows are **in progress**; the other 13 table rows remain planned. No row is marked released.
- The rebase retained M14's cached row and column indexes, binary-search viewport and clipped fixed row headers, M18's selection, resizing and native cell semantics, and M21's selected-cell clipboard action. The editing view now uses the cached indexes, and an edited row's larger minimum height is reflected in page navigation and its resize handle. The earlier generated coverage conflict kept M21's copying milestone assignment and M22's editing rows. This DatePicker-parent rebase regenerated Kotlin tokens from the shared token source and retained TopBar and table Pages links.

## Verification

- On this rebased source tree, token generation and coverage `--check`, JavaScript syntax, Pages build (**147 inventory rows, 41 guides**), and Git whitespace checks pass. Generated coverage reports **0/121** stable applicable rows and 0/94 stable components.
- Before this rebase, the full table instrumentation suite passed **41/41** on API 36, including **12/12** editing tests for keyboard, IME, pointer, TalkBack actions, native accessibility nodes, validation, state restoration, large text, RTL, high contrast, focus return, and stale-row cleanup. The pre-rebase repository `build lint checkTokenGeneration checkInventory apiCheck --offline` gate passed **653 tasks**. Six aligned snapshot artifacts published to Maven Local, and the separate coordinate-only consumer assembled. These results do not validate the new rebased head.
- Before this rebase, a 320 × 640, 160 dpi catalog review found the table and editor navigable, with the editor visible above the keyboard. A native accessibility inspection found a row/column context node followed by the editable text node with Save and Cancel actions. Post-rebase visual and native-node review remain.
- This branch checkout still needs its own Gradle build/lint/API, full table instrumentation, Maven Local publication, and independent consumer. The pushed source-head [run 36249697635](https://github.com/joelromanpr/brace-android/actions/runs/36249697635) on `f5882da` marked `verify` and API 34 `instrumented` failed with **zero steps run**; both job annotations cite failed recent payments or an Actions spending limit. This is an external account gate, not a test result. Required checks on this report-only head and API 34 remain pending.

- **Descendant integration evidence:** M26 head `e615553` contains this table source and passed a local broad build/lint/token/inventory/API/catalog gate (**653 tasks**), API 36 full table instrumentation (**58/58**, zero failures or skips), six-artifact Maven Local publication (**230 tasks**), and a separate coordinate-only consumer assemble (**37 tasks**). This validates the combined descendant checkout; it does not replace this branch checkout's own current-head gates or hosted API 34 verification.

## Remaining and next branch

Single-line cell text is the only editor in this slice. Multiline and typed editors, editable column names, batched paste, row transactions, disjoint selection, reordering, freezing, and advanced formatting remain planned. The next stacked table branch, M26, covers `EditableName`, tracked separately from `EditableCell` in the pinned inventory. No Maven Central publication or release tag is part of this branch.
