# M22: table cell editing

**Status:** local source branch rebased onto M21 copying head `254d67c`, which is stacked on M18 and M14. PR #31 remains draft and stacked. These APIs are **in progress**, `firstRelease` is null, and released applicable coverage remains **0/121**. The rebased head has not been pushed or run through Gradle and device gates.

## Source delivered in this branch

- `BraceEditableCell` provides a saveable single-line draft, token-driven focus and error states, synchronous validation, IME Done and hardware Enter commit, Escape cancel, and localized accessibility save/cancel actions.
- `BraceDataTable` accepts controlled edit state and commit/validation callbacks. Editable columns opt in. Enter/F2, double-tap or double-click, and a TalkBack cell action open the editor. Commit and cancel restore table focus; removal of the edited row or column closes the stale session.
- The catalog and separate Maven consumer contain interactive controlled examples. The pinned EditableCell and Editing rows are **in progress**; the other 13 table rows remain planned. No row is marked released.
- The rebase retained M14's cached row and column indexes, binary-search viewport and clipped fixed row headers, M18's selection, resizing and native cell semantics, and M21's selected-cell clipboard action. The editing view now uses the cached indexes, and an edited row's larger minimum height is reflected in page navigation and its resize handle. The generated coverage conflict kept M21's copying milestone assignment and M22's editing rows.

## Verification

- On this rebased source tree, token generation and coverage `--check`, JavaScript syntax, Pages build (**147 inventory rows, 39 guides**), and Git whitespace checks pass. Generated coverage reports **0/121** stable applicable rows and 0/94 stable components.
- Before this rebase, the full table instrumentation suite passed **41/41** on API 36, including **12/12** editing tests for keyboard, IME, pointer, TalkBack actions, native accessibility nodes, validation, state restoration, large text, RTL, high contrast, focus return, and stale-row cleanup. The pre-rebase repository `build lint checkTokenGeneration checkInventory apiCheck --offline` gate passed **653 tasks**. Six aligned snapshot artifacts published to Maven Local, and the separate coordinate-only consumer assembled. These results do not validate the new rebased head.
- Before this rebase, a 320 × 640, 160 dpi catalog review found the table and editor navigable, with the editor visible above the keyboard. A native accessibility inspection found a row/column context node followed by the editable text node with Save and Cancel actions. Post-rebase visual and native-node review remain.
- Current-head Gradle build, lint, API check, full table instrumentation, Maven Local publication, independent consumer, and hosted checks are pending. The M14 parent [Actions run](https://github.com/joelromanpr/brace-android/actions/runs/36247229045) was blocked before executing jobs because GitHub reported failed account payments or an Actions spending limit. This is an external gate, not a test result. API 34 also needs a rerun after M14's density-corrected test.

## Remaining and next branch

Single-line cell text is the only editor in this slice. Multiline and typed editors, editable column names, batched paste, row transactions, disjoint selection, reordering, freezing, and advanced formatting remain planned. The next focused table branch should cover `EditableName`, tracked separately from `EditableCell` in the pinned inventory. No Maven Central publication or release tag is part of this branch.
