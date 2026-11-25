# 0009. OpenAPI 3.1 contracts, linted by vacuum in the build

- Status: Accepted
- Date: 2026-11-25

## Context

The contracts are the source of truth for both sides of the repository ([ADR 0002](0002-one-repository-with-a-black-box-boundary.md)). They are written by hand, before the code. Phase 2 is done when they lint clean, so "clean" needs a linter, a set of rules and a place in the build.

The rules will grow. The conventions of the API, such as the error model and the idempotency key, become rules of our own, so that no specification can drift from them.

The build needs only a JDK. Gradle resolves every other tool, and that already decided the generator of the report ([ADR 0007](0007-allure-behind-a-listener-of-the-framework.md)). None of the linters that were weighed runs on the JDK: two run on Node, and one is a program for one platform.

## Decision

- **The contracts are written in OpenAPI 3.1.2.**
- **vacuum lints them.** The rules are in `contracts/ruleset.yaml`, in the format of Spectral. They start from the rules that vacuum recommends.
- **Clean means no finding.** The build fails on a finding of any level. A rule that does not fit is switched off in the ruleset, with the reason next to it.
- **The build runs vacuum itself.** `contracts` is a Gradle project, and its `check` lints every specification. Gradle resolves the archive of vacuum from its GitHub release, through a repository in `settings.gradle.kts` that serves this one module. A task of the build unpacks the archive and another one runs the program.
- **The build checks the archive.** The checksums file of the release lies in the repository, next to the version. The archive is unpacked only when its SHA-256 is the one in that file.

## Options considered

Three linters ran in a spike, on the first specification and on a sample with ten planted defects. Each one found all ten, and each one reads OpenAPI 3.1.2 and 3.2.1.

The linter:

- **vacuum.** Chosen. It is one program without a runtime, so the build still needs only a JDK. It reads the rulesets of Spectral, so our rules stay in a format that a second linter reads. It contacts no other host when the build runs it. One person writes most of it, and since 2022 its version starts with 0.
- **Spectral.** The linter that the ruleset format comes from, maintained by a company. It runs on Node and installs about 240 npm packages.
- **Redocly CLI.** One npm package, maintained by a company, and it also renders an API reference. It runs on Node, and it sends telemetry unless a setting turns that off. A rule of the kind we expect, that every `POST` takes an idempotency key, needed JavaScript there and only YAML in the other two.

How the build gets the linter:

- **Gradle resolves the archive from the GitHub release.** Chosen. It is how the build gets Allure Report 2, with one difference: vacuum is not on Maven Central, so the settings name one more repository, for this module only.
- **Node as a requirement of the build,** with the linter from npm. It is the usual way for Spectral and Redocly CLI, and Dependabot would raise the linter. Every machine that builds would need Node, the runner of CI included.
- **Node from the build.** Gradle can download Node the way it downloads vacuum. That is two downloads and a runtime to keep current.
- **The Node inside the Playwright driver.** The driver bundle of Playwright for Java carries Node for each of its platforms, and Redocly CLI runs on it. The layout of that bundle is not an API.
- **A container.** vacuum and Redocly CLI also ship as images. The build would then need Docker, which only the system tests need today.

The OpenAPI version:

- **3.1.2.** Chosen. It is the last patch of the line that the first specification already used, and the three linters read it.
- **3.2.1.** The current version. The 3.2 line adds the `QUERY` method, a hierarchy of tags and streaming media types, and no contract here needs one of them yet. The linters read it. The tools of the later phases were not checked against it.

The integrity of the archive:

- **The checksums file of the release, in the repository.** Chosen. The file is the `checksums.txt` of the release, unchanged, so raising vacuum replaces one file. It ties the build to the archive that the release held when the version was set. It does not prove who built that release.
- **The dependency verification of Gradle.** It is the tool that Gradle has for this. It checks every artifact of a build, so checking this one module takes a rule that trusts all the others.
- **No check.** A file on a release page can be replaced later, and nothing would notice.

## Consequences

- The build still needs a JDK and nothing else. It downloads from one more host, `github.com`.
- vacuum has releases for Linux, macOS and Windows, on x86-64 and ARM64. On another platform the lint cannot run, and every other task still can.
- Dependabot does not follow GitHub releases. Raising vacuum is done by hand: the version in `gradle/libs.versions.toml` and the checksums file next to it. `contracts/README.md` has the steps.
- The build owns the two tasks. They have to follow vacuum when the layout of its release or its command line changes.
- If vacuum falls behind, Spectral reads the same ruleset. That move costs the build that needs only a JDK, and the built-in rules of the two differ in places: the recommended set of Spectral also asks for a contact in `info`.
- The rules of our own arrive with the conventions of the API. Each one then fails the build like a built-in rule.
- A move to OpenAPI 3.2 is one line in each specification, once a contract needs it.
