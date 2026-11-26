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
| `payment-service` | [openapi.yaml](payment-service/openapi.yaml) | [hooks.yaml](payment-service/hooks.yaml) |
| the clearing network, simulated | [openapi.yaml](clearing-network/openapi.yaml) | [hooks.yaml](clearing-network/hooks.yaml) |
| the identity provider, simulated | [openapi.yaml](identity-provider/openapi.yaml) | none |

The clearing network is outside the platform, and a simulator stands in for it. Its directory is like the one of a service: `openapi.yaml` is what `payment-service` calls, and `hooks.yaml` is how a test steers the simulator.

The identity provider is outside the platform too, and it has a simulator of its own. Its `openapi.yaml` holds the status and the operation that gives a test a token. It has no `hooks.yaml`: a hook needs a token, and this is where the tokens come from.

The parts that the specifications share are in [common/components.yaml](common/components.yaml): the access token, the problem document, money and balances, the pages of a list, the idempotency key, the correlation ID, the names that several services use and the clock of the hooks. A specification refers to them and does not copy them.

## Conventions

Six conventions have a decision record:

- **Errors.** Every 4xx and 5xx response is a problem document of RFC 9457. A client relies on its `type` and its `status`. See [ADR 0010](../docs/adr/0010-problem-details-for-every-error-response.md).
- **Money.** Money is an object such as `{ "amount": "10.50", "currency": "BRL" }`. The amount is a string with exactly two decimals, and no schema has the type `number`. See [ADR 0011](../docs/adr/0011-money-as-a-decimal-string-with-its-currency.md).
- **Idempotency.** Every `POST` takes an `Idempotency-Key`. Every other change is a `PUT` or a `DELETE` on a URL that names the thing. See [ADR 0012](../docs/adr/0012-an-idempotency-key-for-every-post.md).
- **Callers.** A caller sends an access token of the identity provider, as a bearer token. Its scopes say what the caller may do, and every operation names the scopes that it needs. See [ADR 0013](../docs/adr/0013-access-tokens-from-an-identity-provider-outside-the-platform.md).
- **Correlation IDs.** A caller may send the W3C header `traceparent`. Its trace ID is the correlation ID of the request: a service passes it on with every call that it makes, and writes it into its logs and its audit events. A service starts one for a request that has none. The clearing network gives its answer for a payment in the trace of the request that sent the payment. See [ADR 0014](../docs/adr/0014-testability-hooks-in-the-services-off-by-default.md).
- **Hooks.** A hook is an operation that only a test needs: the clock, and the data that onboarding would create. It is off in production, it needs a scope of its own, and it has a specification of its own. See [ADR 0014](../docs/adr/0014-testability-hooks-in-the-services-off-by-default.md).

The smaller ones:

- A specification is written in OpenAPI 3.1.2.
- Its `servers` block names the address of the service in the local stack, as `deploy/compose.yaml` publishes it.
- The local stack has a fixed port for each: 8080 for `account-service`, 8081 for `payment-service`, 8082 for the simulator of the clearing network and 8083 for the simulator of the identity provider.
- A path starts with `/v1`, and the path of a hook with `/hooks/v1`. A breaking change makes `/v2`.
- Names are in kebab-case in a path, in camelCase for a property, a parameter and an operation ID, and in UPPER_SNAKE_CASE for an enum value.
- An identifier that the platform issues is a UUID, in a field named like `paymentId`.
- A point in time is an RFC 3339 string in UTC that ends in `Z`, in a field named like `createdAt`.
- A list comes in pages: `limit` and `cursor` in the query, `items` and `nextCursor` in the response. The last page has no `nextCursor`.
- A `POST` that creates a thing answers 201, with the thing in the body and its path in the `Location` header.
- A caller that creates a thing under an identifier of its own uses a `PUT` on its URL. A repeat with the same content changes nothing. When the thing cannot change, such as a hold, another content under the same identifier gets 409.
- A change that ends a thing is a `PUT` on a noun under it, such as `/v1/holds/{holdId}/release`. The thing stays readable.
- Every object is closed, with `additionalProperties: false`. A request with an unknown property is refused with 400.
- A request body with more than one form is a `oneOf` of closed objects. One property with a fixed value tells the forms apart, such as the `kind` of a new payment.
- There is no `null`. A property without a value is absent.
- Every operation has a `default` response, a problem, for what no other response covers.
- Every request and every response has an example.

### Who may call an operation

Every operation has a `security` block of its own.

