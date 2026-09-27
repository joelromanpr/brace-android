# Small table

`BraceSimpleTable` is the compact, fully composed table in `brace-core`. It adapts Blueprint's documented `HTMLTable` visual role into a native Compose API; the large virtualized table is provided by `brace-table`. The pinned comparison is Blueprint 6.18.0 at `a60d4c92257612808fbfac81cfeee4fcba91a8b4`.

It supports bordered, striped, compact, selected, disabled and keyboard-focus states through Brace tokens. Optional row activation works with touch, mouse, Enter, and Space. Cells expose their complete values to TalkBack; RTL arrows follow visual direction. The catalog has a live sample and copyable usage, and the independent Maven consumer compiles the public API.

The focused API 36 small-table suite passed **4/4** before integration. The combined branch reruns its build and device checks; see the [table integration report](table-integration.md) for current evidence. The small table composes every row, so use `BraceDataTable` for large datasets and viewport behavior. Manual TalkBack acceptance and a published release remain open; the inventory stays **in progress**.
