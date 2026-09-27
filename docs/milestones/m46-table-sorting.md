# M46 delivery slice: table sorting

**Status:** sorting is rebased onto protected main `283dfba` after the editable-header slice merged. PR #55 remains draft until hosted checks pass. The pinned Blueprint Table `Sorting` row is **in progress**. Stable coverage stays **0/122** applicable rows, and no first release is assigned.

## Scope

- `BraceTableSort`, direction enum, and saveable sort state; controlled `sort`/`onSortChange` and per-column `sortable` API.
- Separate 48 dp sort buttons in fixed headers; selection, editable names, and resize grips remain separate actions. Only sortable columns reserve the extra width, preserving dense unsortable columns.
- Next-action and current-direction TalkBack semantics; English and Spanish labels; keyboard, mouse, touch, RTL, large text, high-contrast, and theme-token states.
- Catalog and independent Maven consumer examples with host-owned stable ordering, source guide and Pages integration, inventory evidence, and device tests.

## Verification

| Gate | Result |
| --- | --- |
| Inventory and tokens | Passed `python3 scripts/generate_coverage.py --check` (148 rows, 0/122 stable) and `python3 scripts/generate_tokens.py --check`. This slice uses existing table and semantic tokens. |
| Pages and JavaScript | Passed `node scripts/build-docs.mjs --check` (76 guides, 25 real Android captures) and JavaScript syntax checks. English/Spanish string XML parses; whitespace check passes. |
| Kotlin build and API baseline | On exact M26-merged main, `:brace-table:apiCheck :brace-table:assembleDebugAndroidTest :catalog:assembleDebug :brace-table:lintDebug checkInventory checkTokenGeneration` passed in **292 actionable tasks**. The sorting API snapshot matches. |
| API 36 interaction and accessibility | Focused `BraceTableSortTest` passed **7/7** on exact main; the complete table module passed **70/70** on API 36, with zero failed or skipped. Tests cover cycle, stable row-key selection, independent header and resize controls, native accessibility action and direction, keyboard/mouse, RTL/high contrast/2× text, viewport and state restoration. |
| Maven Local and separate consumer | All eight aligned `0.1.0-SNAPSHOT` artifacts published to Maven Local in **308 actionable tasks**; each has AAR, sources, documentation jar, POM, and Gradle metadata. The independent coordinate-only consumer assembled in **37 tasks**. |
| Visual, manual TalkBack, hosted CI | Hosted verify and CodeQL passed on the prior exact-main head; API 34 instrumentation and the final evidence commit are running. Manual TalkBack, mouse and tablet review remain. |

## Known limits and next branch

Brace reports sorting intent and state but does not reorder rows. This follows Blueprint's data-agnostic table behavior while using a native header control instead of a web menu. Apps choose comparator and tie policy or fetch sorted pages from a server, then pass a replacement list. This slice handles one sorted column at a time. Multi-column sorting, custom sort menus, and header action overflow remain follow-up work. The next branch builds table formatters after sorting merges.
