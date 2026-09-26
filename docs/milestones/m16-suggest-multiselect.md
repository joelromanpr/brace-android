# M16 delivery slice: Suggest and MultiSelect

**Status:** source implementation in progress. These two pinned rows belong to roadmap phase **M3 selects**. No Maven Central version has shipped; released applicable coverage remains **0/121**.

## Scope

- Add controlled `BraceSuggest` with `TextFieldValue` IME composition and explicit suggestion acceptance.
- Add controlled `BraceMultiSelect` with removable tags, filterable options, keyboard toggling, and a composition-preserving `TextFieldValue` query bridge.
- Record Blueprint to Android adaptations, document the APIs, add interactive catalog and independent consumer examples, and update the inventory and public API baseline.
- Size and place the shared `BracePopover` above the live keyboard on narrow screens. Both field triggers announce localized expanded/collapsed state.

## Verification

| Gate | Result |
| --- | --- |
| Token generation, inventory, docs, API, build, and lint | `build lint checkTokenGeneration checkInventory apiCheck` passed 467 tasks after the M12 rebase and final M16 source changes. Generated coverage remains 0/121 stable; the documentation site built 147 inventory rows and 31 guides. |
| API 36 Compose interaction and accessibility | Complete `brace-select` instrumentation suite passed **32/32**. It covers free text, real `InputConnection.setComposingText` followed by hardware Enter without a selection, Search action, external query replacement after moving the caret into the middle, touch, mouse, keyboard, disabled choices, restoration, RTL, large text, compact high contrast, 48dp targets, and supported automated accessibility checks. The library test hosts target API 36 to avoid Android's legacy-target modal blocking IME focus. |
| Shared popover regression | `BracePopoverTest` passed **5/5** on its full rerun, including keyboard dismissal and positioner behavior. Its first full run had a transient overlay-stack count assertion failure; the isolated test and full rerun passed. |
| Maven Local artifact and separate consumer | Foundation/core/icons/select AARs, sources, KDoc JARs, POMs, and Gradle metadata published to Maven Local. The independent consumer project assembled in **37 tasks** using only Maven coordinates. |
| 320dp catalog visual and touch checks | Inspected Suggest and MultiSelect at 320 × 640 dp with Gboard open after the keyboard placement patch. Both anchored surfaces and their final actions/options remained above the keyboard; Suggest Done and MultiSelect Central were visible. Field triggers filled the available width and removable tags fit. One-line option descriptions truncate visually at this width while full text remains in Android accessibility nodes. |
| Hosted CI, review, and release | Pending. No release or stable inventory row. |

## Known limits and next branch

The source APIs are not stable or released. Blueprint's Suggest and MultiSelect use an input or TagInput as the web popover target; these Android controls move editing into a focusable anchored popup. Manual TalkBack, physical keyboard and mouse review, additional form factors, and the full theme matrix remain before stable status. The next select slice is `joelromanpr/m17-command-palette` for the Omnibar row.
