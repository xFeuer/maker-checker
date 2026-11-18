# 0002. One repository with a black-box boundary

- Status: Accepted
- Date: 2026-11-18

## Context

The project has two halves. The platform is the system under test: services and a UI. The automation is the framework and the suites that test it.

The automation has to prove that it tests the platform from the outside, the way an independent test codebase tests a deployed system. A reader also has to be able to clone the project and run all of it with one command.

## Decision

Keep everything in one repository, with a hard boundary inside it.

- `automation/` never depends on code in `platform/`.
- `automation/` knows the platform only through the OpenAPI specifications in `contracts/` and through the running system.
- The build enforces the boundary. It fails if a project under `automation/` depends on a project under `platform/`. The check arrives with the first modules, in phase 1.

## Options considered

- **One repository with a hard boundary.** Chosen.
- **One repository per service, plus one for the automation.** It mirrors how many organizations work. It also adds contract versioning and cross-repository pipelines that say nothing about framework design.
- **One repository without a boundary.** The simplest to build. Tests could then reach into service internals, and nothing would show that the framework is black-box.

## Consequences

- One clone, one build and one CI pipeline.
- A contract change, the service change and its tests land in one pull request.
- New domains join as new services in this repository.
- `automation/` can move to its own repository later without code changes.
- The boundary holds only as long as the build enforces it.
