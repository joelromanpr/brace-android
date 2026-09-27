# M16 · Suggestions and multiple selection

**Status:** in-progress source. Nothing in this slice is published or marked stable.

## Included

- `BraceSuggest` provides a controlled query, keyboard acceptance, and a focusable anchored result list.
- `BraceMultiSelect` filters options and manages removable selections with touch and keyboard controls.
- The shared popover positions above the on-screen keyboard when needed. Both controls use localized expanded/collapsed announcements.
- The catalog, independent consumer, API baseline, docs, and inventory include both controls.

## Verification

The earlier source branch passed a 467-task build/lint/API/inventory gate, 32/32 API 36 select tests, 5/5 popover tests on rerun, four Maven Local artifacts, and a separate 37-task consumer build. Those results predate the current main integration. This replay passes inventory generation and the Pages build with **148 rows and 52 guides**. Integrated Android and hosted checks remain pending.

## Limits

Manual TalkBack, hardware input, form-factor, and full theme review remain before stable status. The command palette is the next select slice. Released coverage remains **0/122 applicable rows**.
