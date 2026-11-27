# Requirements Implementation and Test Traceability

This table reconciles requirements with source and meaningful evidence merged through ELS-36.
An implementation reference establishes where behavior exists; only an observed test/run record
establishes execution. Jira state and planned assertions are not substitutes for evidence.

| Requirement | Implementation | Meaningful tests | State and evidence |
| --- | --- | --- | --- |
| FR01 student registration/retrieval/update | [StudentController](../../src/main/java/edu/university/equipmentlending/student/StudentController.java), [StudentService](../../src/main/java/edu/university/equipmentlending/student/StudentService.java), Student DTO/repository/entity package | [StudentControllerTest](../../src/test/java/edu/university/equipmentlending/student/StudentControllerTest.java), [StudentServiceTest](../../src/test/java/edu/university/equipmentlending/student/StudentServiceTest.java), [StudentPersistenceIT](../../src/test/java/edu/university/equipmentlending/student/StudentPersistenceIT.java) | **Technical acceptance passed.** [TC01-TC03, TC24](../../reports/sprint-4/els-47-final-release-acceptance.md) bind final observations to exact sources/artifacts. Human release peer review remains pending. |
| FR02 equipment create/list/update/delete | [EquipmentController](../../src/main/java/edu/university/equipmentlending/equipment/EquipmentController.java), [EquipmentService](../../src/main/java/edu/university/equipmentlending/equipment/EquipmentService.java), equipment persistence package | [EquipmentControllerTest](../../src/test/java/edu/university/equipmentlending/equipment/EquipmentControllerTest.java), [EquipmentServiceTest](../../src/test/java/edu/university/equipmentlending/equipment/EquipmentServiceTest.java), [EquipmentPersistenceIT](../../src/test/java/edu/university/equipmentlending/equipment/EquipmentPersistenceIT.java) | **Technical acceptance passed.** [TC04-TC06, TC24](../../reports/sprint-4/els-47-final-release-acceptance.md) bind final observations to exact sources/artifacts. Human release peer review remains pending. |
| FR03 valid borrowing and business failures | [LoanController](../../src/main/java/edu/university/equipmentlending/loan/LoanController.java), [LoanService](../../src/main/java/edu/university/equipmentlending/loan/LoanService.java), strict loan DTOs | [BorrowingContractIT](../../src/test/java/edu/university/equipmentlending/loan/BorrowingContractIT.java), [LoanServiceTest](../../src/test/java/edu/university/equipmentlending/loan/LoanServiceTest.java), [LoanDateValidationIT](../../src/test/java/edu/university/equipmentlending/loan/LoanDateValidationIT.java) | **Technical acceptance passed.** [TC07-TC10, TC20](../../reports/sprint-4/els-47-final-release-acceptance.md) bind final observations to exact sources/artifacts. Human release peer review remains pending. |
| FR04 one active loan under concurrency | [LoanService](../../src/main/java/edu/university/equipmentlending/loan/LoanService.java), equipment pessimistic lock, [V1 partial unique index](../../src/main/resources/db/migration/V1__initial_lending_schema.sql) | Coordinated cases in [BorrowingContractIT](../../src/test/java/edu/university/equipmentlending/loan/BorrowingContractIT.java) and [LendingIntegrityIT](../../src/test/java/edu/university/equipmentlending/loan/LendingIntegrityIT.java) | **Technical acceptance passed.** [TC11](../../reports/sprint-4/els-47-final-release-acceptance.md) bind final observations to exact sources/artifacts. Human release peer review remains pending. |
| FR05 return once, restore availability and preserve history | [LoanReturnController](../../src/main/java/edu/university/equipmentlending/loan/LoanReturnController.java), [LoanReturnService](../../src/main/java/edu/university/equipmentlending/loan/LoanReturnService.java), ReturnWrite and repository locks | [LoanReturnContractIT](../../src/test/java/edu/university/equipmentlending/loan/LoanReturnContractIT.java) | **Technical acceptance passed.** [TC12-TC15, TC20](../../reports/sprint-4/els-47-final-release-acceptance.md) bind final observations to exact sources/artifacts. Human release peer review remains pending. |
| FR06 availability, filtered history and overdue | [EquipmentController/Service](../../src/main/java/edu/university/equipmentlending/equipment/EquipmentController.java), [LoanQueryController](../../src/main/java/edu/university/equipmentlending/loan/LoanQueryController.java), [LoanQueryService](../../src/main/java/edu/university/equipmentlending/loan/LoanQueryService.java), [LoanQueryRepository](../../src/main/java/edu/university/equipmentlending/loan/persistence/LoanQueryRepository.java) | [EquipmentControllerTest](../../src/test/java/edu/university/equipmentlending/equipment/EquipmentControllerTest.java), [LoanQueryControllerTest](../../src/test/java/edu/university/equipmentlending/loan/LoanQueryControllerTest.java), [LendingQueriesIT](../../src/test/java/edu/university/equipmentlending/loan/LendingQueriesIT.java) | **Technical acceptance passed.** [TC16-TC18, TC25](../../reports/sprint-4/els-47-final-release-acceptance.md) bind final observations to exact sources/artifacts. Human release peer review remains pending. |
| FR07 prevent equipment-history deletion and delete/borrow orphaning | [EquipmentService.delete](../../src/main/java/edu/university/equipmentlending/equipment/EquipmentService.java), restrictive loan FK and equipment lock | Equipment persistence/history cases, [LendingIntegrityIT](../../src/test/java/edu/university/equipmentlending/loan/LendingIntegrityIT.java) and packaged HTTP/SQL races | **Technical acceptance passed.** [TC19-TC20](../../reports/sprint-4/els-47-final-release-acceptance.md) bind final observations to exact sources/artifacts. Human release peer review remains pending. |
| NFR01 PostgreSQL integrity and rollback | V1 constraints/index, service-owned transactions and common equipment lock order | DatabaseSetupIT, LendingIntegrityIT and borrowing/return/query PostgreSQL integration suites | **Technical acceptance passed.** [TC03/05/07-15/19-20/24-25](../../reports/sprint-4/els-47-final-release-acceptance.md) bind final observations to exact sources/artifacts. Human release peer review remains pending. |
| NFR02 reproducible build/run | Maven Wrapper, pinned POM, Compose files, [local setup](../deployment/local-setup.md) | Main/database Maven profiles; [ELS-36 runner](../../tools/validation/README.md); executable [ELS-37 API workflow](../api/examples.ps1) | **Technical acceptance passed.** [TC23, AQ03](../../reports/sprint-4/els-47-final-release-acceptance.md) bind final observations to exact sources/artifacts. Human release peer review remains pending. |
| NFR03 access and privacy | DTO-only controllers, strict deserialization, sanitized [ApiExceptionHandler](../../src/main/java/edu/university/equipmentlending/shared/error/ApiExceptionHandler.java), [SecurityConfiguration](../../src/main/java/edu/university/equipmentlending/shared/config/SecurityConfiguration.java) and environment-bound staff credential | Shared error/strict-write tests and [StaffAccessSecurityIT](../../src/test/java/edu/university/equipmentlending/shared/config/StaffAccessSecurityIT.java) | **Technical acceptance passed.** [TC21-TC22, AQ03](../../reports/sprint-4/els-47-final-release-acceptance.md) bind final observations to exact sources/artifacts. Human release peer review remains pending. |
| NFR04 maintainable layers and current contracts | Feature packages, service transaction boundaries, repository-only persistence, ELS-37 API/diagram reconciliation | Source-to-contract review, link/OpenAPI/Mermaid checks and peer review | **Technical acceptance passed.** [AQ01](../../reports/sprint-4/els-47-final-release-acceptance.md) bind final observations to exact sources/artifacts. Human release peer review remains pending. |
| NFR05 meaningful test quality and measurement | Test strategy and feature suites | Unit/controller/integration suites; later quality cards | **Technical acceptance passed.** [AQ02](../../reports/sprint-4/els-47-final-release-acceptance.md) bind final observations to exact sources/artifacts. Human release peer review remains pending. Historical metrics remain revision-scoped; no fresh coverage/static-analysis measurement is inferred. |

