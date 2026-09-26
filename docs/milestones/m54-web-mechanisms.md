# M54: web mechanisms in Compose

**Status:** source slice on `joelromanpr/m54-web-mechanisms`; review, Gradle validation, device validation, and release pending. The [generated coverage ledger](../coverage.md) remains authoritative. No row is stable and no Maven Central version has shipped.

## Scope delivered in source

- Reconciled the pinned Blueprint 6.18.0 Classes, ResizeSensor, and BlueprintProvider documentation and source with the existing Compose theme, overlay, and shortcut APIs.
- Documented why CSS selectors and namespaces, a DOM resize observer wrapper, and a mandatory root React provider do not become separate Android components. The guide gives Compose examples and platform behavior boundaries.
- Added inventory evidence and interactive catalog demonstrations for token styling, measured size, and scoped theme/overlay/shortcut behavior. The rows retain their M0/M1/M2 roadmap assignments and remain **in progress**, not shipped coverage.
- Connected the guide and report to the generated Pages source and inventory-driven documentation links.

## Verification

| Gate | Result |
| --- | --- |
| Pinned Blueprint source review | Read the Classes, ResizeSensor, and BlueprintProvider MDX and provider source at commit `a60d4c92257612808fbfac81cfeee4fcba91a8b4`. |
| Inventory generation and `--check` | Passed: 147 rows, 0/121 applicable rows stable; no row was marked stable. |
| Documentation site build and JavaScript syntax | Passed: 33 guides, including this report and the mapping guide; `node --check` passed for the generator and catalog site script. |
| Gradle compile, lint, API check, and catalog install | Not run in this documentation slice while the shared Gradle/device lane is occupied. |
| Device accessibility and interaction | Not run. Existing theme, overlay, and shortcut tests belong to their implementation slices; the new catalog combinations still need a focused review. |
| Hosted CI, review, and release | Pending. |

## Known limits and next branch

`Modifier.onSizeChanged` is an AndroidX Compose API, not a Brace artifact. The CSS mapping does not expose `Classes` constants, and the provider mapping deliberately uses independently scoped existing APIs. The catalog sample must still be compiled and inspected on a small phone at large text and in RTL, light/dark/high-contrast modes, with touch, keyboard, mouse, and TalkBack. Static documentation checks alone cannot validate those interactions.

Complete the pending compile and device review on this M54 branch, and apply any findings here before review. The next focused implementation branch is `joelromanpr/m56-table-accessibility`; the larger component plan continues in the [roadmap](../../ROADMAP.md).
