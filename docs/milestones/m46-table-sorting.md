# M46 delivery slice: table sorting

**Status:** static implementation prepared on `joelromanpr/m46-table-sorting`, preflighted on protected M31 Tree main `fee948c` plus the focused M26 header-edit slice while PR #40 is hosted. The pinned Blueprint Table `Sorting` row is **in progress**. Stable coverage stays **0/122** applicable rows, and no first release is assigned.

## Scope

- `BraceTableSort`, direction enum, and saveable sort state; controlled `sort`/`onSortChange` and per-column `sortable` API.
- Separate 48 dp sort buttons in fixed headers; selection, editable names, and resize grips remain separate actions.
- Next-action and current-direction TalkBack semantics; English and Spanish labels; keyboard, mouse, touch, RTL, large text, high-contrast, and theme-token states.
- Catalog and independent Maven consumer examples with host-owned stable ordering, source guide and Pages integration, inventory evidence, and device tests.

## Verification

| Gate | Result |
| --- | --- |
| Inventory and tokens | Passed `python3 scripts/generate_coverage.py --check` (148 rows, 0/122 stable) and `python3 scripts/generate_tokens.py --check`. This slice uses existing table and semantic tokens. |
| Pages and JavaScript | Passed `node scripts/build-docs.mjs` (70 guides, 24 real Android captures) and JavaScript syntax checks. English/Spanish string XML parses; whitespace check passes. |
| Kotlin build, lint, exact API baseline | Pending shared Gradle lane. |
| API 36 interaction and accessibility | Pending shared emulator lane. Tests cover cycle, stable row-key selection, independent header and resize controls, keyboard/mouse, TalkBack action, RTL/high contrast/2× text, viewport and state restoration. |
| Maven Local and separate consumer | Pending shared Gradle lane. |
| Visual, manual TalkBack, hosted CI | Pending review and runner availability. |

## Known limits and next branch

Brace reports sorting intent and state but does not reorder rows. This follows Blueprint's data-agnostic table behavior while using a native header control instead of a web menu. Apps choose comparator and tie policy or fetch sorted pages from a server, then pass a replacement list. This slice handles one sorted column at a time. Multi-column sorting, custom sort menus, and header action overflow remain follow-up work. The next integration branch is `joelromanpr/m46-table-sorting` after M26 merges and the shared Gradle/emulator lane is released; a focused follow-up will address any device findings.
