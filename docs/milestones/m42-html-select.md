# M42 delivery slice: HTMLSelect adaptation

**Status:** validated local implementation on `joelromanpr/m42-html-select`, pending hosted CI, review, and merge. The pinned `core-htmlselect` row is **in progress**. Stable coverage remains **0/121** applicable rows; no first release is assigned.

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
| Kotlin compile, lint, API baseline | Exact API dump/check and broad build/lint/token/inventory/Maven Local/catalog gate passed (491 tasks). |
| API 36 interaction and accessibility | 7/7 focused tests passed, including the native named click target, touch, mouse, keyboard, RTL, high contrast, large text, and restoration. |
| Maven Local and separate consumer | Four local artifacts published; separate Maven-coordinate consumer assembled (37 tasks). |
| Catalog visual review | 320 dp light and dark high contrast field/menu/selected states, plus 200% Android text. |
| Manual TalkBack and real hardware | Pending. |
| Hosted CI and review | Required before merge; no hosted pass claimed. |

## Adaptation and limits

A Compose selector replaces Blueprint's browser `<select>` and HTML wrapper. The pinned API excludes multiple selection; filtering and custom rendering belong to the separate select family. DOM refs, option children, arbitrary HTML attributes, CSS classes, and caret icon props have no Android API. The popup is transient, while the caller persists the chosen value. A disabled option was verified to remain visible and unselectable on the emulator. Manual TalkBack and real hardware remain before stable status.

The next concrete branch is M43 ButtonGroup after this focused draft pull request.
