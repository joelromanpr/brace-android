# M25 delivery slice: top app bar

**Status:** rebased onto merged M12 (`f457366`) after M11 (`e03bde9`), locally verified; PR #33 is in review on `joelromanpr/m25-top-bar`. Four pinned Blueprint Navbar family rows are **in progress**. Stable coverage stays **0/121** applicable rows; no release version is assigned.

## Scope

- `BraceTopBar`, `BraceTopBarGroup`, `BraceTopBarTitle`, and `BraceTopBarDivider` in `brace-core`.
- Platform-neutral top-bar color and dimension tokens generated into the Kotlin token API.
- Logical start/end slots for RTL, heading semantics, decorative divider semantics, 48 dp child action targets, and an optional elevation token.
- Interactive catalog example, copyable usage for all four inventory rows, Pages guide, Android interaction tests, and a separate Maven consumer example.

## Verification

| Gate | Result |
| --- | --- |
| Token generation | Passed `python3 scripts/generate_tokens.py --check`. |
| Inventory and documentation | Passed `python3 scripts/generate_coverage.py --check`, `node scripts/build-docs.mjs`, and `node --check docs/site/app.js` (147 rows, 31 guides, zero stable) after the M12 rebase. The TopBar guide and milestone report are now linked from Pages navigation. |
| Kotlin compilation, API baseline, lint | Passed post-M12 `build lint checkTokenGeneration checkInventory apiCheck :catalog:assembleDebug` (467 tasks, zero failures), including core AndroidTest and catalog compilation. The combined Select/TopBar foundation API baseline was regenerated and `apiCheck` passed. |
| API 36 interaction and accessibility checks | Passed post-M12 focused `BraceTopBarTest`, 3 tests, 0 failures. Checks cover touch, mouse, keyboard, heading semantics, density switching, RTL, large text, high contrast, target size, and automated accessibility on API 36. |
| Maven Local and independent consumer | Passed post-M12 aligned foundation/core/icons/select Maven Local publication (156 tasks) and independent coordinate-only `verification/consumer-smoke :app:assembleDebug` (37 tasks). AAR, sources, KDoc, POM, and Gradle metadata were produced locally. |
| 320×640 light/dark/high-contrast catalog review | Passed visual review on API 36: Navbar list entry, light detail, live Edit/Done change, dark high-contrast detail, raised variant, and copyable usage. |
| Hosted CI and PR review | PR #33 is ready for review. Hosted [`verify` and API 34 `instrumented` passed](https://github.com/joelromanpr/brace-android/actions/runs/36243928441) on the M11-based head; the M12-based head needs its own hosted rerun before merge. No merge is claimed. |

## Limits and next branch

The screen owns navigation state and uses `Scaffold(topBar = ...)` for fixed placement and host window insets. The bar intentionally leaves action overflow to the screen; use a menu when the title and actions cannot fit at large text. Manual TalkBack and release validation are outstanding. The next concrete integration branch is `joelromanpr/m27-tabs`, already in draft review for Tabs/Tab/TabPanel/TabsExpander as one cohesive state and focus model.
