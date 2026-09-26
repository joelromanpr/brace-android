# M3: navigation labels and messages

**Status:** implemented on `joelromanpr/m3-core-navigation-feedback`; review and hosted CI pending. No Maven Central version has shipped. The six new inventory rows remain **in progress** and released coverage is **0/121** applicable rows.

## What this slice adds

- `BraceBreadcrumbs` and `BraceBreadcrumbItem`: an ordered native path, current and disabled semantics, touch/keyboard activation, RTL separators, and a width-aware overflow dropdown.
- `BraceTag` and `BraceCompoundTag`: intent, size, density, selection, icon and rich visual slots, and separate 48 dp action and remove targets with stable spoken labels.
- `BraceCallout` and `BraceEmptyState`: token-driven messages and placeholders with original Canvas intent marks, headings, separate action slots, optional polite announcements, and high-contrast/RTL/large-text layouts.
- KDoc, API baseline, Android catalog samples with copyable usage, a Pages guide, pinned inventory evidence, and an expanded independent consumer sample.

## Verification

| Gate | Result |
| --- | --- |
| `./gradlew --offline -I /tmp/brace-temp-repo.init.gradle build lint checkTokenGeneration checkInventory apiCheck` | Passed: 287 tasks. The temporary init script mirrors official Gradle dependencies for the local machine; it is not part of the repository. API dump ran separately because Gradle treats `apiDump` and `apiCheck` on the same invocation as an undeclared task dependency. |
| `ANDROID_SERIAL=emulator-5556 ./gradlew --offline -I /tmp/brace-temp-repo.init.gradle :brace-core:connectedDebugAndroidTest` | Passed: 40/40 core tests on an isolated API 36 AVD. Review fixes keep both compound-tag segments visible at narrow widths and close breadcrumb overflow after resizing; both have device regressions. The initial 24 dp actionable Tag target was corrected to 48 dp. |
| `node scripts/build-docs.mjs` and `node --check docs/site/app.js` | Passed: 147 inventory rows and eleven guides built. |
| Maven Local publication and independent consumer assemble | Passed: published foundation/core AARs, sources, and KDoc locally; the separate app compiled against `0.1.0-SNAPSHOT` coordinates using M3 Breadcrumbs, Tag, Callout and earlier APIs. |
| Hosted GitHub checks | Pending PR creation and CI run. |

## Remaining and limits

No M3 row is stable or released. Blueprint's arbitrary breadcrumb renderer/popover props, HTML links, and DOM OverflowList are represented by a typed Compose item model, callbacks, text measurement, and a native menu; custom breadcrumb rendering remains follow-up parity work. The native menu uses Material 3 positioning mechanics with Brace colors. A custom icon or action slot must provide its own accessible artwork/control. Manual TalkBack, keyboard, mouse, theme, and representative screen review remain before stable status. Other pinned core navigation, feedback, overlays, select, date/time, icon, and table rows remain planned.

The next concrete branch is `joelromanpr/m4-core-overlays`, starting with Menu, Dialog, Drawer, and Popover families. Each component slice will update inventory, catalog, docs, and interaction tests in its PR. The full pinned inventory remains the long-term scope.
