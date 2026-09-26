# M14: first table viewport slice

**Status:** in-progress source in [draft PR #24](https://github.com/joelromanpr/brace-android/pull/24), integrated with `main` at `286d04e` (through M17, M35, and M54). The pinned Blueprint Table rows remain unreleased; generated stable coverage is **0/121 applicable rows**. This report records the current local checks. Hosted verify, API 34 instrumentation, and CodeQL remain required before merge.

## Shipped in this branch

- New `brace-table` Android library and aligned `0.1.0-SNAPSHOT` Maven metadata with AAR, sources, and KDoc archives. CI, API baseline, release staging list, and the separate Maven consumer include the artifact.
- `BraceDataTable`, typed `BraceTableColumn`, controlled `BraceTableSelection`, and saveable `BraceTableViewport`. Rows are lazy; columns outside the horizontal viewport are not composed except for adjacent buffers. Fixed column and row headers, single-cell/whole-row selection, keyboard navigation, RTL, and localized grid semantics are present.
- Token-driven light, dark, high-contrast, compact, and comfortable styling; cell and row targets are at least 48 dp high. The inventory-backed catalog has a 120-row interactive sample, and the [usage guide](../table-viewport.md) documents the public API and limits.
- Five pinned table rows are **in progress**. The remaining 18 table rows are planned. Nothing is marked stable or released.

## Review fixes and limits

- The row-header width now measures the localized corner label and maximum row number, including at 3× font scale. The large-text device test checks the expanded corner and row-header bounds and full accessible cell value.
- The table requires a bounded parent width and finite column widths. It checks Compose's measured-layout constraint before building an excessively wide schema, returning a clear request for fewer or narrower columns. Unlimited horizontal extent remains future work; the 5,000-row × 400-column regression covers the current large-grid use case.
- The fixed row-header overlay draws after the scrolling cells without a different semantic z-index. A table traversal group and explicit row-major indices supply Compose reading-order hints; device tests verify those properties, native nodes, and fixed-header touch after scroll. API 36 returned null for native `traversalBefore`/`traversalAfter` links, so manual TalkBack reading-order QA remains open. No end-to-end TalkBack ordering is claimed.
- The first slice does not include range/column selection, resizing, copying, editing, sorting, reordering, frozen data regions, loading states, or formatting helpers. Caller-owned sorting and selection persistence are documented.

## Verification

| Gate | Result |
| --- | --- |
| Full local build, lint, token/inventory generation, API checks, catalog assembly | Passed `build lint checkTokenGeneration checkBlueprintIconGeneration checkInventory apiCheck :catalog:assembleDebug --no-parallel` on JDK 21 and SDK 36: **744 Gradle tasks**. |
| API 36 Compose device tests | Passed **16/16**, zero failures/skips. These cover duplicate/blank keys, two-axis virtualization, fixed-header touch/RTL/partial-scroll alignment, mouse and touch, controlled selection, keyboard reveal, state restoration, density and 48 dp targets, 3× text, native accessibility nodes, explicit traversal hints, an automated accessibility check, and bounded 5,000 × 400 composition/key-index work. This is not a frame-time or memory benchmark. |
| Maven Local and external consumer | Seven aligned artifacts published with AAR, sources, KDoc JAR, POM, and Gradle module metadata (**271 tasks**). The separate coordinate-only consumer assembled (**37 tasks**). No Maven Central deployment was attempted. |
| Coverage and documentation site | Token and coverage source checks pass: 147 pinned rows, 0/121 stable applicable rows. `node scripts/build-docs.mjs` builds 11 real Android captures and **43** guides; all local targets resolve across 44 HTML pages. JavaScript syntax and Git whitespace checks pass. |
| Visual review | An earlier 320 × 640 catalog inspection covered light and dark high-contrast modes and found the fixed row-header paint/touch issue that this slice corrected. The final enlarged-text row-header layout still needs manual visual inspection. |
| Hosted CI | Fresh hosted verify, API 34 instrumentation, and CodeQL results are pending on the integrated head. |

## Next table branch

The next focused table branch is `joelromanpr/m18-table-selection-resize`: controlled range/cell/column selection and accessible column resize handles. Copying, editing, frozen data regions, reordering, loading, and formatting follow in separate slices. Blueprint parity remains governed by the pinned inventory.
