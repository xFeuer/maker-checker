# 0017. Unit tests and component tests for a service

- Status: Accepted
- Date: 2026-11-26

## Context

The [roadmap](../roadmap.md) asks for services with their own unit and component tests. Those tests belong to the platform. They are not the system tests of `automation/`, which know the platform only from the outside ([ADR 0002](0002-one-repository-with-a-black-box-boundary.md)).

A service has two sorts of things to prove. Some are rules that are plain logic: the rounding of a fee, the moves of a state machine, the hours of a night. Others exist only when the real parts meet. A race for a balance is decided in the database ([ADR 0016](0016-a-postgresql-database-for-each-service.md)). An error that no controller raised is answered by the server, and it has to be a problem document too ([ADR 0010](0010-problem-details-for-every-error-response.md)).

So the levels need a definition: what a test of each level is, what it needs and where it runs. Until now `./gradlew build` needed a JDK and nothing else, and only the system tests needed Docker.

## Decision

- **A service has two levels of tests,** told apart by what a test needs.
- **A unit test needs the JVM alone:** no database, no network and no container. It lives in `src/test`. Most unit tests call a rule in plain Java. A few start a slice of Spring, for a class that only works inside the framework.
- **A component test runs the whole service:** the real application on a real port, with its real database and with stand-ins for the services that it calls. It lives in `src/componentTest`.
- **A component test is a client of the service.** It calls over HTTP, with the `RestTestClient` of Spring. It sets up and reads through the API and the hooks ([ADR 0014](0014-testability-hooks-in-the-services-off-by-default.md)), and it writes no SQL.
- **A component test replaces nothing inside the service.** Its classpath has no mocking library.
- **The database is PostgreSQL in a container.** Testcontainers starts it, from the image that `deploy/compose.yaml` names. One container serves all component tests of a service in a run.
- **No test wipes data.** Each test seeds a customer of its own, as a system test does.
- **A caller is real.** A test signs its tokens with a key of its own, and the service validates them as it validates every token. A stand-in for another service speaks real HTTP. The tools for both come with the first test that needs them.
- **Both levels are part of `check`.** So `./gradlew build` runs them, and it needs Docker.

## Options considered

The levels:

- **Two, told apart by what a test needs.** Chosen. Where a test belongs is a fact: it needs a container, or it does not.
- **A third level between them,** for a repository or a controller alone, with the rest mocked. It is the classic pyramid. What such a test proves, a component test proves with the real parts, and every test would need a decision about its level.
- **Component tests alone.** Everything is proven through the API. Six rows of a fee table would be six payments, where six calls of a function do.

How a component test calls the service:

- **Over HTTP, on a real port.** Chosen. The test gets what a client gets: the status line, the headers and the body, also when the server answers before any controller runs. Requests that race arrive on the threads of the server.
- **Through MockMvc.** No socket, and a little faster. It mocks at the layer of Spring MVC, so it cannot show what the server itself does with an error.

The database of a component test:

- **PostgreSQL in a container, started by Testcontainers.** Chosen. It is the engine and the image of the stack.
- **PostgreSQL as a process,** from binaries that a library unpacks. No Docker. The binaries come from another source than the image of the stack.
- **H2 in the mode for PostgreSQL.** No Docker either. It is not the engine whose locks decide a race.

Where the component tests run:

- **In `check`.** Chosen. One command proves a service, and no run leaves them out without saying so. Every machine that builds needs Docker.
- **In a task outside `build`.** The build keeps needing only a JDK. CI needs a second step, and a green `build` says less.
- **Skipped where Docker is missing.** The build is green everywhere, also where it has tested nothing.

How a component test sets up its data:

- **Through the API and the hooks.** Chosen. The hooks get tested on the way, the schema can change without a test noticing, and a scenario can move between a component test and a system test.
- **With SQL.** Faster to write. The tests would know the tables, and they could create a state that no rule allows.

## Consequences

- The build needs Docker, and the README says so. CI has it.
- The component tests of a service share one application and one database. A test that needs another configuration pays for a second start, so those stay few.
- No test may count on an empty database. It asserts on what it created, by identifiers of its own.
- A test that moves the clock of a service cannot share it. It runs alone, as ADR 0014 has it for the system tests.
- A component test and a system test can prove the same rule. A rule of one service is proven here first, where a failure points at one service. The system tests prove what crosses a boundary.
- The helpers of the component tests start in `account-service`. They move to a shared place when a second service needs them.
- A unit test stays fast, so the rules get many of them: every row of the fee table, every move of the state machine.
