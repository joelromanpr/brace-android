# M53: table freezing

**Status:** source slice in progress on `joelromanpr/m53-table-freezing`, stacked on the M52 table integration head `d51ec88`. No release version or full Blueprint Table parity is claimed.

## Implemented in source

- Caller-controlled `frozenRows` and `frozenColumns` pin leading data positions beside fixed headers. Four clipped panes share the same logical row and column keys; the frozen intersection is composed once.
- Table-owned keyboard navigation and editing reveal scrollable targets while pinned targets stay in place. Selection, copy, sort, resize, loading, and reorder callbacks retain their existing contracts.
- Frozen row, column, and intersection states are announced by localized cell/header semantics. The catalog and Maven consumer demonstrate runtime counts.
- The pinned Blueprint inventory's `table-freezing` capability row is updated, with implementation, sample, documentation, and instrumented-test links.

## Verification

Static inventory generation and check passed (147 pinned rows; 0/121 released applicable rows). Pages generation passed with 57 guides, as did Android string XML parsing, JavaScript syntax, Git whitespace, and conflict-marker checks. Gradle compilation, API dump/check, device tests, Maven Local publication, independent consumer assembly, and visual/TalkBack inspection are **pending the shared validation lane**. `firstRelease` remains null.

## Limits and next branch

The caller should keep pinned counts small enough to leave a useful scrolling viewport on compact and large-text layouts. This slice freezes leading positions only. Multi-block drag, drag auto-scroll at edges, and column header menus remain open. Once M53 integrates, run all table regressions across the combined M46–M53 renderer and target the next unimplemented pinned table row.
