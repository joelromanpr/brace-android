# M51: table reordering

**Status:** source slice in progress on `joelromanpr/m51-table-reordering`, stacked on M26 `ceba218`. No release version or full Blueprint Table parity is claimed.

## Implemented in source

- Public `BraceTableReorder.move` and `applyOrder` helpers plus controlled `onRowOrderChange` and `onColumnOrderChange` callbacks reporting complete key orders.
- Token-driven, separate 48 dp grips for fixed row and column headers. Touch and mouse drag target visible items; focused arrows/Home/End, TalkBack click and custom actions, position announcement, RTL behavior, and focus restoration support other input modes. Existing selection, editing, resizing, and viewport rendering retain their targets.
- The catalog offers a live reorder sample and copyable usage. The independent Maven consumer uses both callbacks. The pinned `table-reordering` inventory row is in progress, with implementation, sample, documentation, and device-test links.

## Verification

Static token and inventory checks passed (147 pinned rows; 0/121 released applicable rows). The Pages build passed with 45 guides, along with JavaScript syntax, Android string XML parsing, Git whitespace, and conflict-marker checks. Gradle build, API dump/check, device tests, Maven Local publication, independent consumer assembly, CI, and visual/TalkBack inspection are **pending the shared validation lane**. `firstRelease` remains null.

## Limits and integration

This source moves one key at a time and only drags to composed visible headers. Multi-item drag, edge auto-scroll, drop guides, and server-sort conflict policy remain open. M46 sorting owns sort indicators; M48 owns cell/header primitives; M49 owns table loading; M50 owns regions. This branch changes the shared `BraceDataTable` and Pages/catalog files, so integration must rerun all table tests and reconcile the public API snapshot. The next branch should address a separate pinned inventory row after the table slices integrate.
