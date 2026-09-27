# M29 · Adaptive overflow list

**Status:** in progress on [PR #36](https://github.com/joelromanpr/brace-android/pull/36). The inventory stays at 0/122 released Android items until publication.

## In this slice

- Generic `BraceOverflowList<T>` measures the available Compose width, keeps a caller-set minimum visible, and collapses from the logical start or end in RTL.
- A stable overflow slot can preserve menu state as width changes. The app supplies accessible actions and a labeled menu trigger.
- The catalog shows narrow/wide, collapse direction, and trigger states. The public guide, API dump, consumer sample, and interaction tests travel with the component.

## Checks

| Environment | Result |
| --- | --- |
| Earlier implementation on M11 main | Local build/lint/API gate: 377 tasks. API 36 focused tests: 7/7. [Hosted verify and API 34 instrumented passed](https://github.com/joelromanpr/brace-android/actions/runs/36244980139) on `aa65f42`. Maven Local Foundation/Core/Icons and a separate coordinate-only consumer built. |
| Current main replay | Static inventory, token, Pages, JavaScript, and diff checks pass: 148 rows, 24 real captures, 72 guides. Current-main API/core/catalog compile, Android-test compile, catalog lint, token generation, and inventory gates passed. API 36 focused tests passed 7/7. All eight aligned artifacts were published to Maven Local with AAR, sources, documentation, POM, and Gradle metadata, and the separate coordinate-only consumer built. Fresh hosted checks are pending. |
| Visual review | Earlier 320 × 640 emulator review checked start/end collapse, menu selection, widening, and clipping. |

Manual TalkBack, tablet and physical-device review, and a large-list performance check remain. This row has no first release version. The pinned comparison and Android mapping are in the [coverage inventory](../coverage.md); there is no Maven Central release yet.
