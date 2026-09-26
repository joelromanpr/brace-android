# External consumer smoke project

This independent Gradle project resolves Brace **only by Maven coordinates**. From the repository root:

```sh
./gradlew :brace-foundation:publishToMavenLocal :brace-core:publishToMavenLocal :brace-icons:publishToMavenLocal :brace-select:publishToMavenLocal :brace-datetime:publishToMavenLocal :brace-table:publishToMavenLocal
./gradlew -p verification/consumer-smoke :app:assembleDebug
```

Its settings use `mavenLocal()` before remote repositories. The source modules are not included. The sample compiles the theme, core controls, radio and segmented choices, top bar, overlays, context menus, shortcut scopes and discovery, form fields, text areas, inline editable text, scoped icon registry and icon actions, select and query state, localized date controls, a virtualized data table, tooltip, and toast from published AARs. Its context-menu, shortcut, radio, top bar, datetime, table, and icon usage checks those public APIs without including source modules. `0.1.0-SNAPSHOT` is local only; change the version after an actual public release. Verify the `-sources.jar`, `-javadoc.jar`, POM, and Gradle Module Metadata in Maven Local before claiming a publication gate has passed.
