# M43 delivery slice: button group

**Status:** implementation staged on `joelromanpr/m43-button-group` from main `47d2d38`; the pinned Blueprint ButtonGroup row is **in progress**. Stable applicable coverage remains 0/121 and `firstRelease` is empty.

## Scope

- Public `BraceButtonGroup`, `BraceButtonGroupAction`, and visual configuration enums in `brace-core`.
- Versioned platform-neutral ButtonGroup colors and dimensions, generated Kotlin tokens, and scoped brand/contrast tests.
- Separate button roles and 48 dp targets; connected horizontal/vertical layout, equal-width fill, logical RTL alignment, optional selected state, loading/disabled behavior, visible focus, and icon-only accessible labels.
- Interactive catalog and copyable usage, Pages guide, independent Maven consumer source, focused device tests, and an updated inventory row.

## Verification

| Gate | Result |
| --- | --- |
| Token generator and inventory | Passed `python3 scripts/generate_tokens.py --check` and `python3 scripts/generate_coverage.py --check`; 147 rows, 121 applicable, zero stable. |
| Pages build and JavaScript | Passed `node scripts/build-docs.mjs`, `node --check docs/site/app.js`, and `node --check scripts/build-docs.mjs`; 33 guides. |
| Kotlin build, lint, API check | Pending shared Gradle lane. |
| API 36 Compose tests | Pending shared device lane. |
| Maven Local and coordinate-only consumer | Pending shared Gradle lane. |
| Visual inspection | Pending emulator lane. |
| Hosted CI and review | No PR yet. |

## Limits and next branch

Blueprint's CSS `height: 100%`, border z-index, and arbitrary React child wrappers have Compose-specific behavior documented in the guide. Manual TalkBack and real input-device review are outstanding. The next concrete branch after this slice is `joelromanpr/m44-button-group-accessibility` if device review exposes a per-action semantics or focus gap; otherwise continue the next planned core inventory row on its own branch.
