# 0001. Record architecture decisions

- Status: Accepted
- Date: 2026-11-18

## Context

This repository shows how a test automation framework for a financial system is designed. The reasoning behind the design matters as much as the code. Reasoning that lives only in commit messages gets lost.

## Decision

Record every decision that is costly to reverse, or that a reviewer would question, as an architecture decision record in `docs/adr`. Each record holds one decision, is numbered in order and is written from `template.md`.

An accepted ADR is not edited to change its decision. A new ADR supersedes it.

## Options considered

- **ADRs in the repository.** Chosen. They are versioned and reviewed with the code they explain.
- **A wiki or an external document.** Easier to edit, but it drifts from the code and is not reviewed in pull requests.
- **No written record.** No overhead, and no way for a reader to tell a decision from an accident.

## Consequences

- Each decision is reviewed in the pull request that introduces it.
- The ADR log reads as the design story of the project.
- Every such decision costs a short document.
