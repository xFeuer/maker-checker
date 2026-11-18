# Roadmap

The work runs in phases. Each phase has one exit criterion, and a phase is done when that criterion holds on `main`.

| # | Phase | Done when |
|---|---|---|
| 0 | Foundations: repository, Gradle skeleton, CI, ADR template | The build is green in CI |
| 1 | Walking skeleton: one endpoint, one Playwright API test, a published report | One command runs everything |
| 2 | Domain, OpenAPI contracts, testability hooks | The specifications lint clean |
| 3 | Services, with their own unit and component tests | The stack starts with one command |
| 4 | Framework core and API suites | The API suite is green in CI and the report is online |
| 5 | UI, then the UI layer and journey tests | The UI suite is green and traces are in the report |
| 6 | Hardening: parallel runs, flake policy, fault injection, documentation | The README tells the whole story |
| 7 | Optional: one service reimplemented in Rust | The same suite passes unchanged |

## Notes

- **Walking skeleton.** One thin slice from endpoint to published report, built before the real services. Problems with CI and reporting surface early, and the repository is showable from then on.
- **Testability hooks.** Clock control, correlation IDs and data seeding. They are designed together with the contracts, so the services do not need rework when the suites arrive.
- **Rust.** The last phase reimplements one service in another language. The suite passing unchanged proves that the framework is black-box.

## Working rules

1. One GitHub milestone per phase.
2. One issue and one pull request per task.
3. `main` stays green.
4. One ADR per decision that is costly to reverse.
