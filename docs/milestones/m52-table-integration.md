# M52: integrated table interaction slice

**Status:** in progress on `joelromanpr/m52-table-integration`, stacked on M26 `ceba218`. No Maven Central release or full Blueprint Table parity is claimed.

## Integrated in source

- M46 sorting, M47 value formatters, M48 public cell/header primitives, M49 granular loading and empty/error states, M50 disjoint regions, and M51 controlled row/column reordering now share one `BraceDataTable` API.
- Public KDoc, semantic and component tokens, Spanish accessibility strings, catalog controls, a separate Maven consumer example, table guides, and an API snapshot cover the combined surface.
- Interaction boundaries were reconciled: header selection has its own touch area beside sort/resize/reorder controls; loading pauses actions and marks visible cells disabled; the keyboard-active cell alone receives a focus ring. A keyed selection survives sorting, reordering, and loading. Row/column grip focus is restored after reorder layout changes.

## Verification

The generated pinned inventory still contains 147 rows, with **0/121 applicable rows stable** and `firstRelease` unset. Static inventory/token checks and the Pages build pass (56 guides). The final offline Gradle build, lint, API check, Maven Local publication, catalog assembly, and separate Maven consumer assembly passed. The full API 36 table suite passed **95/95** tests, including combined sorting, reordering, loading, selection, accessibility, RTL, large text, and state restoration checks. Compact light and dark high-contrast catalog table views were inspected on the emulator; the horizontally clipped columns can be reached by scrolling. Hosted CI has not run job steps because the account billing gate prevents job startup.

## Limits and next branch

Frozen panes, table accessibility completion, and a human TalkBack traversal review remain open. The table does not implement multi-item drag, drag edge auto-scroll, or automatic server sorting. Source rows remain in progress until PR review and verification. The next concrete branch is `joelromanpr/m53-table-freezing`, which must be integrated and checked against this combined API before a release decision.
