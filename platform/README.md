# platform

The system under test: an instant-payments platform on a double-entry ledger.

- Its rules are in the [domain model](../docs/domain.md).
- It implements the OpenAPI specifications in `contracts/`. A service carries its specification in its jar, and its tests hold it to that: see [ADR 0019](../docs/adr/0019-a-service-held-to-its-contract.md).
- Its services carry their own unit and component tests.
- Its services are built on the [chassis](../chassis/README.md), which keeps the conventions of the contracts for them. Every error response is a problem document.
- Each service keeps its data in a PostgreSQL server of its own. See [ADR 0016](../docs/adr/0016-a-postgresql-database-for-each-service.md).
- It knows nothing about `automation/`.

## Services

| Service | Contract |
|---|---|
| [`account-service`](account-service) | [openapi.yaml](../contracts/account-service/openapi.yaml) |

## Test a service

A service has two levels of tests. See [ADR 0017](../docs/adr/0017-unit-tests-and-component-tests-for-a-service.md).

| Level | Lives in | Needs | Runs |
|---|---|---|---|
| Unit test | `src/test` | The JVM | A rule in plain Java, or a slice of Spring. |
| Component test | `src/componentTest` | Docker | The whole service on a real port, with its database in a container. |

```
./gradlew :platform:account-service:test
./gradlew :platform:account-service:componentTest
```

`./gradlew build` runs both.

A component test is a client of the service. It calls over HTTP, and it sets up and reads through the API alone. Its client checks every response against the contract of the service, so a response that the contract does not allow fails the test. All component tests of a service share one application and one database, so each test works on data of its own.

## Run a service

A service needs its database, so it runs in the stack. See [`deploy/`](../deploy/README.md).

`account-service` then listens on port 8080. Open `http://localhost:8080/v1/status` to see its name and version.
