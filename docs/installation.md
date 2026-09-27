# Installation

Brace Android `0.1.0-alpha01` is an early preview on [Maven Central](https://central.sonatype.com/artifact/io.github.joelromanpr.brace/brace-core/0.1.0-alpha01). The APIs are still changing; check the [component status](coverage.md) before relying on a specific behavior.

Add Google and Maven Central to your Android project's repository list. Start with `brace-core` for buttons, forms, navigation, and overlays; add the other families you use. All eight published artifacts share the same version:

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

// app/build.gradle.kts
dependencies {
    implementation("io.github.joelromanpr.brace:brace-foundation:0.1.0-alpha01")
    implementation("io.github.joelromanpr.brace:brace-core:0.1.0-alpha01")
    implementation("io.github.joelromanpr.brace:brace-icons:0.1.0-alpha01")
    implementation("io.github.joelromanpr.brace:brace-blueprint-icons:0.1.0-alpha01")
    implementation("io.github.joelromanpr.brace:brace-blueprint-icons-next:0.1.0-alpha01")
    implementation("io.github.joelromanpr.brace:brace-select:0.1.0-alpha01")
    implementation("io.github.joelromanpr.brace:brace-datetime:0.1.0-alpha01")
    implementation("io.github.joelromanpr.brace:brace-table:0.1.0-alpha01")
}
```

Add only the artifacts your app needs. `brace-foundation` provides tokens and `BraceTheme`; core, select, datetime, and table pull in their required Brace dependencies. `brace-icons` contains original Brace vectors. The two optional Blueprint icon packs contain attributed Apache-2.0 licensed artwork and stay separate so apps do not download icons they do not use. [Icon and asset details](icons.md)

The source build targets Android SDK 36 and Android 26 as its minimum. The compatibility table below records the tested source baseline.

The catalog app is a separate Android application in this repository. With JDK 21 and Android SDK 36 installed, run `./gradlew :catalog:installDebug` to try live states and the electric fleet and mission control examples. The [coverage page](coverage.md) distinguishes installable preview APIs from stable component coverage.

## Verify a Maven Central install

The repository includes a separate Android project that imports Brace through Maven coordinates. A fresh Gradle user home avoids cached Brace artifacts, and `-PbraceRepository=central` removes Maven Local from its repository list:

```sh
BRACE_GRADLE_HOME="$(mktemp -d)"
GRADLE_USER_HOME="$BRACE_GRADLE_HOME" ./gradlew -p verification/consumer-smoke \
  -PbraceRepository=central -PbraceVersion=0.1.0-alpha01 :app:assembleDebug
```

The command needs network access for Android and Compose dependencies as well as Brace. [Consumer smoke project](../verification/consumer-smoke/README.md)

## Current source compatibility baseline

| Tool or platform | Version |
| --- | --- |
| Minimum Android | API 26 |
| Compile SDK | API 36 |
| Android Gradle Plugin | 8.13.2 |
| Kotlin and Compose compiler plugin | 2.2.20 |
| Compose BOM | 2025.08.00 |
| Contributor build JDK | 21 (JVM target 17) |

These are the versions pinned by the source build and consumer sample; other combinations have not been claimed as tested. See the [compatibility policy](compatibility.md) for API changes during the preview series.