- An operation that needs a token names the scheme `AccessToken` and the scopes that a caller needs, at least one.
- An operation that anybody may call has an empty block, `security: []`. The status of a service is one.
- An operation for two kinds of callers has two entries, one for each scope. Reading an account is one: a user needs `accounts:read`, and a service `ledger:read`.
- An operation that needs a token has a 401 and a 403 response. The shared parts have both: `Unauthenticated` and `InsufficientScope`.
- A specification takes the scheme from the shared parts, with a reference under its own `components.securitySchemes`.

The scopes:

| Scope | It lets the caller | Who has it |
|---|---|---|
| `accounts:read` | read the accounts of its customer, with their limits | every user |
| `payments:read` | read the payments of its customer, with their audit trails | every user |
| `payments:create` | create a payment | a maker |
| `payments:cancel` | cancel a payment that it made | a maker |
| `payments:decide` | approve or reject a payment | a checker |
| `payments:deliver` | deliver an inbound payment | the clearing network |
| `payments:answer` | give the final answer for an outbound payment | the clearing network |
| `ledger:read` | read every account, every hold and every ledger transaction | `payment-service`, and a test that checks the ledger |
| `ledger:write` | place and release a hold, and book a ledger transaction | `payment-service`, and a test that puts money into an account |
| `network:use` | resolve an alias at the clearing network, send a payment to it and read the status of a payment | `payment-service`, and a test that asks the network what it received |
| `hooks:use` | call the hooks of a service | a test: only the simulator of the identity provider issues it |

The list is closed. A contract that needs a new scope adds it here and to the rule `scopes-are-known` in [ruleset.yaml](ruleset.yaml).

