# M29 · Overflow list

**Status:** In progress. The current integration branch carries `BraceOverflowList`, its catalog example, guide, inventory link, and tests. No first release version is recorded.

The component measures generic keyed items against available width, moves excess items to a caller-provided overflow slot, and supports logical start or end collapse. Measurement probes stay out of accessibility. The caller supplies reachable actions and a labeled overflow menu.

The earlier focused branch passed 7/7 API 36 tests and [hosted verify and API 34 checks](https://github.com/joelromanpr/brace-android/actions/runs/36244980139). The integrated branch passed API checks, token and inventory generation, catalog build, 30/30 focused API 36 tests across these core controls, Maven Local publication of all eight modules, and a separate Maven-coordinate consumer build. Hosted checks remain pending.

For very large hidden lists, callers should provide a virtualized menu. Manual TalkBack and tablet review remain before stable status. The [coverage ledger](../coverage.md) records current availability.
