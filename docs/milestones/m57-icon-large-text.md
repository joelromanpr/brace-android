# M57 — Next-icon catalog large-text navigation

**Status:** source and review in progress on `joelromanpr/m57-icon-large-text`, stacked on M55. This catalog-only follow-up does not add icon artwork or a public library API. The pinned next-icon capability remains **in progress** with `firstRelease: null`; the [generated coverage ledger](../coverage.md) owns the counts.

## Finding and change

On the API 36 emulator at 320 × 640 px, 160 dpi, M55's next-icon detail remained scrollable after changing Android font scale from 1.0 to 2.0 while the detail was open. The live sample and usage content were reachable. The previously observed blank lazy-list viewport did **not** recur in this run, so this branch does not claim a root-cause fix for it.

At 2×, the complete pinned behavior, adaptation rationale, and source URL take many screenfuls before the live icon sample. The catalog now offers a next-icon-specific **Jump to live icon sample** action immediately after the entry status. It scrolls to the interactive sample and leaves the inventory text and copyable usage in place. Other catalog entries retain their order.

## Verification

| Gate | Evidence |
| --- | --- |
| M55 baseline 2× reproduction | Installed the M55 catalog APK on `emulator-5556`, opened the next-icon entry, changed `font_scale` to 2.0 while open, scrolled through the live sample and Compose usage, and restored `font_scale` to 1.0. No blank viewport reproduced. |
| Source/static and documentation | `git diff --check`, `node --check scripts/build-docs.mjs`, and `node scripts/build-docs.mjs` passed. Pages generated 148 inventory rows and 34 guides; the generated ledger still reports 0 stable rows. |
| M57 Kotlin build and 2× jump interaction | `./gradlew --offline --init-script /tmp/brace-temp-repo.init.gradle :catalog:assembleDebug --no-parallel` passed (157 tasks). Installed that APK on API 36 at 320 × 640 px and 2× font scale. The next-icon-specific jump action fit within the viewport; touch activation immediately showed the live sample counts, search field, and glyph preview without clipping or a blank viewport. Restored `font_scale` to 1.0. |

## Limits and next work

This control addresses the sample's discoverability at large text; it does not establish why the earlier transient blank screenshot occurred. Manual TalkBack, keyboard/mouse, cross-device large-text visual review, hosted checks, review, and release remain open. No coverage row is stable. The next scheduled table slice is `joelromanpr/m58-table-formatting`, stacked on M56; its completion is tracked separately.
