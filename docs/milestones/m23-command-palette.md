# M23 · Command palette

**Status:** in-progress source. The Omnibar comparison row has no first release version; released coverage remains **0/122 tracked Android items**.

## Included

`BraceCommandPalette` is a controlled native modal for grouped typed commands. It filters commands, supports disabled, selected, loading, and empty states, keeps IME composition in its editor, restores caller focus, and exposes touch, keyboard, pointer, and TalkBack actions. Its source slice includes a catalog example, Pages guide, API baseline, inventory evidence, device tests, and independent Maven consumer usage.

## Verification

The earlier dependent source branch passed a 467-task build/lint/API/inventory gate, 14/14 palette tests, and a core overlay regression suite with one preexisting skipped Back-injection test. Four Maven Local artifacts and a separate 37-task consumer built. A 320×640 Gboard review found commands and Close visible above the keyboard. Those results predate this replay. The replay incorporating selection, time zone, table editing, and the redesigned showcase passed a **228-task** catalog/select API/token/inventory gate and **14/14** API 36 command-palette tests before the final selection squash. The final source is rebuilt as a focused patch on protected main; generated inventory and Pages checks pass (**148 rows, 23 real captures, 66 guides**). Final-head hosted checks and combined Maven consumer verification remain pending.

## Limits

Manual TalkBack and physical keyboard review, additional device sizes, and the full theme matrix remain before stable status. On 320 dp, long descriptions truncate visually while option accessibility labels retain full text. Android Back behavior was checked manually because a core test harness does not reliably inject Back into dialog windows. The next select slice follows after this PR is reviewed.
