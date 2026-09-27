# M44 — Step dialog

**Status:** draft [PR #53](https://github.com/joelromanpr/brace-android/pull/53), locally replayed on protected main `1b802c39c1980afc7fed7756c95fc9cb135b88e9`. Pinned Blueprint 6.18.0 rows `MultistepDialog` and `DialogStep` remain **in progress**, with no first release version. Stable coverage remains **0/122** applicable rows.

## Included in this slice

- Controlled `BraceStepDialog` and typed `BraceDialogStep` with localized labels, logical top/start/end navigation, synchronous validation, disabled advance, visited-step navigation, saveable panel history, and launcher focus return while dismissal stays composed.
- Native modal Back/Escape and outside-touch policy, TalkBack pane/heading/tab/selected/disabled semantics, 48 dp rail and action targets, and semantic token styling.
- Public API and KDoc, interactive catalog states for both rows, copyable usage, Pages guide, inventory evidence, Android tests, and an independent Maven-coordinate consumer example. The catalog sample was extracted into a private composable after the combined catalog exceeded the JVM method-size limit.

## Verification

| Gate | Result |
| --- | --- |
| Current source and docs | Token and coverage generation checks, both pinned icon checks, contrast, Pages build, JavaScript syntax, and Git whitespace passed. Output: **148 rows, 0/122 stable, 25 real catalog captures, 86 guides**. |
| Current-main compile and API | Core `apiDump` passed **19 tasks**. Foundation/core `apiCheck`, core Android-test Kotlin, and catalog Kotlin compilation passed **151 tasks** after the sample extraction. |
| Current-main API 36 | StepDialog tests passed **9/9**, 0 skipped or failed (**71 tasks**) on 320×640, 160 dpi, font scale 1.0. They cover validation, transitions, saveable state, launcher focus, Escape, mouse, RTL, compact 2× targets, automated accessibility, and native rail nodes. |
| Earlier broader gate | The old topic branch passed root build/lint/API checks; current-main scope is covered by the focused checks above. |
| Earlier Maven and visual | Foundation, core, icons, and select artifacts plus a separate coordinate-only consumer passed from Maven Local. A 320×640 light, dark high-contrast, and 2× visual pass showed rail, panel, field, and actions; font scale was restored to 1.0. |
| Hosted | Required checks on the current-main replay are pending. No release or Maven Central publication is claimed. |

## Adaptation and limits

Blueprint React child inspection and fixed desktop navigation become a typed Compose step list and responsive Android modal. The synchronous validator expects apps to own async loading and set `canAdvance = false` until validation resolves. Apps must hoist business state that should outlive removal of the dialog. Focus return requires the dialog to remain composed while `open` becomes false.

Per-step custom footer slots, manual TalkBack and physical keyboard/mouse review, and wide-screen RTL Start/End rail QA remain before stability. Full Blueprint parity is not claimed.
