# M8 delivery slice: form fields and editable text

**Status:** merged through [PR #18](https://github.com/joelromanpr/brace-android/pull/18) as commit `ee121b4` after local verification and both required hosted checks passed. This slice was based on the squash merge of M7. The three pinned rows remain **in progress**; there is no stable component coverage or Maven Central release from this slice. The [generated ledger](../coverage.md) owns all coverage counts.

## Scope

- `BraceFormField` adapts Blueprint FormGroup to a Compose control modifier slot, with label-tap focus, helper and error semantics, required announcement, intent styling, and an inline layout that stacks on narrow or large-text screens.
- `BraceTextArea` supplies controlled multiline text, IME callbacks, token-driven intent and size states, fixed or bounded auto-resizing, a scrolling viewport, and editable shortcut suppression.
- `BraceEditableText` supplies inline display/edit behavior with controlled value, confirm/cancel and blur handling, single/multiline hardware shortcuts, and a TalkBack edit action.
- The pinned inventory, interactive catalog, documentation site, and independent Maven consumer are updated in the same reviewable slice.

## Verification

| Gate | Current result |
| --- | --- |
| Core Kotlin and AndroidTest compilation | Passed locally for the three source/test families on the pinned offline toolchain. The combined focused API 36 run passed 25/25 tests with zero failures, errors, or skips. |
| FormField and TextArea API 36 device tests | Passed: 14/14 focused tests (6 FormField, 8 TextArea), zero failures, errors, or skips. Covers merged semantics, 48dp label focus target, narrow and large-text fallback, RTL, dark high contrast, controlled editing/IME, restoration, size/intent states, viewport cap and internal touch scrolling, disabled/read-only semantics, shortcut suppression, and automated Compose accessibility. |
| EditableText API 36 device tests | Passed on the earlier diff: 11/11 focused tests, zero failures, errors, or skips, including localized default edit-action, focus, restoration, and IME regressions. The final-diff disable/re-enable edit-session regression passed 1/1 on API 36. |
| Full core device suite and accessibility/manual visual review | Earlier API 36 full run: 142/143 core tests passed, 0 failures/errors, 1 pre-existing native Back injection skip; foundation 2/2 passed. The post-fix FormField 6/6 suite passed; final-diff hosted API 34 instrumentation passed: foundation 2 tests; core 146 finished with one previously documented native Back injection skip. Three catalog examples visually checked at 320 × 640 dp with no clipping or overlap. Manual TalkBack review remains. |
| Generated coverage, catalog, Pages build, API/lint checks | `build lint checkTokenGeneration checkInventory apiCheck` passed locally (287 Gradle tasks). Inventory and coverage generation check passed (147 rows; 0/121 applicable stable); catalog compiled; Pages source built with 21 guides and JavaScript syntax checked. |
| Maven Local artifacts and independent consumer | Passed: foundation/core AAR, sources, KDoc JAR, POM, and Gradle metadata published locally; separate consumer project assembled from Maven coordinates only. |
| Hosted CI, review, and PR merge | Initial hosted `verify` passed. Initial API 34 `instrumented` ran 145 tests with one failure: an exact fixed-viewport dp equality differed by 0.000011 dp across measurement paths. The assertion now tolerates 0.01 dp and passed a focused API 36 rerun. Independent review then found an uncontrolled edit-session disable bug and a standalone error example conveyed by color alone; both are fixed in source/sample with a new regression. The final-diff hosted `verify` and API 34 `instrumented` checks both passed; PR #18 was squash merged. The core hosted run finished 146 tests with the documented native Back skip. |

## Limits and next branch

The form wrapper does not disable an arbitrary child or infer its visual error state; callers pass those values to the child. TextArea uses a bounded Compose viewport rather than a browser resize handle. EditableText focused hardware and restoration tests pass; broader device and manual review remain. Manual TalkBack, pointer-device, and cross-device visual review are still needed. The next focused branch is `joelromanpr/m9-form-layout` for pinned Label and ControlGroup; NumericInput follows in `joelromanpr/m10-numeric-input`; the remaining core, select, datetime, icons, and table rows stay on the full coverage roadmap.
