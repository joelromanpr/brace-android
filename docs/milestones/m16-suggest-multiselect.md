# M16 · Suggestions and multiple selection

**Status:** in-progress source. Nothing in this slice is published or marked stable; released coverage remains **0/122 applicable rows**.

## Included

- `BraceSuggest` provides a controlled query, keyboard acceptance, and a focusable anchored result list.
- `BraceMultiSelect` filters options and manages removable selections with touch and keyboard controls.
- The shared popover positions above the on-screen keyboard when needed. Both controls use localized expanded/collapsed announcements.
- The catalog, independent consumer, API baseline, docs, and inventory include both controls.

## Verification

The source passed an **836-task** broad build/lint/API/inventory/catalog gate, all **eight** aligned Maven Local artifacts (**308 tasks**), and an independent offline consumer (**37 tasks**). A hosted compiler failure in the large catalog sample was fixed by moving select examples into `SelectCatalogSample.kt`; the corrected head passed hosted verify, API 34 instrumentation, CodeQL, and analysis. On merged table-copy main, the replay passes a **228-task** catalog/select API/token/inventory gate, **32/32** API 36 select tests, **5/5** shared popover tests, and the Pages build with **148 rows, 23 real captures, 60 guides**. The showcase now generates its guide routes and presents Brace APIs first. Final hosted checks remain pending after the last main restack.

## Limits

Manual TalkBack, physical keyboard and mouse, form-factor, and full theme review remain before stable status. The command palette is the next select slice.
