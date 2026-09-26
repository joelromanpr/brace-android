# GitHub repository settings

This page distinguishes configured repository controls from actions still needed. The workflows in this repository are source files; hosted checks, Pages, and Maven Central each need separate verification.

## Configured for the M1 pull request

- Discussions and Issues are enabled. Squash merge is the only merge method; merged topic branches are deleted automatically.
- `main` branch protection requires a pull request, the `verify` and `instrumented` checks, an up-to-date branch, linear history, and resolved review conversations. It applies to admins and blocks force pushes and deletion. The required approval count is **zero** while there is one maintainer; CODEOWNERS approval is not required, so the solo maintainer can review and merge without a second account.
- The active `Immutable release tags` ruleset blocks updates and deletion of `v*` tags. The release workflow separately requires a GitHub-verified signed annotated tag on `main`.
- Labels cover inventory claims, core/select/datetime/icons/table families, design tokens, experimental work, releases, accessibility, bugs, and documentation.
- Dependabot vulnerability alerts and automated security fixes are enabled; `dependabot.yml` supplies scheduled Gradle and Actions update pull requests.
- The M1 draft PR is [#1](https://github.com/joelromanpr/brace-android/pull/1). Confirm its hosted checks and review before squash merge.

## Remaining hosted setup

1. **Visibility and Pages:** the repository is currently private. A public open-source project and public Pages site require a reviewed visibility change. After the M1 workflow reaches `main`, set **Settings → Pages → Build and deployment → GitHub Actions**. Restrict the `github-pages` environment to `main`, verify the deployed site and coverage counts, then set the repository homepage URL. [GitHub Pages setup](https://docs.github.com/en/pages/getting-started-with-github-pages/using-custom-workflows-with-github-pages).
2. **Security:** enable private vulnerability reporting where available, confirm the `SECURITY.md` contact path works, and review the first successful hosted CodeQL result before claiming scanning is active.
3. **Public project board:** create a board with inventory ID, milestone, status, and priority fields; link component issues to inventory rows. The board is a work view, while `inventory/blueprint-components.json` remains the coverage authority. The current GitHub CLI token lacks `read:project`; refresh it with `gh auth refresh -s read:project -s project`, then create or configure the board. Do not copy a token into repository files.
4. **Maven Central release environment:** create `maven-central`, restrict deployments to `main`, and require maintainer approval. Keep “prevent self-review” off while there is one maintainer. Environment reviewer rules on GitHub Free/Pro/Team are available for public repositories; make the repository public before relying on that gate. Add environment secrets `MAVEN_CENTRAL_USERNAME`, `MAVEN_CENTRAL_PASSWORD`, `SIGNING_IN_MEMORY_KEY`, and `SIGNING_IN_MEMORY_KEY_PASSWORD` only there. Set `MAVEN_CENTRAL_ENVIRONMENT_READY=configured` only after the approval rule is active. The release workflow fails closed without it.
5. **Sonatype Central Portal:** verify ownership of the exact namespace `io.github.joelromanpr.brace`, create a Portal user token, and confirm signing material is available. Keep credentials in maintainer-controlled secret fields. The [release checklist](releasing.md) stages artifacts after an explicit signed-tag workflow dispatch; publication in the Portal is a separate maintainer action.

After each setting change, read it back through GitHub settings or the API. Never infer hosted success from a local workflow file.
