# 0008. Two services and a simulator of the clearing network

- Status: Accepted
- Date: 2026-11-25

## Context

The platform exists to pose test problems ([ADR 0003](0003-java-and-spring-boot-for-the-platform.md)). The [domain model](../domain.md) lists its rules and the problems that they pose. Every rule needs an owner, and the split into services decides which problems exist at all: a failure between two services needs two services.

The platform also depends on a system that it does not own. The clearing network carries its payments to other institutions and settles them. The repository needs a stand-in for that network, and the stand-in is neither the system under test nor a test.

## Decision

- **`account-service` owns the accounts and the ledger.** It keeps the balances, the holds, the ledger transactions and their entries: the rules `LED` of the domain model. It does not know why money moves.
- **`payment-service` owns the payments.** It keeps their state machine, the approvals, the limits, the fees and the audit trail, and it talks to the clearing network: the rules `PAY`, `APR`, `LIM`, `FEE`, `NET` and `AUD`. It moves money only by asking `account-service`.
- **The clearing network stays outside the platform.** A simulator stands in for it. The simulator has a contract in `contracts/`, like a service, and it lives in a new top-level directory, `simulators/`.
- **The services talk over HTTP, through their contracts.** `payment-service` calls `account-service` and the clearing network. The clearing network calls `payment-service`. `account-service` calls nobody.

The rules `MON` and `TIM` hold in both services. Customers, users and their roles get their owner together with authentication, in an ADR of their own.

## Options considered

The split:

- **Two services.** Chosen. The boundary runs where a real one does: between the books and the rules of the business. It puts a failure between a hold and its booking within reach of a test.
- **One service.** The least to build and to run. Payments and ledger would share one database transaction, so nothing could go wrong between them, and no test could show what happens when it does.
- **More services**, such as one each for limits, approvals and users. Every boundary costs a contract, an image and a way to fail. None of these adds a test problem that two services do not already pose.

How the services talk:

- **HTTP through the contracts.** Chosen. One kind of contract covers the whole platform, and Playwright reaches every operation of it ([ADR 0004](0004-playwright-for-java-on-the-api-and-ui-layers.md)).
- **A message broker between the services.** It is how many payment platforms decouple their ledger. It needs a second kind of contract and one more piece of infrastructure. The platform already has an asynchronous edge where it is natural: the clearing network.

The clearing network:

- **A simulator with a contract of its own.** Chosen. It keeps state, it answers late or twice, and it delivers inbound payments. Its contract says what `payment-service` may rely on, and how a test steers it.
- **A generic stub server.** Nothing to build. The behavior of the network would live in the stub mappings of each test, and the network would have no contract.

The home of the simulator:

- **`simulators/`.** Chosen. The directory says what its content is: a stand-in for a system outside the platform.
- **`platform/`.** The build check of [ADR 0002](0002-one-repository-with-a-black-box-boundary.md) would cover it at once. It would also say that the simulator is under test, and it is not.
- **`automation/`.** The simulator is a tool for tests, so the framework could own it. It is a service of the stack and not a library of the framework, and the platform would then call code of the side that tests it.

## Consequences

- `contracts/` gets three specifications: one for each service and one for the simulator.
- The boundary of [ADR 0002](0002-one-repository-with-a-black-box-boundary.md) gets a third side. `platform/` and `automation/` never depend on code in `simulators/`, and it depends on neither. The build check follows when the first module arrives there.
- A payment crosses a boundary, so every call across it has to be safe to repeat. The domain model demands that: one hold and one ledger transaction for each payment.
- `account-service` can be tested as a ledger alone, without a payment in sight.
- The limits and the approvals sit in `payment-service`. `account-service` stops a payment for one reason only: the money is not there.
- The stack grows from one container to three.
- A broker can still join later, behind the contracts, if a test problem needs one.
