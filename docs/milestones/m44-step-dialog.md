# M44 delivery slice: step dialog

**Status:** locally verified on `joelromanpr/m44-multistep-dialog`, based on main `47d2d38`. Both pinned Blueprint 6.18.0 rows, `MultistepDialog` and `DialogStep`, remain **in progress**. Stable coverage is **0/121** applicable rows, and neither row has a first release version.

## Shipped in this slice

- Controlled `BraceStepDialog`, `BraceDialogStep`, localized labels, and logical top/start/end navigation.
- Synchronous validation, disabled advance, visited-step navigation tracked by stable IDs, saveable panel and rail history, and launcher focus return while the dialog remains composed through dismissal.
- Native modal Back/Escape and outside-touch policy, TalkBack pane/heading/tab/selected/disabled semantics, 48 dp rail and action targets, and token-driven themes.
- Interactive catalog states for both rows, copyable usage, independent Maven consumer example, Pages guide, KDoc, exact API baselines, and API36 interaction tests.

## Verification

| Gate | Result |
| --- | --- |
| Pinned scope and inventory | Blueprint `@blueprintjs/core@6.18.0` documentation/source at `a60d4c92257612808fbfac81cfeee4fcba91a8b4`; generated coverage check passed with 147 rows, 0/121 applicable stable, 0/94 components stable. |
| Static and Pages | PASS: token and inventory generation checks, JavaScript syntax for documentation scripts, Pages build with 147 rows and 33 guides, and `git diff --check`. This slice uses existing dialog and semantic tokens. |
| Gradle and API | PASS: foundation/core exact `apiDump` baselines, component/catalog/test compilation, and root `build lint checkTokenGeneration checkInventory apiCheck`. A temporary mirror of official Maven artifacts supplied downloads that Gradle had stalled on; repository dependencies were unchanged. |
| API36 device | PASS: 9/9 focused tests for metadata validation, controlled forward/back/revisit transitions, validator rejection, saveable panels and visited IDs across activity recreation, dynamic reordering, launcher focus return, Escape, mouse input, RTL/high-contrast/compact 2× targets, automated accessibility checks, and native named/selected/disabled rail nodes. A visited unselected step has its click action on the same native node as its label. |
| Maven Local and consumer | PASS: foundation, core, icons, and select published to Maven Local with AAR, POM, Gradle metadata, sources, and KDoc JARs; separate Maven-coordinate consumer `:app:assembleDebug` passed. Maven Central staging remains unverified. |
| Visual | PASS locally: 320×640 light, dark high-contrast, and 2× large-text dialog inspected with rail, panel, field, and fixed actions visible. The emulator font scale was restored to 1.0. |
| Hosted | Pending current-head GitHub CI and CodeQL. No hosted pass or release is claimed. |

## Adaptations, limits, and next branch

The API supplies a synchronous validator. Apps that validate remotely should own a loading state and set `canAdvance = false` until it resolves. Business state must be hoisted when it must outlive removal of the dialog composable. Focus return runs when `open` becomes false while `BraceStepDialog` remains in composition. Blueprint React child inspection and fixed desktop width become a typed Compose step list and responsive native modal.

Per-step custom footer slots, manual TalkBack/physical keyboard/mouse review, and wide-screen RTL Start/End rail QA remain before stability. Hosted API34 and maintainer review are pending. The next concrete branch is `joelromanpr/m45-html-table`, a separate pinned table slice.
