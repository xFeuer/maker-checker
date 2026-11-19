# automation

The test automation framework and its suites.

- It never imports code from `platform/`.
- It reads the OpenAPI specifications in `contracts/`.
- It tests the running system from the outside.

## Modules

| Module | Holds |
|---|---|
| [`core`](core) | Configuration and the Playwright lifecycle. See [ADR 0006](../docs/adr/0006-a-playwright-lifecycle-owned-by-the-framework.md). |
| [`api`](api) | Typed clients, one per service. |
| [`suites`](suites) | The tests. |

## Run the tests

The tests need the stack, so start it first. See [`deploy/`](../deploy/README.md).

```
./gradlew :automation:suites:systemTest
```

`./gradlew build` compiles them but does not run them.

## Configuration

| Environment variable | Default | Meaning |
|---|---|---|
| `ACCOUNT_SERVICE_URL` | `http://localhost:8080` | Where `account-service` answers. |
