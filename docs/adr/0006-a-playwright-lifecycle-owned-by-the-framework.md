# 0006. A Playwright lifecycle owned by the framework

- Status: Accepted
- Date: 2026-11-18

## Context

Playwright objects are not thread-safe, and every Playwright instance runs a driver process. Something has to decide when that process starts, which thread uses it and when it stops.

Two test engines will share the framework: JUnit Jupiter now and Cucumber later ([ADR 0005](0005-junit-platform-with-cucumber-on-top.md)). Both need the same lifecycle.

## Decision

The framework owns the lifecycle. `PlaywrightSession` in `automation/core` is plain Java and knows no test engine. A thin JUnit extension binds it to Jupiter.

The scopes are fixed:

- One Playwright session per test class.
- One request context per test, so no test sees the cookies of another.

The tests of one class run on one thread. Classes may run in parallel.

## Options considered

- **A lifecycle owned by the framework.** Chosen.
- **Playwright's `@UsePlaywright` fixtures.** They need no code of ours and the Playwright team maintains them. They are JUnit Jupiter extensions, so Cucumber scenarios cannot use them, and the framework would end up with two lifecycles. Playwright's documentation still calls them experimental.
- **One session per test.** The strongest isolation. Every test would pay for a driver process.
- **One session per thread, shared by its classes.** The fewest driver processes. Such a session outlives its classes, so the end of the run has to close it from another thread. Playwright's documentation asks for every call to stay on the thread that created the instance.

## Consequences

- Cucumber hooks can drive the same `PlaywrightSession` when the scenarios arrive.
- Parallel runs are safe at class level. Running the tests of one class in parallel is ruled out.
- The framework has lifecycle code of its own to maintain.
- The session skips Playwright's browser download, because API tests need no browser. That changes when the UI layer arrives.
