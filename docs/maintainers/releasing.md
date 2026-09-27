# Release checklist

The release workflow is manual and requires an annotated tag signed by the Brace release key pinned in the repository. `0.1.0-alpha01` is the first published preview, with no claim of full component coverage or stable inventory rows. Choose a new version for every later release; Maven Central versions and signed tags are immutable.

## Before tagging

- Finish milestone issue/PRs and review the generated coverage counts. Verify every stable row links to real implementation, catalog sample, docs, and meaningful tests.
- Run `./gradlew build lint checkTokenGeneration checkBlueprintIconGeneration checkBlueprintNextIconGeneration checkInventory apiCheck` and the local Maven consumer smoke test. Review Android accessibility checks, table/overlay interaction tests, and known limitations. For the chosen version, run `./gradlew -PreleaseVersion=<version> publishToMavenLocal`, `python3 verification/check-maven-publication.py <version>`, and `./gradlew -p verification/consumer-smoke -PbraceVersion=<version> :app:assembleDebug`.
- Verify `-PreleaseVersion=<version>` aligns all published modules and update `CHANGELOG.md`, migration notes, artifact list, dependency snippets, and API baselines in one release PR. Confirm Maven POM name, description, URL, Apache-2.0 license, developer, SCM metadata, sources jar, documentation jar, and signing. For `brace-blueprint-icons`, confirm the AAR includes the pinned path manifest, Apache-2.0 license, attribution/modification notice, and all 706 names; review the upstream generator audit on the release commit.
- Merge the release PR by squash after CI passes. Confirm the final `main` commit SHA.

## Tag and stage

The dedicated release key has fingerprint `F152 FD63 0BE9 9A9B B248 3995 BA23 075E 89D1 23B5`. Confirm the local private key matches [the pinned public key](../../.github/release-signing-key.asc). On 2026-09-27, the public key was retrieved from [Ubuntu's keyserver](https://keyserver.ubuntu.com/pks/lookup?op=get&search=0xF152FD630BE99A9BB2483995BA23075E89D123B5) with that exact fingerprint; Ubuntu is [supported by Sonatype](https://central.sonatype.org/publish/requirements/gpg/). Recheck retrieval before staging. CI imports the pinned key and checks both the tag signature and fingerprint. A GitHub Verified badge is useful when available, but is not the release gate.

```sh
VERSION=0.1.0-alpha02 # replace with the approved next version
git switch main
git pull --ff-only
git -c user.signingkey=F152FD630BE99A9BB2483995BA23075E89D123B5 tag -s "v$VERSION" -m "Brace Android $VERSION"
git tag -v "v$VERSION"
git push origin "v$VERSION"
```

Use the actual release version. Confirm the tag points to the reviewed `main` commit. Run the **Release to Maven Central** workflow manually from `main` and enter that tag as its input. The workflow verifies the tag against the pinned public key, then checks eight Maven Local artifact sets and the independent consumer. Inside the protected `maven-central` stage it builds a signed candidate and verifies all 40 AAR, POM, module, sources, and docs signatures against that key before uploading. The environment needs the readiness marker, a Central Portal user token, and an in-memory signing key and password; see [GitHub settings](github-settings.md). It stages via `publishToMavenCentral(automaticRelease = false)`; the workflow never publishes automatically.

In the [Portal deployments view](https://central.sonatype.com/publishing/deployments), inspect every deployment created by this workflow run or a retry. The pinned Vanniktech plugin shares an upload service across projects in one Gradle invocation, so one bundle containing eight modules is expected, but the Portal is the authority. Confirm a `VALIDATED` set with exactly one copy of each `io.github.joelromanpr.brace` artifact at the tagged version: `brace-foundation`, `brace-core`, `brace-icons`, `brace-blueprint-icons`, `brace-blueprint-icons-next`, `brace-select`, `brace-datetime`, and `brace-table`. Review validation errors, files, and signatures. If the Portal shows multiple deployments, account for all eight modules without duplicate coordinates before publishing each required validated deployment. Do not publish an incomplete or failed set. Record the deployment ID or IDs and only then use the Portal's manual **Publish** action; wait for `PUBLISHED` and verify all eight artifacts remotely.

## After publishing

Verify each artifact and its sources/docs files on Maven Central. Build the [separate consumer](../../verification/consumer-smoke/README.md) with `-PbraceRepository=central`, the released version, and a fresh `GRADLE_USER_HOME` so Maven Local and cached Brace artifacts cannot mask resolution failures. Publish GitHub Release notes with the tag, coverage counts, migration notes, and known limitations. If any validation fails, stop the release and fix forward with a new version; never replace a tag or overwrite an artifact.

## Hotfix

Reproduce against the latest released tag. Implement a focused fix with tests on a topic branch, merge to `main`, update the patch changelog/version, and run the same release gates. The release workflow requires the signed tag to point to the reviewed tip of `main`. If unrelated work on `main` cannot ship in the hotfix, prepare a temporary maintenance branch from the latest release tag and review its tests. A separate reviewed workflow change must then explicitly authorize that branch commit; the standard workflow will refuse it. Merge or cherry-pick the fix back to `main` afterward. Do not create a permanent `develop` branch.
