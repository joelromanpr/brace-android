# Blueprint comparison baseline

Brace Android compares its scope against the signed, stable Blueprint release [`@blueprintjs/core@6.18.0`](https://github.com/palantir/blueprint/releases/tag/%40blueprintjs%2Fcore%406.18.0), published 2026-07-28. Its annotated tag resolves to commit [`a60d4c92257612808fbfac81cfeee4fcba91a8b4`](https://github.com/palantir/blueprint/commit/a60d4c92257612808fbfac81cfeee4fcba91a8b4). This full commit, rather than Blueprint's moving `develop` branch or current website, is the inventory authority.

The published package versions in that commit are:

| Blueprint package | Version |
| --- | --- |
| `@blueprintjs/colors` | `5.1.16` |
| `@blueprintjs/core` | `6.18.0` |
| `@blueprintjs/icons` | `6.13.0` |
| `@blueprintjs/datetime` | `6.2.4` |
| `@blueprintjs/select` | `6.3.4` |
| `@blueprintjs/table` | `6.2.4` |
| `@blueprintjs/labs` | `6.4.4` |

## Inventory method

The machine-readable [inventory](inventory/blueprint-components.json) records every page in the pinned [`packages/docs-data/nav.json`](https://github.com/palantir/blueprint/blob/a60d4c92257612808fbfac81cfeee4fcba91a8b4/packages/docs-data/nav.json) for the six requested stable packages and labs. A [verbatim navigation snapshot](inventory/blueprint-nav.json) is checked by SHA-256 during coverage generation. The navigation file contains 85 relevant documentation pages: 68 core, 6 datetime, 2 icons, 5 select, 2 table, and 2 labs. Each row also links its immutable MDX source file at that commit. The inventory stores each page route and the SHA-256 hashes of the pinned navigation and [`llms.txt`](https://github.com/palantir/blueprint/blob/a60d4c92257612808fbfac81cfeee4fcba91a8b4/llms.txt) summaries so a future audit can repeat the extraction.

Some pages document multiple public components. The inventory splits them into rows, including the three control cards, slider variants, dialog parts, menu parts, tabs, tree nodes, and the nine UI components in [`packages/table/src/docs/table-api.mdx`](https://github.com/palantir/blueprint/blob/a60d4c92257612808fbfac81cfeee4fcba91a8b4/packages/table/src/docs/table-api.mdx). The table feature page also becomes explicit capability rows for viewport rendering, fixed headers, resizing, selection, copying, editing, keyboard navigation, and accessibility. Individual icon glyph assets are tracked as an icon catalog capability, not counted as hundreds of separate Compose components.

The `@blueprintjs/colors` package exposes palette values rather than UI components; its row points to the pinned core colors documentation. Typography and token capabilities are separate rows. `@blueprintjs/labs` rows remain on a separate track and are excluded from the main parity denominator.

React providers, hooks, DOM portals, HTML wrappers, CSS classes, resize sensors, and deprecated duplicate overlay APIs remain visible as `web-specific` rows. Their `reason` fields state the Compose mapping or why no separate Android component is appropriate. They are not silently dropped. Stable status for a web-specific mapping requires a linked explanation. Other stable rows require linked implementation, interactive sample, documentation, tests, and a first release version.

The [generated coverage page](docs/coverage.md) and JSON outputs come from the inventory. `python3 scripts/generate_coverage.py --check` rejects missing pinned pages, malformed rows, invalid evidence links, and stale generated output. A status is a maintainer claim backed by files, not proof that all Android interaction tests passed; component pull requests must still demonstrate those tests. Brace must not claim full applicable Blueprint coverage until every applicable row is stable and every web-specific mapping is documented.

## Licensing and identity

Blueprint is an Apache-2.0 project by Palantir Technologies. The inventory records its public component names and documentation links for comparison. Brace is an independently named Android library; any future copying of Blueprint code, token values, or icon assets must preserve the upstream license and attribution in the relevant artifact.
