# M41 · selection cards

**Status:** local implementation in progress on `joelromanpr/m41-selection-cards`, based on main `47d2d38`. The three pinned Blueprint 6.18.0 SwitchCard, CheckboxCard, and RadioCard rows have source, tokens, catalog, documentation, and test/consumer call sites. They remain in progress, not released or stable; applicable stable coverage remains **0/121**. The ButtonGroup row stays planned and outside this slice.

## Scope

- Controlled full-card Switch, Checkbox (including indeterminate), and Radio APIs with one logical focus/TalkBack target, minimum 48 dp size, logical indicator placement, selected-style option, compact padding and card elevation.
- Card-specific RadioCardGroup adjunct with named group, exclusive selection, one Tab stop, and RTL-aware arrows that skip disabled options.
- Versioned platform-neutral `controlCard` tokens generated into Kotlin; inventory-driven catalog samples, Pages guide, tests, and independent consumer call sites.

## Verification

Static token and coverage generation/checks, JavaScript syntax, Pages build, inventory paths, and `git diff --check` passed locally: **147 pinned rows, 33 guides, 0/121 stable**. Static source review completed; compiler validation remains pending. Focused compilation, exact API dump/check, API 36 instrumentation, broad build/lint, Maven Local, and independent consumer are **not yet run** while another milestone owns the shared Gradle/ADB lane. No hosted PR or CI result exists for this branch. The Android test file is planned evidence, not a passing result.

## Known limits and next branch

The pinned Blueprint docs say RadioCard's indicator starts by default, while the 6.18.0 source and tests default to end; Brace follows the source and offers either logical placement. Blueprint `inputProps`, DOM refs, and arbitrary HTML children have no direct Android API; rich Compose content slots are still open. The separate RadioGroup and ButtonGroup inventory rows remain planned. Manual TalkBack, large text, RTL, physical keyboard and mouse, and CardList composition QA remain before stable acceptance. After static review and shared-lane handoff, compile and run the component/device gates, update this evidence, then prepare one focused draft PR.
