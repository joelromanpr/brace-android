# M11 delivery slice: icon foundation

**Status:** source and review in progress on `joelromanpr/m11-icons`. The pinned core Icon and icons loading rows are **in progress**; the 500+ glyph catalog is **planned**. There is no stable coverage or Maven Central release from this branch. The [generated ledger](../coverage.md) owns the counts.

## Scope

- A separate `brace-icons` AAR with aligned publication metadata, sources, KDoc artifact, and API baseline.
- Typed bundled names, runtime lookup/fallback, immutable custom registration, and a Compose subtree registry provider.
- Token-driven sizes, semantic intent colors, optional custom dp/tint, decorative and announced icons, and an accessible icon-only button.
- Eleven original Apache-2.0 Brace vectors with a per-glyph manifest packaged in the AAR. No Blueprint paths, SVGs, fonts, or code are copied.
- Inventory evidence, generated coverage, catalog interactions, Pages guide, and independent Maven consumer usage in the same reviewable slice.

## Verification

| Gate | Result |
| --- | --- |
| Source, AndroidTest, and catalog compilation | Passed `:brace-icons:compileDebugKotlin`, `:brace-icons:compileDebugAndroidTestKotlin`, and `:catalog:compileDebugKotlin`. |
| Focused API 36 device tests | Passed **6/6** on `Brace_API36(AVD) - 16` after restoring the original single-node icon semantics and adding bounded synchronization for native accessibility queries; zero failures, errors, or skips. This covers manifest, fallback/registration, semantics and sizing, touch/mouse/keyboard/disabled actions, a 24dp caller-size request retaining a 48dp target, real Android accessibility nodes, Compose accessibility, and RTL mirroring. |
| Full build, lint, token, inventory, and API gates | Passed on the prior rebased icon head `911b408`: local `build lint checkTokenGeneration checkInventory apiCheck` (377 actionable tasks, zero failures). The `brace-icons/api/brace-icons.api` baseline was checked. |
| Documentation and generated coverage | Passed local `node scripts/build-docs.mjs`, `node --check docs/site/app.js`, and `python3 scripts/generate_coverage.py --check` (147 inventory rows, 27 guides; 0/121 applicable stable). |
| Maven Local publication and independent consumer | Passed on the prior rebased icon head `911b408`: foundation/core/icons Maven Local publication and independent `verification/consumer-smoke :app:assembleDebug`. The icons AAR includes the per-glyph manifest; Maven metadata includes POM, sources, and KDoc JAR. |
| 320×640 catalog visual and interaction QA | Passed both Icon and Icon loading examples: size/intent/action count, custom alias, and fallback changes displayed without overlap or horizontal clipping. |
| Hosted CI, review, and PR merge | Rebased onto merged M10 squash commit `b8dc999`; PR #21 is open. Hosted `verify` passed. The first API 34 `instrumented` run found an icon-button native accessibility-node mismatch after all 169 core tests passed. A later semantics edit split the label from the click action in local API 36 native nodes, so that edit was reverted. The test now waits for the native tree before checking enabled and disabled states, and local API 36 passes 6/6. A second hosted run failed before tests because the runner had 6,990 MB free and the 2,048 MB emulator partition requested 7,373 MB. The workflow now requests 1,024 MB. A third hosted run is being replaced with this corrected head; hosted API 34 validation remains pending. |

## Limits and next branch

The 11 bundled icons are intentionally useful but do not implement Blueprint's complete glyph catalog. Brace uses one original 24-unit vector per icon and native Compose scaling rather than Blueprint's separate 16px and 20px assets or icon fonts. `BraceIconButton` handles accessibility for icon-only actions; consumers that put `BraceIcon` in their own clickable container must label and size that container themselves. A later focused icon-catalog branch should enumerate, license-check, and implement additional pinned glyph names without treating an incomplete subset as parity. The next queued branch is `joelromanpr/m12-select-query`; other core, datetime, and table rows stay on the roadmap.
