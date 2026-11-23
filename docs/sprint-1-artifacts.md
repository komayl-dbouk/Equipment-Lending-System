# Sprint 1 Artifact Index

This index maps the seven Sprint 1 cards marked Done to concrete project documents. Jira status alone does not establish human review, a completed sprint demonstration, application test results or actual effort.

| Card | Artifact package |
| --- | --- |
| [ELS-20](https://equipment-lending-system.atlassian.net/browse/ELS-20) | [Proposal](proposal.md), [team agreement and task plan](team-plan.md), [initial kickoff record](../ai-log/project-kickoff.md) |
| [ELS-21](https://equipment-lending-system.atlassian.net/browse/ELS-21) | [Detailed requirements](requirements/requirements.md), [requirements traceability](requirements/traceability.md), [API conventions/examples](api/contract.md), [OpenAPI](api/openapi.json) |
| [ELS-22](https://equipment-lending-system.atlassian.net/browse/ELS-22) | [Domain and use cases](analysis/domain-and-use-cases.md), [architecture and data design](design/architecture.md), [ADR 001](decisions/001-layered-application.md), [ADR 002](decisions/002-database-and-concurrency.md), [ADR 003](decisions/003-dates-privacy-and-contract.md) |
| [ELS-24](https://equipment-lending-system.atlassian.net/browse/ELS-24) | [Test strategy](testing/test-strategy.md), [GQM plan](testing/gqm-plan.md), [reference evidence map](reference-practice-traceability.md), [test/effort templates](testing/evidence-templates.md), [AI-log template](../ai-log/interaction-template.md) |
| [ELS-27](https://equipment-lending-system.atlassian.net/browse/ELS-27) | [Borrowing specification](specifications/borrow-equipment.md), [agent understanding and plan](specifications/borrow-agent-plan.md), [plan review](reviews/borrowing-plan-review.md) |
| [ELS-28](https://equipment-lending-system.atlassian.net/browse/ELS-28) | [Student contract and privacy review](reviews/student-contract-and-privacy.md) |
| [ELS-31](https://equipment-lending-system.atlassian.net/browse/ELS-31) | [Progress/review/retrospective preparation and effort table](../reports/sprint-1/progress-review-and-retrospective.md) |

## How to use the baseline

Hussein Chalhoub reads requirements, contract and design before ELS-23/25/30. Mostafa implements students against FR01 and Student schemas. Komayl implements equipment against FR02/07 and Equipment schemas. Hussein Chreif coordinates actual review and borrowing specification approval. Commit documentation before the substantial feature's tests and code so future evidence has genuine chronology.

Models are Mermaid sources embedded in Markdown, keeping them editable with the repository. OpenAPI is the machine-readable interface definition. The [verification record](../reports/sprint-1/artifact-verification.md) records checks actually run. No application code, migrations, pipeline or agent-context implementation is included in this documentation task.
