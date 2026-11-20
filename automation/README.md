# automation

The test automation framework and its suites.

- It never imports code from `platform/`.
- It reads the OpenAPI specifications in `contracts/`.
- It tests the running system from the outside.

## Modules

| Module | Holds |
|---|---|
| [`core`](core) | Configuration and the Playwright lifecycle. See [ADR 0006](../docs/adr/0006-a-playwright-lifecycle-owned-by-the-framework.md). |
| [`api`](api) | Typed clients, one per service, and the transport that sends their requests. |
| [`allure`](allure) | The link to the report: it attaches each request and response to the Allure result of its test. See [ADR 0007](../docs/adr/0007-allure-behind-a-listener-of-the-framework.md). |
| [`suites`](suites) | The tests. |

## The boundary

The build fails when a module here depends on a project under `platform/`, directly or through another project. The task is `checkBoundary`, and `./gradlew build` runs it. See [ADR 0002](../docs/adr/0002-one-repository-with-a-black-box-boundary.md).

## Run the tests

One command builds the image, starts the stack, runs the tests and stops the stack:

```
./gradlew systemTest
```

To run the tests against a stack that is already up, start it first. See [`deploy/`](../deploy/README.md). Then run the suite alone:

```
./gradlew :automation:suites:systemTest
```

A filter such as `--tests` works only with the suite's own task. `./gradlew build` compiles the tests but does not run them.

## The report

Every run of the tests ends with an Allure report, also when a test fails:

```
automation/suites/build/reports/allure/index.html
```

It is one file that loads nothing from another host. Open it in a browser.

A typed client sends its requests through `ApiTransport`. The transport tells its listeners about every request and its response, and the `allure` module attaches them to the test that made them. A client and a test hold no reporting code.

To generate the report again from the results of the last run:

```
./gradlew allureReport
```

## Configuration

| Environment variable | Default | Meaning |
|---|---|---|
| `ACCOUNT_SERVICE_URL` | `http://localhost:8080` | Where `account-service` answers. |
