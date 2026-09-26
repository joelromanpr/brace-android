# M9 delivery slice: labels and control groups

**Status:** source implementation in progress in draft [PR #19](https://github.com/joelromanpr/brace-android/pull/19) on `joelromanpr/m9-form-layout`, rebased onto the M8 squash merge. The two pinned Blueprint 6.18.0 rows belong to roadmap phase **M1 forms**. Both remain **in progress** and have no first Maven Central release; released applicable coverage remains **0/121** in the [generated ledger](../coverage.md).

## Scope

- `BraceFieldLabel` associates a visual label with one focusable control through a modifier slot, with label tap-to-focus, spoken semantics, disabled styling, large-text layout, and an accessible target.
- `BraceControlGroup` lays out separate controls horizontally or vertically, with token spacing, logical RTL order, equal fill, per-item fixed sizing, and independent child actions.
- Pinned inventory rows, interactive catalog samples, a Pages guide, and an independent Maven consumer use are updated in the same slice.

## Verification

| Gate | Result |
| --- | --- |
| Core Android test source and catalog Kotlin compilation | Passed `:brace-core:compileDebugAndroidTestKotlin :catalog:compileDebugKotlin` (52 tasks). |
| Focused API 36 device interaction tests | **6 passed, 0 failed, 0 skipped** in `BraceFormLayoutTest` on the 320×640 px, 160 dpi emulator. Covers touch, mouse, keyboard traversal, RTL, large text, separate TalkBack actions, and a Compose automated accessibility check. |
| Catalog visual review | Label and horizontal and vertical ControlGroup samples fit the 320 dp viewport with readable text and no clipping or overlap. |
| Documentation site and generated coverage | `node scripts/build-docs.mjs` built 147 inventory rows and 23 guides after integrating M8; `python3 scripts/generate_coverage.py --check` passed with 0/121 applicable rows stable. |
| API compatibility and token generation | `:brace-core:apiDump` updated the public API baseline; `:brace-core:apiCheck` passed in a separate invocation. `python3 scripts/generate_tokens.py --check` passed. |
| Full build and lint | Pending integrated M9 CI run. |
| Maven Local publication and independent consumer | Foundation and core publication passed, including AAR, POM, Gradle metadata, sources JAR, and documentation JAR. Independent consumer `:app:assembleDebug --offline` passed (37 tasks). |
| Hosted CI and pull request review | The initial M7-based draft PR passed `verify` and `instrumented`; both must rerun on the integrated M8 base before merge. Independent source review and final local broad gate remain. |

## Known limits and next branch

These are source APIs until review, device evidence, and release. `BraceFieldLabel` requires its supplied modifier on the child's focusable node and does not disable the child automatically. `BraceControlGroup` arranges independent controls and does not join borders, infer selection, or change a child's own state. Manual TalkBack, physical keyboard, pointer, visual, and form-factor checks remain before stable status. The next focused form slice should cover the pinned NumericInput and TagInput rows, then the remaining core, select, datetime, icons, and table families.
