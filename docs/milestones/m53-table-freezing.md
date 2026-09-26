# M53: table freezing

**Status:** source slice in progress on `joelromanpr/m53-table-freezing-integration`, stacked on the verified M52 head `fd21983`. No release version or full Blueprint Table parity is claimed.

## Implemented in source

- Caller-controlled `frozenRows` and `frozenColumns` pin leading data positions beside fixed headers. Four clipped panes share the same logical row and column keys; the frozen intersection is composed once.
- Table-owned keyboard navigation and editing reveal scrollable targets while pinned targets stay in place. Selection, copy, sort, resize, loading, and reorder callbacks retain their existing contracts. Drag reordering ignores columns clipped behind the frozen pane; row-keyed Compose groups preserve custom cell state when records move.
- Frozen row, column, and intersection states are announced by localized cell/header semantics. The catalog demonstrates runtime toggles; the Maven consumer compiles explicit counts.
- The pinned Blueprint inventory's `table-freezing` capability row is updated, with implementation, sample, documentation, and instrumented-test links.

## Verification

Static inventory generation and check passed (147 pinned rows; 0/121 released applicable rows). Pages generation passed with 58 guides, as did Android string XML parsing, JavaScript syntax, Git whitespace, and conflict-marker checks. Offline Gradle build, lint, token and inventory checks, API dump/check, and Maven Local publication passed (689 tasks). The independent Maven consumer assembled (37 tasks). Six focused freezing tests passed on API 36, including pinned semantics, keyboard reveal, RTL, 3× text, clipped drag targets, and keyed custom content. The first 101-test table sweep found a transient formatter-dialog focus assertion and a genuine stale lazy-layout index after row removal. The index was guarded and its focused test passed; the formatter test passed in isolation. The final full table sweep passed **101/101** on API 36. The catalog sample was visually inspected on a 320×640 emulator in light and dark high-contrast modes; human TalkBack traversal is pending. `firstRelease` remains null.

## Limits and next branch

The caller should keep pinned counts small enough to leave a useful scrolling viewport on compact and large-text layouts. This slice freezes leading positions only. Multi-block drag, drag auto-scroll at edges, column header menus, and a manual TalkBack traversal audit remain open. The next focused branch is `joelromanpr/m56-table-accessibility`, covering native grid metadata, heading and active-cell semantics, traversal order, and accurate disabled actions.
