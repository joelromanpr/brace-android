# M54: web mechanisms in Compose

**Status:** validated source slice on `joelromanpr/m54-web-mechanisms`; review and release pending. The [generated coverage ledger](../coverage.md) remains authoritative. No row is stable and no Maven Central version has shipped.

## Scope delivered in source

- Reconciled the pinned Blueprint 6.18.0 Classes, ResizeSensor, and BlueprintProvider documentation and source with the existing Compose theme, overlay, and shortcut APIs.
- Documented why CSS selectors and namespaces, a DOM resize observer wrapper, and a mandatory root React provider do not become separate Android components. The guide gives Compose examples and platform behavior boundaries.
- Added inventory evidence and interactive catalog demonstrations for token styling, measured size, and scoped theme/overlay/shortcut behavior. The rows retain their M0/M1/M2 roadmap assignments and remain **in progress**, not shipped coverage.
- Connected the guide and report to the generated Pages source and inventory-driven documentation links. The generator now counts a web mapping as documented when its inventory row links an existing guide: 12/24 are documented after this slice, while 0/24 are stable.

## Verification

| Gate | Result |
| --- | --- |
| Pinned Blueprint source review | Read the Classes, ResizeSensor, and BlueprintProvider MDX and provider source at commit `a60d4c92257612808fbfac81cfeee4fcba91a8b4`. |
| Inventory generation and `--check` | Passed: 147 rows, 0/121 applicable rows stable; no row was marked stable. |
| Documentation site build and JavaScript syntax | Passed after the M17 main integration: 40 guides and 11 real catalog captures, including this report and the mapping guide; `node --check` passed for the generator and catalog site script. |
| Gradle compile, lint, API, token, inventory, and catalog install | Passed offline after the M17 main integration: `./gradlew --offline build lint checkTokenGeneration checkInventory apiCheck` (563 actionable tasks). An earlier M54 source head assembled and installed the catalog on API 36. |
| Device accessibility and interaction | On the earlier M54 source head, used a 320 × 640 API 36 emulator to inspect light and dark high-contrast CSS samples; toggled the button state; observed ResizeSensor sample width change 144 → 224 px; opened and dismissed the scoped overlay and shortcut guide; tabbed to a preview control and sent Ctrl+R, incrementing its counter from 0 to 1. Checked a visible 2× text layout and Arabic RTL catalog mirroring. Existing theme and shortcut Android test sources compiled; their device tests were not rerun in this slice after main integration. |
| Hosted CI, review, and release | Pending. |

## Known limits and next branch

`Modifier.onSizeChanged` is an AndroidX Compose API, not a Brace artifact. The CSS mapping does not expose `Classes` constants, and the provider mapping deliberately uses independently scoped existing APIs. Manual TalkBack traversal, physical mouse review, reduced-motion behavior, and full keyboard focus order across all catalog controls remain unverified. The 2× text check covered the scoped provider sample after scrolling, not every catalog entry. No Android instrumentation tests were executed for this mapping slice; existing implementation tests were compiled. Keep these rows in progress pending review.

Review this M54 branch, then integrate the table viewport on `joelromanpr/m14-table-viewport`. Its selection, copying, editing, and later accessibility slices remain separate reviewable branches in the [roadmap](../../ROADMAP.md).
