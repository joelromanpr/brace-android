# GitHub settings

Checked on 2026-09-27. This is a short record of hosted settings that cannot be expressed fully in repository files.

## In place

- The repository is public. Issues and Discussions are on. Squash merge is the only merge method, and merged branches are deleted automatically.
- `main` requires a pull request, current `verify` and `instrumented` checks, linear history, and resolved conversations. Protection applies to admins. Zero approvals are required while there is one maintainer.
- The `v*` tag ruleset prevents tag updates and deletion. The release workflow also checks that a signed annotated tag points at the reviewed tip of `main`.
- Dependabot alerts, security fixes, and CodeQL are on. Private vulnerability reporting is enabled.
- [GitHub Pages](https://joelromanpr.github.io/brace-android/) deploys from the `main` branch through Actions. The Pages environment accepts deployments from `main` only.
- The `maven-central` environment is limited to `main`, requires the maintainer as reviewer, and has a readiness marker. The release workflow must be dispatched manually and stages artifacts for separate Portal review.

## Before the first release

1. The [Maven Central account](https://central.sonatype.com/publishing/namespaces) has a verified `io.github.joelromanpr` namespace, which covers the planned `io.github.joelromanpr.brace` group. No Brace deployment exists yet.
2. Add a dedicated Central Portal user token and signing material to the protected `maven-central` environment as `MAVEN_CENTRAL_USERNAME`, `MAVEN_CENTRAL_PASSWORD`, `SIGNING_IN_MEMORY_KEY`, and `SIGNING_IN_MEMORY_KEY_PASSWORD`. Keep values out of Git and chat. Follow the [release checklist](releasing.md).
3. Verify that the private vulnerability contact in [SECURITY.md](../../SECURITY.md) reaches the maintainer.
4. A public project board is optional. The current GitHub CLI authorization lacks `read:project`; use `gh auth refresh -s read:project -s project` before creating a board. The [component inventory](../../inventory/blueprint-components.json) remains the source of truth for coverage.

After changing a hosted setting, read it back in GitHub or the Central Portal. A checked-in workflow alone does not prove a deployment or release.
