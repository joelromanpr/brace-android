# M57 — Next-icon catalog large-text navigation

**Status:** source and review in progress on `joelromanpr/m57-icon-large-text`, restacked on merged M55 `main` `1891213`. This catalog-only follow-up does not add icon artwork or a public library API. The pinned next-icon capability remains **in progress** with `firstRelease: null`; the [generated coverage ledger](../coverage.md) owns the counts.

## Finding and change

On the API 36 emulator at 320 × 640 px, 160 dpi, M55's next-icon detail remained scrollable after changing Android font scale from 1.0 to 2.0 while the detail was open. The live sample and usage content were reachable. The previously observed blank lazy-list viewport did **not** recur in this run, so this branch does not claim a root-cause fix for it.

At 2×, the complete pinned behavior, adaptation rationale, and source URL take many screenfuls before the live icon sample. The catalog offers a next-icon-specific **Jump to live icon sample** action immediately after the entry status. It scrolls to the interactive sample and leaves the inventory text and copyable usage in place. Other catalog entries retain their order. A new catalog Android test loads the real inventory row, changes Compose font scale from 1.0 to 2.0 in a 320 × 640 viewport while detail is open, activates the 48 dp jump, and requires the live sample and search field to be visible. This tests recomposition and navigation; the manual Android setting change remains separate evidence for activity recreation.

## Verification

| Gate | Evidence |
| --- | --- |
| M55 baseline 2× reproduction | Installed the M55 catalog APK on `emulator-5556`, opened the next-icon entry, changed Android `font_scale` to 2.0 while open, scrolled through the live sample and Compose usage, and restored `font_scale` to 1.0. No blank viewport reproduced. |
| Source/static and documentation | On merged M55 main, `git diff --check`, token and coverage checks, JavaScript syntax, and Pages build passed. Pages generated **148 inventory rows, 11 real catalog captures, and 43 guides**; the generated ledger still reports **0/122 stable applicable rows** and **0/94 stable components**. |
| Earlier M57 Kotlin build and 2× manual jump | Before the M55 restack, `:catalog:assembleDebug` passed (**157 tasks**). Installed that APK on API 36 at 320 × 640 px and 2× font scale. The next-icon-specific jump fit within the viewport; touch activation showed the live sample counts, search field, and glyph preview without clipping or a blank viewport. Restored `font_scale` to 1.0. The current-main automated API 36 regression is recorded below; the manual system-font run has not been repeated after restacking. |
| Focused automated 2× regression | `CatalogLargeTextTest` exercises the inventory-backed next-icon detail, changes Compose font scale 1.0→2.0 while open, checks the jump target size and accessibility where supported, and checks that activation exposes the sample. After restacking on M55 main, `:catalog:compileDebugAndroidTestKotlin :catalog:assembleDebug :catalog:lint` passed **344 tasks**. `:catalog:connectedDebugAndroidTest` passed **1/1** on `Brace_API36(AVD) - 16` (**207 tasks**). An initial offline compile could not resolve pinned `androidx.lifecycle:lifecycle-livedata:2.9.2` from the temporary mirror; the official Google Maven artifact resolved it. No version override or repository substitution was introduced. |
| Maven Local and independent consumer | Seven aligned artifacts published to Maven Local (**271 tasks**). A clean independent coordinate-only consumer `:app:assembleDebug` passed **38 tasks** (37 executed, including Kotlin compilation). |
| Hosted CI | [Earlier run 36269695465](https://github.com/joelromanpr/brace-android/actions/runs/36269695465) failed before any job steps during the repository's former billing gate and supplies no test evidence. The billing gate is resolved; current-head `verify`, API 34 `instrumented` (including the new catalog test), and CodeQL evidence will be linked in the PR after the restack is pushed. |

## Limits and next work

This control addresses the sample's discoverability at large text; it does not establish why the earlier transient blank screenshot occurred. The automated test changes Compose density during recomposition; it does not simulate Android process death or replace the manual system-font-scale run. Manual TalkBack, keyboard/mouse, cross-device large-text visual review, current-head hosted checks, maintainer review, and release remain open. No coverage row is stable.
