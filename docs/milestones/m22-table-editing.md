# M22: table cell editing

## Source delivered in this branch

- `BraceEditableCell` provides a saveable single-line draft, token-driven focus and error states, synchronous validation, IME Done and hardware Enter commit, Escape cancel, and localized accessibility save/cancel actions.
- `BraceDataTable` accepts controlled edit state and commit/validation callbacks. Editable columns opt in. Enter/F2, double-tap or double-click, and a TalkBack cell action open the editor. Commit and cancel restore table focus; removal of the edited row or column closes the stale session.
- The catalog and separate Maven consumer compile interactive controlled examples. The pinned EditableCell and Editing rows are **in progress**; the other 13 table rows remain planned. Released applicable coverage remains **0/121** until a verified Maven release.

## Verification

- API 36 emulator: the full table instrumentation suite passed **41/41**, including **12/12** editing tests for keyboard, IME, pointer, TalkBack actions, native accessibility nodes, validation, state restoration, large text, RTL, high contrast, focus return, and stale-row cleanup.
- Repository gate: `build lint checkTokenGeneration checkInventory apiCheck --offline` passed (**653** tasks). The `brace-foundation` and `brace-table` API baselines include the new public tokens and editor APIs.
- Maven Local: all six aligned `0.1.0-SNAPSHOT` artifacts published with AAR, sources, documentation JAR, POM, and Gradle Module Metadata. The separate `verification/consumer-smoke` project compiled against those coordinates.
- At 320 × 640 and 160 dpi, the catalog entry and live table remained navigable. Touch selection enabled **Edit selected cell**; the editor stayed visible above the keyboard. The accessibility tree exposed one row/column context node followed by the native editable text node with Save and Cancel actions. Android did not map Compose collection coordinates onto the native context node, so the spoken label supplies row and column identity.
- Documentation and coverage generation passed locally. Hosted PR checks and review are pending. This branch is not a release claim.

## Remaining and next branch

Single-line cell text is the only editor in this slice. Multiline and typed editors, editable column names, batched paste, row transactions, disjoint selection, reordering, freezing, and advanced formatting remain planned. The next focused table branch should cover `EditableName`, tracked separately from `EditableCell` in the pinned inventory. No Maven Central publication or release tag is part of this branch.
