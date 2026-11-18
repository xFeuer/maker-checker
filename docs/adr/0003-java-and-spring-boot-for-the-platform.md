# 0003. Java and Spring Boot for the platform

- Status: Accepted
- Date: 2026-11-18

## Context

The platform exists to be tested. Its job is to pose realistic test problems, not to be interesting in itself. The automation is written in Java and built with Gradle.

Two stacks were candidates for the services: Java with Spring Boot, and Rust with Axum.

## Decision

Build the services in Java 25 with Spring Boot 4.1.

Reimplementing one service in Rust is the last, optional phase. The same suite has to pass against it unchanged.

## Options considered

- **Java and Spring Boot.** Chosen. One language and one Gradle build cover the repository. It is the common stack in banking back ends, so reviewers can read both halves. The effort goes into the framework.
- **Rust and Axum.** A platform in another language would make the black-box boundary self-evident. It adds a second toolchain outside Gradle and moves effort from the framework to the fixture.

## Consequences

- One build, one set of conventions and one CI pipeline cover both halves.
- Platform and automation share a language, so the boundary needs the build check from [ADR 0002](0002-one-repository-with-a-black-box-boundary.md).
- The Rust phase later proves that the suite does not depend on the platform's language.
