# External consumer smoke project

This independent Gradle project resolves Brace **only by Maven coordinates**. From the repository root:

```sh
./gradlew :brace-foundation:publishToMavenLocal :brace-core:publishToMavenLocal :brace-icons:publishToMavenLocal :brace-blueprint-icons:publishToMavenLocal :brace-blueprint-icons-next:publishToMavenLocal :brace-select:publishToMavenLocal :brace-datetime:publishToMavenLocal :brace-table:publishToMavenLocal
./gradlew -p verification/consumer-smoke :app:assembleDebug
```

This separate Gradle app imports Brace by Maven coordinates from `mavenLocal()`; it does not include source modules. It exercises theme switching, core controls and navigation, forms and selection, overlays, icons, date and time inputs, and table interactions including selection, resizing, copying, editing, sorting, loading, reordering, frozen panes, accessibility, and full-value reveal. `0.1.0-SNAPSHOT` is local only. Before claiming a publication check, confirm each AAR, sources jar, documentation jar, POM, and Gradle metadata file exists in Maven Local.
