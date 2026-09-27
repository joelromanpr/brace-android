# External consumer smoke project

This independent Gradle project resolves Brace **only by Maven coordinates**. From the repository root:

```sh
./gradlew :brace-foundation:publishToMavenLocal :brace-core:publishToMavenLocal :brace-icons:publishToMavenLocal :brace-blueprint-icons:publishToMavenLocal :brace-blueprint-icons-next:publishToMavenLocal :brace-select:publishToMavenLocal :brace-datetime:publishToMavenLocal :brace-table:publishToMavenLocal
./gradlew -p verification/consumer-smoke :app:assembleDebug
```

This separate Android app resolves Brace from `mavenLocal()` by Maven coordinates. It compiles examples for theming, controls, Collapse and Text, PanelStack, forms, overlays, icons, selection, date/time, and editable data tables, including column names. The `0.1.0-SNAPSHOT` version is local only. Check that each published artifact includes its AAR, sources, documentation JAR, POM, and Gradle metadata before claiming a publication check.
