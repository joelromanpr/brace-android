# M25 delivery slice: top app bar

**Status:** rebased onto merged M11 (`e03bde9`), locally verified; draft review on `joelromanpr/m25-top-bar`. Four pinned Blueprint Navbar family rows are **in progress**. Stable coverage stays **0/121** applicable rows; no release version is assigned.

## Scope

- `BraceTopBar`, `BraceTopBarGroup`, `BraceTopBarTitle`, and `BraceTopBarDivider` in `brace-core`.
- Platform-neutral top-bar color and dimension tokens generated into the Kotlin token API.
- Logical start/end slots for RTL, heading semantics, decorative divider semantics, 48 dp child action targets, and an optional elevation token.
- Interactive catalog example, copyable usage for all four inventory rows, Pages guide, Android interaction tests, and a separate Maven consumer example.

## Verification

| Gate | Result |
| --- | --- |
| Token generation | Passed `python3 scripts/generate_tokens.py --check`. |
| Inventory and documentation | Passed `python3 scripts/generate_coverage.py --check`, `node scripts/build-docs.mjs`, and `node --check docs/site/app.js` (147 rows, 27 guides, zero stable) after the M11 rebase. |
| Kotlin compilation, API baseline, lint | Passed post-rebase `build lint checkTokenGeneration checkInventory apiCheck publishToMavenLocal` (395 tasks, zero failures), including core AndroidTest and catalog compilation. |
| API 36 interaction and accessibility checks | Passed post-rebase focused `BraceTopBarTest`, 3 tests, 0 failures. Checks cover touch, mouse, keyboard, heading semantics, density switching, RTL, large text, high contrast, target size, and automated accessibility on API 36. |
| Maven Local and independent consumer | Passed post-rebase aligned foundation/core/icons Maven Local publication and independent `verification/consumer-smoke :app:assembleDebug` against those local artifacts. |
| 320×640 light/dark/high-contrast catalog review | Passed visual review on API 36: Navbar list entry, light detail, live Edit/Done change, dark high-contrast detail, raised variant, and copyable usage. |
| Hosted CI and PR review | Draft PR #33 is open. Earlier hosted `verify` passed on the pre-rebase head; required checks on the rebased head are pending push and rerun. No merge is claimed. |

## Limits and next branch

The screen owns navigation state and uses `Scaffold(topBar = ...)` for fixed placement and host window insets. The bar intentionally leaves action overflow to the screen; use a menu when the title and actions cannot fit at large text. Manual TalkBack and release validation are outstanding. The next concrete integration branch is `joelromanpr/m27-tabs`, already in draft review for Tabs/Tab/TabPanel/TabsExpander as one cohesive state and focus model.
