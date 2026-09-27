# M23 · Command palette

**Status:** in-progress source. The Omnibar comparison row has no first release version; released coverage remains **0/122 tracked Android items**.

## Included

`BraceCommandPalette` is a controlled native modal for grouped typed commands. It filters commands, supports disabled, selected, loading, and empty states, keeps IME composition in its editor, restores caller focus, and exposes touch, keyboard, pointer, and TalkBack actions. Its source slice includes a catalog example, Pages guide, API baseline, inventory evidence, device tests, and independent Maven consumer usage.

## Verification

The earlier dependent source passed a **467-task** build/lint/API/inventory gate and **14/14** focused API 36 palette tests. On the focused M16 main source, the final local API/catalog/device gate passed **261 tasks** and **46/46** select tests on API 36, including the palette suite. All **eight** aligned Maven Local artifacts were published with AAR, sources, documentation, POM, and Gradle metadata; the independent coordinate-only Android consumer built. Hosted verify, API 34 instrumentation, CodeQL, and analysis all passed on source head `64a6dcf`.

The Tree milestone is now integrated. Generated inventory and Pages checks pass with **148 rows, 24 real captures, 68 guides**. The Tree-integrated head still needs its own hosted checks; release status remains in progress.

## Limits

Manual TalkBack and physical keyboard review, additional device sizes, and the full theme matrix remain before stable status. On 320 dp, long descriptions truncate visually while option accessibility labels retain full text. Android Back behavior was checked manually because a core test harness does not reliably inject Back into dialog windows. The next select slice follows after this PR is reviewed.
