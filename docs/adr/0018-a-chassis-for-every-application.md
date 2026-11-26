# 0018. A chassis for every application

- Status: Accepted
- Date: 2026-11-26

## Context

The stack will have four applications: two services and two simulators ([ADR 0008](0008-two-services-and-a-simulator-of-the-clearing-network.md), [ADR 0013](0013-access-tokens-from-an-identity-provider-outside-the-platform.md)). A test meets all four, so they have to keep the same conventions: the problem document ([ADR 0010](0010-problem-details-for-every-error-response.md)), the access token (ADR 0013), and the hooks, the clock and the correlation ID ([ADR 0014](0014-testability-hooks-in-the-services-off-by-default.md)). A convention that is written four times drifts.

ADR 0008 keeps the simulators out of `platform/`. So code that all four share can live under neither.

The problem document is the first of those conventions to be built, and Spring does not give it for free. ADR 0010 expected the problem class of Spring to be enough. A spike on `account-service` with Spring Boot 4.2 found three ways for an error to leave an application, and none of them keeps the contract:

- **Spring MVC.** With its switch for problem details on, Spring answers an unknown path or a wrong method with a document that has no `type`, which the contract requires. The detail is in its own words, such as "No static resource v1/nope."
- **The error page of the server.** An exception in a filter, an error that a filter sends and a `TRACE` request arrive there. Spring Boot answers them in a format of its own, with a timestamp and a path.
- **Tomcat.** It refuses some requests before any code of Spring runs: a character in the target that HTTP does not allow, a header that is too large, a request without a `Host`. It answers with a page of HTML.

## Decision

The library:

- **One library holds what every application needs to keep a convention of the contracts.** It is the chassis: the top-level directory `chassis/`, one Gradle project.
- **It holds a thing when a convention of [`contracts/README.md`](../../contracts/README.md) asks every application for it.** It holds no rule of the domain and nothing that needs a database.
- **An application gets it by the dependency alone.** The chassis configures itself, as a Spring Boot auto-configuration. An application adds no annotation and no setting.
- **The automation never depends on it.** It is code of the applications, behind the boundary of [ADR 0002](0002-one-repository-with-a-black-box-boundary.md), and the boundary check of the build covers it.

Its first part, the problem documents:

- **The chassis writes the document itself,** as a record with exactly the members of the contract. Spring still finds the errors of a request and names their status.
- **Every way out is covered.** A handler takes every exception that reaches Spring MVC. A controller takes the error page of the server. A valve answers what Tomcat refuses.
- **Code refuses a request by throwing an exception** that carries the type of the problem. A transaction that the exception leaves rolls back.
- **An application declares its types by hand,** in an enum: the name, the status and the title of each. The types that every application answers with are in the chassis.
- **A 400 is always an `invalid-request` that lists what is wrong.** That holds for a refusal of Tomcat too: it lists one violation, without a place.
- **Every other status that Spring or the server answers keeps its code.** Its type is `about:blank`, and it has no detail: 404, 405, 406, 415 and the rest.
- **A failure that no code expected is a 500 that says nothing.** The log has the exception.
- **The probes of the actuator are no part of this.** No contract names them, and the 503 of a readiness probe stays a health document.

## Options considered

The home and the name:

- **`chassis/`.** Chosen. It is the name of the pattern: the part that every service is built on, with what cuts across all of them. The name says what belongs there.
- **`shared/` or `common/`.** Both say who uses the code, and not what it is for. A directory with such a name collects whatever two modules have in common.
- **A module under `platform/`.** The simulators would then depend on the platform, against ADR 0008.

How an application gets it:

- **Auto-configuration.** Chosen. The dependency is enough, so no application can leave out a part of a convention.
- **An annotation or an import in each application.** The code then shows what is switched on. Four applications would repeat it, and one could forget it.
- **A base class for the controllers.** It cannot answer what never reaches a controller.

Who writes the document:

- **A record of the chassis.** Chosen. The members of the contract are in one place, and no other member can appear. The valve of Tomcat writes the same record, where no Spring is.
- **The `ProblemDetail` of Spring.** No class to write. It leaves out the type `about:blank`, it has members that the contract does not have, and Spring writes its details.
- **`ProblemDetail`, and a contract that follows it.** The contract would then describe a library, and not a decision.

How code refuses a request:

- **An exception.** Chosen. The code that finds a broken rule says so where it is, at any depth, and the transaction rolls back by itself.
- **A result that the controller maps.** The compiler then sees every refusal. Each layer has to pass it up, and a refusal after a write needs a rollback that somebody has to remember.

Where a type is declared:

- **By hand, in an enum of the application.** Chosen. The code says what it answers with, and a search for the name of a type finds it.
- **Generated from the contracts.** Code and contract could not disagree about a title or a status. The build would write source code, and how a service is held to its contract is a decision of its own.

What Tomcat refuses:

- **A valve of the chassis.** Chosen. "Every response" of ADR 0010 then has no exception.
- **The page of Tomcat.** No code. A test that sends such a request would get HTML from a service whose contract promises a problem.

## Consequences

- The stack gets a third home for its code, next to `platform/` and `simulators/`.
- The chassis depends on Spring MVC and on Tomcat. An application on another server would need another valve.
- The valve leans on a class of Tomcat, `ErrorReportValve`, and on the place that a host gives it among its valves. The tests of the chassis send the requests that Tomcat refuses, so a new Tomcat that changes either one fails the build.
- A consequence of ADR 0010 said that the class of Spring makes a library needless. It did not hold, and the context above says why.
- A unit test of a slice of Spring MVC gets the problem documents too. It sees a refusal as a client sees it.
- The chassis tests itself, with a small application on a real port. A service only proves that it has the chassis.
- A type can still have another title or status in the code than in the contract. How a service is held to its contract is the next decision.
- Spring stops at the first thing that is wrong with a request, so a 400 that Spring finds lists one violation. Every violation of a body comes with the first operation that has a body.
- The next parts of the chassis are the access tokens, the hooks switch, the clock and the correlation IDs. Each arrives with its issue.
