# maker-checker

[![CI](https://github.com/xFeuer/maker-checker/actions/workflows/ci.yml/badge.svg)](https://github.com/xFeuer/maker-checker/actions/workflows/ci.yml)

A test automation framework for financial systems, built against a small instant-payments platform that exists to be tested.

In banking, maker-checker is the control where one party makes a change and a second party checks it. In this repository the platform is the maker and the automation is the checker.

## Status

Phase 2: domain and contracts, done. The [domain model](docs/domain.md) states the rules of the platform. The contracts of its two services and of two simulators are written, with the hooks that a test needs, and they lint clean. The build renders them as an [API reference](https://xfeuer.github.io/maker-checker/api/), with a page for each [problem type](https://xfeuer.github.io/maker-checker/problems/).

Phase 1: walking skeleton, done. The first service, `account-service`, answers `GET /v1/status` and runs in a container. The first Playwright test checks it from the outside. One command builds, starts, tests and stops all of it, and leaves a report that shows each request with its response. CI runs that command for every pull request and for `main`, and it publishes the [report of the last run on `main`](https://xfeuer.github.io/maker-checker/).

Phase 3 comes next: the services. See the [roadmap](docs/roadmap.md).

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
| `chassis/` | What every application of the stack is built on: the conventions of the contracts, in code. |
| `contracts/` | OpenAPI specifications, the source of truth for both sides. |
| `deploy/` | Docker Compose files and environment configuration. |
| `build-logic/` | Gradle convention plugins and build checks, with their tests. |
| `docs/` | The roadmap, the [domain model](docs/domain.md) and the architecture decision records. |

One rule holds the design together: `automation/` never imports code from `platform/` or from `chassis/`. It knows the platform only through `contracts/` and the running system. The build enforces the rule: it fails when a project under `automation/` depends on a project under `platform/` or on the chassis. See [ADR 0002](docs/adr/0002-one-repository-with-a-black-box-boundary.md).

## Stack

| Concern | Choice |
|---|---|
| Platform | Java 25, Spring Boot 4, PostgreSQL 18 |
| Contracts | OpenAPI 3.1, linted, rendered and bundled by vacuum. A JSON Schema validator holds a service to them. |
| Automation | Playwright for Java, JUnit 6, Cucumber |
| Report | Allure |
| Build | Gradle 9, Kotlin DSL, convention plugins, version catalog |
| CI | GitHub Actions |

## Build

Requires JDK 25 and Docker.

```
./gradlew build
```

On Windows, run `gradlew.bat build`. To fix formatting, run the `spotlessApply` task.

Docker is for the component tests of the services. Each one runs a whole service against a real PostgreSQL, which Testcontainers starts in a container. See [ADR 0017](docs/adr/0017-unit-tests-and-component-tests-for-a-service.md).

The build also lints the specifications in `contracts/` with vacuum. Gradle downloads it, so there is nothing to install. See [ADR 0009](docs/adr/0009-openapi-3-1-contracts-linted-by-vacuum-in-the-build.md).

vacuum renders the specifications too. The API reference is in `contracts/build/site/api/index.html`, and the pages of the problem types are next to it. See [ADR 0015](docs/adr/0015-an-api-reference-rendered-by-vacuum-next-to-the-report.md).

vacuum also bundles each specification into one document, and a service carries the bundle of its own. The component tests of a service check every response against it. See [ADR 0019](docs/adr/0019-a-service-held-to-its-contract.md).

## Run the system tests

Requires JDK 25 and Docker.

```
./gradlew systemTest
```

This one command builds the image, starts the stack, runs the system tests against it and stops the stack. The stack stops even when a test fails. Before it stops, the run saves the logs of the stack to `build/stack-logs/stack.log`.

The stack is `account-service` and its database. A service keeps its data in a PostgreSQL server of its own, and that data lives as long as the stack. See [ADR 0016](docs/adr/0016-a-postgresql-database-for-each-service.md).

The run ends with an Allure report, also when a test fails. It is one file, `automation/suites/build/reports/allure/index.html`, and it opens in a browser as it is. See [ADR 0007](docs/adr/0007-allure-behind-a-listener-of-the-framework.md).

CI runs the same command. The report of the last run on `main` is online: <https://xfeuer.github.io/maker-checker/>. The API reference of that run is next to it: <https://xfeuer.github.io/maker-checker/api/>. Every run also keeps its report and the logs of the stack as artifacts.

After the system tests, `./gradlew site` puts the report, the API reference and the pages of the problem types into `build/site`, the way CI publishes them.

## Decisions

Every decision that is costly to reverse is recorded in [docs/adr](docs/adr/README.md).

## How this is built

This repository is built with AI assistance. I set the direction and make the decisions. Claude, an AI assistant, drafts much of the code and documentation. I review and build every change before it reaches `main`.

## License

[MIT](LICENSE)
