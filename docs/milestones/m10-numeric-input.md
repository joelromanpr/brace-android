# M10 delivery slice: numeric input

**Status:** implementation in progress on `joelromanpr/m10-numeric-input`, based on pinned Blueprint 6.18.0 and rebased onto the merged M9 squash commit. The NumericInput row remains **in progress**. There is no first Maven Central release from this slice and no stable coverage claim.

## Scope

- Controlled string drafts and locale-aware numeric input, with exact decimal stepping and configurable bounds.
- Up/Down, Shift major, Alt minor, touch and mouse step actions, IME, focus, read-only/disabled/error states, and 48dp controls.
- Catalog and independent consumer examples, pinned inventory links, a Pages guide, and focused Android tests.

## Verification

| Gate | Result |
| --- | --- |
| Focused API 36 device tests | Passed on the final implementation: `BraceNumericFieldTest` XML **17 tests, 0 failures, 0 errors, 0 skips** on API 36. The suite covers label tap and focus traversal, rejected controlled drafts, fine normal steps, runtime major-step precision changes, bounds, and input error semantics. |
| API and full Gradle gate | `brace-core:apiCheck` and the hosted `verify` build, lint, tests, tokens, inventory, and documentation checks passed on the final code. The earlier local full gate passed 287 tasks. |
| Generated coverage and Pages | `python3 scripts/generate_coverage.py --check`, `node scripts/build-docs.mjs`, and `node --check docs/site/app.js` passed. Coverage remains 0/121 applicable rows stable. |
| Catalog build and visual review | `:catalog:assembleDebug` passed. Installed at a 320 × 800 pixel viewport; NumericInput rendered without horizontal overflow, and tapping `+` changed 0.2 to 1.2 with updated state text. |
| Maven Local and independent consumer | Foundation and core published locally with AAR, sources, Javadoc, POM, and Gradle metadata; `verification/consumer-smoke :app:assembleDebug` passed against those coordinates. |
| Manual TalkBack, tablet, hosted CI, and review | Hosted API 34 `instrumented` passed on the final code after an initial runner boot failure and rerun. Manual TalkBack and tablet/physical-device review remain. The row remains in progress; no public release is claimed. |

## Adaptations and known limits

The value is a caller-owned string to preserve partial numeric drafts, including a locale decimal separator. Android touch actions use two adjacent 48dp targets instead of Blueprint's compact stacked web buttons; logical start/end follows RTL. Built-in stepping uses decimal arithmetic and formats for the current configuration locale, but an app retaining a draft during a language switch should translate that string. The pinned extended expression evaluator is explicitly non-core; long-press auto-repeat also remains open. The inventory keeps this row in progress until the implementation, accessibility, catalog, and release gates are verified.

The next focused branch is `joelromanpr/m11-icons`; remaining inputs and forms, selects, date/time, and the data table remain scheduled by their pinned inventory rows. No full Blueprint parity is claimed.
