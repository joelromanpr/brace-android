# Repository settings

Brace Android is public. `main` requires a pull request, current `verify` and `instrumented` checks, resolved conversations, and linear history. Squash merge is the only merge method; merged topic branches are deleted. A solo maintainer can merge without a second approver. The `v*` tag ruleset blocks tag updates and deletion.

[GitHub Pages](https://joelromanpr.github.io/brace-android/) deploys from Actions on `main`. The [public project board](https://github.com/users/joelromanpr/projects/1) tracks work; the [inventory](../../inventory/blueprint-components.json) remains the component and status record. Issues, Discussions, dependency updates, security alerts, and CodeQL are enabled.

## Release setup

The protected `maven-central` environment is restricted to `main` and requires maintainer review. On 2026-09-27, the readiness marker and four credential secrets were present: `MAVEN_CENTRAL_ENVIRONMENT_READY`, `MAVEN_CENTRAL_USERNAME`, `MAVEN_CENTRAL_PASSWORD`, `SIGNING_IN_MEMORY_KEY`, and `SIGNING_IN_MEMORY_KEY_PASSWORD`. Secret values are not stored in the repository. Confirm their presence again before a release.

The release workflow checks an annotated tag against [the pinned public key](../../.github/release-signing-key.asc) and the reviewed tip of `main`; it does not depend on a GitHub Verified badge. The public signing key has been submitted to `pgp.mit.edu`, a [keyserver supported by Sonatype](https://central.sonatype.org/publish/requirements/gpg/). Recheck that it can be retrieved before staging.

The `io.github.joelromanpr` Central namespace was previously observed as verified. Recheck ownership in the [Central Portal](https://central.sonatype.com/publishing/namespaces) before the first stage. A successful workflow upload creates a deployment for separate maintainer review and manual publication. No public Maven Central release is claimed here; follow the [release checklist](releasing.md) and verify artifacts after publication.
