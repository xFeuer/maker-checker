# 0004. Playwright for Java on the API and UI layers

- Status: Accepted
- Date: 2026-11-18

## Context

The automation tests two layers: the service APIs and the web UI. The usual Java pairing is REST Assured for the APIs and a browser tool for the UI. That means two tools, two lifecycles and two kinds of evidence.

## Decision

Use Playwright for Java on both layers: its API request context for the service tests and its browser contexts for the UI tests.

## Options considered

- **Playwright for Java on both layers.** Chosen.
- **REST Assured for the APIs, Playwright for the UI.** REST Assured has a mature assertion DSL and most Java testers know it. The two tools do not share sessions or traces.
- **REST Assured and Selenium.** The most familiar pairing. It has the same split, and Selenium has no built-in trace viewer.

## Consequences

- One tool, one lifecycle and one reporting pipeline.
- Authentication state moves between API and browser contexts. A UI test can sign in and prepare data through the API.
- API calls that a UI test makes through its browser context are recorded in the same trace.
- Playwright has no assertion DSL for response bodies. The framework needs a thin layer of typed clients and its own assertions.
- Playwright objects are not thread-safe. Parallel runs need one Playwright instance per thread, and the framework's lifecycle has to manage that.
- Readers who expect REST Assured will ask why. This record is the answer.
