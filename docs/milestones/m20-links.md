# M20 delivery slice: Link and AnchorButton

**Status:** source branch rebased onto M11 main `e03bde9` in `joelromanpr/m20-links`. The Link and AnchorButton rows remain **in progress**, `firstRelease` is null, and generated stable coverage remains **0/121** applicable rows. Post-rebase local build, API, device, and Maven consumer gates passed; hosted checks and manual TalkBack review remain. No Maven Central release is claimed.

## Scope

- `BraceLinkDestination.Uri` and `.Action` make URI opening and app navigation explicit. A caller may override URI opening for Custom Tabs or deep links.
- `BraceLink` provides token colors, underline modes, disabled state, 48 dp touch target, focus, hover, keyboard activation, RTL, and destination announcement.
- `BraceLinkButton` composes existing Button visual tokens and adds destination semantics while suppressing disabled/loading navigation.
- The inventory, catalog, Pages guide, focused Android tests, API baseline, and independent Maven consumer are updated in this branch.

## Verification

| Gate | Result |
| --- | --- |
| Pinned Blueprint source and inventory review | Reviewed Link and AnchorButton docs/source from `@blueprintjs/core@6.18.0`, commit `a60d4c92257612808fbfac81cfeee4fcba91a8b4`. |
| Post-M11 static inventory and Pages checks | Passed: token and coverage generation `--check`, `node scripts/build-docs.mjs` (147 rows, 29 guides), `node --check docs/site/app.js`, Link contrast check, and Git whitespace/conflict checks. The rebase preserved the M11 icon catalog, guide, consumer sample, and inventory rows alongside M20 Links. |
| API 36 focused and broad device results | On rebased M11 main, `BraceLinkTest` passed **10/10** with no failures or skips, including enabled/disabled native node click actions, 48 dp bounds, and keyboard focus. Before the M11 rebase, the full core suite finished **179 tests**, zero failures, and one known native Back injection skip after the shared Button semantics fix. The initial native test exposed a split label/action node, now fixed. The pre-rebase broad run is distinct from the post-rebase focused result. |
| Post-M11 build, lint, API, and catalog | `build lint checkTokenGeneration checkInventory apiCheck :catalog:assembleDebug` passed with **378 tasks**. Generated API baselines stayed current; icon and Link samples compiled together. |
| Post-M11 Maven Local and independent consumer | Foundation, core, and icons each published an AAR, sources JAR, Javadoc JAR, POM, and Gradle module metadata. The separate `verification/consumer-smoke :app:assembleDebug` resolved those Maven coordinates and passed (**37 tasks**). |
| Catalog visual and manual TalkBack | The prior 320 × 640 light and dark high-contrast review found no clipping or overlap; Action and URI examples updated their status. Post-M11 visual review and manual TalkBack remain. |
| Hosted CI and review | Draft PR #28 was green before the native semantics fix. Hosted checks must rerun on the rebased branch; no remote publication attempted. |

## Remaining work

Link semantics are a localized announcement and click label because Compose UI 1.9 lacks a public link role. Inline link spans inside a paragraph are still planned; `BraceLink` is currently a standalone 48 dp control. `BraceLinkButton` shares the current Button styles, which do not include Blueprint's minimal button, success/warning button intents, button size/fill/alignment, or a progress indicator. Browser-only anchor attributes and target behaviors are represented by Android URI/app actions rather than separate components. The next concrete integration step is hosted checks and manual accessibility review for `joelromanpr/m20-links`; remaining pinned rows stay planned or in progress as recorded by the generated ledger.
