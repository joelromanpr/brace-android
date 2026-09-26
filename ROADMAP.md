# Roadmap

The [pinned component inventory](inventory/blueprint-components.json) is authoritative for exact rows, milestones, status, and links. The generated [coverage page](docs/coverage.md) reports current counts. A roadmap milestone does not imply a component is released.

| Milestone | Scope | Exit evidence |
| --- | --- | --- |
| M0 — foundation | Pinned scope, design tokens, theme, repository and docs/catalog foundation, first coherent core controls | Inventory and generated counts; token checks; tested public APIs and examples |
| M1 — core | Remaining core actions, content, forms, inputs, feedback, tags, trees, shortcuts | Each applicable inventory row has implementation, catalog example, docs, tests |
| M2 — overlays/navigation | Menus, dialogs, drawers, popovers, tooltips, toasts, navigation | Focus restoration, dismissal, semantics, keyboard and touch tests |
| M3 — selection | Single/multiple selection, suggestions, query, command palette | Selection and query state/interaction tests |
| M4 — datetime and icons | Date/time/range/time-zone experiences and licensed icon strategy | Localization, zone handling, icon attribution and accessibility tests |
| M5 — data table | Viewport rendering, fixed headers, resize, selection, copy, editing, keyboard navigation | Performance and complex interaction tests, accessibility evidence |

Each slice updates its inventory rows, documentation, catalog, tests, and changelog together. Full parity can be claimed only after every applicable pinned row is stable and verified. Experimental/labs rows are tracked separately.