## Substantial specification classification

| Criterion | Classification | Basis or follow-up |
| --- | --- | --- |
| Strict loan IDs/dates and date ordering | Implemented | Strict deserializers, DTO validation, service rules and PostgreSQL checks; expanded/signed-year correction preserved in ELS-33 evidence. |
| Normalized unique student/equipment identifiers | Implemented | Service normalization plus named PostgreSQL uniqueness constraints and conflict classification. |
| One active loan under contention | Implemented | Equipment lock plus partial unique index; PostgreSQL concurrency evidence linked above. |
| Return once with immutable history | Implemented | Equipment-first return lock, locked reread and only `returnDate` mutation; PostgreSQL duplicate/concurrent-return evidence. |
| Availability from active-loan existence | Implemented | No availability column; EquipmentService derives it through LoanRepository. |
| History filters and stable ordering | Implemented | Nullable filters combine with AND in LoanQueryRepository and results order by ID. |
| Overdue boundary | Implemented | Active and `dueDate < today` using injected Beirut clock; due-today and returned cases covered. |
| Integrated principal workflow and delete/borrow acceptance | Implemented and verified | PR #12 merged ELS-36 evidence: 83 packaged-app requests, 626 assertions, SQL state oracles and coordinated borrow/borrow, borrow/return and borrow/delete races. |
| Staff authentication and HTTPS | Staff authentication implemented; remote HTTPS remains a deployment prerequisite | The runtime enforces environment-configured Basic authentication and sanitized `401` responses. Local loopback verification does not prove remote TLS termination. |

