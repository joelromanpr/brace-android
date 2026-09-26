# M28 delivery slice: entity title

**Status:** draft PR [#35](https://github.com/joelromanpr/brace-android/pull/35) on `joelromanpr/m28-entity-title`, rebased onto merged main `e03bde9` (M11 icons). The pinned `core-entitytitle` row remains **in progress**. There is no first release version or stable coverage claim.

## Scope

- `BraceEntityTitle` combines a spoken heading, optional decorative icon, subtitle, independent tags, rich visual slots, and caller-owned title navigation.
- Body, subtitle, title, and display prominence use Brace typography. Light, dark, high-contrast, brand, density, loading, hover, pressed, and keyboard-focus states use semantic and component tokens.
- A 48 dp interactive title target uses one native accessibility node for its label and action, with touch, mouse, and keyboard support. Constrained text can ellipsize while preserving the full accessible label and showing measured-overflow help. Static loading placeholders remove actions and have no animation.
- The catalog has interactive loading, width, prominence, and navigation controls. The separate Maven consumer project includes the API. The pinned inventory, generated coverage, Pages guide, and Android tests accompany the source.

## Verification

| Gate | Result |
| --- | --- |
| Pinned comparison | Reviewed Blueprint 6.18.0 EntityTitle MDX and public React props at commit `a60d4c92257612808fbfac81cfeee4fcba91a8b4`. |
| Post-M11 static integration | Token and coverage generation `--check` passed. Pages build passed with **147 inventory rows and 29 guides**; docs JavaScript syntax and Git whitespace checks passed. Generated coverage remains **0/121** applicable rows stable (0/94 components). Both M11 icons and M28 EntityTitle remain present in inventory, catalog, docs, and independent consumer. |
| Post-M11 broad Gradle and API check | `./gradlew --offline build lint checkTokenGeneration checkInventory apiCheck` **passed, 377 tasks**. The catalog assembled; API baselines match. |
| Post-M11 focused API 36 | `BraceEntityTitleTest` **passed 11/11**, 0 failures or skips, including the native labeled click node, loading state, layout, input, RTL, large text, and automated accessibility checks. |
| Post-M11 Maven consumer | Foundation, core, and icons `publishToMavenLocal` **passed, 115 tasks**. The independent `verification/consumer-smoke :app:assembleDebug` resolved the published coordinates and **passed, 37 tasks**. |
| Post-M11 hosted source checks | [Verify](https://github.com/joelromanpr/brace-android/actions/runs/36245559729/job/108413985864) **passed in 5m27s** and API 34 [instrumented](https://github.com/joelromanpr/brace-android/actions/runs/36245559729/job/108413985942) **passed in 8m22s** on source commit `fa67345`. A documentation-only evidence update follows; its final PR head must pass required checks before review completion. |
| Prior local focused Compose tests | Passed on the pre-M11 branch on API 36: **11 tests, 0 failures**. The suite covers native label/action nodes, loading removal, keyboard activation, independent tags, title hover, subtitle long press, layout wrapping, RTL, 2x text, high contrast, and automated accessibility checks (API 34+). |
| Prior local API and broad Gradle | `brace-foundation:apiDump` and `brace-core:apiDump` refreshed the original committed baselines. `./gradlew --offline build lint checkTokenGeneration checkInventory apiCheck` passed before this rebase. |
| Prior local Maven consumer and visual review | Foundation/core `publishToMavenLocal` and independent consumer `:app:assembleDebug` passed before this rebase. The catalog APK on a 320 × 640 API 36 emulator showed title activation changing the count from 0 to 1, loading hiding title/tags, and no horizontal overflow. This is not post-rebase visual evidence. |
| Prior hosted PR checks | [Verify](https://github.com/joelromanpr/brace-android/actions/runs/36240856421/job/108400947371) passed on the pre-M11 head. API 34 [instrumented](https://github.com/joelromanpr/brace-android/actions/runs/36240856421/job/108400947542) stopped before tests: the old workflow requested a 2048M emulator disk and had 6990.20 MB available versus 7372.80 MB needed. The rebased source commit later passed both hosted jobs with the merged M11 workflow cleanup and smaller emulator disk. |

## Adaptations and limits

Blueprint's React heading element becomes token-backed Compose prominence and Android heading semantics. `titleURL` becomes a caller-owned `onTitleClick` navigation callback. React icon, tag, and rich text nodes become Compose slots; HTML class/ref props become `Modifier`. Rich visual slots require separate spoken strings and own their truncation. The optional icon is decorative; actionable tags have independent focus stops. The loading state uses static placeholders, so reduced motion does not need a special animation path. The core instrumented-test APK now targets SDK 36 to avoid Android's old-target compatibility dialog obscuring native accessibility tests; this setting does not change the library artifact. Title hover and subtitle touch long-press overflow help passed. Subtitle hover did not trigger under the API 36 emulator pointer test and remains an open interaction check. Manual TalkBack, tablet, and final documentation-only head verification remain open.

The next focused branch is `joelromanpr/m29-overflow-list` for the pinned navigation OverflowList row. Select, date/time, the full icon glyph catalog, and the data table remain planned or in separate active slices as recorded by the inventory. Full Blueprint parity is not claimed.
