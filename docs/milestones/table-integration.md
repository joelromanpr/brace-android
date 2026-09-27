# Table family integration

This review brings the small `BraceSimpleTable` in `brace-core` together with the larger `BraceDataTable` in `brace-table`. It combines the earlier focused M45 and M47–M58 source slices in one PR. The pinned comparison remains Blueprint 6.18.0 at `a60d4c92257612808fbfac81cfeee4fcba91a8b4`.

## Included

- Small table with row activation, selection, accessible grid semantics, density and theme variants.
- Large table with viewport rendering, fixed headers, selection regions, copying, editing, sorting, row and column resizing and reordering, loading and error states, frozen panes, native grid accessibility, and full-value reveal.
- Public Kotlin APIs and KDoc, token styling, catalog samples, focused guides, device tests, API snapshot, and Maven Local consumer usage.

## Verification

The generated inventory has **148 rows**, with **0/122 applicable rows stable** and no first release set. Local `build lint checkTokenGeneration checkInventory apiCheck publishToMavenLocal :catalog:assembleDebug` passed (882 tasks). The full API 36 table suite passed **116/116**; the focused small-table suite passed **4/4**. The independent Maven-coordinate consumer assembled, and all eight local publications contain an AAR, sources jar, documentation jar, POM, and Gradle metadata. Token, inventory, Pages, JavaScript, and Git whitespace checks passed. Hosted CI for this integrated PR remains pending.

## Limits

A human TalkBack traversal audit remains open, including actual reading order through frozen panes and large-text/RTL behavior. Multi-item drag, drag edge auto-scroll, rich cell editing, and column header menus are separate planned work. This PR does not publish Maven Central artifacts or mark the table family stable.
