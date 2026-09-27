# Maintainer guide

## Repository administration

See [GitHub settings checklist](docs/maintainers/github-settings.md). Keep `main` protected through pull requests and required CI, permit the sole maintainer to merge after checks without an impossible self-approval rule, and use squash merges. Enable Discussions, private vulnerability reporting, dependency alerts, and Pages when available. Do not grant publication credentials to PR workflows.

## Release process

1. Complete the [release checklist](docs/maintainers/releasing.md) on a focused branch. Review inventory claims and generated counts against shipped APIs.
2. Confirm build, lint, tests, docs, token generation, inventory validation, API compatibility, and local Maven consumption on the release commit.
3. Update `CHANGELOG.md`, compatibility notes, and release documentation in one PR; verify `-PreleaseVersion=<version>` aligns every public artifact. Squash merge after CI.
4. Create a **signed annotated** `vMAJOR.MINOR.PATCH` tag on that exact `main` commit. Verify it with `git tag -v` and GitHub's verified badge. Tags are immutable release records; never move one.
5. Trigger the manual release workflow against that tag. The protected `maven-central` environment, its `MAVEN_CENTRAL_ENVIRONMENT_READY=configured` sentinel, and signing/portal credentials must be configured by the maintainer. Approve the deployment only after reviewing the tag, staged artifacts, metadata, sources, docs jars, and checksums.
6. After publication, verify Maven Central coordinates and a fresh consumer build, then create a GitHub Release referencing the tag and changelog entry.

For a hotfix, branch from the latest release tag, make a focused fix and tests, merge it to `main` (cherry-pick if necessary), and release a new patch version from `main`. Do not rewrite a release tag or publish the same version twice.

No automatic CI event publishes to Maven Central. The maintainer explicitly starts and approves publication.
