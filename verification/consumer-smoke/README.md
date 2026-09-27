# External consumer smoke project

This independent Gradle project resolves Brace **only by Maven coordinates**. From the repository root:

```sh
./gradlew :brace-foundation:publishToMavenLocal :brace-core:publishToMavenLocal :brace-icons:publishToMavenLocal :brace-blueprint-icons:publishToMavenLocal :brace-blueprint-icons-next:publishToMavenLocal :brace-select:publishToMavenLocal :brace-datetime:publishToMavenLocal :brace-table:publishToMavenLocal
./gradlew -p verification/consumer-smoke :app:assembleDebug
```

This separate Gradle app imports Brace by Maven coordinates from `mavenLocal()`; it does not include source modules. It exercises theme switching, controls, forms, navigation, selection, overlays, icons, date and time inputs, and table selection, resizing, copying, editing, sorting, loading, reordering, freezing, accessibility, and full-value reveal. `0.1.0-SNAPSHOT` is local only.

For the proposed first preview, run `./gradlew -PreleaseVersion=0.1.0-alpha01 publishToMavenLocal`, check the eight artifact sets with `python3 verification/check-maven-publication.py 0.1.0-alpha01`, then build this app with `./gradlew -p verification/consumer-smoke -PbraceVersion=0.1.0-alpha01 :app:assembleDebug`. This verifies local Maven coordinates; it does not claim a Central release.