A scope opens an operation, and the rules of the [domain model](../docs/domain.md#what-each-actor-may-do) still decide. A request for something of another customer is refused like one for something that does not exist, whatever its scopes: with 404 when the URL names the thing, and with 422 when the body does.

### Access tokens

The simulator of the identity provider issues every token. Its [specification](identity-provider/openapi.yaml) says what a token holds.

- A token names the services that it is for, in its audience: `account-service`, `payment-service` or `clearing-network`. A service refuses a token that does not name it, with 401.
- A test gets a token for any caller with `POST /v1/tokens`. It names the caller, the customer, the scopes and the audience, and the simulator vouches for them. Anybody may call that operation.
- A test also gets a token that is wrong in one way: expired, wrongly signed, from another issuer or of another type. For the wrong audience, it names another service.
- `payment-service` and the clearing network are clients of the provider. Each gets its tokens at the token endpoint, one for each service that it calls.
- The discovery document, the keys and the token endpoint follow their standards and not these conventions. The specification names them in words, so no rule has an exception for them.
- Issuing a token answers 200 and not 201, because a token has no path.

### Hooks

A service has its hooks only when it starts with them switched on, as the local stack does. Without the switch, a path under `/hooks` answers 404.

- Every hook needs the scope `hooks:use`, and no other operation takes that scope.
- A hook is a `GET`, a `PUT` or a `DELETE`. A repeat changes nothing, so no hook needs an idempotency key.
- **The clock.** Both services have it, on `/hooks/v1/clock`. A test sets the clock of the rules to an instant with `PUT`, and it stands still there until the test sets it again or resets it with `DELETE`. Its operations are a shared part.
- **Seeding.** A test creates what onboarding would, and it chooses the identifiers: an account in `account-service`, the approval limit of a customer and the limits of an account in `payment-service`.
- No hook puts money into an account. A test books it with the ledger operation of `account-service`.
- No hook wipes data. Every test seeds a customer of its own.
- **The clearing network.** Its simulator always has its hooks, because it has no production.
  - A test seeds an alias with the payee behind it and with what the network does with a payment to it: settle, refuse or hold. An alias that no test seeded is unknown.
  - For a payment that the network holds, the test gives the answer when it wants.
  - An answer says how many times the network sends it: 0, 1 or 2. With 0, only the status of the payment shows it.
  - A test makes the network deliver an inbound payment, once or twice.
  - What a test asks for through a hook has happened before the hook responds. What the network does on its own comes a moment later, so a test waits for it.

### Sensitive fields

A field that holds a secret or personal data is marked with `x-sensitive: true`.

- A secret is an access token.
- Personal data is a name, a tax ID, a phone number, an e-mail address or an alias.
- What the platform issues is not sensitive: an identifier, an account number, an amount.
- A marked value can be in a URL. The alias is in the path of two operations of the clearing network, as a key is in the directory of Pix.

The report of a test run has to show the value of a marked field masked, in a body and in a URL ([ADR 0007](../docs/adr/0007-allure-behind-a-listener-of-the-framework.md)). The framework learns that before its first test handles such a field.

## Lint

The build lints every specification with [vacuum](https://github.com/daveshanley/vacuum). See [ADR 0009](../docs/adr/0009-openapi-3-1-contracts-linted-by-vacuum-in-the-build.md).

```
./gradlew :contracts:check
```

- The rules are in [ruleset.yaml](ruleset.yaml): the rules that vacuum recommends, one more rule of vacuum, and rules of our own for the conventions.
- A specification is clean when vacuum reports nothing, at any level. One finding fails the build.
- A rule that does not fit is switched off in the ruleset, with the reason next to it.
- The findings of the last run are in `build/reports/vacuum/findings.txt`.

Gradle downloads vacuum from its GitHub release and checks the archive against a checksum, so there is nothing to install. vacuum has releases for Linux, macOS and Windows, on x86-64 and ARM64.

### The rules of our own

| Rule | It checks that |
|---|---|
| `error-responses-are-problems` | every 4xx, 5xx and `default` response is `application/problem+json` |
| `operations-have-a-default-response` | every operation has a `default` response |
| `problem-types-are-well-formed` | the type of a problem example is `about:blank`, or a URL of this repository that ends in a name in kebab-case |
| `problem-statuses-match-their-response` | a problem example states the status code of its response |
| `no-number-type` | no schema has the type `number` |
| `post-takes-an-idempotency-key` | every `POST` takes a required `Idempotency-Key` header |
| `no-patch` | no operation is a `PATCH` |
| `paths-start-with-a-version` | every path starts with a version, such as `/v1`, or with `/hooks` and a version |
| `operation-ids-in-camel-case` | every operation ID is in camelCase |
| `enum-values-in-upper-snake-case` | every enum value is in UPPER_SNAKE_CASE |
| `objects-are-closed` | every object has `additionalProperties: false` |
| `no-null-type` | no schema has the type `null` |
| `sensitive-marker-is-true` | `x-sensitive` is `true` wherever it appears |
| `operations-have-a-summary-and-a-description` | every operation has a summary and a description |
| `bodies-have-an-example` | every request body and every response body has an example |
| `examples-have-a-summary` | every example of a body has a summary and a value |
| `schemas-have-a-description` | every schema that states its type has a description: in a property, in a parameter and in a header |
| `values-have-an-example` | every string, integer, boolean and array has an example: in a property, in a parameter and in a header |
| `example-descriptions-differ` | no two examples of a specification have the same description |
| `discriminators-map-their-one-of` | the mapping of a `discriminator` names exactly the schemas of the `oneOf` next to it |
| `operations-state-who-may-call` | every operation has a `security` block of its own |
| `security-names-a-scope` | an entry of a `security` block names a scheme and at least one scope |
| `scopes-are-known` | every scope is on the list of the ruleset |
| `operations-with-a-token-have-401-and-403` | an operation that needs a token has a 401 and a 403 response |
| `hooks-need-the-hooks-scope` | every operation under `/hooks` needs the scope `hooks:use` |
| `only-hooks-take-the-hooks-scope` | no other operation takes the scope `hooks:use` |

The rules of vacuum already check the kebab-case of a path and the camelCase of a property. They also check that a `security` block names a scheme that the specification has. They check the descriptions and the examples only in part, and the rules of our own hold the rest.

The ruleset switches on one rule of vacuum that its recommended set leaves out: `tag-description`, for a tag without a description.

No rule checks the name of a parameter, or the conventions for identifiers, points in time and pages.

### The functions of our own

Three rules need a check that vacuum has no function for: `problem-statuses-match-their-response`, `example-descriptions-differ` and `discriminators-map-their-one-of`. Each has a function in [functions](functions/): a JavaScript file that vacuum runs by itself. The build needs no Node.js for it.

- The lint passes the directory to vacuum with `--functions`, and the suite of the tests names it.
- vacuum cannot lint with a rule whose function it did not load. So a function that is missing fails the build.
- A function gets what its rule selects, and it returns one message for each finding.

### The tests of the rules

`check` also tests the rules, with `vacuum test`. The suite is in [ruleset-tests](ruleset-tests/suite.yaml).

- `keeps-the-rules.yaml` is a specification that follows every convention of an API and uses shared parts. It is linted with every rule, and vacuum has to report nothing.
- `keeps-the-rules-of-hooks.yaml` is the same for hooks. It uses the shared clock.
- Each rule of our own has a document that breaks it, and vacuum has to report exactly that rule. The rule of vacuum that the ruleset switches on has one too.
- A new rule comes with its document and its case in the suite.

### Raising vacuum

Dependabot does not follow GitHub releases, so this is done by hand.

1. Set `vacuum` in `gradle/libs.versions.toml` to the new version.
2. Replace `gradle/vacuum-checksums.txt` with the `checksums.txt` of that release.
3. Run `./gradlew build`.
