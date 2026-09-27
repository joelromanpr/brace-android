# M41 · selection cards

**Status:** locally verified on `joelromanpr/m41-selection-cards`, based on main `47d2d38`. The pinned Blueprint 6.18.0 SwitchCard, CheckboxCard, and RadioCard rows have public APIs, versioned tokens, catalog states, documentation, Android tests, API baselines, and Maven consumer call sites. They remain **in progress**, with no first release; generated applicable stable coverage remains **0/121**. The ButtonGroup row stays planned and outside this slice.

## Shipped in this slice

- Controlled full-card switch, checkbox (including indeterminate), and radio APIs with a single native label/action target, minimum 48 dp size, logical indicator placement, selected visual option, compact padding, and card elevation.
- RadioCardGroup adjunct with a named group, exclusive selection, one Tab stop, and RTL-aware arrows that skip disabled options.
- Platform-neutral `controlCard` visual tokens and generated Kotlin at additive token-contract version `1.2.0`. A dedicated disabled switch track/thumb pair remains visible against the disabled card surface.
- Inventory-driven catalog examples, Pages guide, and an independent Maven-coordinate consumer example.

## Verification

| Gate | Result |
| --- | --- |
| Pinned scope | Blueprint `@blueprintjs/core@6.18.0` documentation and source at `a60d4c92257612808fbfac81cfeee4fcba91a8b4` inspected for all three rows. |
| Static and Pages | PASS: token and coverage generation checks, JavaScript syntax, inventory links, `git diff --check`, and Pages build with 147 pinned rows, 33 guides, 0/121 stable applicable rows. |
| Gradle and API | PASS: exact foundation/core API dumps, core and catalog compilation, and root `build lint checkTokenGeneration checkInventory apiCheck`. Gradle used a temporary mirror of official Maven artifacts after network downloads stalled; repository dependencies were unchanged. |
| API36 device | PASS: 11/11 focused tests for touch, mouse, keyboard, exclusivity, disabled and indeterminate states, RTL arrows and switch-thumb pixels, focus, state restoration, caller-small 48 dp target, 2× text, high contrast, automated accessibility checks, and native same-node label/action/disabled state. |
| Maven consumer | PASS: foundation, core, icons, and select published to Maven Local with AAR, POM, Gradle metadata, sources, and KDoc JARs; separate Maven-coordinate consumer `:app:assembleDebug` passed. Maven Central staging remains unverified. |
| Visual | PASS locally: 320×640 light SwitchCard, CheckboxCard, and RadioCard samples and dark high-contrast/2× text SwitchCard inspected. No card content clipped in inspected states. The disabled switch track was initially visually lost against its card surface and was corrected with token-driven colors and all-scheme contrast assertions. |
| Hosted | Initial PR head `3aab966` [CI run 36261045604](https://github.com/joelromanpr/brace-android/actions/runs/36261045604) failed before any steps in verify and instrumented; [CodeQL run 36261045622](https://github.com/joelromanpr/brace-android/actions/runs/36261045622) skipped analysis. The repository account billing blocker remains; these are infrastructure outcomes, not code-test results. No hosted pass or release is claimed. |

## Adaptations, limits, and next branch

The pinned Blueprint docs say RadioCard's indicator starts by default, while the 6.18.0 source and tests default to end; Brace follows the source and offers either logical placement. Blueprint `inputProps`, DOM refs, and arbitrary HTML children have no direct Android API; rich Compose content slots remain open. The separate RadioGroup and ButtonGroup inventory rows remain planned. Manual TalkBack, physical-keyboard and mouse review, all-card dark/high-contrast visual review, and CardList composition QA remain before stable acceptance.

The next concrete branch is `joelromanpr/m42-html-select` for the pinned HTMLSelect adaptation; it is a separate draft slice.
