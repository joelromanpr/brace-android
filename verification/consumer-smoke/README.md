# External consumer smoke project

This independent Gradle project resolves Brace **only by Maven coordinates**. From the repository root:

```sh
./gradlew :brace-foundation:publishToMavenLocal :brace-core:publishToMavenLocal :brace-icons:publishToMavenLocal :brace-blueprint-icons:publishToMavenLocal :brace-blueprint-icons-next:publishToMavenLocal :brace-select:publishToMavenLocal :brace-datetime:publishToMavenLocal :brace-table:publishToMavenLocal
./gradlew -p verification/consumer-smoke :app:assembleDebug
```

This separate Gradle app imports Brace by Maven coordinates from `mavenLocal()`; it does not include source modules. It exercises the theme, core controls, radio and segmented choices, top bar, saveable PanelStack, overlays, context menus, shortcuts, form fields, text areas, inline editable text, TagInput, scoped icons and pinned Blueprint legacy and `/next` packs, select and query state, localized date controls, virtualized table selection, resizing and copying, links, tooltips, and toasts. `0.1.0-SNAPSHOT` is local only. Before claiming a publication check, confirm each AAR, sources jar, documentation jar, POM, and Gradle metadata file exists in Maven Local.
