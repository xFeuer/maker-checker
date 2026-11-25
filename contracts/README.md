# contracts

The OpenAPI specifications of the platform's APIs.

- They are written before the code.
- `platform/` implements them.
- `automation/` tests against them.

Each service has its own directory: `contracts/<service>/openapi.yaml`.

| Service | Specification |
|---|---|
| `account-service` | [openapi.yaml](account-service/openapi.yaml) |

## Conventions

- A specification is written in OpenAPI 3.1.2.
- Its `servers` block names the address of the service in the local stack, as `deploy/compose.yaml` publishes it.

## Lint

The build lints every specification with [vacuum](https://github.com/daveshanley/vacuum). See [ADR 0009](../docs/adr/0009-openapi-3-1-contracts-linted-by-vacuum-in-the-build.md).

```
./gradlew :contracts:check
```

- The rules are in [ruleset.yaml](ruleset.yaml).
- A specification is clean when vacuum reports nothing, at any level. One finding fails the build.
- A rule that does not fit is switched off in the ruleset, with the reason next to it.
- The findings of the last run are in `build/reports/vacuum/findings.txt`.

Gradle downloads vacuum from its GitHub release and checks the archive against a checksum, so there is nothing to install. vacuum has releases for Linux, macOS and Windows, on x86-64 and ARM64.

### Raising vacuum

Dependabot does not follow GitHub releases, so this is done by hand.

1. Set `vacuum` in `gradle/libs.versions.toml` to the new version.
2. Replace `gradle/vacuum-checksums.txt` with the `checksums.txt` of that release.
3. Run `./gradlew build`.
