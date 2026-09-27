# Visual showcase

The [gallery](site/index.html#showcase) contains captures of the actual Compose catalog on an Android emulator. It is a view into work in progress, not a release announcement or a browser implementation of the Android controls. Names, families, and availability labels come from the pinned [coverage inventory](coverage.md). A card marked **Draft branch capture** records source under review; its inventory row may already have merged code or may still be planned on `main`. The release checklist, tests, and `firstRelease` field determine whether a row becomes stable.

Each capture records its source branch and full commit, emulator, pixel dimensions, date, appearance, density, inventory row IDs, and a Kotlin usage example. The image is linked at full resolution. The source link points to the catalog implementation at the exact captured commit so a later source change cannot silently rewrite the provenance of an earlier image.

## Capture and review a new state

1. Build and install the catalog from the branch you are recording: `./gradlew :catalog:installDebug`. Launch `io.github.joelromanpr.brace.catalog/.CatalogActivity` on a named emulator. Record the full `git rev-parse HEAD` value. Commit the source before capturing so the image can be tied to a reviewable version.
2. Open the relevant inventory row in the catalog and set the theme, contrast, brand, and density shown in the image. Capture with `adb exec-out screencap -p > capture.png`. Crop or redact only when needed for personal data, and disclose that change in the caption. Do not reconstruct UI in HTML or image editing software.
3. Inspect the PNG at full size. Check legibility, clipped content, system bars, keyboard overlap where relevant, and whether the image actually shows the named component. Keep useful light, dark, and high-contrast states; a single screenshot does not prove accessibility.
4. Add the PNG under `docs/site/showcase/` and a record in `docs/site/showcase/captures.json`. Use actual inventory IDs, a descriptive alternative text, the source branch and commit, and a copyable usage example from the catalog. Mark a branch that is not merged as `draft`.
5. Run `python3 scripts/generate_coverage.py --check`, `node scripts/build-docs.mjs`, and `node --check docs/site/app.js`. The site build validates the capture IDs against the generated inventory and checks PNG dimensions and provenance fields. Review the responsive gallery and full-size image links in a browser.

The static site cannot demonstrate TalkBack, touch, mouse, keyboard, state restoration, or motion behavior. Use the Android catalog and component tests for those checks. Screenshots do not change inventory status or coverage counts.

## Runnable operations examples

The catalog also contains two fictional screens assembled from Brace source:

- [Electric fleet](site/showcase/fleet-operations-wide.png) combines search, status filters, a selectable seven-column table, vehicle details, and charge progress. The [phone capture](site/showcase/fleet-operations-320.png) shows the same screen at 320 × 640; the table supports horizontal navigation.
- [Mission control](site/showcase/mission-control-dark-400.png) combines spacecraft metrics, a signal callout, an original Compose orbit illustration, selection, and a table. A [light capture](site/showcase/mission-control-400.png) shows the same sample at 400 × 800.

Fleet records, orbital positions, and telemetry are sample data. These screens do not add stable inventory coverage. Their source commit and emulator settings are recorded in the capture manifest; run the catalog to test the interactions.
