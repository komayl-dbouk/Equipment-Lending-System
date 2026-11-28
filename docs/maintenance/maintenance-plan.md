# Equipment Lending Maintenance Plan

Owner: Hussein Chreif. ELS-48 report review. This plan distinguishes completed engineering examples from procedures for future changes; it does not claim that proposed features or final human release gates are implemented.

## Maintenance categories

| Category | Equipment project example | Validation and ownership |
| --- | --- | --- |
| Corrective | Completed examples: strict date/constraint classification and missing staff-variable/log-privacy corrections. Future defects need a reproduced failure and issue-linked fix | Feature owner writes a discriminating regression; peer checks requirements, exact errors, committed state and PostgreSQL results; see [ELS-41](../../reports/sprint-3/els-41-design-corrections.md) and [ELS-46](../../reports/sprint-4/els-46-correction-results.json) |
| Adaptive | Completed package/auth integration changed runtime configuration. Future runtime/database upgrades or browser clients require approved compatibility/security changes | Chalhoub coordinates environment/tool changes; Komayl checks persistence/dependencies; verify pinned candidates, licensing, migration order, rollback and clean setup |
| Perfective | Completed example: bounded equipment projection replaces N+1 queries. Future UI/search/pagination work needs an explicit contract and measured value | Chreif confirms scope; feature owner measures comparable before/after outcomes and preserves exact DTOs/filter/order semantics; see [measurement](../../reports/sprint-3/els-43-measured-quality-improvement.md) |
| Preventive | Completed examples: stronger audit oracles, shared helpers, secret/target guards and automated release acceptance. Future changes maintain these protections | Mostafa audits tests; Chalhoub maintains tooling; keep context and evidence current; see [audit](../../reports/sprint-3/els-39-test-audit.md) and [acceptance](../../reports/sprint-4/els-47-final-release-acceptance.md) |

## Change and rollback workflow

Create a Jira item with the requirement, observed problem, reproduction, owner/reviewer, risk and estimate. Distinguish a behavior correction from safe refactoring. Review significant agent plans and document accepted, modified or rejected concerns with evidence.

Use one issue branch without a codex prefix and issue-key commits. Coordinate shared/schema changes. Run focused tests during development, both documented full verification profiles before review, and packaged HTTP/database checks where the affected boundary warrants them. Inspect explicit staged paths and whitespace. A green pipeline and generated text do not replace human acceptance.

After integration, update affected requirements, API examples, models, traceability, context and runbooks. Record the tested revision. A release regression requires a reviewed revert/rollback and retesting; do not erase evidence or weaken assertions. Define the recovery path before risky environment or schema work.

## Persistence and historical data

Flyway is the sole delivery schema owner. Add sequential migrations; never edit an applied migration or enable JPA schema creation/update. Coordinate multi-feature work with the foundation owner. Check existing data compatibility, backup/restore arrangements and deployment order before execution.

Preserve restrictive references, normalized uniqueness, date checks, history indexes and the active-loan partial index. Borrowing, return and deletion retain compatible equipment-first locking and commit-before-success behavior. Test migration and race scenarios on guarded disposable PostgreSQL databases with synthetic data. Production data correction requires its own reviewed script and recovery plan; test cleanup scripts are not administration runbooks.

## Dependencies deployment and security

Maintain the [runtime/dependency/license inventory](../../reports/sprint-4/els-46-access-privacy-license-review.md). For updates, record the reason, authoritative compatibility/security information, pinned candidates and rollback path. Review generated/copied examples for provenance. Staff access and the local delivery boundary are implemented and verified; remote TLS, browser policy and exact redistribution notices remain separate obligations. No legal approval or vulnerability scan is inferred.

Keep credentials outside Git and real borrower information out of examples/AI prompts. Recheck successful/rejected authentication and full no-write snapshots after boundary changes. Preserve production application-log sanitization and public aggregate-only health. Verify HTTPS before remote credential transport; coordinate browser CSRF/origin policy before the planned frontend. Recheck templates, isolation, target guards, checkpoint secrecy and clean startup after deployment changes.

Use `tools/validation/run-release-acceptance.py` for repeatable packaged HTTP/SQL regression and the documented Docker smoke for ordinary-image verification. Keep actual source/JAR/image identities, test Clock scope and cleanup together. Never replace a blocked Docker run with a relabeled JAR pass. Back up real data before risky changes and rehearse restoration in an owned environment; disposable seed/reset helpers are not production backup/restore tools. After credential rotation, update private runtime settings and restart the owned application; no automated rotation service is claimed.

## Test context and documentation upkeep

Maintain requirement-to-test links and assertions that discriminate incorrect behavior. Fixed Clocks should expose relevant local/UTC boundaries; index tests should vary related keys independently. Preserve red-test chronology and restore temporary experiment changes exactly.

Keep measurement settings/scope compatible and explain denominator changes. Update AGENTS.md when commands, responsibilities, tooling or recurring mistakes change. Process changes identify their authority; templates never establish approval, effort or test success.

## Debt and follow-up register

| Area | Current evidence or limitation | Follow-up |
| --- | --- | --- |
| Access/transport | Staff-only local API verified; remote TLS and browser authentication/CSRF/origin not verified | Komayl/Chalhoub coordinate approved frontend/deployment work before exposure |
| Dependency/distribution | License/provenance inventory exists; final notices bundle and supported-stack updates remain explicit | Release owner checks ELS-50 distribution obligations; update inventory after any dependency/image change |
| Static analysis | Historical same-configuration result is nine to eight warnings; eight priority-2 findings remain visible with dispositions | Preserve DTO/null/exception-hierarchy assumptions; remeasure changed source before accepting a new warning disposition |
| Refactoring/models | Three accepted refactorings and current architecture include shared classifier, mapper and validation factory | Keep models/API/ADRs/context synchronized for future changes; do not report a new refactoring without before/after evidence |
| Release reproducibility | Ordinary Docker and expanded acceptance passed; base image uses a maintained major-version tag | Retain actual image identity; pin an immutable digest if byte-identical rebuilds become a requirement; rehearse under ELS-49 |
| Environment ownership | New final-run cleanup passed; old failed-run IDs absent only on the current engine | Original owner checks another workstation if applicable; never reset/prune unrelated Docker projects |
| Effort | Sprint 2 20h and Sprint 3 22h confirmed approximate actual totals; Sprint 1 actuals missing and Sprint 4 open | Record only supplied/confirmed actuals and known variance reasons; no inferred sessions, AI savings or retrospective velocity forecast |
| Documentation/understanding | Report content consolidated; four-member review and live rehearsal remain human gates | [ELS-48 review record](../../reports/els-48-team-review.md), ELS-49 and ELS-50 |

Warnings are not automatically defects. Reservations, notifications, quotas or institutional integration require explicit scope decisions rather than opportunistic maintenance additions.

## Related sources

[Architecture](../design/architecture.md), [persistence decision](../decisions/002-database-and-concurrency.md), [strategy](../testing/test-strategy.md), [GQM](../testing/gqm-plan.md), [runbook](../deployment/local-setup.md), [report](../../reports/final-report.md) and [evidence templates](../testing/evidence-templates.md).
