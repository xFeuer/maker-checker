# 0014. Testability hooks in the services, off by default

- Status: Accepted
- Date: 2026-11-25

## Context

Some rules of the [domain model](../domain.md) cannot be tested from the outside without help. Nobody can wait for a night or for 24 hours (`TIM-1`, `APR-5`). Nothing in the platform creates a customer or an account (`CUS-5`). An audit event names the request that caused it (`AUD-2`), so a test has to know which request that was.

The [roadmap](../roadmap.md) calls that help the testability hooks: clock control, data seeding and correlation IDs. It wants them designed with the contracts, so that the services need no rework when the suites arrive.

A hook is dangerous in production. Whoever moves the clock moves the limits and the expiry of a payment, and whoever seeds an account does what only onboarding may do. So this decision also has to say how a hook stays out of production.

`automation/` knows the platform through its contracts and the running stack, and through nothing else ([ADR 0002](0002-one-repository-with-a-black-box-boundary.md)). A hook has to fit that.

## Decision

Where the hooks are:

- **A hook is an HTTP operation of the service that it works on.** Its path starts with `/hooks`. A test reaches it like every other operation.
- **A service has its hooks only when it starts with them switched on.** The switch is a setting of the service, and it is off unless somebody turns it on. `deploy/compose.yaml` turns it on. Without it, a path under `/hooks` answers 404, and the clock is the real one.
- **A hook needs a token with the scope `hooks:use`,** as [ADR 0013](0013-access-tokens-from-an-identity-provider-outside-the-platform.md) has it for every operation. Only the simulator of the identity provider issues that scope. So a service that has its hooks switched on by mistake still refuses every caller of production.
- **The hooks have a specification of their own,** next to the API of the service: `contracts/<service>/hooks.yaml`. The API of a service shows nothing that production lacks.

The clock:

- **Each service has one clock of the rules.** Every rule that depends on time reads it (`TIM-1`), and so does every point in time that the service writes down.
- **A test sets the clock to an instant, and it stands still there** until the test sets it again or resets it. A reset lets it run with real time again.
- **Both services have the same clock hook.** A test that needs another time sets both.
- **Nothing technical reads that clock:** not a timeout, and not the expiry of a token ([ADR 0013](0013-access-tokens-from-an-identity-provider-outside-the-platform.md)).

Seeding:

- **Each service seeds what it owns** ([ADR 0008](0008-two-services-and-a-simulator-of-the-clearing-network.md)). `account-service` seeds an account of a customer. `payment-service` seeds the approval limit of a customer and the limits of an account.
- **A seeding hook is a `PUT` on a URL that names the thing.** The test chooses the identifiers. A repeat changes nothing, so no hook needs an idempotency key ([ADR 0012](0012-an-idempotency-key-for-every-post.md)).
- **No hook puts money into an account.** A test books it with the ledger operation of `account-service`, out of the settlement account, with the token of a service.
- **No hook wipes data.** Every test seeds a customer of its own.

Correlation IDs:

- **The correlation ID of a request is the trace ID of its `traceparent` header,** as the Trace Context of the W3C defines it: 32 hexadecimal digits.
- **A service starts a trace for a request that has none.** It passes the trace on with every call that it makes, and it writes the trace ID into its logs and its audit events (`AUD-2`).
- **This is no hook.** It works in production too, and it needs no scope.

## Options considered

How a test reaches a hook:

- **Operations of the service, off by default.** Chosen. A test uses one way for everything, and a hook runs the code of the service, with its checks.
- **Rows that a test writes into a database.** No service would hold code for a test. A test would have to know the tables, which no contract describes, and it could write a state that no rule allows.
- **A service of its own for the hooks.** The two services would hold no code for a test. It would need their databases, or hooks in them after all.

Who may call a hook:

- **A caller with a scope that only the simulator issues.** Chosen. A hook has two locks, and its contract states its caller like every other operation.
- **Anybody.** The switch would be the only lock.

The clock:

- **A clock that stands still.** Chosen. A test hits a boundary exactly: the last instant of a day and the first one of a night. Many events then carry the same time.
- **A clock that runs on from the instant.** It is closer to production. A test cannot hold it at a boundary, so a test of 20:00 would depend on the speed of the machine.

The reach of the clock:

- **One clock for a service.** Chosen. The code reads one clock, as `TIM-1` says. A test that moves it cannot share the stack with a test that runs at the same time.
- **A clock for each customer.** Tests could run in parallel. Every rule would have to ask whose time it is, and production would carry that question for no reason of its own.

Money in a seeded account:

- **The ledger operation.** Chosen. The money arrives as it does in production, in a ledger transaction that keeps the invariants.
- **An opening balance in the hook.** A test would need one call fewer. The hook would book money in a way of its own, next to the ledger.

The correlation ID:

- **The trace ID of `traceparent`.** Chosen. It is a W3C Recommendation, so a service needs no header of our own. It promises a caller no header in the response, so a caller that wants to know the ID sends one.
- **A header of our own,** such as `Correlation-Id` with a UUID. It is easy to read, and a response could repeat it. Every service and every tool would have to learn it.

## Consequences

- `contracts/` gets a second specification for each service, and the lint reads both. The shared parts get the clock.
- The ruleset learns the scope and two rules: a hook needs `hooks:use`, and nothing else takes it.
- Phase 3 builds the switch, the clock and the seeding into the services. A service that starts with its hooks says so in its log.
- On a clock that stands still, many events carry the same time. Nothing may sort by time alone: the audit events of a payment need an order of their own (`AUD-4`), and a page of a list needs a second key.
- The clock can go back: a test resets it after a night that it set. A service may not count on time moving forward.
- A test that moves the clock is marked, and it runs alone. The runner has to keep that when tests run in parallel.
- A test seeds in three calls to two services, and it sets two clocks. The framework can hide that behind one step.
- A test puts money into an account with the token of a service. The simulator of the identity provider issues one to a test (ADR 0013).
- A test that follows a request sends its own `traceparent`, so it knows the trace ID. The framework can send one with every request and show it in the report.
- The clearing network calls `payment-service` later, with the answer for a payment. The contract of its simulator says which trace that call continues.
- The hooks of the two simulators are not part of this decision. Their contracts have them.
