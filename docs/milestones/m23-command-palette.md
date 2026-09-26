# M23 delivery slice: CommandPalette for Omnibar

**Status:** local source slice verified; inventory remains in progress pending hosted CI, review, release, and broader accessibility QA. The pinned Blueprint Omnibar row belongs to roadmap phase **M3 selects**. No Maven Central version has shipped; released applicable coverage remains **0/121**.

## Scope

- Add a controlled native modal `BraceCommandPalette` for grouped typed commands, filtering, disabled and selected states, loading and empty feedback, and keyboard, pointer, and TalkBack interaction.
- Reuse `BraceQueryListState` and navigation, `BraceOverlay`, `BraceMenuItem`, and semantic/component tokens. Preserve IME composition with an internal `TextFieldValue` editor, restore caller focus when supplied, and keep results scrollable with large text and a visible keyboard.
- Update the pinned inventory, public API baseline, interactive catalog, Pages guide, independent Maven consumer, and focused device tests in the same slice.

## Verification

| Gate | Result |
| --- | --- |
| Source, inventory, docs, API, build, and lint | Passed: `build lint checkTokenGeneration checkInventory apiCheck`, 467 tasks. `generate_coverage.py --check` and Pages build passed. |
| API 36 Compose interaction and accessibility | Passed: CommandPalette focused suite 14/14; shared Overlay/Dialog regression 16 tests, 1 preexisting skipped Back injection test. Includes real IME composition, keyboard focus/navigation, pointer, restoration, large text, RTL, high contrast, reduced motion, accessibility checks, and 320dp IME geometry. |
| Maven Local artifact and separate consumer | Passed: foundation/core/icons/select published locally; separate consumer `:app:assembleDebug`, 37 tasks. |
| 320dp catalog visual and IME check | Passed on API 36 at 320×640 in light and dark high contrast with Gboard: commands and Close remain above keyboard. Manual Android Back first hid IME, then dismissed dialog; accessibility tree confirmed closure. |
| Hosted CI, review, and release | Pending. No stable inventory row. |

## Known limits and next branch

The implementation is locally verified and remains an unreleased source candidate. Blueprint's React `Overlay2` provider and DOM keyboard event model are represented by a native dialog and Compose key handling. The palette shows commands before typing as an Android discovery choice, while Blueprint's default initial content is empty. Manual TalkBack and physical keyboard review, additional device sizes, hosted CI, review, and a maintainer-controlled release remain before stable status. Long command descriptions truncate visually on 320dp, while their full text remains in the option accessibility label. The existing core device Back injection test is skipped because that test harness does not reliably deliver Back into Dialog windows; a real emulator Back sequence was checked manually here. The next proposed branch is `joelromanpr/m24-date-input-range`, stacked after the date picker slice to cover the pinned DateInput and DateRangeInput rows. It remains a proposal until this branch and its dependencies are reviewed.
