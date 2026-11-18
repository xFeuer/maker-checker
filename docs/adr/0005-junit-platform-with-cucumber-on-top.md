# 0005. JUnit Platform with Cucumber on top

- Status: Accepted
- Date: 2026-11-18

## Context

The suites need a test engine. Engineers want code-first tests with extensions, parameterisation and parallel execution. Financial institutions often expect business-readable scenarios as well.

## Decision

JUnit 6 is the foundation. The framework is built on the JUnit Platform and Jupiter, and uses them for lifecycle, extensions, parallel execution and reporting hooks.

Cucumber sits on top and runs as an engine on the same platform. Step definitions are a thin layer over the framework. The framework itself never depends on Cucumber.

## Options considered

- **JUnit only.** The simplest setup and the full power of Jupiter, but no business-readable layer.
- **Cucumber only.** Every test is a scenario. Technical tests, such as concurrency and contract checks, read poorly as Gherkin.
- **JUnit with Cucumber on top.** Chosen.

## Consequences

- Business-facing journeys can be written as scenarios. Technical tests stay in code.
- One platform runs both styles, so there is one run and one report.
- There are two test styles to maintain. A rule for which test goes where is needed before the suites grow, and it gets its own ADR.
- Step definitions add a layer of indirection.
