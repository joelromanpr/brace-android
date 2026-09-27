# Installation

Brace Android is in source development. The planned Maven Central coordinates below are **not published yet**. To try a local snapshot, build and publish the repository artifacts to Maven Local, then consume them from a separate Android project.

```sh
./gradlew publishToMavenLocal
```

Add Maven Local in the consumer project's repository list and use aligned versions:

```kotlin
// settings.gradle.kts in the consumer project
dependencyResolutionManagement {
    repositories {
        mavenLocal()
        google()
        mavenCentral()
    }
}

// app/build.gradle.kts in the consumer project
dependencies {
    implementation("io.github.joelromanpr.brace:brace-foundation:0.1.0-SNAPSHOT")
    implementation("io.github.joelromanpr.brace:brace-core:0.1.0-SNAPSHOT")
    implementation("io.github.joelromanpr.brace:brace-icons:0.1.0-SNAPSHOT")
    implementation("io.github.joelromanpr.brace:brace-blueprint-icons:0.1.0-SNAPSHOT")
    implementation("io.github.joelromanpr.brace:brace-blueprint-icons-next:0.1.0-SNAPSHOT")
    implementation("io.github.joelromanpr.brace:brace-select:0.1.0-SNAPSHOT")
    implementation("io.github.joelromanpr.brace:brace-datetime:0.1.0-SNAPSHOT")
    implementation("io.github.joelromanpr.brace:brace-table:0.1.0-SNAPSHOT")
}
```

`brace-icons` is optional for apps that need Brace vectors or a scoped registry; `brace-blueprint-icons` is a further opt-in artifact for 706 licensed legacy Blueprint glyphs; `brace-blueprint-icons-next` separately carries the pinned public `/next` artwork (695 outlined names and 386 filled variants). Core does not pull icon artwork transitively. After the first release, remove `mavenLocal()` and use the released version from Maven Central. All public Brace artifacts use the same version. The source build currently targets Android SDK 36 and Android 26 as its minimum; verify the release's compatibility table before adoption.

The datetime and table artifacts depend on core and foundation transitively. Add only the artifacts whose APIs you use.

The catalog app is a separate Android application in this repository. Run `./gradlew :catalog:assembleDebug` to inspect examples. The [coverage page](coverage.md) distinguishes implemented components from planned ones.

## Current source compatibility baseline

| Tool or platform | Version |
| --- | --- |
| Minimum Android | API 26 |
| Compile SDK | API 36 |
| Android Gradle Plugin | 8.13.2 |
| Kotlin and Compose compiler plugin | 2.2.20 |
| Compose BOM | 2025.08.00 |
| Contributor build JDK | 21 (JVM target 17) |

These are the versions pinned by this source build, not a claim that every other version combination has been tested. The separate Maven consumer smoke test uses the published local artifacts before a release. Release notes will name the tested consumer matrix.
