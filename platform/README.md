# platform

The system under test: an instant-payments platform on a double-entry ledger.

- Its rules are in the [domain model](../docs/domain.md).
- It implements the OpenAPI specifications in `contracts/`.
- Its services carry their own unit and component tests.
- It knows nothing about `automation/`.

## Services

| Service | Contract |
|---|---|
| [`account-service`](account-service) | [openapi.yaml](../contracts/account-service/openapi.yaml) |

## Run a service

```
./gradlew :platform:account-service:bootRun
```

The service listens on port 8080. Open `http://localhost:8080/v1/status` to see its name and version.

To run it in a container, see [`deploy/`](../deploy/README.md).
