# M39 — Android file picker field

**Status:** source implementation in review; no release or stable inventory claim.

## Delivered in this slice

- Public `BraceFilePickerField` with controlled selected names, one/multiple document contracts, MIME filtering, disabled/error/helper/size/fill states, and URI callback.
- Existing Brace input/button tokens drive the field; localized browse and placeholder strings cover English, Spanish, and French; caller error copy remains app-owned.
- Inventory, generated coverage, catalog states and copyable usage, documentation, Android contract tests, Maven consumer reference, and changelog updated together.

## Verification

- Inventory and token generation checks passed; Pages built 147 rows and 33 guides; XML parsing, JavaScript syntax, and diff checks passed.
- Exact API dumps, broad build/lint, API 36 device suite, Maven Local, independent consumer, 320 dp catalog visual review: pending shared Gradle/ADB lane.
- Manual system picker and TalkBack: pending.

## Limits and next branch

The caller owns URI permission, display-name lookup, selected state and actual file reading/upload. The component does not implement camera capture or browser DOM props. The pinned FileInput row stays in progress and has no first release version. After validation, the next input branch should address HTMLSelect and the remaining picker/forms inventory rows.
