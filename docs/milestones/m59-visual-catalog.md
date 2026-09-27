# M59: App examples and clearer documentation

Status: in review. Branch: `joelromanpr/m59-visual-catalog`. No Maven Central release is associated with this milestone.

## Scope

- Added two runnable, fictional Compose workspaces to the Android catalog: an electric fleet with search, filters, a selectable seven-column table, and vehicle charge details; and mission control with spacecraft metrics, a signal alert, an original Compose orbit illustration, selection, and a table.
- Reworked the catalog entry screen with a clear Components/App examples choice, inventory-driven search and availability filters, padded chevrons, and soft status badges. The examples are labeled runnable; catalog status still comes from the generated inventory.
- Rebuilt the Pages landing around six new, unedited API 36 emulator captures. The gallery manifest records the immutable sample source commit `c5509d84c1fb065bdf92b150d44eeba8b8efbc82`, device, appearance, dimensions, inventory IDs, and copyable usage for every new capture. The site now has 21 recorded captures in total.
- Simplified the README and guide navigation. Component guides, coverage, attribution, and the milestone audit remain reachable; the audit list is collapsed out of the main reading path. The README's release count remains generated from the inventory.

## Checks

- `:catalog:assembleDebug` passed after integrating protected main `217b1b9f253bca79dcd57ea5d9071fbc9724e2a6` and the table selection API. The six final sample captures were inspected on API 36 at 320 × 640, 400 × 800, and 800 × 800, all at 160 dpi and font scale 1.0. The shared emulator was restored to 320 × 640 afterward.
- `python3 scripts/generate_coverage.py --check`, `node --check docs/site/app.js`, `node --check scripts/build-docs.mjs`, and `node scripts/build-docs.mjs` passed. The site build validated 148 inventory rows, 21 capture records, and 54 guides.
- The built site loaded at true 390 × 844 and 1440 × 900 Chrome viewports with `scrollWidth` equal to `innerWidth` in both. The Planned chip selected 60 of 148 rows at 390 px. The phone guide page shows a compact, collapsed navigation menu.

## Open work and limits

The new images are static evidence of layout, not proof of TalkBack, 2× text, pointer, or keyboard interaction. Component behavior is tracked in its own inventory row, guides, and tests. No inventory row was marked stable by this milestone: released coverage remains 0 of 122 tracked Android items. The repository still has no Maven Central release.

Next focused component branch: `joelromanpr/m36-semantic-content` ([draft PR #46](https://github.com/joelromanpr/brace-android/pull/46)), after this visual slice and its required checks settle.
