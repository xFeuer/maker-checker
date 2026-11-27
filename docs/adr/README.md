# Architecture decision records

An ADR records one decision that is costly to reverse, with its context and its consequences.

## Adding one

1. Copy `template.md` to `NNNN-short-title.md`, using the next number.
2. Fill it in with the status `Proposed` and open a pull request.
3. Set the status to `Accepted` when the pull request merges.
4. Add it to the index below.

An accepted ADR is not edited to change its decision. A new ADR supersedes it.

## Index

| # | Decision | Status |
|---|---|---|
| [0001](0001-record-architecture-decisions.md) | Record architecture decisions | Accepted |
| [0002](0002-one-repository-with-a-black-box-boundary.md) | One repository with a black-box boundary | Accepted |
| [0003](0003-java-and-spring-boot-for-the-platform.md) | Java and Spring Boot for the platform | Accepted |
| [0004](0004-playwright-for-java-on-the-api-and-ui-layers.md) | Playwright for Java on the API and UI layers | Accepted |
| [0005](0005-junit-platform-with-cucumber-on-top.md) | JUnit Platform with Cucumber on top | Accepted |
| [0006](0006-a-playwright-lifecycle-owned-by-the-framework.md) | A Playwright lifecycle owned by the framework | Accepted |
| [0007](0007-allure-behind-a-listener-of-the-framework.md) | Allure behind a listener of the framework | Accepted |
| [0008](0008-two-services-and-a-simulator-of-the-clearing-network.md) | Two services and a simulator of the clearing network | Accepted |
| [0009](0009-openapi-3-1-contracts-linted-by-vacuum-in-the-build.md) | OpenAPI 3.1 contracts, linted by vacuum in the build | Accepted |
| [0010](0010-problem-details-for-every-error-response.md) | Problem details for every error response | Accepted |
| [0011](0011-money-as-a-decimal-string-with-its-currency.md) | Money as a decimal string with its currency | Accepted |
| [0012](0012-an-idempotency-key-for-every-post.md) | An idempotency key for every POST | Accepted |
| [0013](0013-access-tokens-from-an-identity-provider-outside-the-platform.md) | Access tokens from an identity provider outside the platform | Accepted |
| [0014](0014-testability-hooks-in-the-services-off-by-default.md) | Testability hooks in the services, off by default | Accepted |
| [0015](0015-an-api-reference-rendered-by-vacuum-next-to-the-report.md) | An API reference rendered by vacuum, next to the report | Accepted |
| [0016](0016-a-postgresql-database-for-each-service.md) | A PostgreSQL database for each service | Accepted |
| [0017](0017-unit-tests-and-component-tests-for-a-service.md) | Unit tests and component tests for a service | Accepted |
| [0018](0018-a-chassis-for-every-application.md) | A chassis for every application | Accepted |
| [0019](0019-a-service-held-to-its-contract.md) | A service held to its contract | Accepted |
