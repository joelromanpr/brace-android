# Compatibility and versioning

Brace Android uses one version across all public Gradle artifacts. `1.0.0` is the current Maven Central release. The [component list](coverage.md) records what each component supports and which reviews are still open.

Versions follow Semantic Versioning. A stable public Compose API removal, incompatible signature change, or meaningful behavior change requires a major version. New compatible APIs and new components use minor versions; fixes use patch versions. Breaking changes are called out in the changelog and migration notes.

Public APIs include documented composables, state types, token and theme customization contracts, and published artifact coordinates. Internal and experimental APIs can change; experimental status is explicit in the coverage inventory and KDoc. Deprecations should include a replacement path and remain for at least one minor release when practical. API compatibility checks compare each artifact against its committed baseline during CI. Binary compatibility does not replace interaction or visual review.

The build files define the exact supported Android, Kotlin, Compose, and JDK ranges for each release. A release note records changes to those minimums. New modules should depend in one direction, from specialized modules toward foundation and core, and use the same published version.

## Material 3 interoperability

BraceTheme owns Brace's semantic colors, typography, density, component tokens, and interactions. An application may place Material 3 and Brace components in one Compose hierarchy. Brace does not silently replace `MaterialTheme`, nor should a component read Material colors as its default. Where explicit bridges are provided, developers opt in and can scope them to a subtree. See [theming](theming.md) for the current public API and examples.
