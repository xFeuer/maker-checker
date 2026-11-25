# 0013. Access tokens from an identity provider outside the platform

- Status: Accepted
- Date: 2026-11-25

## Context

The [domain model](../domain.md) says who may do what. A maker creates a payment, a checker decides it, and a user reads only what belongs to their customer (`CUS-3`, `CUS-4`, `PAY-3`, `APR-2`). So a service has to know three things about a caller: who it is, for which customer it acts and what it may do. `APR-3` and `APR-4` also need the user who made a payment.

Not every caller is a user. `payment-service` calls `account-service`, and the clearing network calls `payment-service` ([ADR 0008](0008-two-services-and-a-simulator-of-the-clearing-network.md)). That ADR left the owner of customers, users and their roles to this one.

Onboarding is outside the platform: nothing in it creates a customer or a user (`CUS-5`). The test problems of this repository are in what a caller may do. None of them is in how a person logs in.

## Decision

- **An identity provider outside the platform vouches for every caller.** It knows the users, and it says what each of them may do. A simulator stands in for it, as one does for the clearing network. It lives in `simulators/`, and it has a contract in `contracts/`.
- **A caller sends an access token of the provider with every request,** as a bearer token of RFC 6750: `Authorization: Bearer <token>`.
- **The token is a JWT that the provider signs,** as RFC 9068 profiles it. A service validates it alone: its type, its issuer, its audience, its signature and its expiry. The audience has to name the service. The provider publishes its keys, so a service asks it nothing about a single token.
- **The token says who calls and what the caller may do.**
  - `sub` names the user.
  - `customer_id` names the customer that the user acts for (`CUS-2`).
  - `scope` lists what the caller may do.
- **A role is a set of scopes.** Every user has `accounts:read` and `payments:read`. A maker also has `payments:create` and `payments:cancel`. A checker also has `payments:decide`. A user who is both has all five (`CUS-3`).
- **A scope opens an operation, and the rules of the domain still decide.** A checker with `payments:decide` is refused for a payment that they made (`APR-3`), and a payment of another customer does not exist for them (`CUS-4`).
- **The platform keeps no users and no roles.** A user is whoever the provider vouches for. A payment remembers the `sub` of its maker, and an audit event the `sub` of the user who caused it (`AUD-2`).
- **A customer is only an identifier.** The token names it. `account-service` keeps the accounts of a customer under it, and `payment-service` its approval limit. No service keeps a record of the customer itself, such as its name.
- **A service and the clearing network prove who they are in the same way.** Each gets a token of its own from the provider. Its `sub` names that caller, and it has scopes of its own and no `customer_id`.
- **A contract states who may call each operation.** The `security` of an operation names the scopes that a caller needs. An operation that needs no token says so with an empty `security`. The scheme is `openIdConnect`, and it points at the provider. The list of scopes is closed, and the ruleset holds it. The linter checks that every operation states its callers, and that every scope is on the list.
- **A refusal is a problem of [ADR 0010](0010-problem-details-for-every-error-response.md).** A request without a valid token gets 401, with the type `about:blank`. A token that lacks a scope of the operation gets 403, with a type of its own: `insufficient-scope`.

## Options considered

Who vouches for a caller:

- **An identity provider outside the platform, with a simulator.** Chosen. Logins and passwords stay out of the platform, as onboarding does. A test can ask the simulator for any caller.
- **A login of the platform's own,** with users and passwords in a service. The platform would be whole by itself. It would have to keep passwords and to issue tokens, in one of two services that have other jobs or in a third one.
- **A real provider in the stack,** such as Keycloak. It is the most realistic. It is also a large product to run, to configure and to keep current, for a part that poses none of the test problems.

The token:

- **A signed JWT that each service validates.** Chosen. A request costs no call to the provider, and a service keeps answering while the provider is down. A token cannot be taken back before it expires.
- **An opaque token that a service asks the provider about,** on every request. The provider can take a token back at once. Every request would then depend on the provider.

What a caller may do:

- **Scopes.** Chosen. A scope names one right, and OpenAPI lists the scopes that an operation needs. A role of the domain is a set of them.
- **Roles in a claim of our own,** such as `maker`. They are the words of the domain. Each service would have to know which role may call which operation, and a caller that is no user would need a role that the domain does not have.

The proof of a service:

- **A token of the same provider.** Chosen. Every caller proves itself in one way, a service checks one thing, and a contract states one thing.
- **Certificates, with mutual TLS.** The proof would sit in the connection and not in the request. It needs a certificate authority, a certificate in every container and TLS inside the stack, and a second way to state the caller in a contract.

The security scheme of the contracts:

- **`openIdConnect`.** Chosen. The scheme names the provider, and an operation names its scopes. How a caller gets its token stays the business of the provider. OpenAPI 3.1 gives this scheme no place for the list of scopes, so the ruleset holds it.
- **`oauth2`, with its flows.** The list of scopes would be inside the specification. So would the flows of the provider and their URLs, and they would change there when the provider gets a login for the UI.
- **`http` with `bearer`.** It says how the token travels, and nothing about where it comes from.

## Consequences

- The stack grows by one container, the simulator of the identity provider, and `contracts/` by one specification. ADR 0008 counted three of each.
- A test gets a token for any caller from the simulator: a maker, a checker, both in one user, a user of another customer, a service. A test also needs tokens that are wrong in one way, such as an expired one. The contract of the simulator has to offer both.
- The services are resource servers of OAuth 2.0, and Spring Security has that. They have to answer a missing token and a missing scope with a problem document, as they answer every refusal.
- A service needs the keys of the provider before it can validate a token. So the provider is up before the first request with a token arrives.
- A token cannot be taken back, so it is short-lived. Its expiry follows real time. The clock that a test moves is the clock of the rules (`TIM-1`), and moving it expires no token.
- `customer_id` is a claim of our own. RFC 9068 names every other claim that a service reads.
- An idempotency key belongs to the customer of the caller ([ADR 0012](0012-an-idempotency-key-for-every-post.md)). That is the customer that the token names.
- The contracts share the scheme, a 401 and a 403. Four rules of the linter hold the convention, and a contract that needs a new scope adds it to the ruleset.
- The scopes of the services and of the clearing network come with their contracts.
- The UI will need a login. The simulator gets one then, and the contracts of the services stay as they are.
