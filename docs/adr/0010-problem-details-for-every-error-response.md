# 0010. Problem details for every error response

- Status: Accepted
- Date: 2026-11-25

## Context

The [domain model](../domain.md) tells a refused request from a failed payment. A refusal has to say which rule turned the request down, in a form that a client and a test can rely on. A sentence in English is not such a form.

The platform has two services and a simulator ([ADR 0008](0008-two-services-and-a-simulator-of-the-clearing-network.md)). A test meets the errors of all three, so they have to look the same.

## Decision

- **Every response with a 4xx or a 5xx status is a problem document of RFC 9457**, with the media type `application/problem+json`.
- **Every problem has `type`, `title` and `status`.** `detail` may add a sentence for a person. A problem never shows the inside of a service, such as a stack trace.
- **`type` identifies the problem.** It is a URL under the site of this repository: `https://xfeuer.github.io/maker-checker/problems/<name>`, with the name in kebab-case. The API reference will serve the description of the problem at that address. A problem that its status code explains fully has the type `about:blank`, as the RFC provides.
- **A test asserts on `type` and `status`.** `title` and `detail` are for people, and they may change.
- **The status code says what kind of refusal it is.**

  | Status | The request |
  |---|---|
  | 400 | breaks the contract: its syntax, a missing field, a wrong format |
  | 401 | comes without a valid proof of who the caller is |
  | 403 | asks for something that this caller may not do |
  | 404 | names something that does not exist for this customer |
  | 409 | is forbidden by the state of the thing that it names |
  | 422 | keeps the contract and breaks a rule of the domain |

- **A 400 lists every violation.** Its member `errors` has one entry for each. An entry has a `detail`, and it says where the violation is: `pointer` is a JSON Pointer into the body, and `parameter` is the name of a header, a query parameter or a path parameter.
- **A failed payment is not a problem.** The request that created it succeeded. The payment itself says why it failed.

## Options considered

The format:

- **RFC 9457.** Chosen. It is a standard with a media type of its own, so a client can tell a problem from any other body. It leaves room for members of our own.
- **A format of our own,** such as a code and a message. It would be no better, and every reader would have to learn it.
- **The status code alone.** That is enough for a thing that does not exist. Two rules of the domain that refuse with the same status would look the same to a test.

The identifier of a problem:

- **A URL under the site of the repository.** Chosen. A type has one identifier in every service, and its description can sit at that address.
- **A relative reference,** such as `/problems/payment-limit-exceeded`. It carries no host. The RFC resolves it against the address of the request, so one problem would get a different identifier from each service.
- **A code in a member of our own,** next to `about:blank`. It would put the identifier where the RFC does not look for it.

The status codes:

- **400 for the contract and 422 for the domain.** Chosen. The status alone tells a client whether to correct its request or to accept a refusal.
- **400 for both.** One status less. A malformed request and a refused payment would then differ only in their type.

The detail of a 400:

- **Every violation, with its place.** Chosen. One answer shows a client everything that is wrong, and a test can name the field that it broke.
- **The first violation only.** It is simpler to build. Which violation comes first is an accident of the implementation, and a test would depend on it.

## Consequences

- The contracts share one schema for a problem. The linter checks that every 4xx and 5xx response has the media type.
- Each specification names the problems that an operation can answer with. A rule of the domain that refuses a request gets a type of its own.
- Until the API reference exists, the address of a type leads nowhere. The RFC allows that. The reference then has to serve a page for each type.
- The identifier holds the host of the site. A move of the repository changes every type, so the framework keeps the common part in one place.
- An unexpected failure answers 500 with the type `about:blank`. The services have to turn every exception into a problem, so that no stack trace leaves them.
- Spring Framework has a class for a problem document, so the services need no library for it.
