# Visual showcase

The [gallery](site/index.html#showcase) contains captures of the actual Compose catalog on an Android emulator. Names, families, and availability labels come from the [component list](coverage.md). Screenshots show appearance; the Android app and component tests cover behavior.

Each capture records its emulator, size, date, appearance, related inventory rows, copyable Kotlin example, and the image's SHA-256 hash. The site build checks the hash and image dimensions. A source link opens the current catalog implementation on `main`.

## Capture and review a new state

1. Build and install the catalog: `./gradlew :catalog:installDebug`. Launch `io.github.joelromanpr.brace.catalog/.CatalogActivity` on a named emulator.
2. Open the relevant inventory row in the catalog and set the theme, contrast, brand, and density shown in the image. Capture with `adb exec-out screencap -p > capture.png`. Crop or redact only when needed for personal data, and disclose that change in the caption. Do not reconstruct UI in HTML or image editing software.
3. Inspect the PNG at full size. Check legibility, clipped content, system bars, keyboard overlap where relevant, and whether the image actually shows the named component. Keep useful light, dark, and high-contrast states; a single screenshot does not prove accessibility.
4. Add the PNG under `docs/site/showcase/` and a record in `docs/site/showcase/captures.json`. Use actual inventory IDs, descriptive alternative text, a catalog source file, a copyable usage example, and the PNG's SHA-256 hash.
5. Run `python3 scripts/generate_coverage.py --check`, `node scripts/build-docs.mjs`, and `node --check docs/site/app.js`. The site build checks inventory IDs, image hashes, and PNG dimensions. Review the responsive gallery and full-size image links in a browser.

The static site cannot demonstrate TalkBack, touch, mouse, keyboard, state restoration, or motion behavior. Use the Android catalog and component tests for those checks. Screenshots do not change inventory status or coverage counts.

## Component details

- [PanelStack](site/showcase/panel-stack-400.png) shows the Filters pane pushed above Workspace, a labeled back action, and stack depth two in the real API 36 catalog. The inventory remains in progress.
- [Tree and TreeNode](site/showcase/tree-400.png) shows an expanded branch, selected Alpha child, disabled Beta child, and collapsed branches in the real API 36 catalog. The inventory remains in progress.

## Runnable operations examples

The catalog also contains two fictional screens assembled from Brace source:

- [Electric fleet](site/showcase/fleet-operations-wide.png) combines search, status filters, a selectable seven-column table, vehicle details, and charge progress. The [phone capture](site/showcase/fleet-operations-320.png) shows the same screen at 320 × 640; the table supports horizontal navigation.
- [Mission control](site/showcase/mission-control-dark-400.png) combines spacecraft metrics, a signal callout, an original Compose orbit illustration, selection, and a table. A [light capture](site/showcase/mission-control-400.png) shows the same sample at 400 × 800.

Fleet records, orbital positions, and telemetry are sample data. These screens do not add stable inventory coverage. Run the catalog to test the interactions.
