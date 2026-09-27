# Release checklist

The release workflow is manual and must use a signed annotated tag that GitHub marks verified. This is a preparation guide; there is no published Brace Android version yet.

## Before tagging

- Finish milestone issue/PRs and review the generated coverage counts. Verify every stable row links to real implementation, catalog sample, docs, and meaningful tests.
- Run `./gradlew build lint checkTokenGeneration checkBlueprintIconGeneration checkBlueprintNextIconGeneration checkInventory apiCheck` and the local Maven consumer smoke test. Review Android accessibility checks, table/overlay interaction tests, and known limitations.
- Verify `-PreleaseVersion=<version>` aligns all published modules and update `CHANGELOG.md`, migration notes, artifact list, dependency snippets, and API baselines in one release PR. Confirm Maven POM name, description, URL, Apache-2.0 license, developer, SCM metadata, sources jar, documentation jar, and signing. For `brace-blueprint-icons`, confirm the AAR includes the pinned path manifest, Apache-2.0 license, attribution/modification notice, and all 706 names; review the upstream generator audit on the release commit.
- Merge the release PR by squash after CI passes. Confirm the final `main` commit SHA.

## Tag and stage

Configure a signing key in Git and add its public key to GitHub first. See [GitHub's tag signing guide](https://docs.github.com/en/authentication/managing-commit-signature-verification/signing-tags).

```sh
git switch main
git pull --ff-only
git tag -s v0.1.0 -m "Brace Android 0.1.0"
git tag -v v0.1.0
git push origin v0.1.0
```

Use the actual release version. Confirm GitHub reports the tag as verified and that it points to the reviewed `main` commit. Run the **Release to Maven Central** workflow manually from `main` and enter that tag as its input. Its protected `maven-central` environment requires maintainer approval and the `MAVEN_CENTRAL_ENVIRONMENT_READY=configured` environment secret. It stages via `publishToMavenCentral`; the maintainer reviews the Central Portal staging deployment and explicitly releases it there. The workflow should never close and publish a deployment automatically.

## After publishing

Verify each artifact and its sources/docs files on Maven Central. Build a clean separate consumer with the released coordinates and no Maven Local. Publish GitHub Release notes with the tag, coverage counts, migration notes, and known limitations. If any validation fails, stop the release and fix forward with a new version; never replace a tag or overwrite an artifact.

## Hotfix

Reproduce against the latest released tag. Implement a focused fix with tests on a topic branch, merge to `main`, update the patch changelog/version, and run the same release gates. The release workflow requires the signed tag to point to the reviewed tip of `main`. If unrelated work on `main` cannot ship in the hotfix, prepare a temporary maintenance branch from the latest release tag and review its tests. A separate reviewed workflow change must then explicitly authorize that branch commit; the standard workflow will refuse it. Merge or cherry-pick the fix back to `main` afterward. Do not create a permanent `develop` branch.
