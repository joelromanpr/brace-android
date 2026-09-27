# M16 · Suggestions and multiple selection

**Status:** in-progress source. Nothing in this slice is published or marked stable; released coverage remains **0/122 applicable rows**.

## Included

- `BraceSuggest` provides a controlled query, keyboard acceptance, and a focusable anchored result list.
- `BraceMultiSelect` filters options and manages removable selections with touch and keyboard controls.
- The shared popover positions above the on-screen keyboard when needed. Both controls use localized expanded/collapsed announcements.
- The catalog, independent consumer, API baseline, docs, and inventory include both controls.

## Verification

The source passed an **836-task** broad build/lint/API/inventory/catalog gate, all **eight** aligned Maven Local artifacts (**308 tasks**), and an independent offline consumer (**37 tasks**). A hosted compiler failure in the large catalog sample was fixed by moving select examples into `SelectCatalogSample.kt`; that corrected earlier head passed hosted verify, API 34 instrumentation, CodeQL, and analysis. On the merged TagInput main, the replay passed a **230-task** catalog/select API/token/inventory gate. On the earlier merged table-copy main, API 36 passed **32/32** select tests and **5/5** shared popover tests. The final table-editing main is integrated; coverage and Pages checks pass with **148 rows, 23 real captures, 64 guides**. The showcase generates its guide routes and presents Brace APIs first. Final-head hosted and device checks are pending.

## Limits

Manual TalkBack, physical keyboard and mouse, form-factor, and full theme review remain before stable status. The command palette is the next select slice.
