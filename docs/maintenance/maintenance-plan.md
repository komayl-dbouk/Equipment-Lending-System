# Equipment Lending Maintenance Plan

Owner: Hussein Chreif. ELS-48 preparation. This plan defines safe change procedures; its examples are proposed maintenance actions rather than implemented features or completed release controls.

## Maintenance categories

| Category | Equipment project example | Validation and ownership |
| --- | --- | --- |
| Corrective | Repair a reproduced lending, validation, error-classification or integrity defect | Feature owner writes a meaningful regression; peer checks requirements, committed state and PostgreSQL results |
| Adaptive | Support an approved runtime/database/environment update or new staff-access requirement | Chalhoub coordinates environment/tool changes; Komayl checks persistence/dependency effects; pin candidates and verify compatibility, licensing/security and clean setup |
| Perfective | Improve a measured query/maintainability concern or approved pagination/filter behavior | Chreif confirms value/scope; feature owner measures before/after and versions changed contracts where necessary |
| Preventive | Strengthen oracles, update agent context and maintain repeatable quality/release checks | Mostafa audits tests; Chalhoub maintains tooling; all members preserve evidence and remove verified causes of future failure |

## Change and rollback workflow

Create a Jira item with the requirement, observed problem, reproduction, owner/reviewer, risk and estimate. Distinguish a behavior correction from safe refactoring. Review significant agent plans and document accepted, modified or rejected concerns with evidence.

Use one issue branch without a codex prefix and issue-key commits. Coordinate shared/schema changes. Run focused tests during development, both documented full verification profiles before review, and packaged HTTP/database checks where the affected boundary warrants them. Inspect explicit staged paths and whitespace. A green pipeline and generated text do not replace human acceptance.

After integration, update affected requirements, API examples, models, traceability, context and runbooks. Record the tested revision. A release regression requires a reviewed revert/rollback and retesting; do not erase evidence or weaken assertions. Define the recovery path before risky environment or schema work.

## Persistence and historical data

Flyway is the sole delivery schema owner. Add sequential migrations; never edit an applied migration or enable JPA schema creation/update. Coordinate multi-feature work with the foundation owner. Check existing data compatibility, backup/restore arrangements and deployment order before execution.

Preserve restrictive references, normalized uniqueness, date checks, history indexes and the active-loan partial index. Borrowing, return and deletion retain compatible equipment-first locking and commit-before-success behavior. Test migration and race scenarios on guarded disposable PostgreSQL databases with synthetic data. Production data correction requires its own reviewed script and recovery plan; test cleanup scripts are not administration runbooks.

## Dependencies deployment and security

Maintain runtime/dependency/license inventories. For updates, record the reason, upstream compatibility/security information, pinned candidates and rollback path. Review generated/copied examples for provenance. Initial governance/access verification remains ELS-46 and is not claimed complete by this plan.

Keep credentials outside Git and real borrower information out of examples/AI prompts. Review response/log exposure when error handling changes. Verify successful and rejected authentication plus transport controls once implemented. Recheck environment templates, database isolation, health checks and clean startup when deployment changes.

## Test context and documentation upkeep

Maintain requirement-to-test links and assertions that discriminate incorrect behavior. Fixed Clocks should expose relevant local/UTC boundaries; index tests should vary related keys independently. Preserve red-test chronology and restore temporary experiment changes exactly.

Keep measurement settings/scope compatible and explain denominator changes. Update AGENTS.md when commands, responsibilities, tooling or recurring mistakes change. Process changes identify their authority; templates never establish approval, effort or test success.

## Debt and follow-up register

| Area | Current evidence or limitation | Follow-up |
| --- | --- | --- |
| Access/transport | Local runtime is unauthenticated; delivery controls pending | ELS-46, then acceptance/rehearsal |
| Static analysis | Baseline has nine untriaged warnings; intentional patterns require context | ELS-43 verifies a significant concern and compares results |
| Refactoring/models | ELS-42 underway; final structure not yet accepted | Link three changes and synchronize models/context after merge |
| Release reproducibility | Packaged checks exist; final clean-clone verification pending | ELS-45 and ELS-47 |
| Effort | Sprint 2 approximate actuals confirmed; precise sessions/other actuals incomplete | Collect supplied records through the effort template |
| Documentation/understanding | Report preparation exists; four-member review/rehearsal pending | ELS-48, ELS-49 and ELS-50 |

Warnings are not automatically defects. Reservations, notifications, quotas or institutional integration require explicit scope decisions rather than opportunistic maintenance additions.

## Related sources

[Architecture](../design/architecture.md), [persistence decision](../decisions/002-database-and-concurrency.md), [strategy](../testing/test-strategy.md), [GQM](../testing/gqm-plan.md), [runbook](../deployment/local-setup.md), [report](../../reports/final-report.md) and [evidence templates](../testing/evidence-templates.md).
