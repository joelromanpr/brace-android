# M26: editable table column names

## Source delivered in this branch

- `BraceEditableColumnName` provides a token-driven, saveable single-line header draft with select-all-on-focus, required-name and caller validation, IME Done/Enter commit, Escape cancel, and localized TalkBack actions.
- `BraceDataTable` has a separate controlled header edit session identified by stable column key. Opted-in headers support selected-column Enter/F2, double-tap/double-click, and an accessibility edit action. Offscreen headers are revealed, the active resize grip yields to the editor, unavailable cell Edit accessibility actions are hidden, switching to another header cannot discard an active draft, and commit/cancel return table focus.
- The inventory's pinned `EditableName` row is **in progress**. The catalog and independent Maven consumer include the API. Released applicable coverage remains **0/121** until a verified Maven release.

## Verification

- Static checks passed: inventory generation/check (147 pinned rows, 0/121 released), documentation build (41 guides), JavaScript syntax, Android string XML parse, and `git diff --check`.
- The M26 public API baseline was stale: it did not contain `BraceEditableColumnName`, `BraceTableColumn.editableName`, or the new table parameters. Exact ABI regeneration with `:brace-table:apiDump` and `apiCheck` is pending the shared Gradle lane.
- Focused API 36 instrumentation, full table regressions, repository build/lint/token/API gates, Maven Local consumer, and 320 × 640 catalog inspection remain pending until the shared Gradle/emulator lane is available.
- Hosted PR checks and review are pending. No publication is claimed.

## Remaining and next branch

This slice edits one plain-text column title at a time. Blueprint's per-keystroke `onChange`, custom header renderer, interaction bar, and menus; typed or multiline editing; row transactions; reordering and freezing remain planned. The next focused table branch should take a separate pinned inventory row. No Maven Central publication or release tag is part of this branch.
