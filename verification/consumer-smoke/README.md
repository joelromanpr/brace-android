# External consumer smoke project

This independent Gradle project resolves Brace **only by Maven coordinates**. From the repository root:

```sh
./gradlew :brace-foundation:publishToMavenLocal :brace-core:publishToMavenLocal :brace-icons:publishToMavenLocal :brace-blueprint-icons:publishToMavenLocal :brace-blueprint-icons-next:publishToMavenLocal :brace-select:publishToMavenLocal :brace-datetime:publishToMavenLocal :brace-table:publishToMavenLocal
./gradlew -p verification/consumer-smoke :app:assembleDebug
```

This separate Gradle app imports Brace by Maven coordinates from `mavenLocal()`; it does not include source modules. It exercises the theme, core controls, radio and segmented choices, numeric and multi-handle sliders, TagInput, overlays, context menus, shortcuts, forms, saveable PanelStack navigation, icons, select and suggestion controls, date and time inputs, links, tooltip, toast, and table selection, resizing, copying, single-cell editing, and controlled header renaming, and sorting. `0.1.0-SNAPSHOT` is local only. Before claiming a publication check, confirm each AAR, sources jar, documentation jar, POM, and Gradle metadata file exists in Maven Local.
