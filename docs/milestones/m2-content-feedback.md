# M2: content and progress feedback

**Status:** implemented on `joelromanpr/m2-core-content-feedback`; review and hosted CI pending. No Maven Central version has shipped, so all six inventory rows remain **in progress** and released coverage remains **0/121** applicable rows.

## What this slice adds

- Public `brace-core` APIs for Card, CardList, Divider, Section, SectionCard, and ProgressBar, with KDoc and API baseline updates.
- Token-driven surfaces, depth, density, selection, focus, disabled, pointer and progress intent states. Section expansion supports controlled or saveable state; the progress bar supports ranges, RTL, and reduced motion.
- Interactive Android catalog entries with copyable Compose examples. The generated coverage asset and the documentation site use the pinned inventory as their source for names, status, and counts.
- Dedicated Android interaction tests for card/list semantics and keyboard actions, section expansion and state restoration, progress values and visual direction, 2x text, RTL, dark high contrast, and automated API 34+ Compose accessibility checks.

## Verification

| Gate | Result |
| --- | --- |
| `./gradlew --offline -I /tmp/brace-temp-repo.init.gradle build lint checkTokenGeneration checkInventory apiCheck` | Passed: 287 tasks, including debug/release catalog and library builds, lint, unit tests, token/inventory generation checks, and API compatibility. The init script is a temporary local mirror for official dependencies and is not part of the repository. |
| `ANDROID_SERIAL=emulator-5556 ./gradlew --offline -I /tmp/brace-temp-repo.init.gradle :brace-core:connectedDebugAndroidTest` | Passed: 21/21 tests on an isolated API 36 AVD. Three initial focus/semantics test failures were corrected and the full suite reran clean. |
| `node scripts/build-docs.mjs` and `node --check docs/site/app.js` | Passed: 147 inventory rows and nine guides built. |
| Maven Local publication and `verification/consumer-smoke` assemble | Passed: foundation and core AARs, source and KDoc jars published locally; the separate app compiled against `0.1.0-SNAPSHOT` coordinates and now exercises Card, Section, ProgressBar, and Button. A temporary consumer init script kept Maven Local ahead of the official dependency mirror. |
| Hosted GitHub checks | Pending PR creation and CI run. |

## Remaining and limits

The inventory still has 94 applicable Blueprint component rows, 27 applicable capability rows, and 24 web-specific mappings; no row is recorded as stable before a release. Cards and sections cover their documented base interactions, but manual TalkBack, keyboard, mouse, large text layout, and all theme variants across representative apps remain acceptance work. `BraceCardList` is intended for bounded lists, not virtualized data. The data table, selects, date/time, icons, overlays, and remaining core rows are scheduled separately. Maven Central and GitHub Pages are not live from this branch.

The next concrete topic branch is `joelromanpr/m3-core-navigation-feedback`, focused on pinned core navigation and feedback rows (including Callout, Tag, Breadcrumbs, and related APIs) with inventory, catalog, docs, and tests in the same PR. Later branches handle overlays, selects, date/time, icons, and table behavior according to the inventory.
