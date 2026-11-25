# contracts

The OpenAPI specifications of the platform's APIs.

- They are written before the code.
- `platform/` implements them.
- `automation/` tests against them.

Each service has its own directory: `contracts/<service>/openapi.yaml`.

| Service | Specification |
|---|---|
| `account-service` | [openapi.yaml](account-service/openapi.yaml) |

The parts that the specifications share are in [common/components.yaml](common/components.yaml): the problem document, money and the idempotency key. A specification refers to them and does not copy them.

## Conventions

Three conventions have a decision record:

- **Errors.** Every 4xx and 5xx response is a problem document of RFC 9457. A client relies on its `type` and its `status`. See [ADR 0010](../docs/adr/0010-problem-details-for-every-error-response.md).
- **Money.** Money is an object such as `{ "amount": "10.50", "currency": "BRL" }`. The amount is a string with exactly two decimals, and no schema has the type `number`. See [ADR 0011](../docs/adr/0011-money-as-a-decimal-string-with-its-currency.md).
- **Idempotency.** Every `POST` takes an `Idempotency-Key`. Every other change is a `PUT` or a `DELETE` on a URL that names the thing. See [ADR 0012](../docs/adr/0012-an-idempotency-key-for-every-post.md).

The smaller ones:

- A specification is written in OpenAPI 3.1.2.
- Its `servers` block names the address of the service in the local stack, as `deploy/compose.yaml` publishes it.
- A path starts with `/v1`. A breaking change makes `/v2`.
- Names are in kebab-case in a path, in camelCase for a property, a parameter and an operation ID, and in UPPER_SNAKE_CASE for an enum value.
- An identifier that the platform issues is a UUID, in a field named like `paymentId`.
- A point in time is an RFC 3339 string in UTC that ends in `Z`, in a field named like `createdAt`.
- A list comes in pages: `limit` and `cursor` in the query, `items` and `nextCursor` in the response.
- Every object is closed, with `additionalProperties: false`. A request with an unknown property is refused with 400.
- There is no `null`. A property without a value is absent.
- Every operation has a `default` response, a problem, for what no other response covers.
- Every request and every response has an example.

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
| `paths-start-with-a-version` | every path starts with a version, such as `/v1` |
| `operation-ids-in-camel-case` | every operation ID is in camelCase |
| `enum-values-in-upper-snake-case` | every enum value is in UPPER_SNAKE_CASE |
| `objects-are-closed` | every object has `additionalProperties: false` |
| `no-null-type` | no schema has the type `null` |
| `sensitive-marker-is-true` | `x-sensitive` is `true` wherever it appears |

The rules of vacuum already check the kebab-case of a path, the camelCase of a property, the descriptions and the examples. No rule checks the name of a parameter, or the conventions for identifiers, points in time and pages.

### The tests of the rules

`check` also tests the rules, with `vacuum test`. The suite is in [ruleset-tests](ruleset-tests/suite.yaml).

- `keeps-the-rules.yaml` is a specification that follows every convention and uses every shared part. It is linted with every rule, and vacuum has to report nothing.
- Each rule of our own has a document that breaks it, and vacuum has to report exactly that rule.
- A new rule comes with its document and its case in the suite.

### Raising vacuum

Dependabot does not follow GitHub releases, so this is done by hand.

1. Set `vacuum` in `gradle/libs.versions.toml` to the new version.
2. Replace `gradle/vacuum-checksums.txt` with the `checksums.txt` of that release.
3. Run `./gradlew build`.
