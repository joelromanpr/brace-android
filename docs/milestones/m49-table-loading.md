# M49: table loading and status states

Status: **in progress**, unreleased. Branch: `joelromanpr/m49-table-loading`, stacked on M26 `ceba218`. Blueprint authority: Table 6.2.4 at `a60d4c92257612808fbfac81cfeee4fcba91a8b4`.

## Delivered in this branch

- Public `BraceTableState` controls Ready, Loading, Empty, and Error in `BraceDataTable`.
- Public `BraceTableLoading` resolves table, column, row, cell, and header masks with explicit false overrides. Viewport-only static skeletons use Brace tokens and hide stale labels.
- Loading pauses selection, copying, editing, resizing, and keyboard navigation; empty and error states replace the grid with accessible status panels. Retry is a 48 dp keyboard and TalkBack action.
- Catalog state controls, copyable usage, separate Maven consumer usage, Pages guide, inventory evidence, and focused Android tests cover precedence, interactions, transitions, high contrast, RTL, large text, and accessibility.

## Validation

Static token, coverage, Pages, JavaScript syntax, XML, and diff checks are run before handoff. Gradle compilation, API dump/check, Android device tests, and separate Maven consumer compilation await the shared lane. No artifact is published.

## Remaining and next branch

Blueprint's independent interaction with nonloading cells, per-component props, and optional loading animation are not implemented. An active edit draft remains caller-owned across refresh. Complete device checks, then choose the next pinned table row after coordinating with the parallel M46, M47, and M48 branches. Coverage remains **0 stable** until release validation.
