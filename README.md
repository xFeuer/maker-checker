# maker-checker

[![CI](https://github.com/xFeuer/maker-checker/actions/workflows/ci.yml/badge.svg)](https://github.com/xFeuer/maker-checker/actions/workflows/ci.yml)

A test automation framework for financial systems, built against a small instant-payments platform that exists to be tested.

In banking, maker-checker is the control where one party makes a change and a second party checks it. In this repository the platform is the maker and the automation is the checker.

## Status

Phase 1: walking skeleton, done. The first service, `account-service`, answers `GET /v1/status` and runs in a container. The first Playwright test checks it from the outside. One command builds, starts, tests and stops all of it, and leaves a report that shows each request with its response. CI runs that command for every pull request and for `main`, and it publishes the [report of the last run on `main`](https://xfeuer.github.io/maker-checker/). Phase 2 comes next: the domain and its contracts. See the [roadmap](docs/roadmap.md).

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
| `build-logic/` | Gradle convention plugins and build checks, with their tests. |
| `docs/` | The roadmap and the architecture decision records. |

One rule holds the design together: `automation/` never imports code from `platform/`. It knows the platform only through `contracts/` and the running system. The build enforces the rule: it fails when a project under `automation/` depends on a project under `platform/`. See [ADR 0002](docs/adr/0002-one-repository-with-a-black-box-boundary.md).

## Stack

| Concern | Choice |
|---|---|
| Platform | Java 25, Spring Boot 4 |
| Automation | Playwright for Java, JUnit 6, Cucumber |
| Report | Allure |
| Build | Gradle 9, Kotlin DSL, convention plugins, version catalog |
| CI | GitHub Actions |

## Build

Requires JDK 25.

```
./gradlew build
```

On Windows, run `gradlew.bat build`. To fix formatting, run the `spotlessApply` task.

## Run the system tests

Requires JDK 25 and Docker.

```
./gradlew systemTest
```

This one command builds the image, starts the stack, runs the system tests against it and stops the stack. The stack stops even when a test fails. Before it stops, the run saves the logs of the stack to `build/stack-logs/stack.log`.

The run ends with an Allure report, also when a test fails. It is one file, `automation/suites/build/reports/allure/index.html`, and it opens in a browser as it is. See [ADR 0007](docs/adr/0007-allure-behind-a-listener-of-the-framework.md).

CI runs the same command. The report of the last run on `main` is online: <https://xfeuer.github.io/maker-checker/>. Every run also keeps its report and the logs of the stack as artifacts.

## Decisions

Every decision that is costly to reverse is recorded in [docs/adr](docs/adr/README.md).

## How this is built

This repository is built with AI assistance. I set the direction and make the decisions. Claude, an AI assistant, drafts much of the code and documentation. I review and build every change before it reaches `main`.

## License

[MIT](LICENSE)
