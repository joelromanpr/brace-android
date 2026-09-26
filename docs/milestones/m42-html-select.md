# M42 delivery slice: HTMLSelect adaptation

**Status:** local implementation on `joelromanpr/m42-html-select`, pending shared Gradle and API 36 device validation, separate Maven consumer verification, review, and merge. The pinned `core-htmlselect` row is **in progress**. Stable coverage remains **0/121** applicable rows; no first release is assigned.

## Scope

- `BraceDropdown`, `BraceDropdownOption`, and `BraceDropdownSize` in `brace-core` for one controlled selection from a short static list.
- Token-driven light, dark, high-contrast, compact, comfortable, focused, disabled, error, minimal, and large-text states.
- Anchored option menu, keyboard and pointer operation, TalkBack selection and expanded state, RTL, focus return, 48 dp minimum trigger, and saveable caller choice.
- Inventory row, interactive catalog, copyable example, Pages guide, Android tests, and independent Maven consumer source in the same slice.

## Verification

| Gate | Result |
| --- | --- |
| Blueprint scope | Inspected pinned 6.18.0 HTMLSelect source and documentation at `a60d4c92257612808fbfac81cfeee4fcba91a8b4`. |
| Token and coverage generation | Passed `python3 scripts/generate_tokens.py --check` and `python3 scripts/generate_coverage.py --check`; 147 rows and 0/121 stable. |
| Documentation site | Passed `node scripts/build-docs.mjs` and JS syntax checks; 33 guides generated. |
| Kotlin compile, lint, API baseline | Pending shared Gradle lane. |
| API 36 interaction and accessibility | Pending shared device lane. |
| Maven Local and separate consumer | Pending shared Gradle lane. |
| Manual TalkBack and real hardware | Pending. |
| Hosted CI and review | No PR yet. |

## Adaptation and limits

A Compose selector replaces Blueprint's browser `<select>` and HTML wrapper. The pinned API excludes multiple selection; filtering and custom rendering belong to the separate select family. DOM refs, option children, arbitrary HTML attributes, CSS classes, and caret icon props have no Android API. The popup is transient, while the caller persists the chosen value. Selection of an unavailable option and visual review of this branch still require device evidence.

The next concrete work is validation and a focused draft pull request for `joelromanpr/m42-html-select` after the shared Gradle and device lane becomes available.
