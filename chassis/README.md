# Chassis

What every application of the stack is built on: the conventions of the [contracts](../contracts/README.md), in code. The services and the simulators depend on it, so a convention is written once. See [ADR 0018](../docs/adr/0018-a-chassis-for-every-application.md).

- It holds a thing when a convention of the contracts asks every application for it.
- It holds no rule of the domain and nothing that needs a database.
- An application gets it by the dependency alone. It configures itself.
- `automation/` never depends on it. The build checks that.

## Problem documents

Every error response of an application is a problem document of RFC 9457, with exactly the members that the contracts give it. See [ADR 0010](../docs/adr/0010-problem-details-for-every-error-response.md).

An error has three ways out of an application, and the chassis covers each:

| The error | Answered by |
|---|---|
| An exception that reaches Spring MVC | `ProblemExceptionHandler` |
| What arrives at the error page of the server, such as an exception in a filter | `ProblemErrorController` |
| A request that Tomcat refuses before Spring sees it | `ProblemReportValve` |

To refuse a request, the code of an application throws a `ProblemException` with the type of the problem. A transaction that the exception leaves rolls back.

```java
throw new ProblemException(AccountProblem.INSUFFICIENT_FUNDS);
```

An application declares its types in an enum that implements `ProblemType`: the name, the status and the title of each, as its contract has them. The types that every application answers with are in `CommonProblem`.

A request that breaks the contract is an `InvalidRequestException`. It lists every violation, each with its place.

```java
throw new InvalidRequestException(Violation.inBody("#/amount/amount", "The amount has exactly two decimals."));
```

What Spring and the server find themselves needs no code: an unknown path, a wrong method, a header that is missing. A status of 400 is an `invalid-request`. Every other status keeps its code, with the type `about:blank`.

## Tests

```
./gradlew :chassis:test
```

The tests run a small application on top of the chassis, on a real port, and send it every kind of error. They need the JVM alone.
