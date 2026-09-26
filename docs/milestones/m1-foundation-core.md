# M1 — foundation and first core controls

Status: **source milestone in draft [PR #1](https://github.com/joelromanpr/brace-android/pull/1); no public release or merged PR**. Branch: `joelromanpr/m1-foundation-core`. The [generated coverage ledger](../coverage.md) remains the authority for component status. At this snapshot it reports **0/121 applicable rows stable** (0/94 components; 0/27 capabilities), 24 web-specific mappings awaiting verified documentation, and two labs rows tracked separately. Fifteen rows are in progress; planned rows are not shipped coverage.

## Delivered in the source branch

- A pinned Blueprint comparison at commit `a60d4c92257612808fbfac81cfeee4fcba91a8b4`, machine-readable inventory, generated README and coverage counts, and an inventory-fed catalog asset.
- Versioned platform-neutral tokens, generated typed Kotlin tokens, and `BraceTheme` with light, dark, high-contrast, brand, scoped overrides, density, and motion controls.
- Initial `brace-core` APIs for Button, Checkbox, Switch, and a single-line text field, plus catalog examples and a [core component guide](../core-components.md). Their inventory status is **in progress** pending release and remaining acceptance checks.
- Multi-module Android build, API snapshots, local Maven consumer project, Pages site source with seven guides, contribution policies, CI, CodeQL, dependency updates, and manual signed-tag release staging workflow.

## Validation snapshot

| Gate | Result |
| --- | --- |
| Core Kotlin compile and catalog debug APK assembly | Passed locally |
| Foundation unit tests | Passed locally |
| Both modules' Android instrumentation test compilation | Passed locally |
| API 36 and API 37 isolated AVD instrumentation | API 37 passed 2 foundation and 4 core tests before the final text-field and large-text test additions; API 36 foundation passed 2/2 and core passed 5/5 after those additions. Supported Compose accessibility checks ran. |
| API dump generation; token and inventory generation checks; docs site build | Passed locally |
| Normal `publishToMavenLocal` and separate consumer compile | Passed for both modules with all publication tasks enabled, using a temporary mirror for official Maven dependencies. The independent consumer assembled from those artifacts. AARs, POMs, Gradle metadata, sources jars (9 files each), and KDoc jars (589 foundation and 83 core HTML files) were inspected; core POM declares foundation transitively. |
| Full `build lint checkTokenGeneration checkInventory apiCheck` | Passed locally: 287 tasks with a temporary init script resolving official AGP helper JARs from `/tmp`; normal online/offline dependency resolution remains unverified |
| Direct Dokka HTML generation | Passed for foundation and core after the official Dokka analysis JAR was made available through the temporary mirror |
| Maven Central staging and release | Unverified; no Central Portal credentials or release were used |
| Hosted CI, CodeQL, Pages, branch rules, PR, and release | `verify` and API 34 `instrumented` checks passed on PR #1. CodeQL skipped because the private repository has no GitHub Code Security; its workflow will run after public visibility. Main protection, squash-only merge, tag ruleset, labels, Issues, and Discussions were configured and read back. Pages and publication remain unverified. |

Lint HTML reports were generated for foundation, core, and catalog; the observed Kotlin warning is deprecation of `LocalClipboardManager`. The full gate and Maven Local publication relied on a temporary local mirror for official, previously uncached AGP and Dokka JARs; repository dependencies and publication tasks were not changed or disabled. Plain online Gradle stalled downloading those artifacts, and plain offline Gradle could not resolve them until mirrored. **Maven Central staging remains unverified.** No artifact has been published publicly. A pre-existing `Pixel_Tablet` AVD was offline during one device attempt; the isolated `Brace_API36` AVD completed the retest. The GitHub Pages workflow has not deployed, and the site URL has not been verified live.

## Known limits and next slice

Button, Checkbox, Switch, and TextField still need the remaining device/theme/input review described in the [component guide](../core-components.md) before their rows can become stable. The catalog is a starting set, not full Blueprint coverage. Select, datetime, icons, table, and most core rows remain planned.

Next branch: **`joelromanpr/m2-core-content-feedback`**, now in draft [PR #2](https://github.com/joelromanpr/brace-android/pull/2). Its six Card, CardList, Divider, Section, SectionCard, and ProgressBar rows have source APIs, catalog states, documentation, and tests; they remain in progress before release. Continue through the remaining rows and milestones in the [roadmap](../../ROADMAP.md).
