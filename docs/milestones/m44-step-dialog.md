# M44 delivery slice: step dialog

**Status:** static implementation prepared on `joelromanpr/m44-multistep-dialog`. Both pinned Blueprint rows, `MultistepDialog` and `DialogStep`, are **in progress**. Stable coverage remains **0/121** applicable rows, and no release version is assigned.

## Scope

- `BraceStepDialog`, `BraceDialogStep`, localized labels, and logical top/start/end navigation.
- Controlled selection and completion, synchronous validation, disabled advance, visited-step navigation, `rememberSaveable` panel retention, responsive rail, and launcher focus return.
- Native dialog Back/Escape and outside-touch policy, TalkBack pane/heading/selected/disabled semantics, 48 dp rail and action targets, and theme tokens.
- Interactive catalog example for both rows, copyable usage, independent Maven consumer example, Pages guide, KDoc, and API 36 device tests.

## Verification

| Gate | Result |
| --- | --- |
| Pinned inventory and generated coverage | Passed `python3 scripts/generate_coverage.py --check`: 147 rows, 0/121 applicable stable, 0/94 components stable. |
| Token generation | Passed `python3 scripts/generate_tokens.py --check`; this slice uses existing dialog and semantic tokens. |
| Pages build and JavaScript syntax | Passed `node scripts/build-docs.mjs` (147 rows, 33 guides) and `node --check` for both documentation scripts. |
| Kotlin build, lint, API baseline | Pending shared Gradle lane. |
| API 36 device interactions and accessibility | Pending shared emulator lane. Tests are authored for invalid metadata, validation, navigation, saveable state, focus return, mouse input, RTL/high contrast/compact 2× text targets, and supported automated checks. |
| Maven Local and separate consumer | Pending shared Gradle lane. |
| 320 dp visual, manual TalkBack/mouse, hosted CI | Pending review and hosted runner availability. |

## Known limits and next branch

The API supplies a synchronous validator. Apps that validate remotely should own a loading state and set `canAdvance = false` until it resolves. The step panels are retained within the dialog composition but business state must be hoisted to persist after removing that composable. Per-step custom footer slots and manual TalkBack/mouse review remain before stability. The next integration branch is `joelromanpr/m44-multistep-dialog` after the current shared Gradle/emulator lane is released, followed by a focused follow-up for any device findings.
