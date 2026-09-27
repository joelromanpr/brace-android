# External consumer smoke project

This independent Gradle project resolves Brace **only by Maven coordinates**. From the repository root:

```sh
./gradlew :brace-foundation:publishToMavenLocal :brace-core:publishToMavenLocal :brace-icons:publishToMavenLocal :brace-blueprint-icons:publishToMavenLocal :brace-blueprint-icons-next:publishToMavenLocal :brace-select:publishToMavenLocal :brace-datetime:publishToMavenLocal :brace-table:publishToMavenLocal
./gradlew -p verification/consumer-smoke :app:assembleDebug
```

This separate Gradle app imports Brace by Maven coordinates; it does not include source modules. By default, `mavenLocal()` is checked first. It exercises theme switching, controls, forms, navigation, selection, overlays, icons, date and time inputs, and table selection, resizing, copying, editing, sorting, loading, reordering, freezing, accessibility, and full-value reveal. `1.0.0-SNAPSHOT` is local only.

To test a candidate locally before publishing, run `./gradlew -PreleaseVersion=<version> publishToMavenLocal`, check the eight artifact sets with `python3 verification/check-maven-publication.py <version>`, then build this app with `./gradlew -p verification/consumer-smoke -PbraceVersion=<version> :app:assembleDebug`. This verifies local Maven coordinates only.

To independently verify the published `1.0.0` release, resolve it in a fresh Gradle user home:

```sh
BRACE_GRADLE_HOME="$(mktemp -d)"
GRADLE_USER_HOME="$BRACE_GRADLE_HOME" ./gradlew -p verification/consumer-smoke \
  -PbraceRepository=central -PbraceVersion=1.0.0 :app:assembleDebug
```

`-PbraceRepository=central` excludes `mavenLocal()`; the fresh `GRADLE_USER_HOME` also excludes previously cached Brace artifacts. Inspect the resolved coordinates and build result before announcing another release.
