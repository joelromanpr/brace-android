# M28 — Entity title

**Status:** draft [PR #35](https://github.com/joelromanpr/brace-android/pull/35), locally replayed on protected main `1b802c39c1980afc7fed7756c95fc9cb135b88e9` (FilePicker). The pinned `core-entitytitle` row remains **in progress**. No first release version or stable coverage claim is assigned.

## Delivered in this slice

- `BraceEntityTitle` provides a spoken heading, optional decorative icon, subtitle, separate tag actions, rich visual slots, and caller-owned title navigation.
- Body, subtitle, title, and display prominence use Brace typography. Light, dark, high-contrast, brand, density, loading, hover, pressed, and keyboard-focus states use semantic and component tokens generated from the versioned platform-neutral source.
- The interactive title has a 48 dp target and one native labeled action node. Constrained text can ellipsize while preserving the full accessible label and showing measured-overflow help. Static loading placeholders remove actions.
- The inventory, public API snapshots, catalog sample, Pages guide, tests, and separate Maven-coordinate consumer were updated together. The catalog sample is a private composable so the enlarged catalog stays below the JVM method-size limit.

## Verification

| Gate | Result |
| --- | --- |
| Pinned comparison | Reviewed Blueprint 6.18.0 EntityTitle MDX and public React props at commit `a60d4c92257612808fbfac81cfeee4fcba91a8b4`. |
| Current source and docs | Token and coverage generation `--check`, both pinned icon checks, link contrast, Pages build, JavaScript syntax, and Git whitespace passed. Generated output has **148 inventory rows, 0/122 applicable rows stable, 25 existing real catalog captures, and 86 guides**; `docs/entity-title.md` has a generated route and guide link. |
| Current-main API and catalog compile | The merged token source generated EntityTitle, Control Card, Slider, and MultiSlider type-safe tokens. Foundation/core `apiCheck`, core Android-test Kotlin compilation, and catalog Kotlin compilation passed on `1b802c3` (**151 tasks**). Foundation `apiDump` passed separately on the prior `56e719c` main (10 tasks). |
| M23-main focused Gradle/API before the site merge | Foundation/core compile, core lint and JVM tests, both API checks, catalog APK, token generation, and inventory **passed: 269 tasks**. The regenerated foundation API baseline contains EntityTitle color and metric types. |
| M23-main API 36 before the site merge | `BraceEntityTitleTest` **passed 11/11**, 0 failures or skips (71 Gradle tasks), on the 320 × 640, 160 dpi emulator. The suite covers native heading/action semantics, loading removal, keyboard activation, independent tags, hover and long-press overflow help, layout wrapping, RTL, 2× font scale, high contrast, and automated Compose accessibility checks. |
| M23-main Maven consumer before the site merge | All eight aligned `0.1.0-SNAPSHOT` artifacts published to Maven Local (**308 tasks**). The separate coordinate-only consumer, including EntityTitle use, **passed 37 tasks**. This is local validation, not Maven Central publication. |
| Earlier hosted source checks | [Verify](https://github.com/joelromanpr/brace-android/actions/runs/36245559729/job/108413985864) and API 34 [instrumented](https://github.com/joelromanpr/brace-android/actions/runs/36245559729/job/108413985942) passed on old source commit `fa67345`. Required hosted checks on this current-main replay are pending. |
| Earlier visual review | The old topic branch's catalog at 320 × 640 showed title activation, loading hiding title/tags, and no horizontal overflow. That visual review has not been repeated on this replay. |

## Adaptations and limits

Blueprint's React heading element becomes Compose typography and Android heading semantics. `titleURL` becomes a caller-owned `onTitleClick` callback. React icon, tag, and rich text nodes become Compose slots; HTML class/ref props become `Modifier`. Rich visual slots require separate spoken strings and own their truncation. The icon is decorative; actionable tags remain separate focus stops. Loading has no animation and respects reduced motion.

Title hover and subtitle touch long press overflow help passed. Subtitle hover did not trigger under the API 36 emulator pointer test and remains open. Manual TalkBack, tablet/physical-device review, and current-head hosted checks remain before this row can be called stable. The next focused related row is OverflowList; exact priorities remain in the inventory and roadmap. Full Blueprint parity is not claimed.
