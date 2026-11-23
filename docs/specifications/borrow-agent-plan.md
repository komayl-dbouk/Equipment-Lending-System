# Borrowing Agent Understanding and Implementation Plan

Prepared by Codex for ELS-27. This is an actual proposed plan, not an implementation or human approval record.

## Understanding assumptions and clarified decisions

One student borrows one physical item per request. Active means returnDate is null. Date-only semantics, normalization, DTOs and error codes follow the requirements/contract. Item availability must be checked under the same transaction that inserts the loan, using PostgreSQL row locking and a partial unique index. Keep existing historical rows. Authentication is a delivery responsibility; tests must clearly identify when security is isolated.

There is no application skeleton yet. Package root, actual file names, migration numbering, exact dependency versions and Maven plugin commands must be resolved against ELS-23 before editing. Planned paths below are structural targets, not verified existing files.

## Proposed files and boundaries

| Area | Proposed additions or changes |
| --- | --- |
| loan model/repository | src/main/java/<base>/loan/Loan.java and LoanRepository.java |
| loan transport/service | LoanController.java, LoanService.java and request/response DTOs under the loan feature |
| equipment persistence | Add explicit equipment locking query to existing EquipmentRepository |
| shared errors | Add stable missing-record/date/unavailable exception mapping without changing unrelated outcomes |
| migration | Next src/main/resources/db/migration version creates loan constraints and active-loan uniqueness if not already present |
| unit/controller tests | src/test/java/<base>/loan service and endpoint scenarios |
| database tests | Independent-connection PostgreSQL borrowing contention and rollback scenarios |
| evidence | ai-log, test-first record, requirements traceability and contract alignment |

Do not add a second transaction manager, duplicate shared exceptions or an alternative schema setup. Avoid modification of unrelated controllers/services. Return and query endpoints belong to their own cards.

## Proposed execution sequence

1. Read actual AGENTS.md, build, current migrations, approved specification and existing conventions.
2. Compare the plan against source and list any conflicts before editing.
3. Write behavior-focused tests for success, missing references, invalid dates, active-loan conflict, persistence rollback and coordinated concurrency.
4. Run the applicable tests, retain meaningful expected red results and commit the tests on the feature branch.
5. Have a human review the minimum implementation plan and record decisions.
6. Add the transaction/model/repository/migration/DTO/controller behavior; flush writes before reporting success and classify confirmed conflicts safely after rollback.
7. Run focused tests, then the complete suite; record green results and implementation commit.
8. Refactor only from a green baseline when there is an actual smell, rerun the suite and preserve history.
9. Compare BOR01â€“08 with observed results, update traceability/AI log and request peer review.

## Proposed commands and approval

Once Maven Wrapper exists, use the repository's documented Windows wrapper command for focused tests and full verify. Select test class names and coverage/static-analysis commands from the actual build; do not state they ran before they exist. Database setup follows the recorded test profile, with disposable data.

Human review record: reviewer/iteration/decision/changes requested remain unrecorded. The user authorized creation of artifacts; that is not a recorded team approval of this future substantial implementation plan. Obtain it before feature code generation. No tests, migrations or application code were executed or modified by preparing this plan.
