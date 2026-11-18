# maker-checker

[![CI](https://github.com/xFeuer/maker-checker/actions/workflows/ci.yml/badge.svg)](https://github.com/xFeuer/maker-checker/actions/workflows/ci.yml)

A test automation framework for financial systems, built against a small instant-payments platform that exists to be tested.

In banking, maker-checker is the control where one party makes a change and a second party checks it. In this repository the platform is the maker and the automation is the checker.

## Status

Phase 1: walking skeleton, in progress. The first service, `account-service`, answers `GET /v1/status`. The container image, the first Playwright test and the published report come next. See the [roadmap](docs/roadmap.md).

## What it will show

- A layered framework: configuration, typed API clients, page and component objects, test data and suites.
- One tool on both layers: Playwright for Java drives the API tests and the UI tests.
- The test problems that make finance hard: idempotency, double-spend under concurrency, ledger invariants, money precision, state transitions, eventual consistency, customer isolation, audit trail and fault injection.
- Evidence: reports with traces, published from CI.

## Layout

| Path | Holds |
|---|---|
| `automation/` | The framework and its suites. This is the point of the repository. |
| `platform/` | The system under test: the services and the UI. |
| `contracts/` | OpenAPI specifications, the source of truth for both sides. |
| `deploy/` | Docker Compose files and environment configuration. |
| `build-logic/` | Gradle convention plugins. |
| `docs/` | The roadmap and the architecture decision records. |

One rule holds the design together: `automation/` never imports code from `platform/`. It knows the platform only through `contracts/` and the running system. See [ADR 0002](docs/adr/0002-one-repository-with-a-black-box-boundary.md).

## Stack

| Concern | Choice |
|---|---|
| Platform | Java 25, Spring Boot 4.1 |
| Automation | Playwright for Java, JUnit 6, Cucumber |
| Build | Gradle 9, Kotlin DSL, convention plugins, version catalog |
| CI | GitHub Actions |

## Build

Requires JDK 25.

```
./gradlew build
```

On Windows, run `gradlew.bat build`. To fix formatting, run the `spotlessApply` task.

## Decisions

Every decision that is costly to reverse is recorded in [docs/adr](docs/adr/README.md).

## How this is built

This repository is built with AI assistance. I set the direction and make the decisions. Claude, an AI assistant, drafts much of the code and documentation. I review and build every change before it reaches `main`.

## License

[MIT](LICENSE)
