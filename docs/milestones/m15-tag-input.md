# M15: Tag input

**State:** source complete on `joelromanpr/m15-tag-input`, integrated with main `7d6fa174` after Links. The pinned TagInput row is **in progress**; `firstRelease` is empty and no Maven Central artifact has shipped. Generated stable coverage remains **0/122** applicable rows.

## What this adds

- A controlled `BraceTagInput` with hoisted tag values and draft text, wrapping layout, and semantic input/tag tokens.
- Enter, IME, separators, bulk paste, optional blur submission, atomic validation, and duplicate policies.
- 48 dp remove actions, RTL-aware keyboard selection, localized accessibility labels, visible focus, and saveable state.
- Interactive catalog states and copyable code, [usage guide](../tag-input.md), Android tests, public API baseline, and a Maven-coordinate consumer example.

The Android adaptation uses `String` values and Compose text editing. Blueprint's React child nodes, HTML input props, DOM resize, and clipboard events have no separate Android controls. The [pinned baseline](../../BLUEPRINT_BASELINE.md) records the source version.

## Current validation

| Check | Result |
| --- | --- |
| Build | `build lint checkTokenGeneration checkBlueprintIconGeneration checkBlueprintNextIconGeneration checkInventory apiCheck :catalog:assembleDebug` passed **836 tasks** on the M20 main integration. Both catalog variants compiled. |
| Device | API 36 TagInput and Tag suites passed **23/23**, zero failures or skips. The first run found a test focus request made in touch mode; the blur test now requests keyboard mode before moving focus. The full rerun passed. |
| Site and inventory | Coverage generation and Pages build passed: **148 pinned rows, 14 real Android captures, 50 guides**. The TagInput row has implementation, sample, docs, and tests, and remains in progress. |
| Maven Local | All **eight** aligned snapshot artifacts published to an isolated Maven Local with AAR, sources, KDoc, POM, and Gradle metadata (**308 tasks**). A separate coordinate-only Android consumer assembled offline (**37 tasks**). Its first compile found a missing TagInput import in the consumer sample; adding the import resolved it. |
| Hosted | Current-head verify, API 34 instrumentation, and CodeQL are pending. Earlier branch failures do not validate this head. |

## Remaining acceptance

Manual TalkBack, physical keyboard, non-Latin IME composition, clipboard context-menu paste, large-text, and wider device review remain. Hosted checks, manual acceptance, and a public release are required before a stable row is recorded. The next select milestone is Suggest/MultiSelect.
