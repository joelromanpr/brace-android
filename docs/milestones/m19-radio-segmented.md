# M19 delivery slice: radio and segmented choices

**Status:** PR [#30](https://github.com/joelromanpr/brace-android/pull/30) is under review. The branch includes merged main `b412d49` (M12 Select/QueryList, M25 TopBar, and the live visual showcase). No Maven Central release exists. Released applicable coverage remains **0/121**.

## Scope

The pinned Blueprint `Radio`, `RadioGroup`, and `SegmentedControl` rows are present and applicable. M19 adds their controlled Compose APIs, semantic/component tokens, keyboard and TalkBack behavior, catalog samples, documentation, Android interaction tests, and an independent Maven consumer example. The separate `RadioCard` inventory row remains planned.

## Verification

| Gate | Result |
| --- | --- |
| Current post-showcase static checks | Token generation and coverage `--check`, Pages build (**147 inventory rows, 9 real catalog captures, 34 guides**), JavaScript syntax, and Git whitespace/conflict checks passed. Radio/Segmented, M12 Select/QueryList, and M25 TopBar remain in tokens, inventory, catalog, docs, and the independent consumer. Stable coverage remains 0/121 applicable rows. |
| Current broad Gradle and API checks | `./gradlew --offline build lint checkTokenGeneration checkInventory apiCheck` **passed** on the combined tree: 467 actionable tasks, 223 executed, 244 up to date. API baselines verified. |
| Current API 36 device checks | Focused `BraceRadioSegmentedTest` passed **18/18**, 0 failed/skipped, on the combined tree (71 Gradle tasks). |
| Current Maven consumer | Foundation, core, icons, and select published to an isolated Maven Local directory (**156 tasks**); the separate `verification/consumer-smoke` app assembled from those coordinates (**37 tasks**). |
| Current hosted checks | Required PR `verify` and API 34 `instrumented` jobs must pass on the pushed head before merge. Earlier hosted results below do not validate this integrated tree. |
| Prior post-M11 static checks | Passed: token generation and coverage `--check`, Pages build (147 rows, 29 guides), JavaScript syntax, and Git whitespace checks. Generated coverage remains 0/121 stable applicable rows. |
| Prior post-M11 broad gate | `./gradlew --offline build lint checkTokenGeneration checkInventory apiCheck` passed, **377 tasks**. This includes foundation token/contrast unit tests, release/debug compilation, catalog assembly, lint, and API compatibility snapshots. |
| Prior post-M11 Maven Local and independent consumer | Foundation, core, and icons `publishToMavenLocal` passed (115 tasks), including source and KDoc publication tasks. The separate `verification/consumer-smoke` project assembled from Maven coordinates (37 tasks) and uses both icon and radio APIs. Maven Central staging was not attempted. |
| Prior post-M11 API 36 interaction suite | `BraceRadioSegmentedTest` passed **18/18**, 0 failures, 0 errors, 0 skipped. The added tests verify a 48 × 48 dp radio target under caller `Modifier.size(24.dp)`, six filled segments in 160 dp with actual horizontal scrolling, and native selected/available/disabled radio nodes. |
| Prior installed 320×640 catalog visual/interaction QA | Passed before this rebase in light and dark high contrast: RadioGroup and SegmentedControl rendered within the narrow viewport and responded to real touch selection. This visual evidence does not establish post-M12/M25 visual parity. |
| Prior hosted CI on M11 base | Source commit `498ca37` passed GitHub [verify](https://github.com/joelromanpr/brace-android/actions/runs/36244237924/job/108410314574) in 5m25s and API 34 [instrumented](https://github.com/joelromanpr/brace-android/actions/runs/36244237924/job/108410314469) in 8m42s. Documentation-only head `9009221` also passed [verify](https://github.com/joelromanpr/brace-android/actions/runs/36244815759/job/108411950731) in 5m55s and [instrumented](https://github.com/joelromanpr/brace-android/actions/runs/36244815759/job/108411950578) in 8m43s. CodeQL was skipped. All these results predate M12/M25 integration. |
| Review and release | PR is under review; merge waits for current-head hosted gates. No Maven Central staging or release was attempted. |

## Known limits and next branch

The inventory now marks `Radio` and `RadioGroup` as Android adaptations: the standalone radio is Boolean-controlled, and the group uses String-valued options instead of Blueprint's numeric values or child Radio/RadioCard composition. The Android API uses hoisted values instead of Blueprint's uncontrolled `defaultValue`; browser-only DOM refs, CSS inline layout, and alternate ARIA group, toolbar, and menu roles map to native Compose modifiers and selection semantics. Filled segments scroll when equal distribution would make a choice narrower than 48 dp. Native API 36 nodes expose checked state on selected choices, a click action on available unselected choices, and no click action on disabled choices. Segment option icons are decorative; a spoken text label is required. Manual TalkBack speech, external hardware keyboard, broader pointer-device and form-factor checks remain for release acceptance. UIAutomator node inspection and Compose interaction tests cover the machine-verifiable semantics and controls. All three rows stay **in progress** until review and publication. OverflowList is in a separate M29 draft branch; `RadioCard` remains a separate planned row.
