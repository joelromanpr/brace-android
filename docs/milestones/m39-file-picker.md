# M39 — Android file picker field

**Status:** source implementation in review; no release or stable inventory claim.

## Delivered in this slice

- Public `BraceFilePickerField` with controlled selected names, one/multiple document contracts, MIME filtering, disabled/error/helper/size/fill states, and URI callback.
- Existing Brace input/button tokens drive the field; localized browse and placeholder strings cover English and Spanish; caller error copy remains app-owned.
- Inventory, generated coverage, catalog states and copyable usage, documentation, Android contract tests, Maven consumer reference, and changelog updated together.

## Verification

- Inventory and token generation checks passed; Pages built 147 rows and 33 guides; XML parsing, JavaScript syntax, and diff checks passed.
- Exact API dumps and broad build/lint/tests/token/inventory/API/Maven Local/catalog checks passed (491 Gradle tasks). The separate Maven Local consumer passed (37 tasks).
- API 36 emulator: 3/3 focused tests passed, including single and multiple document contracts, MIME filtering, cancellation, disabled/error state, touch, Enter, Space, mouse, RTL, dark high contrast, native accessibility click, and a 48 dp minimum target.
- Visually reviewed the catalog field at 320 dp in light and dark high contrast, and with 200% Android text. Manual system picker and human TalkBack journeys remain pending.

## Limits and next branch

The caller owns URI permission, display-name lookup, selected state and actual file reading/upload. The component does not implement camera capture or browser DOM props. The pinned FileInput row stays in progress and has no first release version. The next input branch is M42 HTMLSelect/BraceDropdown; its static slice awaits the same full validation gates.
