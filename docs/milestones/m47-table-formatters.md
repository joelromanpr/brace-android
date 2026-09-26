# M47 — Table value formatters

## Source delivered

- `BraceTruncatedCell` and `BraceJsonCell` provide token-driven long-text preview and full-value reveal for independent Compose use; `BraceTruncatedFormatter` and `BraceJsonFormatter` produce values for table `cellText` and copying.
- Unicode code point clipping, exact Compose layout overflow detection, controlled or saveable dialog state, selectable full text, touch/mouse/keyboard input, focus restoration, light/dark/high-contrast colors, large text, RTL and localized EN/ES action labels.
- JSON formatting supports nested JSON-compatible Kotlin and Android values with two-space indentation, optional top-level string quotes, null handling and rejection of invalid/cyclic input.
- Catalog live samples/copyable snippets, independent consumer source, tests, Pages guide and two pinned inventory rows marked **in progress**; no first release version.

## Verification

| Gate | Evidence |
| --- | --- |
| Pinned scope | Blueprint Table 6.2.4 at `a60d4c92257612808fbfac81cfeee4fcba91a8b4`, table API docs and `truncatedFormat.tsx`/`jsonFormat.tsx`. |
| Inventory, token, Pages, JavaScript and diff static checks | Passed: 147 pinned rows, 0/121 applicable stable, 0/94 component stable; tokens current; 45 Pages guides built; JavaScript syntax and diff whitespace checks passed. |
| Gradle build, lint and API check | Pending shared validation lane. |
| API 36 device suite | Pending shared validation lane. |
| Maven-local artifacts and separate consumer | Pending shared validation lane. |
| Catalog visual and manual TalkBack review | Pending. |

## Limits and next branch

Inside `BraceDataTable`, `cellContent` is visual and the table owns selection gestures and its single TalkBack node. Full value is available through `cellText` and clipboard copying; nested reveal actions are not exposed from viewport cells. Use the formatter outside the table for its dialog action. The formatters accept JSON-compatible values, not arbitrary Kotlin objects. Approximate DOM measuring and a DOM popover map to exact Compose layout detection and an Android dialog. The rows stay in progress pending runtime validation, native TalkBack review and a real release. A later table branch should add an explicit table-level full-value action for formatted cells.
