# Brace Android

An open-source Jetpack Compose design system for complex, data-dense Android apps. Brace has its own tokens, visual language, and Android behavior. The long-term comparison target is the public user-facing surface of [Palantir Blueprint](https://github.com/palantir/blueprint), pinned to [`@blueprintjs/core@6.18.0`](BLUEPRINT_BASELINE.md) at commit `a60d4c92257612808fbfac81cfeee4fcba91a8b4`. Blueprint is a web toolkit; the [inventory](docs/coverage.md) records Android adaptations and web-specific mappings explicitly. Brace is independent of Palantir.

## Coverage

<!-- coverage:begin -->
**Released coverage: 0/122 applicable rows** (0/94 components; 0/28 capabilities). Web-specific mappings documented: 12/24 (stable: 0). Labs tracked separately: 2 rows. Full applicable parity: no.
<!-- coverage:end -->

[Live documentation and component showcase](https://joelromanpr.github.io/brace-android/) · [Browse every inventory row and its evidence](docs/coverage.md) · [Machine-readable inventory](inventory/blueprint-components.json) · [Roadmap](ROADMAP.md)

The source milestones contain versioned platform-neutral design tokens, a generated Kotlin token API, `BraceTheme`, an interactive inventory-driven catalog, a documentation site source, actions and form controls, M2 content and progress feedback, M3 navigation labels and messages, M4 menu and modal overlay source, M5 drawer and anchored popover source, M6 tooltip and toast source, M7 context-menu and shortcut source, M8 form-field and editable-text source, M9 labels/control groups, M10 numeric input, M11 icon foundation, M12 select/query state, M17 Spinner/Skeleton loading feedback, M19 radio/segmented choices, M25 top bar, M13 date picker/date field, M30 time picker/time field, M33 date range picker/field, an optional M35 pinned legacy Blueprint glyph pack, M54 documented Compose mappings for web mechanisms, an optional M55 pinned `/next` glyph pack, the M14 table viewport, M18 table selection and resizing, and M20 links/navigation buttons. These APIs remain **in progress** until release evidence is recorded. Blueprint parity is a project goal, not a current claim. Suggest, MultiSelect, remaining time-zone interaction, and further table interactions remain planned; the legacy and `/next` icon packs and the M14/M18 table slices are in-progress source. Gallery previews do not change released coverage.

## Try the source build

Requires JDK 21 and Android SDK 36. The Gradle wrapper uses 8.14.3; the build pins AGP 8.13.2, Kotlin/Compose compiler 2.2.20, Compose BOM 2025.08.00, and minSdk 26.

```sh
./gradlew :catalog:assembleDebug
./gradlew build lint checkTokenGeneration checkBlueprintIconGeneration checkBlueprintNextIconGeneration checkInventory apiCheck
```

The catalog APK is `catalog/build/outputs/apk/debug/catalog-debug.apk`. It lists component names and availability from the generated inventory asset, with runnable examples for implemented controls and runtime light/dark, high-contrast, brand, density, and motion controls. The [core](docs/core-components.md), [content and feedback](docs/content-feedback.md), [loading feedback](docs/loading-feedback.md), [navigation and messages](docs/navigation-feedback.md), [menus and overlays](docs/overlays.md), [drawers and popovers](docs/drawers-popovers.md), [tooltips and toasts](docs/tooltip-toast.md), [context menus and shortcuts](docs/context-shortcuts.md), [form fields and editable text](docs/form-text.md), [labels and control groups](docs/form-layout.md), [numeric input](docs/numeric-input.md), [icons](docs/icons.md), [select/query](docs/select-query.md), [top bar](docs/top-bar.md), [radio/segmented choices](docs/radio-segmented.md), [date picker/input](docs/datetime-picker-input.md), [time picker/field](docs/time-picker-input.md), [date ranges](docs/datetime-range.md), [table viewport](docs/table-viewport.md), [table selection and resizing](docs/table-selection-resize.md), [links](docs/links.md), and [web mechanisms](docs/web-mechanisms.md) guides document APIs, states, accessibility, and current limits. The [M1](docs/milestones/m1-foundation-core.md), [M2](docs/milestones/m2-content-feedback.md), [M3](docs/milestones/m3-navigation-feedback.md), [M4](docs/milestones/m4-overlays.md), [M5](docs/milestones/m5-drawers-popovers.md), [M6](docs/milestones/m6-tooltip-toast.md), [M7](docs/milestones/m7-context-shortcuts.md), [M8](docs/milestones/m8-form-text.md), [M9](docs/milestones/m9-form-layout.md), [M10](docs/milestones/m10-numeric-input.md), [M11](docs/milestones/m11-icons.md), [M12](docs/milestones/m12-select-query.md), [M17](docs/milestones/m17-loading-feedback.md), [M19](docs/milestones/m19-radio-segmented.md), [M25](docs/milestones/m25-top-bar.md), [M13](docs/milestones/m13-datetime-picker.md), [M30](docs/milestones/m30-time-picker-input.md), [M33](docs/milestones/m33-date-range.md), [M35](docs/milestones/m35-blueprint-icon-pack.md), [M14](docs/milestones/m14-table-viewport.md), [M18](docs/milestones/m18-table-selection-resize.md), [M20](docs/milestones/m20-links.md), [M54](docs/milestones/m54-web-mechanisms.md), and [M55](docs/milestones/m55-blueprint-next-icons.md) reports track verification and open work.

No Maven Central release has been published. To try the local snapshot, publish the foundation, core, icons, optional Blueprint legacy and `/next` icon packs, select, datetime, and table AARs to Maven Local, then compile the [independent consumer sample](verification/consumer-smoke/README.md):

```sh
./gradlew :brace-foundation:publishToMavenLocal :brace-core:publishToMavenLocal :brace-icons:publishToMavenLocal :brace-blueprint-icons:publishToMavenLocal :brace-blueprint-icons-next:publishToMavenLocal :brace-select:publishToMavenLocal :brace-datetime:publishToMavenLocal :brace-table:publishToMavenLocal
./gradlew -p verification/consumer-smoke :app:assembleDebug
```

The consumer uses aligned `brace-core`, `brace-icons`, `brace-blueprint-icons`, `brace-blueprint-icons-next`, `brace-select`, `brace-datetime`, and `brace-table` Maven Local coordinates at `0.1.0-SNAPSHOT`. Core exposes foundation transitively; icons can be added separately without bundling artwork into core. The [installation guide](docs/installation.md) has complete dependency snippets and the supported toolchain. Maven Central publishing is a manual, protected maintainer action after a verified release tag.

## A Compose screen

```kotlin
var projectName by rememberSaveable { mutableStateOf("") }
var includeArchived by rememberSaveable { mutableStateOf(false) }

BraceTheme {
    Column {
        BraceTextField(projectName, { projectName = it }, label = "Project name")
        BraceCheckbox(includeArchived, { includeArchived = it }, label = "Include archived")
        BraceButton(label = "Save", onClick = { save(projectName, includeArchived) })
    }
}
```

Theme tokens support light, dark, and high-contrast schemes, brand colors, scoped overrides, compact/comfortable density, and reduced motion. See [theming](docs/theming.md) for the token contract and Material 3 interoperability.

## Documentation and contribution

The [GitHub Pages site](https://joelromanpr.github.io/brace-android/) is live and is built from `docs/site` by `node scripts/build-docs.mjs`. The [visual showcase](docs/showcase.md) uses real Android catalog captures with source-commit and appearance metadata. Component names, availability labels, and coverage counts come from the pinned inventory; a draft capture is not shipped coverage.

[Contributing](CONTRIBUTING.md) · [Code of conduct](CODE_OF_CONDUCT.md) · [Security](SECURITY.md) · [Support](SUPPORT.md) · [Governance](GOVERNANCE.md) · [Maintainer guide](MAINTAINERS.md) · [Attribution](docs/attribution.md)

Apache-2.0 licensed; see [LICENSE](LICENSE).
