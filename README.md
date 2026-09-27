# Brace Android

[![Maven Central](https://img.shields.io/maven-central/v/io.github.joelromanpr.brace/brace-core?filter=%21%2A-%2A&label=Maven%20Central&color=3156b8)](https://central.sonatype.com/artifact/io.github.joelromanpr.brace/brace-core/1.0.0)

Compose components for Android apps with a lot going on: forms, filters, dialogs, and data tables. Brace has its own look and works with touch, keyboard, and screen readers.

![Electric fleet example in the Android catalog](docs/site/showcase/fleet-operations-wide.png)

**[Explore the live showcase](https://joelromanpr.github.io/brace-android/)** · [Browse components](https://joelromanpr.github.io/brace-android/#coverage) · [View the Android captures](https://joelromanpr.github.io/brace-android/#showcase)

The catalog includes two runnable examples: an electric fleet workspace and a spacecraft mission screen. The data is fictional; the screenshots come from the Android app.

## Try the catalog

With JDK 21 and Android SDK 36 installed, run:

```sh
./gradlew :catalog:installDebug
```

Open the app to search components, try their states, and change light or dark theme, contrast, brand color, and density. [Setup and supported versions](docs/installation.md)

## Use a component

Add `brace-core` from Maven Central:

```kotlin
implementation("io.github.joelromanpr.brace:brace-core:1.0.0")
```

```kotlin
var name by rememberSaveable { mutableStateOf("") }

BraceTheme {
    Column {
        BraceTextField(name, { name = it }, label = "Project name")
        BraceButton("Save", onClick = { save(name) })
    }
}
```

[Theming](docs/theming.md) · [Component guides](docs/core-components.md) · [Data tables](docs/table-viewport.md)

## Availability

<!-- coverage:begin -->
**Component status:** 0 of 94 components and 0 of 28 design tools have passed stable review. [Check individual readiness](docs/coverage.md).
<!-- coverage:end -->

Version `1.0.0` is [on Maven Central](https://central.sonatype.com/artifact/io.github.joelromanpr.brace/brace-core/1.0.0). Install only the [artifacts you need](docs/installation.md). The [component list](docs/coverage.md) shows current behavior, tests, and known limits. Its counts come from the [inventory](inventory/blueprint-components.json), so a release number or screenshot does not imply every component is finished.

The comparison uses a [pinned reference version](BLUEPRINT_BASELINE.md). [License and asset attribution](docs/attribution.md) are recorded separately.

## Contribute

Pick a component from the [public project board](https://github.com/users/joelromanpr/projects/1) or [roadmap](ROADMAP.md), then follow [the contribution guide](CONTRIBUTING.md). See [security](SECURITY.md) for private vulnerability reports and [support](SUPPORT.md) for help. Apache-2.0 [license](LICENSE).
