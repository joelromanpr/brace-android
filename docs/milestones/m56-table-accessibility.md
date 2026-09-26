# M56: table accessibility

**Status:** source slice in progress on `joelromanpr/m56-table-accessibility`, stacked on verified M53 head `da8668c`. No release version or full Blueprint Table parity is claimed.

## Implemented in source

- The actual `BraceDataTable`, `BraceTableCell`, `BraceColumnHeader`, and `BraceRowHeader` APIs expose collection dimensions, coordinates, native heading markers, selected and active-cell state, and pinned-state context. The previously proposed `BraceTableSemantics` object was replaced in the inventory by these real APIs.
- Frozen and scrolling panes share logical row-major TalkBack traversal indices, including controls and RTL. The table owns one traversal group; scroll containers opt out of their automatic grouping.
- Disabled sort controls and resize handles expose only available accessibility actions at size bounds. English and Spanish active-cell labels, an interactive catalog entry, copyable usage, and a focused instrumented suite accompany the change.
- `table-table-accessibility` remains **in progress**, with `firstRelease` null.

## Verification

Static inventory generation and check passed: 147 pinned rows, 0/121 released applicable rows. Pages generation passed with 60 guides; JavaScript syntax, Android string XML parsing, and Git whitespace checks passed. Offline Kotlin and Android-test compilation passed. `:brace-table:apiDump` ran separately and the broad `build lint checkTokenGeneration checkInventory apiCheck publishToMavenLocal :catalog:assembleDebug` run passed (689 tasks); the separate Maven Local consumer assembled (37 tasks).

Focused M56 API 36 tests passed **3/3**. The first run exposed a disabled sort control retaining an `OnClick` semantic. The modifier now omits click behavior when disabled. The first full sweep passed 103/104: an older test expected a Decrease action on a row already at its minimum. That assertion was corrected to match the bounded action contract, its class passed **14/14**, and the final full table suite passed **104/104** on API 36. Native Android nodes expose collection dimensions, cell coordinates, heading markers, active/pinned state, and disabled actions. Compose semantics tests check logical pane traversal indices in RTL after scrolling; a human TalkBack traversal audit remains open.

Hosted CI for [draft PR #65](https://github.com/joelromanpr/brace-android/pull/65) failed before any job steps on [run 36268470780](https://github.com/joelromanpr/brace-android/actions/runs/36268470780). The check annotation says recent account payments failed or the spending limit needs to be increased; CodeQL was skipped. This is not a hosted source-test result.

The interactive catalog sample was inspected on a 320×640 API 36 emulator in light and dark high-contrast modes. Frozen-pane toggles, headers, first rows, selection summary, and partially visible horizontally scrollable data remained legible. Large-text visual inspection was not repeated for this slice; the inherited 3× text instrumentation check remained in the 104-test sweep. `firstRelease` remains null.

## Limits and next branch

Virtualized offscreen cells are not individually exposed until composed, while the table root still reports full dimensions. A human screen-reader audit must confirm actual service traversal on a device, including row-major order, controls, large text, and RTL. Compose traversal indices and native metadata alone do not prove what TalkBack speaks or which virtual node it visits next. This slice does not add multi-item drag, drag edge auto-scroll, or column header menus. The next focused table branch is `joelromanpr/m58-table-formatting` after this source slice is reviewed.
