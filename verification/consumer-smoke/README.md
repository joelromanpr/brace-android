# External consumer smoke project

This independent Gradle project resolves Brace **only by Maven coordinates**. From the repository root:

```sh
./gradlew :brace-foundation:publishToMavenLocal :brace-core:publishToMavenLocal
./gradlew -p verification/consumer-smoke :app:assembleDebug
```

Its settings use `mavenLocal()` before remote repositories. The source modules are not included. The sample compiles `BraceTheme` and `BraceButton` from the published AARs and exercises a stateful click. `0.1.0-SNAPSHOT` is local only; change the version after an actual public release. Verify the `-sources.jar`, `-javadoc.jar`, POM, and Gradle Module Metadata in Maven Local before claiming a publication gate has passed.
