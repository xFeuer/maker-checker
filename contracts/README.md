# contracts

The OpenAPI specifications of the platform's APIs.

- They are written before the code.
- `platform/` implements them.
- `automation/` tests against them.

Each service has its own directory, with two specifications:

- `openapi.yaml` is the API of the service.
- `hooks.yaml` holds its testability hooks: what a test needs, and production never has.

| Service | API | Hooks |
|---|---|---|
| `account-service` | [openapi.yaml](account-service/openapi.yaml) | [hooks.yaml](account-service/hooks.yaml) |
| `payment-service` | not written yet | [hooks.yaml](payment-service/hooks.yaml) |

The parts that the specifications share are in [common/components.yaml](common/components.yaml): the access token, the problem document, money, the idempotency key and the clock of the hooks. A specification refers to them and does not copy them.

## Conventions

Six conventions have a decision record:

- **Errors.** Every 4xx and 5xx response is a problem document of RFC 9457. A client relies on its `type` and its `status`. See [ADR 0010](../docs/adr/0010-problem-details-for-every-error-response.md).
- **Money.** Money is an object such as `{ "amount": "10.50", "currency": "BRL" }`. The amount is a string with exactly two decimals, and no schema has the type `number`. See [ADR 0011](../docs/adr/0011-money-as-a-decimal-string-with-its-currency.md).
- **Idempotency.** Every `POST` takes an `Idempotency-Key`. Every other change is a `PUT` or a `DELETE` on a URL that names the thing. See [ADR 0012](../docs/adr/0012-an-idempotency-key-for-every-post.md).
- **Callers.** A caller sends an access token of the identity provider, as a bearer token. Its scopes say what the caller may do, and every operation names the scopes that it needs. See [ADR 0013](../docs/adr/0013-access-tokens-from-an-identity-provider-outside-the-platform.md).
- **Correlation IDs.** A caller may send the W3C header `traceparent`. Its trace ID is the correlation ID of the request: a service passes it on with every call that it makes, and writes it into its logs and its audit events. A service starts one for a request that has none. See [ADR 0014](../docs/adr/0014-testability-hooks-in-the-services-off-by-default.md).
- **Hooks.** A hook is an operation that only a test needs: the clock, and the data that onboarding would create. It is off in production, it needs a scope of its own, and it has a specification of its own. See [ADR 0014](../docs/adr/0014-testability-hooks-in-the-services-off-by-default.md).

The smaller ones:

- A specification is written in OpenAPI 3.1.2.
- Its `servers` block names the address of the service in the local stack, as `deploy/compose.yaml` publishes it.
- The local stack has a fixed port for each: 8080 for `account-service`, 8081 for `payment-service`, 8082 for the simulator of the clearing network and 8083 for the simulator of the identity provider.
- A path starts with `/v1`, and the path of a hook with `/hooks/v1`. A breaking change makes `/v2`.
- Names are in kebab-case in a path, in camelCase for a property, a parameter and an operation ID, and in UPPER_SNAKE_CASE for an enum value.
- An identifier that the platform issues is a UUID, in a field named like `paymentId`.
- A point in time is an RFC 3339 string in UTC that ends in `Z`, in a field named like `createdAt`.
- A list comes in pages: `limit` and `cursor` in the query, `items` and `nextCursor` in the response.
- Every object is closed, with `additionalProperties: false`. A request with an unknown property is refused with 400.
- There is no `null`. A property without a value is absent.
- Every operation has a `default` response, a problem, for what no other response covers.
- Every request and every response has an example.

### Who may call an operation

Every operation has a `security` block of its own.

- An operation that needs a token names the scheme `AccessToken` and the scopes that a caller needs, at least one.
- An operation that anybody may call has an empty block, `security: []`. The status of a service is one.
- An operation that needs a token has a 401 and a 403 response. The shared parts have both: `Unauthenticated` and `InsufficientScope`.
- A specification takes the scheme from the shared parts, with a reference under its own `components.securitySchemes`.

The scopes:

| Scope | It lets the caller | Who has it |
|---|---|---|
| `accounts:read` | read the accounts of its customer | every user |
| `payments:read` | read the payments of its customer | every user |
| `payments:create` | create a payment | a maker |
| `payments:cancel` | cancel a payment that it made | a maker |
| `payments:decide` | approve or reject a payment | a checker |
| `hooks:use` | call the hooks of a service | a test: only the simulator of the identity provider issues it |

The list is closed. A contract that needs a new scope adds it here and to the rule `scopes-are-known` in [ruleset.yaml](ruleset.yaml).

