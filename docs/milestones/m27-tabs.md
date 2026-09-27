# M27 · Tabs

**Status:** In progress. The current integration branch carries `BraceTabs`, `BraceTab`, `BraceTabPanel`, and `BraceTabSpacer` with tokens, catalog examples, guide, inventory links, and tests. No first release version is recorded.

Tabs use stable IDs, controlled selection, disabled states, badges, a trailing action, horizontal or vertical layout, and saveable panel content. Keyboard arrows follow layout direction; each tab has its own focus and accessibility state.

The earlier focused branch passed 12/12 API 36 tests and [hosted verify and API 34 checks](https://github.com/joelromanpr/brace-android/actions/runs/36245172251). The integrated branch passed API checks, token and inventory generation, catalog build, 30/30 focused API 36 tests across these core controls, Maven Local publication of all eight modules, and a separate Maven-coordinate consumer build. Hosted checks remain pending.

Vertical tabs stack above the panel on narrow screens. Manual TalkBack, tablet, and physical keyboard review remain before stable status. The [coverage ledger](../coverage.md) records current availability.
