# M22: table cell editing

**Status:** draft PR [#31](https://github.com/joelromanpr/brace-android/pull/31), replayed on protected main `98f3d46` after M15 TagInput, M21 table copying, and M34 time-zone selection. GitHub automatically closed the old stacked PR when its deleted M21 base disappeared; it was reopened, retargeted to main, and updated to the focused source. EditableCell and Editing remain **in progress**, `firstRelease` is null, and released coverage is **0/122** applicable rows (0/94 components).

## Source in this slice

- `BraceEditableCell` provides a saveable single-line draft, token-driven focus and error states, synchronous validation, IME Done and hardware Enter commit, Escape cancel, and localized accessibility save/cancel actions.
- `BraceDataTable` accepts controlled edit state and commit/validation callbacks. Editable columns opt in. Enter/F2, double-tap or double-click, and a TalkBack cell action open the editor. Commit and cancel restore table focus; removal of the edited row or column closes the stale session. Table navigation and table-level copying yield keyboard ownership to the active editor.
- The interactive 120-row catalog sample exposes editable case/status columns, a controlled edit action, validation, and the confirmed value. The independent Maven consumer uses the public API. The pinned EditableCell and Editing rows are **in progress** with implementation, sample, guide, and tests linked. Ten of 23 main-track table rows are in progress and 13 planned.
- The replay retains measured row-header width, bounded table dimensions, row-major traversal, focused resize-handle identity, and M21 clipboard behavior. The versioned token source adds table editor visual states; generated Kotlin tokens come from the same source.

## Verification

- On the exact M15 protected-main replay, foundation/table API, table Android-test compile and lint, catalog assemble, token and pinned icon generation, inventory and link contrast passed **296/296 Gradle tasks**. JavaScript syntax, whitespace, generated coverage `--check`, and the Pages build also pass: **148 inventory rows, 23 real Android captures, 62 guides**, and **0/122** stable applicable rows.
- On the exact M15-main replay, the attached Android 16/API 36 emulator at physical **320 × 640, 160 dpi, font scale 1.0** passed **50/50 table tests**, zero failed or skipped, in 71 tasks. This includes the focused regression that prevents table copying from intercepting Ctrl+C while the editor owns focus. Emulator settings were unchanged after the run.
- On the protected M21 main before M15 joined, all eight aligned `0.1.0-SNAPSHOT` artifacts published to Maven Local (**308 tasks**), each with an AAR, sources JAR, KDoc JAR, POM, and Gradle Module Metadata. The separate clean, offline, coordinate-only consumer passed **38 tasks** (37 executed) using the public editing API. No Maven Central upload was attempted.
- The earlier stacked M22 source passed **41/41** API36 table tests, a **653-task** gate, six-artifact Maven Local publication, an independent consumer, and a 320 × 640 catalog review. Those results are historical only. The exact M15-main replay still needs hosted verify, API34, and CodeQL gates; manual TalkBack spoken-order review is open.

## Limits and next table slice

This slice edits one line of text in one cell at a time. Multiline and typed editors, editable column names, batched paste, row transactions, disjoint selection, reordering, freezing, and advanced formatting remain planned. M26 covers the separately inventoried EditableName behavior. No Maven Central publication or release tag is part of this branch.