A scope opens an operation, and the rules of the [domain model](../docs/domain.md#what-each-actor-may-do) still decide. A request for something of another customer gets 404, whatever its scopes.

### Hooks

A service has its hooks only when it starts with them switched on, as the local stack does. Without the switch, a path under `/hooks` answers 404.

- Every hook needs the scope `hooks:use`, and no other operation takes that scope.
- A hook is a `GET`, a `PUT` or a `DELETE`. A repeat changes nothing, so no hook needs an idempotency key.
- **The clock.** Both services have it, on `/hooks/v1/clock`. A test sets the clock of the rules to an instant with `PUT`, and it stands still there until the test sets it again or resets it with `DELETE`. Its operations are a shared part.
- **Seeding.** A test creates what onboarding would, and it chooses the identifiers: an account in `account-service`, the approval limit of a customer and the limits of an account in `payment-service`.
- No hook puts money into an account. A test books it with the ledger operation of `account-service`.
- No hook wipes data. Every test seeds a customer of its own.

### Sensitive fields

A field that holds a secret or personal data is marked with `x-sensitive: true`.

- Personal data is a name, a tax ID, a phone number, an e-mail address or an alias.
- What the platform issues is not sensitive: an identifier, an account number, an amount.

The report of a test run has to show the value of a marked field masked ([ADR 0007](../docs/adr/0007-allure-behind-a-listener-of-the-framework.md)). The framework learns that before its first test handles such a field.

## Lint

The build lints every specification with [vacuum](https://github.com/daveshanley/vacuum). See [ADR 0009](../docs/adr/0009-openapi-3-1-contracts-linted-by-vacuum-in-the-build.md).

```
./gradlew :contracts:check
```

- The rules are in [ruleset.yaml](ruleset.yaml): the rules that vacuum recommends, and rules of our own for the conventions.
- A specification is clean when vacuum reports nothing, at any level. One finding fails the build.
- A rule that does not fit is switched off in the ruleset, with the reason next to it.
- The findings of the last run are in `build/reports/vacuum/findings.txt`.

Gradle downloads vacuum from its GitHub release and checks the archive against a checksum, so there is nothing to install. vacuum has releases for Linux, macOS and Windows, on x86-64 and ARM64.

### The rules of our own

| Rule | It checks that |
|---|---|
| `error-responses-are-problems` | every 4xx, 5xx and `default` response is `application/problem+json` |
| `operations-have-a-default-response` | every operation has a `default` response |
| `no-number-type` | no schema has the type `number` |
| `post-takes-an-idempotency-key` | every `POST` takes a required `Idempotency-Key` header |
| `no-patch` | no operation is a `PATCH` |
| `paths-start-with-a-version` | every path starts with a version, such as `/v1`, or with `/hooks` and a version |
| `operation-ids-in-camel-case` | every operation ID is in camelCase |
| `enum-values-in-upper-snake-case` | every enum value is in UPPER_SNAKE_CASE |
| `objects-are-closed` | every object has `additionalProperties: false` |
| `no-null-type` | no schema has the type `null` |
| `sensitive-marker-is-true` | `x-sensitive` is `true` wherever it appears |
| `operations-state-who-may-call` | every operation has a `security` block of its own |
| `security-names-a-scope` | an entry of a `security` block names a scheme and at least one scope |
| `scopes-are-known` | every scope is on the list of the ruleset |
| `operations-with-a-token-have-401-and-403` | an operation that needs a token has a 401 and a 403 response |
| `hooks-need-the-hooks-scope` | every operation under `/hooks` needs the scope `hooks:use` |
| `only-hooks-take-the-hooks-scope` | no other operation takes the scope `hooks:use` |

The rules of vacuum already check the kebab-case of a path, the camelCase of a property, the descriptions and the examples. They also check that a `security` block names a scheme that the specification has. No rule checks the name of a parameter, or the conventions for identifiers, points in time and pages.

### The tests of the rules

`check` also tests the rules, with `vacuum test`. The suite is in [ruleset-tests](ruleset-tests/suite.yaml).

- `keeps-the-rules.yaml` is a specification that follows every convention and uses the shared parts of an API. It is linted with every rule, and vacuum has to report nothing.
- `keeps-the-rules-of-hooks.yaml` is the same for hooks. It uses the shared clock.
- Each rule of our own has a document that breaks it, and vacuum has to report exactly that rule.
- A new rule comes with its document and its case in the suite.

### Raising vacuum

Dependabot does not follow GitHub releases, so this is done by hand.

1. Set `vacuum` in `gradle/libs.versions.toml` to the new version.
2. Replace `gradle/vacuum-checksums.txt` with the `checksums.txt` of that release.
3. Run `./gradlew build`.
