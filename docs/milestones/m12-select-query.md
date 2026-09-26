# M12 delivery slice: Select and QueryList

**Status:** source implementation in progress on `joelromanpr/m12-select-query`, rebased onto merged M11 main `e03bde9`. These two pinned rows belong to roadmap phase **M3 selects**. No Maven Central version has shipped; released applicable coverage remains **0/121**.

## Scope

- Add the `brace-select` Android library and aligned local Maven coordinates.
- Implement typed `BraceSelectOption`, controlled `BraceSelect`, and restorable headless `rememberBraceQueryListState`.
- Add versioned select component tokens, catalog examples, documentation, API baseline, inventory evidence, Android interaction tests, and independent consumer usage.

## Verification

| Gate | Result |
| --- | --- |
| Token generation, inventory, docs, API and local build/lint | Passed after the M11 squash rebase: `build lint checkTokenGeneration checkInventory apiCheck` (467 actionable Gradle tasks), including the select API baseline. Token generation, coverage, JSON/JS syntax, conflict markers, and docs checks also pass; the site builds 147 inventory rows and 29 guides. |
| Focused API 36 Compose interaction and accessibility tests | Passed **14/14** after the rebase on API 36 with zero failures, errors, or skips. This includes validation, viewport navigation, rapid Down then Space activation, IME composition guard, localized trigger expansion state, and real Android accessibility nodes for enabled and disabled popup options. Earlier behavior remains covered: filtering, custom predicate, controlled selection, restoration, touch/mouse, RTL high contrast, large text, 48dp targets, and automated Compose accessibility. A real composing-text InputConnection integration test remains open. |
| Maven Local artifact and separate consumer | Passed after the rebase: aligned foundation/core/icons/select Maven Local artifacts with sources, KDoc JAR, POM, and Gradle metadata (156 Gradle tasks); the independent consumer assembled from Maven coordinates only with `:app:assembleDebug --offline` (37 tasks). |
| 320dp catalog visual and touch checks | After the rebase, Select and QueryList details were visually inspected at 320 × 640 dp and labeled in progress. Select search narrowed to West; the popup repositioned above the software keyboard with the option fully visible, and touch selection updated the controlled value and dismissed the popup. QueryList touch selection updated its controlled value. Hardware keyboard and TalkBack manual review remain. |
| Hosted CI, review, and release | Pending. |

## Known limits and next branch

These APIs are not stable or released. Manual TalkBack, physical keyboard and mouse, large text, RTL, theme matrix, and additional form factors remain before stable status. Current state supplies exact text filtering and a caller predicate; ranked/fuzzy search belongs in an app predicate. MultiSelect, Suggest, and command palette interaction are separate planned slices. The next concrete branch after this slice is `joelromanpr/m13-datetime-picker`; Suggest, MultiSelect, and command palette remain separate select slices.
