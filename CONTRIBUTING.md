# Contributing to Brace Android

Brace Android is an Android-native Compose design system for dense applications. The [coverage inventory](inventory/blueprint-components.json) is the scope ledger; a component is not complete just because its name appears in a catalog.

## Find and claim work

1. Pick a `planned` row in the [coverage page](docs/coverage.md), or open a component proposal if a documented Blueprint row is missing.
2. Open an issue identifying the inventory `id`, the intended Android behavior, accessibility risks, and the proposed module. Ask a maintainer to mark it claimed before investing in a large change.
3. Keep each pull request to a coherent component or small family. Work from a short-lived topic branch off `main`; do not add a permanent integration branch.
4. Update the inventory, catalog example, documentation, and tests in the same pull request as implementation. A row can move to `stable` only when all completion fields are backed by real files and checks.

Discuss design decisions in the issue before changing a public API. Directly copying Blueprint's React implementation usually produces the wrong Android interaction model. Describe the native adaptation in the inventory.

## Local setup

Install JDK 21 and Android SDK platform 36. Accept the Android SDK licenses, set `ANDROID_HOME` or `local.properties` locally, then run:

```sh
./gradlew build
./gradlew lint
./gradlew checkTokenGeneration checkBlueprintIconGeneration checkInventory apiCheck
```

Build the catalog with `./gradlew :catalog:assembleDebug`. The repository does not commit SDK paths or signing secrets. CI is the final source of truth for the supported toolchain; check the version catalog and CI workflow before changing local versions.

## Component acceptance criteria

Each stable row needs:

- A usable public Compose API with KDoc and an explicit state contract, including save and restore behavior where state matters.
- Styling through semantic and component tokens; light, dark, high-contrast, compact, and comfortable variants.
- An interactive catalog example with significant states, plus documentation and a copyable usage example.
- Meaningful behavior tests and Compose semantics checks, including disabled, error, loading, empty, or selection states when relevant.
- Touch, keyboard, and mouse behavior appropriate to the component; visible keyboard focus, logical focus order, TalkBack labels/actions, large text, RTL, reduced motion, and accessible target sizes.
- Coverage of overlay dismissal/focus and table editing/navigation with interaction tests rather than screenshots alone.

Automated Compose accessibility checks should run where the test environment supports them. Explain any platform limitation in the pull request and add the strongest available semantic or interaction assertion.

## Tokens and generated files

Edit versioned platform-neutral token sources first, then regenerate Kotlin output. Do not hand-edit generated tokens. Token changes need a rationale, before/after catalog views in relevant themes and densities, and contrast or touch-target evidence where applicable. Run `checkTokenGeneration` before opening the pull request.

## Pinned Blueprint icon artwork

The 11 original Brace vectors in `brace-icons` remain independent. The optional `brace-blueprint-icons` artifact holds licensed Blueprint paths from the exact commit in `BLUEPRINT_BASELINE.md`. To refresh that pack for a newly approved baseline, update the baseline and run `python3 scripts/generate_blueprint_icons.py --upstream /path/to/pinned/blueprint`, then `python3 scripts/generate_blueprint_icons.py --check --upstream /path/to/pinned/blueprint`. Review the SVG and metadata hashes, Apache-2.0 license/attribution, generated Kotlin names, both artwork resolutions, and the inventory row. Do not hand-edit the generated manifest or names. CI runs `checkBlueprintIconGeneration` against committed files; include the pinned upstream audit in PR evidence.

## Pull requests and review

Use the pull request template. Include the inventory IDs, user-facing behavior, accessibility evidence, test commands/results, and API implications. A maintainer reviews the API and design behavior; CI must pass before squash merge to `main`. For a solo maintainer, branch rules should require a pull request and CI but no mandatory approving review. Public API removals and behavior changes follow the [compatibility policy](docs/compatibility.md).

By submitting a contribution you agree it is licensed under the repository's [Apache-2.0 license](LICENSE), unless you explicitly state otherwise. Do not submit assets or copied code without identifying their licenses and attribution in the pull request.
