# M8 delivery slice: form fields and editable text

**Status:** source and focused FormField/TextArea verification in progress on `joelromanpr/m8-form-text`. This branch is stacked behind the earlier milestones. The three pinned rows remain **in progress**; there is no stable component coverage or Maven Central release from this slice. The [generated ledger](../coverage.md) owns all coverage counts.

## Scope

- `BraceFormField` adapts Blueprint FormGroup to a Compose control modifier slot, with label-tap focus, helper and error semantics, required announcement, intent styling, and an inline layout that stacks on narrow or large-text screens.
- `BraceTextArea` supplies controlled multiline text, IME callbacks, token-driven states, fixed or bounded auto-resizing, and a scrolling viewport.
- `BraceEditableText` supplies inline display/edit behavior with controlled value, confirm/cancel and blur handling, single/multiline hardware shortcuts, and a TalkBack edit action.
- The pinned inventory, catalog, documentation, and independent Maven consumer are planned for this same pull request after the M8 branch is rebased onto M7, so generated files do not conflict with prior slices.

## Verification

| Gate | Current result |
| --- | --- |
| Core Kotlin and AndroidTest compilation | Passed locally for the three source/test families on the pinned offline toolchain. |
| FormField and TextArea API 36 device tests | Passed: seven distinct focused tests, zero failures or skips. Covers FormField/TextArea merged semantics, label focus, large text, RTL, dark high contrast, controlled editing/IME, restoration, resize bounds, and automated Compose accessibility. |
| EditableText API 36 device tests | In progress. The first focused run exposed behavior failures; the implementation and tests are being corrected before a passing claim. |
| Full core device suite and accessibility/manual visual review | Pending after source fixes and branch integration. |
| Generated coverage, catalog, Pages build, API/lint checks | Pending M7 rebase and same-PR integration. |
| Maven Local artifacts and independent consumer | Pending integrated build and publication to Maven Local. |
| Hosted CI, review, and PR merge | Pending. |

## Limits and next branch

The form wrapper does not disable an arbitrary child or infer its visual error state; callers pass those values to the child. TextArea uses a bounded Compose viewport rather than a browser resize handle. EditableText hardware and restoration behavior remain under device verification. Manual TalkBack, pointer, visual, and small-screen review are still needed. The next focused branch should be `joelromanpr/m9-input-controls`, beginning with pinned ControlGroup and NumericInput rows while M8 review completes; the remaining core, select, datetime, icons, and table rows stay on the full coverage roadmap.
