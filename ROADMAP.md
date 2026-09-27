# Roadmap

Brace Android is growing into a complete Compose design system for data-rich apps. The [component inventory](inventory/blueprint-components.json) defines the exact scope, and the generated [coverage page](docs/coverage.md) shows what is available today. The [public project board](https://github.com/users/joelromanpr/projects/1) tracks work in progress and places to contribute.

## Now

- Polish the catalog and real app examples so developers can inspect components, states, themes, and behavior quickly.
- Finish cross-device and accessibility review of the controls, overlays, selection tools, date and time controls, icons, and table already in source.
- Review the alpha components against the acceptance checks, then mark each finished inventory row stable.

## Next

- Close the remaining interaction and accessibility gaps in core components, menus, dialogs, navigation, and selection.
- Expand date, time, time-zone, and table behavior where the inventory still lists gaps.
- Review each completed component in light, dark, and high-contrast themes, at large text sizes and in RTL.

## Later

Implement and verify every applicable component in the pinned inventory. Experimental components stay on a separate track. A preview artifact or screenshot does not make an inventory row stable.

A component becomes stable only after its public API, token-driven styles, interactive catalog sample, guide, and meaningful tests are complete, including relevant touch, keyboard, mouse, TalkBack, restoration, and accessibility checks. Update the inventory, sample, docs, and tests together in each pull request.
