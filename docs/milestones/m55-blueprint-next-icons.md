# M55 — Public Blueprint next glyph catalog

**Status:** source and review in progress on `joelromanpr/m55-blueprint-next-icons`, stacked on M35. The separate pinned inventory capability row is **in progress** with `firstRelease: null`; no Maven Central or full Blueprint icon parity is claimed. The [generated coverage ledger](../coverage.md) owns the counts.

## Pinned audit and scope

- Blueprint core 6.18.0 pin `a60d4c92257612808fbfac81cfeee4fcba91a8b4` contains `@blueprintjs/icons` 6.13.0. M35 covers all 706 **legacy** `icons.json` names in 16px and 20px artwork. The same public package ships `@blueprintjs/icons/next` with `icons-next.json`, `icons-name-map.json`, and `next/` package exports.
- The next catalog has **695 canonical outlined 16px glyphs** and **386 filled variants**, or **1,081 source SVGs**. Its name set overlaps legacy by 229; **466 next names are absent from the legacy pack**. All 706 legacy names map to canonical next names, and the map reaches all 695 next names.
- The separate, aligned, opt-in `brace-blueprint-icons-next` AAR preserves typed canonical names, exact viewBoxes and paths, variant availability, metadata search, a legacy migration map, semantic Brace tint/size, decorative or announced image semantics, explicit RTL mirroring, a known-icon outlined fallback, and original Brace Help for unknown names. It leaves `BraceIconRegistry.Default` and M35's legacy pack unchanged.
- The asset manifest records SHA-256 for every upstream SVG, metadata, name map and license. The Apache-2.0 license and attribution/modification notice ship in the AAR. The pinned empty `blank` and 17×16 `cube-pen` viewBox remain. One source `<rect height="16"/>` has default zero width and draws no pixels; it is omitted from the Compose path.
- Inventory, catalog live variant/search/action example, Pages guide, independent Maven consumer example, API baseline and Android tests are part of this topic branch.

## Verification

| Gate | Evidence |
| --- | --- |
| Pinned source audit | `python3 scripts/generate_blueprint_next_icons.py --check --upstream /private/tmp/brace-blueprint-6.18.0` passed for 695 outlined SVGs, 386 filled SVGs, 706 mappings, metadata, and copied license. |
| Inventory, token, Pages and JavaScript static checks | Passed `python3 scripts/generate_blueprint_icons.py --check --upstream /private/tmp/brace-blueprint-6.18.0`, `python3 scripts/generate_blueprint_next_icons.py --check --upstream /private/tmp/brace-blueprint-6.18.0`, `python3 scripts/generate_coverage.py --check`, `python3 scripts/generate_tokens.py --check`, `node scripts/build-docs.mjs`, `node --check docs/site/app.js`, and `git diff --check`. Pages built 148 rows and 33 guides; 0/122 applicable rows and 0/94 components are stable. |
| API/build/lint/catalog compilation | Pending shared Gradle lane. |
| Focused API 36 device interactions/accessibility | Pending shared emulator lane. |
| Maven Local and separate consumer | Pending shared Gradle lane. |
| Compact visual, manual TalkBack and hosted checks | Pending. |

## Limits and next work

The pack is source-only until verified release and remains in progress. Loading the local manifest synchronously takes work proportional to its 762 KB size; call `load` on a background dispatcher and retain the pack. Vector path parsing is lazy and cached; actual device heap cost and cross-device artwork appearance need measurement. A name without a published filled variant has no filled vector; the composable displays its outlined form. Browser React components, CSS icon fonts, and JavaScript migration imports are not Android components. Manual TalkBack, hardware keyboard/mouse, large-text catalog review, hosted API34, maintainer review and Maven Central release remain open. The next concrete branch for any icon pack findings is `joelromanpr/m57-icon-pack-followups`; M56 is reserved for table accessibility work.
