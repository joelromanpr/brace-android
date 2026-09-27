# M24 · Collapse and text

**Status:** In progress. The current integration branch carries `BraceCollapse` and `BraceText`, their catalog examples, guide, inventory links, and tests. Neither row has a first release version.

`BraceCollapse` animates height with Brace motion tokens, removes closed content from interaction and accessibility, and can retain composed child state. `BraceText` supports wrapping, ellipsis, full spoken text, and a tooltip for truncated or titled text.

The earlier focused branch passed 11/11 API 36 tests and [hosted verify and API 34 checks](https://github.com/joelromanpr/brace-android/actions/runs/36244095893). The integrated branch passed API checks, token and inventory generation, catalog build, 30/30 focused API 36 tests across these core controls, Maven Local publication of all eight modules, and a separate Maven-coordinate consumer build. Hosted checks remain pending.

The caller provides the disclosure trigger and its expanded semantics. Manual TalkBack and hardware pointer review remain before stable status. The [coverage ledger](../coverage.md) records current availability.
