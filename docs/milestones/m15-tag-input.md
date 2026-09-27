# M15 · Tag input

**Status:** in-progress source. The TagInput inventory row has no first release version; stable coverage remains **0/122 applicable rows**.

## Included

`BraceTagInput` controls tag values and draft text. It wraps tags, supports Enter/IME/separator/paste submission, optional blur submission, validation and duplicate policies, RTL keyboard selection, 48 dp remove actions, visible focus, and localized accessibility labels. The catalog, usage guide, Android tests, API baseline, and separate consumer use the public API.

## Verification

The M20-based source passed an **836-task** build/lint/API/inventory/catalog gate, **23/23** API 36 TagInput/Tag tests, all **eight** Maven Local snapshots with sources/KDoc/POM/metadata (**308 tasks**), and an offline independent consumer (**37 tasks**). Its hosted verify, API 34, and CodeQL checks passed. This replay also passes the inventory and Pages build on the M57 source base (**148 rows, 53 guides**). A final integrated gate is pending after the current main merges.

## Remaining acceptance

Manual TalkBack, physical keyboard, non-Latin IME composition, clipboard context-menu paste, large text, and wider-device review remain. Nothing here has been published to Maven Central. Suggestions and multiple selection are the next select slice.
