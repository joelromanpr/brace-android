# M12 delivery slice: Select and QueryList

**Status:** source implementation in progress. These two pinned rows belong to roadmap phase **M3 selects**. No Maven Central version has shipped; released applicable coverage remains **0/121**.

## Scope

- Add the `brace-select` Android library and aligned local Maven coordinates.
- Implement typed `BraceSelectOption`, controlled `BraceSelect`, and restorable headless `rememberBraceQueryListState`.
- Add versioned select component tokens, catalog examples, documentation, API baseline, inventory evidence, Android interaction tests, and independent consumer usage.

## Verification

| Gate | Result |
| --- | --- |
| Token generation, inventory, docs, API and local build/lint | Passed on the reviewed diff: `build lint checkTokenGeneration checkInventory apiCheck` (467 actionable Gradle tasks), with a separately generated and checked select API baseline. Token and coverage checks passed; the site builds 147 inventory rows and 29 guides. |
| Focused API 36 Compose interaction and accessibility tests | Passed **14/14** on API 36 with zero failures, errors, or skips. This includes validation, viewport navigation, rapid Down then Space activation, IME composition guard, localized trigger expansion state, and real Android accessibility nodes for enabled and disabled popup options. Earlier behavior remains covered: filtering, custom predicate, controlled selection, restoration, touch/mouse, RTL high contrast, large text, 48dp targets, and automated Compose accessibility. A real composing-text InputConnection integration test remains open. |
| Maven Local artifact and separate consumer | Passed on the reviewed diff: aligned foundation/core/icons/select Maven Local artifacts with sources, KDoc JAR, POM, and Gradle metadata; the independent consumer assembled from Maven coordinates only. |
| 320dp catalog visual and touch checks | Select detail and open popup visually inspected at 320 × 640 dp; the popup repositioned above the software keyboard without clipping its options. Touch selection updated the sample and dismissed the popup. Hardware keyboard and TalkBack manual review remain. |
| Hosted CI, review, and release | Pending. |

## Known limits and next branch

These APIs are not stable or released. Manual TalkBack, physical keyboard and mouse, large text, RTL, theme matrix, and additional form factors remain before stable status. Current state supplies exact text filtering and a caller predicate; ranked/fuzzy search belongs in an app predicate. MultiSelect, Suggest, and command palette interaction are separate planned slices. The next concrete branch after this slice is `joelromanpr/m13-datetime-picker`; Suggest, MultiSelect, and command palette remain separate select slices.
