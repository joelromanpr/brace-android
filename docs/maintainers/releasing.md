# Release checklist

The release workflow is manual and requires an annotated tag signed by the Brace release key pinned in the repository. This is a preparation guide; there is no published Brace Android version yet. The first proposed version is `0.1.0-alpha01`, a preview with no claim of full component coverage or stable inventory rows.

## Before tagging

- Finish milestone issue/PRs and review the generated coverage counts. Verify every stable row links to real implementation, catalog sample, docs, and meaningful tests.
- Run `./gradlew build lint checkTokenGeneration checkBlueprintIconGeneration checkBlueprintNextIconGeneration checkInventory apiCheck` and the local Maven consumer smoke test. Review Android accessibility checks, table/overlay interaction tests, and known limitations. For the preview version, use `./gradlew -PreleaseVersion=0.1.0-alpha01 publishToMavenLocal`, `python3 verification/check-maven-publication.py 0.1.0-alpha01`, and `./gradlew -p verification/consumer-smoke -PbraceVersion=0.1.0-alpha01 :app:assembleDebug`.
- Verify `-PreleaseVersion=<version>` aligns all published modules and update `CHANGELOG.md`, migration notes, artifact list, dependency snippets, and API baselines in one release PR. Confirm Maven POM name, description, URL, Apache-2.0 license, developer, SCM metadata, sources jar, documentation jar, and signing. For `brace-blueprint-icons`, confirm the AAR includes the pinned path manifest, Apache-2.0 license, attribution/modification notice, and all 706 names; review the upstream generator audit on the release commit.
- Merge the release PR by squash after CI passes. Confirm the final `main` commit SHA.

## Tag and stage

The dedicated release key has fingerprint `F152 FD63 0BE9 9A9B B248 3995 BA23 075E 89D1 23B5`. Confirm the local private key matches [the pinned public key](../../.github/release-signing-key.asc). On 2026-09-27, the public key was retrieved from [Ubuntu's keyserver](https://keyserver.ubuntu.com/pks/lookup?op=get&search=0xF152FD630BE99A9BB2483995BA23075E89D123B5) with that exact fingerprint; Ubuntu is [supported by Sonatype](https://central.sonatype.org/publish/requirements/gpg/). Recheck retrieval before staging. CI imports the pinned key and checks both the tag signature and fingerprint. A GitHub Verified badge is useful when available, but is not the release gate.

```sh
git switch main
git pull --ff-only
git -c user.signingkey=F152FD630BE99A9BB2483995BA23075E89D123B5 tag -s v0.1.0-alpha01 -m "Brace Android 0.1.0-alpha01"
git tag -v v0.1.0-alpha01
git push origin v0.1.0-alpha01
```

Use the actual release version. Confirm the tag points to the reviewed `main` commit. Run the **Release to Maven Central** workflow manually from `main` and enter that tag as its input. The workflow verifies the tag against the pinned public key, then checks the Maven Local artifacts and independent consumer before requesting approval for its protected `maven-central` stage. That environment needs the readiness marker, a Central Portal user token, and an in-memory signing key and password; see [GitHub settings](github-settings.md). It stages via `publishToMavenCentral`; the maintainer reviews the validated deployment in the Central Portal and explicitly publishes it there. The workflow never publishes a deployment automatically.

## After publishing

Verify each artifact and its sources/docs files on Maven Central. Build a clean separate consumer with the released coordinates and no Maven Local. Publish GitHub Release notes with the tag, coverage counts, migration notes, and known limitations. If any validation fails, stop the release and fix forward with a new version; never replace a tag or overwrite an artifact.

## Hotfix

Reproduce against the latest released tag. Implement a focused fix with tests on a topic branch, merge to `main`, update the patch changelog/version, and run the same release gates. The release workflow requires the signed tag to point to the reviewed tip of `main`. If unrelated work on `main` cannot ship in the hotfix, prepare a temporary maintenance branch from the latest release tag and review its tests. A separate reviewed workflow change must then explicitly authorize that branch commit; the standard workflow will refuse it. Merge or cherry-pick the fix back to `main` afterward. Do not create a permanent `develop` branch.
