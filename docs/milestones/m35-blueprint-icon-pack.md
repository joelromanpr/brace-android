# M35 delivery slice: optional pinned Blueprint glyph pack

**Status:** source and review in progress on `joelromanpr/m35-blueprint-icon-pack`. The single glyph catalog inventory row remains **in progress** with `firstRelease: null`; no stable or Maven Central coverage is claimed. The [generated ledger](../coverage.md) owns counts.

## Scope

- An aligned, opt-in `brace-blueprint-icons` AAR leaves the 11 original Brace vectors and `BraceIconRegistry.Default` unchanged.
- All 706 names in pinned `@blueprintjs/icons` 6.13.0 metadata map to exact 16px/20px path and viewBox pairs: 1,410 nonempty paths plus the two intentionally empty `blank` sizes. The empty `blank` glyph and 20×18 `third-party` viewBox are preserved.
- Generated typed names, dynamic lookup, searchable metadata, lazy parsed/cached `ImageVector`s, semantic Brace tint/size, decorative and announced images, and an original Brace Help fallback for unknown names.
- The compact packaged manifest is 844,161 bytes raw (283,934 bytes with gzip in a local measurement) and records SHA-256 for every source SVG and for upstream metadata and license. Apache-2.0 license and an attribution/modification notice ship in the AAR.
- Catalog search and artwork resolution controls, Pages guide, independent Maven-coordinate consumer usage, inventory evidence, and tests are included in this slice.

## Verification

| Gate | Result |
| --- | --- |
| Pinned source audit | `python3 scripts/generate_blueprint_icons.py --check --upstream /private/tmp/brace-blueprint-6.18.0` verifies all 706 names, 1,412 source SVG byte hashes, path/viewBox values, metadata, license, and generated Kotlin names. |
| Static inventory, tokens, docs | Passed `python3 scripts/generate_blueprint_icons.py --check --upstream /private/tmp/brace-blueprint-6.18.0`, `python3 scripts/generate_coverage.py --check`, `python3 scripts/generate_tokens.py --check`, `node scripts/build-docs.mjs`, `node --check docs/site/app.js`, and `git diff --check`; Pages builds 147 rows and 32 guides, with 0/121 applicable stable. |
| Source/API/catalog compilation | Pending shared Gradle lane; exact `:brace-blueprint-icons:apiDump` baseline must be generated. |
| Focused API 36 device tests | Pending shared device lane. Six tests are authored for all paths at both sizes, name parity, packaged license, original Brace registry isolation, metadata search, cache, 48dp icon action integration, RTL mirroring, semantics, and high contrast. |
| Broad build/lint/API, Maven Local, independent consumer | Pending shared Gradle lane. |
| 320dp visual review | Pending catalog APK and shared device lane. |
| Hosted CI, review, release | Pending; hosted GitHub Actions currently has an account billing block that prevents steps from starting. No public release is claimed. |

## Limits and next branch

The pack adds licensed Blueprint artwork in a separate artifact. It does not import React code, CSS icon fonts, or web icon loaders. The first pack load reads an 844,161-byte local JSON asset and should run off the UI thread; individual vectors are parsed when first requested and then cached. The cache is finite at 1,412 size/name combinations (42,358 SVG commands across the pinned set); actual device heap use still needs measurement. Blueprint icon art may not have Brace's native visual language, so product teams should choose the optional dependency deliberately. Manual TalkBack, RTL meaning for directional glyphs, large text, catalog visual review, and cross-device asset checks remain before a stable status or release. The next concrete sibling branch is `joelromanpr/m36-semantic-content` for semantic content components; no stable coverage is claimed here.
