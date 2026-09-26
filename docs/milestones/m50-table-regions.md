# M50: table regions

**Status:** source slice in progress on `joelromanpr/m50-table-region`, stacked on M26 `ceba218`. No release version or full Blueprint Table parity is claimed.

## Shipped in source

- Public stable-key `BraceTableRegion` cardinalities for cells, full rows, full columns, and the whole table; immutable add/update helpers; controlled `BraceTableSelection.Regions`; and a saveable selection helper.
- `BraceDataTable` highlights multiple regions without changing lazy rows, viewport-only columns, or fixed headers. The corner and Ctrl/Cmd+A select the table. Ctrl/Cmd+click and TalkBack actions add regions; the focused table announces selection, exposes selected cells and headers, and supports keyboard extension of the last cell region.
- Copying creates sparse, quoted TSV from the current full data model, including offscreen cells, and refuses stale bounds. The catalog, copyable usage, independent consumer, device tests, and Pages guide reflect the same API. Inventory `table-region` is in progress.

## Verification

Static token generation and inventory generation checks passed (147 pinned rows; 0/121 released applicable rows). The Pages build passed with 45 guides, as did JavaScript syntax, Android string XML parsing, Git whitespace, and conflict-marker checks. Gradle build, API dump/check, device tests, Maven Local publication, independent consumer assembly, CI, and visual/TalkBack inspection are **pending the shared validation lane**. `firstRelease` remains null.

## Limits and next branch

Blueprint's index-based mutable region operations and styled region groups are adapted to immutable key-based values. Mouse drag, touch auto-scroll, drag-based row or column intervals, frozen regions, and background export are not included. M46 sorting owns host sort indicators; M48 owns public cell/header primitives; M49 owns loading states. This slice touches shared table selection and clipboard code, so integration must rerun all table tests. The next branch should address a separate planned table row after M46–M50 integration.
