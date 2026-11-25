# Borrowing Agent Understanding and Implementation Plan

Planning cards: ELS-27 and ELS-32. Implementation card: ELS-33. Baseline: main 854ae16. Test owner: Hussein Chreif. Red-test reviewer: Hussein Chalhoub. Implementation owner: Komayl Dbouk; implementation-plan reviewer: Hussein Chreif.

This is the concrete source-based plan for review, not a record of human approval or implemented borrowing.

## Existing foundation and scope

The application exists under edu.university.equipmentlending. Student and equipment CRUD, shared errors, strict text handling and database isolation are merged. LoanEntity and LoanRepository already exist under loan/persistence. EquipmentRepository.findByIdForUpdate supplies the pessimistic equipment lock. V1 already creates loan foreign keys, date checks and the unique active-loan index ux_loan_active_equipment. Reuse these; do not edit V1, duplicate entities/schema, add a second transaction manager or store an availability flag.

POST /api/loans accepts positive integer studentId/equipmentId and strict ISO date strings borrowingDate/dueDate. References exist, dueDate is later, and the item has no active loan. Return only the six-field Loan DTO after a successful commit. Preserve historical rows and existing student/equipment behavior. Delivery authentication remains ELS-46; no 401 oracle is added to this isolated borrowing red phase.

## ELS-32 test-only phase

Add src/test/java/edu/university/equipmentlending/loan/BorrowingContractIT.java using the real Spring MVC stack, migrated PostgreSQL, synthetic fixtures and independent JdbcTemplate assertions. Compile against the current foundation without importing nonexistent loan DTO/service/controller classes or creating production stubs.

Cover BOR01 success and exact committed values; BOR02 missing references; BOR03 equal/reversed dates; BOR04 active-loan rejection; BOR05 coordinated requests using independent service transactions; BOR06 rejected/failing writes preserving records and availability; BOR07 reborrow with returned history; BOR08 invalid/unknown request fields. Include strict ID/date types, overflow and malformed JSON. Use fixed business-date fixtures, not the machine clock.

Use test-owned fixture identifiers and cleanup only those rows. For the transaction-failure oracle install a conditional, test-only PostgreSQL AFTER INSERT trigger that raises an error for the fixture equipment, then always remove it. This fault fixture is not a delivery migration and does not alter V1. Assert no partial row remains and equipment is available. It tests failure rollback, not a successful commit oracle or production failure frequency.

The red run must reach real request assertions: the current missing POST route returns 404 instead of required success/validation/conflict outcomes. A missing-reference response is red only if its required business error code differs; generic route-not-found 404 must not be mistaken for success. Schema/environment/compilation errors are not accepted as red evidence.

Retain the specification/plan commit, test commit, executable command and actual Failsafe results. Existing non-borrowing tests should stay green. Do not disable the new tests or weaken their oracles to make the branch pass.

## ELS-33 minimum implementation proposal

| Area | Minimum proposed work |
| --- | --- |
| loan/dto | LoanWrite with strict JSON integer/date types and required/positive validation; immutable six-field Loan response DTO; unknown fields remain rejected by existing configuration |
| loan/persistence/LoanEntity | Add the minimal constructor/getters needed to persist immutable references/dates and a null returnDate; reuse the existing mapping |
| loan/persistence/LoanRepository | Reuse active-loan existence checks and JpaRepository persistence; add only queries justified by the borrowing operation |
| loan/LoanService | Validate date order, then verify student and lock equipment before checking active state and saving/flushing in one short transaction |
| loan/LoanController | Add POST /api/loans with validated DTO input and 201 DTO output only after transaction commit |
| shared errors | Reuse existing exception types and safe response advice; retain VALIDATION_FAILED, INVALID_LOAN_DATES, STUDENT_NOT_FOUND, EQUIPMENT_NOT_FOUND and EQUIPMENT_UNAVAILABLE |
| database | Reuse V1 constraints/index and existing equipment lock; no new migration is currently justified |

Use the existing PlatformTransactionManager and a transaction boundary that completes rollback before mapping a confirmed SQLSTATE 23505/ux_loan_active_equipment conflict to EQUIPMENT_UNAVAILABLE. TransactionTemplate, following the existing student convention, is one viable approach. Do not recover by querying an aborted PostgreSQL transaction or translate unrelated integrity errors into business conflicts. A DTO must not escape as success if commit fails. Preserve equipment-first ordering across borrowing, future returns and deletion.

Do not implement returns, filtered history, overdue queries, authentication or unrelated refactoring in ELS-33. Add focused service/controller tests where they provide evidence beyond the contract tests, then execute the complete PostgreSQL profile and compare BOR01-BOR08 against results.

## Commands and isolated database

Use Java 17, Maven Wrapper 3.9.11 and PostgreSQL 17.5. Follow docs/deployment/local-setup.md and compose.test.yaml with a dedicated database ending in _test and test-only credentials. Do not point tests at the application database or commit credentials. ELS-51's test-only startup guard must remain active.

Focused red: mvnw.cmd --batch-mode --no-transfer-progress -Pdatabase-test -Dit.test=BorrowingContractIT verify. Full regression: mvnw.cmd --batch-mode --no-transfer-progress -Pdatabase-test verify. An offline flag may be used when all pinned dependencies are already cached. Record actual commands/environment and distinguish expected behavioral failures from infrastructure failures.

## Review and handoff

Hussein Chalhoub reviews test oracles and observed red output. Hussein Chreif reviews Komayl's minimum implementation plan before ELS-33 production code. Individual reviewer decisions and actual hours remain unrecorded until supplied; the user authorized this test-only task, not a fabricated four-person approval.

Keep the red ELS-32 PR as a draft and do not merge failing tests into main. After review, create a separate ELS-33 issue branch from the exact reviewed ELS-32 red-test commit. Its green PR into main includes the preserved test-first and implementation commits. Link/supersede the draft test PR when those tests reach main. Do not squash away required red/green chronology. Each card retains its owner, reviewer and evidence.
