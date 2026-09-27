# M16 · Suggestions and multiple selection

**Status:** in-progress source. Nothing in this slice is published or marked stable; released coverage remains **0/122 applicable rows**.

## Included

- `BraceSuggest` provides a controlled query, keyboard acceptance, and a focusable anchored result list.
- `BraceMultiSelect` filters options and manages removable selections with touch and keyboard controls.
- The shared popover positions above the on-screen keyboard when needed. Both controls use localized expanded/collapsed announcements.
- The catalog, independent consumer, API baseline, docs, and inventory include both controls.

## Verification

The M57-based replay passed an **836-task** build/lint/API/inventory/catalog gate, **32/32** API 36 select tests, and **5/5** shared popover tests. The first hosted run caught a Kotlin `MethodTooLargeException` in the catalog's combined `ComponentSample`; select examples now live in `SelectCatalogSample.kt`. The corrected M57 tree passed the full local gate and device suites. With M33 date ranges integrated, scoped catalog compile/API/token/inventory checks passed **151 tasks**; the Pages build has **148 rows, 15 captures, and 55 guides**. Hosted CI on this corrected main-integrated head remains pending. Earlier source work also published four local artifacts and built a coordinate-only consumer; those gates will be repeated after the final main restack.

## Limits

Manual TalkBack, physical keyboard and mouse, form-factor, and full theme review remain before stable status. The command palette is the next select slice.