## ELS-37 validation record

The [ELS-37 AI log](../../ai-log/els-37-api-documentation.md) records the commands, initial
failures, corrections and final outcomes for this change. OpenAPI lint completed successfully
with four documented warnings; JSON references, local links, PowerShell parsing, ten Mermaid
renders, the full Maven profiles and the executable API workflow all passed. Merged ELS-36
evidence is linked in the table and remains distinct from this card's execution. Historical
red/green files remain unchanged and keep their original chronology. Human review and remote CI
are separate gates and are not claimed here.

## ELS-41 verified design corrections

Hussein Chreif approved two reproduced important concerns before production edits: equipment
constraint-message misclassification and per-item list queries. [Review and plan](../reviews/els-41-design-review-and-plan.md)
record the human decision and screened alternatives. EquipmentRepository now projects the same
ordered equipment DTO/filter results in one query; EquipmentService matches exact constraint
name and SQLSTATE metadata. FR02/FR06 response behavior and NFR04 repository/service boundaries
are preserved, with unrelated failures correctly retaining sanitized internal errors.

[Execution evidence](../../reports/sprint-3/els-41-design-corrections.md) records 381 passing
unit/controller/PostgreSQL tests, 83 packaged HTTP requests/626 assertions, before/after
statement counts and constraint outcomes. Original borrowing red tests and delivery schema
are unchanged. Final implementation review/merge remains the ELS-41 peer gate; this update
does not claim completion of ELS-39, ELS-42 or ELS-43.

## ELS-47 final technical acceptance

[The final matrix](../../reports/sprint-4/els-47-final-release-acceptance.md) and [structured results](../../reports/sprint-4/els-47-acceptance-results.json) bind TC01-TC25/AQ01-AQ03 to observed outcomes, exact source/environment and direct evidence. Fresh regression, expanded authenticated HTTP/SQL boundaries/rollback/concurrency and ordinary Docker delivery passed on integrated source. Counts and JAR/image identities are revision-scoped in the report; historical quality measurements are not relabeled as new runs.

Original preparation/storage failure remains history. Current-engine resource absence is distinguished from other-workstation cleanup. Technical acceptance does not establish remote HTTPS/browser behavior or human approval. Komayl review, merge, report/presentation closeout and submission remain separate gates.
