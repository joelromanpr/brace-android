# M39 — Android file picker field

**Status:** replayed on M15 protected main for source/static preflight; combined device/consumer and hosted checks pending. No release or stable inventory claim.

## Delivered in this slice

- Public `BraceFilePickerField` with controlled selected names, one/multiple document contracts, MIME filtering, disabled/error/helper/size/fill states, and URI callback.
- Existing Brace input/button tokens drive the field; localized browse and placeholder strings cover English and Spanish; caller error copy remains app-owned.
- Inventory, generated coverage, catalog states and copyable usage, documentation, Android contract tests, Maven consumer reference, and changelog updated together.

## Verification

- The current replay on protected main `98f3d46065b63116dbed249ea60550d7e794f2d4` passes inventory generation, Pages build, JavaScript syntax, and diff checks: 148 rows, 0/122 applicable rows stable, 23 existing captures, 62 guides. FilePicker is the sole inventory row advanced in this slice and stays in progress. The file-picker guide is registered in the site route and guide navigation.
- On the earlier M39 topic branch, exact API dumps and broad build/lint/tests/token/inventory/API/Maven Local/catalog checks passed (491 Gradle tasks), and the separate Maven Local consumer passed (37 tasks). These have not yet been repeated on the current replay.
- On the earlier M39 topic branch, API 36 focused tests passed 3/3, covering single and multiple document contracts, MIME filtering, cancellation, disabled/error state, touch, Enter, Space, mouse, RTL, dark high contrast, native accessibility click, and a 48 dp minimum target. The replay adds assertions for the actual `EXTRA_MIME_TYPES` intent data and awaits a fresh device run.
- On the earlier M39 topic branch, the catalog field was visually reviewed at 320 dp in light and dark high contrast, and with 200% Android text. That visual pass has not been repeated after the replay. Manual system picker and human TalkBack journeys remain pending.

## Limits and next branch

The caller owns URI permission, display-name lookup, selected state and actual file reading/upload. The component does not implement camera capture or browser DOM props. The pinned FileInput row stays in progress and has no first release version. The next input branch is M42 HTMLSelect/BraceDropdown; its static slice awaits the same full validation gates.
