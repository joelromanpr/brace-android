# M58: table formatting integration

**Status:** source slice in progress on `joelromanpr/m58-table-formatting`, stacked on M56 `40cbaed`. No first release, stable inventory row, or full Blueprint Table parity is claimed.

## Implemented in source

- `BraceTableColumn.revealFullValue` opts individual rows into a table-owned, selectable full-value dialog fed by complete `cellText`. `fullValuePreformatted` presents JSON/code values with preserved line breaks and left-to-right text.
- Each opted cell has a separate 48 dp **More** target and a custom action on its one grid cell node. A reveal column enforces room for both the selection target and button. Logical traversal places the button immediately after its cell across frozen and scrolling panes.
- Ctrl/Cmd+Enter opens the active cell when eligible; plain Enter still edits. The dialog does not change controlled selection, reuses the standalone formatter's token-driven visual shell, survives saved-state restoration, and closes when its row or column disappears or the table enters loading/editing. Dismissal returns keyboard focus to the table.
- The catalog shows long, short, and null formatted rows; the independent Maven consumer uses the new column API. The pinned Formatting capability moves from planned to in progress. TruncatedFormat and JSONFormat remain in progress with their table integration gap removed.

## Verification

Static inventory generation/check, JavaScript syntax, Pages generation, English/Spanish string XML parsing, and Git whitespace checks pass: **147** pinned rows, **0/121** applicable rows stable, and **61** Pages guides. Offline table Kotlin, Android-test Kotlin, and catalog Kotlin compilation pass. The public API dump records only the intended column options and cell callback; the shared dialog stays internal.

The focused API 36 M58 suite passes **6/6**. Its first attempts caught an invalid test base width and a focus assertion made while Compose was in touch input mode. The corrected tests use a valid 72 dp base column and explicitly request keyboard input mode before semantic focus checks, consistent with existing table tests. They cover touch, mouse, button Enter, Ctrl+Enter versus editing, TalkBack custom action, separate target bounds, copying, loading and row removal, saved state, frozen pane traversal, high contrast/RTL/large text, and virtualization. The final full API 36 table suite passes **110/110**, 0 skipped and 0 failed; this includes existing formatter, sorting, selection, editing, clipboard, loading, and frozen-pane regressions.

The offline broad gate (`build lint checkTokenGeneration checkInventory apiCheck publishToMavenLocal :catalog:assembleDebug`) passes **689 tasks**. The independent Maven Local consumer assembles **37 tasks** using the new public column API. The 320×640 API 36 catalog Formatting entry and sample were visually inspected in light and dark high-contrast themes using emulator `adb exec-out screencap -p` PNGs rendered as original-resolution images in the review tool (not inferred from UI hierarchy). The long JSON row has a separate More target; short and null rows do not. Both dialog variants show complete JSON, a readable title, and a visible Close control. Screenshots from this local check are under `/private/tmp/brace-m58-catalog-*.png` and are not committed.

No hosted M58 CI run exists before its draft PR. Preceding PRs have failed before job steps due the GitHub account billing/spending gate; that is not a source-test result. A human TalkBack traversal audit, including actual service reading order, remains open. `firstRelease` stays null.
## Limits and next branch

The caller supplies the reveal predicate because Compose cannot infer truncation of arbitrary custom cell content. The full-value dialog is plain selectable text; rich editing and column header menus remain separate planned table work. This slice has no Maven Central publication. The next concrete branch is `joelromanpr/m59-integration-verification`: merge-candidate build and catalog/device checks across component families, plus a human TalkBack traversal audit once review and CI can progress. No inventory row is stable here, hosted CI remains blocked by account billing, and repository visibility is still an owner decision.
