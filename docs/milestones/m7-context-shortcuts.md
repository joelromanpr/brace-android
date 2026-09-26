# M7 delivery slice: context menus and shortcuts

**Status:** implementation in progress in [PR #17](https://github.com/joelromanpr/brace-android/pull/17); hosted CI and release evidence pending. These six pinned rows belong to roadmap phase **M2 overlays/navigation**. No Maven Central version has shipped, so released applicable coverage remains **0/121**.

## Scope

- Triggered context menu for secondary mouse click, touch or stylus long press, keyboard context gesture, and TalkBack action.
- Controlled point-anchored popup mapping for Blueprint `ContextMenuPopover`, with window clamping, focus, dismissal, and overlay stack integration.
- Screen and local shortcut registration, editable-field guard, visual shortcut label, and grouped shortcut discovery dialog.
- Pinned inventory evidence, catalog samples, documentation site source, and separate Maven consumer usage in the same slice.

## Verification

| Gate | Result |
| --- | --- |
| Build, lint, token, inventory, and API checks | Passed locally: 287 Gradle tasks on the pinned toolchain with the temporary official-dependency mirror. |
| Focused API 36 device interaction tests | Passed: final focused ContextMenu 12/12 and Hotkeys 12/12. The earlier full API 36 run before the restoration/positioning follow-up had 115/116 core tests pass, 0 failures, 1 previously documented native Back injection skip; foundation 2/2 passed. Hosted CI will rerun the complete suite on the final diff. |
| Documentation build and generated inventory | Passed: 147 inventory rows, 19 guides, JavaScript syntax, and generated coverage checks; 0/121 applicable rows stable. |
| Maven Local publication and independent consumer | Passed: foundation/core AAR, sources, KDoc JAR, POM, and module metadata published locally; separate consumer assembled with context-menu and shortcut APIs from coordinates only. |
| Installed catalog interactions | Passed on a 320 px API 36 phone emulator: ContextMenu long press opened within the viewport, Copy link dismissed and updated the sample, and the shortcut discovery button opened a grouped dialog without clipping. |
| Hosted CI and pull request review | Pending. |

## Known limits and next branch

All six rows remain in progress pending code review, device evidence, and release. The point-based popup has no DOM virtual target; callers of the lower-level primitive own trigger handling and focus return. The catalog and independent consumer examples restore that focus after dismissal. A restored open target menu waits for target layout and follows later movement; the popup measures its intrinsic menu width so its point placement is not clamped by a full-window child. Global shortcuts are limited to the active Compose host, and custom editable fields require an explicit marker so text entry remains safe. Blueprint's automatic context-menu tooltip suppression and its React/browser mechanisms have documented mappings, but exact web timing and animation are outside this slice. Manual TalkBack, hardware keyboard, pointer, form-factor, theme, and large-text checks remain. The next focused branch is `joelromanpr/m8-form-text` for FormGroup, TextArea, and EditableText; numeric and tag inputs will follow in separate interaction slices.
