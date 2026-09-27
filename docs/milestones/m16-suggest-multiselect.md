# M16 · Suggestions and multiple selection

**Status:** in-progress source. Nothing in this slice is published or marked stable; released coverage remains **0/122 applicable rows**.

## Included

- `BraceSuggest` provides a controlled query, keyboard acceptance, and a focusable anchored result list.
- `BraceMultiSelect` filters options and manages removable selections with touch and keyboard controls.
- The shared popover positions above the on-screen keyboard when needed. Both controls use localized expanded/collapsed announcements.
- The catalog, independent consumer, API baseline, docs, and inventory include both controls.

## Verification

The source passed an **836-task** build/lint/API/inventory/catalog gate, **32/32** API 36 select tests, **5/5** shared popover tests, all **eight** aligned Maven Local artifacts (**308 tasks**), and an independent offline consumer (**37 tasks**). The first hosted run caught a Kotlin `MethodTooLargeException` in the catalog's combined `ComponentSample`; select examples now live in `SelectCatalogSample.kt`. The corrected head passed hosted verify, API 34 instrumented, CodeQL, and analysis. The replay on the redesigned showcase base passes the inventory and Pages build with **148 rows, 21 real captures, and 56 guides**. Final hosted checks are required after this restack.

## Limits

Manual TalkBack, physical keyboard and mouse, form-factor, and full theme review remain before stable status. The command palette is the next select slice.
