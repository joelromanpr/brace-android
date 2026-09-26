# M19 delivery slice: radio and segmented choices

**Status:** draft PR [#30](https://github.com/joelromanpr/brace-android/pull/30) on `joelromanpr/m19-radio-segmented`, rebased onto merged main `e03bde9` (M11 icons). No Maven Central release exists. Released applicable coverage remains **0/121**.

## Scope

The pinned Blueprint `Radio`, `RadioGroup`, and `SegmentedControl` rows are present and applicable. M19 adds their controlled Compose APIs, semantic/component tokens, keyboard and TalkBack behavior, catalog samples, documentation, Android interaction tests, and an independent Maven consumer example. The separate `RadioCard` inventory row remains planned.

## Verification

| Gate | Result |
| --- | --- |
| Post-M11 static checks | Passed: token generation and coverage `--check`, Pages build (147 rows, 29 guides), JavaScript syntax, and Git whitespace checks. Generated coverage remains 0/121 stable applicable rows. |
| Post-M11 broad gate | `./gradlew --offline build lint checkTokenGeneration checkInventory apiCheck` passed, **377 tasks**. This includes foundation token/contrast unit tests, release/debug compilation, catalog assembly, lint, and API compatibility snapshots. |
| Post-M11 Maven Local and independent consumer | Foundation, core, and icons `publishToMavenLocal` passed (115 tasks), including source and KDoc publication tasks. The separate `verification/consumer-smoke` project assembled from Maven coordinates (37 tasks) and uses both icon and radio APIs. Maven Central staging was not attempted. |
| Post-M11 API 36 interaction suite | `BraceRadioSegmentedTest` passed **18/18**, 0 failures, 0 errors, 0 skipped. The added tests verify a 48 × 48 dp radio target under caller `Modifier.size(24.dp)`, six filled segments in 160 dp with actual horizontal scrolling, and native selected/available/disabled radio nodes. |
| Prior installed 320×640 catalog visual/interaction QA | Passed before this rebase in light and dark high contrast: RadioGroup and SegmentedControl rendered within the narrow viewport and responded to real touch selection. This visual evidence does not establish post-rebase visual parity. |
| Hosted CI, review, release | Hosted checks must rerun on the pushed rebased commit. PR remains draft and no release was staged. |

## Known limits and next branch

The inventory now marks `Radio` and `RadioGroup` as Android adaptations: the standalone radio is Boolean-controlled, and the group uses String-valued options instead of Blueprint's numeric values or child Radio/RadioCard composition. The Android API uses hoisted values instead of Blueprint's uncontrolled `defaultValue`; browser-only DOM refs, CSS inline layout, and alternate ARIA group, toolbar, and menu roles map to native Compose modifiers and selection semantics. Filled segments scroll when equal distribution would make a choice narrower than 48 dp. Native API 36 nodes expose checked state on selected choices, a click action on available unselected choices, and no click action on disabled choices. Segment option icons are decorative; a spoken text label is required. Manual TalkBack speech, external hardware keyboard, broader pointer-device and form-factor checks remain for release acceptance. UIAutomator node inspection and Compose interaction tests cover the machine-verifiable semantics and controls. All three rows stay **in progress** until review and publication. The next assigned core branch is `joelromanpr/m29-overflow-list` for the pinned OverflowList row; `RadioCard` remains a separate planned row.
