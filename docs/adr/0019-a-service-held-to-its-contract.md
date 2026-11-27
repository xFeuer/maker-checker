# 0019. A service held to its contract

- Status: Accepted
- Date: 2026-11-26

## Context

The specifications in `contracts/` are the source of truth for both sides ([ADR 0009](0009-openapi-3-1-contracts-linted-by-vacuum-in-the-build.md)). The automation knows the platform through them alone ([ADR 0002](0002-one-repository-with-a-black-box-boundary.md)). So a service that drifts from its specification breaks the tests of the automation for a reason that is hard to see from there.

A service can differ from its specification in five ways:

- It lacks an operation of the specification, or it has one that the specification does not have.
- It answers with a status that the operation does not declare.
- A body or a header of a response breaks its schema.
- It takes a request that the specification forbids, such as one with an unknown member.
- A type of problem has another status or another title in the code ([ADR 0018](0018-a-chassis-for-every-application.md)).

[ADR 0010](0010-problem-details-for-every-error-response.md) also asks a 400 to list every violation with its place. Spring stops at the first thing that is wrong with a request.

A spike ran two validators against the real specification of `account-service`:

- **Kappa 2.1.1** reads OpenAPI 3.1 and has a part for Spring. It could not follow the references of our specifications into the file that they share. On a bundle without such references it checked a request well, and it could not check a response: it fails on the `default` response, which every operation of ours has. It brings a second JSON library and the Kotlin standard library.
- **The JSON Schema validator of networknt, 3.0.9,** read our specifications as they are and found every planted defect of a body. For a request with three violations it listed all three, each with a JSON Pointer. It knows JSON Schema and nothing else of OpenAPI. It brings three small libraries, for dates and for YAML: four jars and 1 MiB in all, on the JSON library that every application has.

## Decision

- **The specification is a part of the service.** The build bundles each specification into one JSON document, and a service carries the bundle of its own in its jar.
- **A JSON Schema validator checks a body and the value of a header against the specification.** The rest of OpenAPI is code of the chassis: the path of an operation, the status of a response, its media type and which headers it has.
- **The tests hold the responses.** The client of the component tests checks every response that a test receives: the status, the media type, the headers and the body. A response that breaks the contract fails the test at the call that received it. A running service does not check its own responses.
- **The chassis holds the requests, at run time.** It checks every request against the specification before a controller sees it, and it answers a 400 that lists every violation with its place. The specification is the one source for the shape of a request, so an application has no validation annotations.
- **A test holds the operations.** It compares the operations of the specification with what the service maps, both ways.
- **A test holds the types of problem.** It compares the types that the code declares with the examples of the specification: the name, the status and the title.
- **What is not built yet stands on a list.** The two comparisons take a list of what the specification has and the service does not build yet. An entry leaves its list with the change that builds it, and a test fails for an entry that is built and still listed.
- **A format is a rule.** `uuid`, `date-time` and `uri` are checked, as the lint of the contracts checks them in an example.
- **What the specification does not name is not checked:** a path that it does not have, such as a probe of the actuator, and a query parameter that it does not declare.

## Options considered

What holds a service to its specification:

- **A validator at work, in the tests and in the service.** Chosen. The specification itself is the measure. Nothing is derived from it that could differ from it.
- **Code that is generated from the specification.** The shapes of the bodies cannot drift. A generator decides how the code of a service looks, and it does not say what a service answers with a wrong status or a wrong header.
- **The tests of the automation alone.** They read strictly, so they find a drift in the end. They find it one layer up, where a failure does not point at one service.

The validator:

- **The JSON Schema validator of networknt, with the rest of OpenAPI in the chassis.** Chosen. It reads our specifications, it is small, and its reports have what a 400 needs. The code around it is ours, and the tests of the chassis hold it.
- **Kappa.** It knows OpenAPI as a whole, so there would be no code of our own. It failed on our specifications twice, and it brings two large libraries into every application.
- **The request validator of Atlassian.** It rests on Draft 4 of JSON Schema, and OpenAPI 3.1 is on 2020-12.

Where a response is checked:

- **In the client of the component tests.** Chosen. The test is a client of the service ([ADR 0017](0017-unit-tests-and-component-tests-for-a-service.md)), and it checks what a client receives, after the server has written it. No test has to ask for the check.
- **In a filter of the service, in tests alone.** It sees the response before the server has finished it, and it has to tell a test about a failure from another thread.
- **In the running service too.** A service would answer 500 for a response that its client could have used. The tests find the same defect before it ships.

Where a request is checked:

- **In the chassis, against the specification.** Chosen. A service cannot take what its contract forbids, and a 400 lists every violation, because the validator reads the whole body.
- **By annotations on the classes of a service.** They say the rules of the specification a second time, and Spring reports only the first thing that is wrong.

How the specification reaches a service:

- **As a bundle in its jar.** Chosen. One file has everything, without a reference to another file, and the build already makes it for the pages of the problem types. The service that runs is the service with that contract.
- **The files as they are written.** The validator can read them. The code of the chassis would have to follow the references between files that are not schemas.
- **Read from `contracts/` when a test runs.** The running service would not have it, and it needs it for its requests.

What is not built yet:

- **A list in the test, which has to shrink.** Chosen. The gap between the specification and the service is written down, and it cannot grow or stay by accident.
- **No comparison until a service is complete.** An operation that nobody built would show only at the end.

## Consequences

- The chassis gets a second part, the contract, and a dependency that the Spring Boot BOM does not manage. Dependabot raises it.
- The chassis gets test fixtures: what the tests of an application use to hold it to its contract. A service takes them for its component tests.
- The jar of a service holds its specification. The build of a service depends on the build of the contracts, and so on vacuum.
- A component test cannot receive a response that breaks the contract without failing, also a test that asserts on nothing but a status.
- The code of the chassis reads OpenAPI as our specifications use it. A feature that they do not use yet, such as a response for a range of statuses or a header that is a number, needs code first.
- That code reads a bundle. It refuses a document with a reference to anything but a schema, because what stands behind such a reference would go unchecked.
- An error response is held when the request is for an operation of the specification. Its `default` response then stands for every status that the operation does not name.
- The check of the requests is an issue of its own. It replaces the fixed sentences of ADR 0018 for what Spring finds in a request.
- The comparison of the operations reads the mappings of Spring MVC. It is the one test of a service that looks inside it.
- The hooks have a specification of their own ([ADR 0014](0014-testability-hooks-in-the-services-off-by-default.md)). The build bundles it too, and the tests hold it when a service gets its hooks.
