# M39 — Android file picker field

**Status:** M36/Tree-main source integrated; final combined checks and hosted review pending. No release or stable inventory claim.

## Delivered in this slice

- Public `BraceFilePickerField` with controlled selected names, one/multiple document contracts, MIME filtering, disabled/error/helper/size/fill states, and URI callback.
- Existing Brace input/button tokens drive the field; localized browse and placeholder strings cover English and Spanish; caller error copy remains app-owned.
- Inventory, generated coverage, catalog states and copyable usage, documentation, Android contract tests, Maven consumer reference, and changelog updated together.

## Verification

- The current replay on protected main `231870a5e7d9876792d9286a7b47d3c0efb1e32d` passes inventory generation, Pages build, JavaScript syntax, and diff checks: 148 rows, 0/122 applicable rows stable, 24 existing captures, 70 guides. FilePicker is the sole inventory row advanced in this slice and stays in progress. The file-picker guide is registered in the site route and guide navigation. The catalog FilePicker sample was extracted into a private composable after the M15 catalog method exceeded the JVM method-size limit; its M15 rerun below passed. The M36/Tree-integrated compile and device gates remain pending.
- Current M15-main replay: focused core compile, lint, JVM tests, API check, inventory, and catalog APK passed offline (264 tasks). Eight aligned `0.1.0-SNAPSHOT` artifacts published to Maven Local (308 tasks); each has an AAR, sources JAR, KDoc JAR, POM, and Gradle Module Metadata. The separate Maven-coordinate-only consumer, including FilePicker use, passed (37 tasks). These are local checks, not a Maven Central release. The wider build/lint suite has not been rerun on this replay.
- Current M15-main replay: API 36 at 320×640, 160 dpi, font scale 1.0 passed all three focused FilePicker tests (71 Gradle tasks). They cover single and multiple document contracts, actual `EXTRA_MIME_TYPES` intent data, URI de-duplication, cancellation, disabled/error state, touch, Enter, Space, mouse, RTL, dark high contrast, native accessibility click, and a 48 dp minimum target.
- On the earlier M39 topic branch, the catalog field was visually reviewed at 320 dp in light and dark high contrast, and with 200% Android text. That visual pass has not been repeated after the replay. Manual system picker and human TalkBack journeys remain pending.

## Limits and next branch

The caller owns URI permission, display-name lookup, selected state and actual file reading/upload. The component does not implement camera capture or browser DOM props. The pinned FileInput row stays in progress and has no first release version. The next input branch is M42 HTMLSelect/BraceDropdown; its static slice awaits the same full validation gates.
