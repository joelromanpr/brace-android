# M20 delivery slice: Link and AnchorButton

**Status:** draft source branch integrated with `main` at `286d04e` (through M17 Spinner/Skeleton, M13 DatePicker/DateField, M35 Blueprint icons, M54 web mappings, and the live visual gallery) in `joelromanpr/m20-links`. The Link and AnchorButton rows remain **in progress**, `firstRelease` is null, and generated stable coverage remains **0/121** applicable rows. No Maven Central release is claimed.

## Scope

- `BraceLinkDestination.Uri` and `.Action` make URI opening and app navigation explicit. A caller may override URI opening for Custom Tabs or deep links.
- `BraceLink` provides token colors, underline modes, disabled state, 48 dp touch target, focus, hover, keyboard activation, RTL, and destination announcement.
- `BraceLinkButton` composes existing Button visual tokens and adds destination semantics while suppressing disabled/loading navigation.
- The inventory, catalog, Pages guide, focused Android tests, API baseline, and independent Maven consumer are updated in this branch.

## Verification

| Gate | Result |
| --- | --- |
| Pinned Blueprint source and inventory review | Reviewed Link and AnchorButton docs/source from `@blueprintjs/core@6.18.0`, commit `a60d4c92257612808fbfac81cfeee4fcba91a8b4`. |
| Current static inventory and Pages checks | Passed after M54 integration: token and coverage generation `--check`, `node scripts/build-docs.mjs` (**147 rows, 11 real catalog captures, 43 guides**), `node --check docs/site/app.js`, Link contrast check, and Git whitespace/conflict checks. M13 datetime, M17 loading, M19 radio/segmented, and M20 links remain in the combined ledger, catalog, and guides. |
| Current build, lint, API, and catalog | `build lint checkTokenGeneration checkInventory apiCheck :catalog:assembleDebug` passed **564 tasks** on the earlier M17-integrated tree. After extracting the two Link catalog samples from the oversized Compose dispatch method, the M54-integrated `build lint checkTokenGeneration checkBlueprintIconGeneration checkInventory apiCheck :catalog:assembleDebug` gate passed **655 tasks**. The initial compile exposed a Kotlin method-size compiler error; the sample extraction fixed it, and debug/release catalog compilation passed. |
| Current API 36 focused device tests | `BraceLinkTest` passed **10/10**, zero failures or skips (**71 tasks**), after M17 integration; this source remains unchanged in the M54 merge. It covers URI and action routing, disabled/loading suppression, accessible click node, touch bounds, keyboard focus, RTL, large text, dark high contrast, and automated accessibility checks. |
| Current Maven Local and independent consumer | On the earlier M17-integrated tree, foundation, core, icons, select, and datetime each published an AAR, sources JAR, Javadoc JAR, POM, and Gradle module metadata into a fresh isolated Maven Local repository (**193 tasks**). The separate coordinate-only consumer assembled (**37 tasks**). On the M54-integrated tree, six AAR modules (including the optional Blueprint icon pack) published with sources, Javadoc, POM, and Gradle metadata to an isolated Maven Local repository (**234 tasks**); the coordinate-only consumer assembled (**37 tasks**) from those artifacts. |
| Earlier broad core device results | On the pre-M17 M12/M25-based head, the full core suite finished **182 tests**, zero failures, and one known native Back injection skip in `BraceOverlayTest.deviceBackDismissesTopmostOverlay`. This is historical evidence, not a current full-suite claim. |
| Catalog visual and manual TalkBack | The earlier 320 × 640 light and dark high-contrast review found no clipping or overlap; Action and URI examples updated their status. Post-M17 visual review and manual TalkBack remain. |
| Hosted CI | Required hosted checks are pending for this newly integrated draft head. The previous M17-integrated draft head `aefc4dc` passed verify, API34 instrumented, and CodeQL; this does not validate the M54-integrated tree. Earlier M11-based head `40b5d25` passed [verify](https://github.com/joelromanpr/brace-android/actions/runs/36244078237/job/108409883030) and [instrumented](https://github.com/joelromanpr/brace-android/actions/runs/36244078237/job/108409883184); those results do not validate the current tree. |

## Remaining work

Link semantics are a localized announcement and click label because Compose UI 1.9 lacks a public link role. Inline link spans inside a paragraph are still planned; `BraceLink` is currently a standalone 48 dp control. `BraceLinkButton` shares the current Button styles, which do not include Blueprint's minimal button, success/warning button intents, button size/fill/alignment, or a progress indicator. Browser-only anchor attributes and target behaviors are represented by Android URI/app actions rather than separate components. The next concrete step is hosted checks on the integrated draft branch, followed by current catalog visual review and manual accessibility assessment; remaining pinned rows stay planned or in progress as recorded by the generated ledger.
