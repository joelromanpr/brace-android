# Governance

Brace Android is maintained in public. The founding maintainer is [@joelromanpr](https://github.com/joelromanpr). Additional maintainers are added after sustained review and contribution work, with responsibilities recorded here before granting repository access.

Proposals start in an issue or Discussion. The maintainer decides API and design direction after considering feedback, test evidence, Android conventions, and the pinned Blueprint scope. A decision can be revisited when new evidence appears. Security reports use the private process in [SECURITY.md](SECURITY.md).

`main` is the integration branch. Contributors use short-lived branches and focused pull requests; passing required CI and a squash merge create the auditable record. Stable API changes follow [compatibility policy](docs/compatibility.md). The maintainer owns release approval, signing, and Maven Central publication. Governance and release rules should not require a second reviewer while the project has one maintainer.
