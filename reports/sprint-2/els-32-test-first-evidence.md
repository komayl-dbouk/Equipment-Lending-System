# ELS-32 Borrowing Test-First Evidence

Owner: Hussein Chreif. Assigned reviewer: Hussein Chalhoub. Branch: ELS-32-borrowing-red-tests. Baseline: merged main 854ae16. This is an intentionally red, test-only increment; it is not borrowing implementation or acceptance success.

## Preserved chronology

1. Existing specification and source baseline are already in main.
2. ef2aa12: reconcile the borrowing implementation plan with actual source before tests.
3. a847e09: commit the executed BorrowingContractIT red tests, with no production loan code or stub classes.
4. This evidence update records observed results and the reviewed handoff requirements. Start ELS-33 from the reviewed ELS-32 branch tip so the plan, unchanged red-test commit and evidence all remain ancestors of the green implementation.

Do not squash or amend away the red-test commit, disable the tests, or merge the failing draft into main. The separate ELS-33 green PR will carry the test-first history into main after the required reviews.

## Test scope and independent oracles

Tests live in src/test/java/edu/university/equipmentlending/loan/BorrowingContractIT.java. They compile against the existing application using Spring Boot/MockMvc and real PostgreSQL; no missing loan Java class is imported. Assertions derive from the six-field Loan DTO, stable ApiError contract, BOR01-BOR08 and database state rather than implementation internals. There is no test-wide rollback transaction, so JDBC queries observe the real committed/rolled-back request outcome.

| Criterion | Test method | Required oracle |
| --- | --- | --- |
| BOR01 / TC07 | createsOneCommittedLoanWithExactDtoAndMakesEquipmentUnavailable | 201, exact six-field DTO, positive ID, literal request dates/references, null returnDate, independent stored row, one loan, unchanged student/equipment fields and derived unavailable state |
| BOR02 / TC08 | missingReferencesHaveBusiness404AndDoNotWrite (2) | Correct STUDENT_NOT_FOUND/EQUIPMENT_NOT_FOUND code, no loan and available item; a generic route-not-found 404 cannot satisfy the test |
| BOR03 / TC09 | equalOrReversedDatesAreRejectedWithoutWrites (2) | 400 INVALID_LOAN_DATES, no row or availability change |
| BOR04 / TC10 | activeLoanConflictPreservesOriginalHistory | 409 EQUIPMENT_UNAVAILABLE and identical existing history |
| BOR05 / TC11 | competingRequestsProduceOneCreatedOneConflictAndOneActiveRow | Two coordinated independent request threads; exactly one 201 and one 409, one stored active row and unavailable item |
| BOR06 / TC20 | failedInsertRollsBackAndDoesNotClaimSuccessOrConsumeAvailability | Conditional test-only AFTER INSERT trigger raises a technical error; nontransactional sequence proves the injected write was attempted; sanitized 500, no partial loan and no false unavailability |
| BOR07 / TC15 | returnedHistoryAllowsReborrowWithoutChangingHistory | New 201, different ID, two history rows, one active row and unchanged returned row |
| BOR08 / TC24 | invalidFieldTypesValuesAndDatesDoNotWrite (28), omittedAndNullFieldsDoNotWrite (8), unknownWriteFieldsDoNotWrite (4), malformedOrAbsentBodiesDoNotWrite (4) | 400 VALIDATION_FAILED, safe errors and no writes; strict integer/ISO date types, overflow, impossible dates, unknown fields and malformed bodies |

Delivery credentials/401 behavior (BOR08 / TC21) remain ELS-46 and are not falsely tested as part of the current unauthenticated foundation. No new date restrictions, quotas, reservations or return/query features are introduced. Business-date fixtures are fixed; no execution-date metadata is added to this record.

Fixture cleanup touches only ELS32-prefixed equipment, the els32.example.test student domain and their loans. The conditional fault trigger/function and rollback-resistant sequence are ephemeral test fixtures, always removed after tests; no applied migration changes. PostgreSQL confirms zero remaining owned rows, function, trigger and sequence after execution.

## Observed execution

Java 17.0.12, Maven Wrapper 3.9.11 and PostgreSQL 17.5-alpine. Docker Desktop was initially stopped and was started for the task. A dedicated compose.test.yaml service used its own credentials, database equipment_lending_test and local port 15433; no application database was used. Commands ran offline against cached pinned dependencies, without disabling the enforcer.

Focused command: mvnw.cmd -o --batch-mode --no-transfer-progress -Pdatabase-test -Dit.test=BorrowingContractIT verify. Result: exit 1; 53 borrowing tests, 53 assertion failures, zero errors/skips. The existing 164 database-free tests passed during this lifecycle.

Full command: mvnw.cmd -o --batch-mode --no-transfer-progress -Pdatabase-test verify. Result: exit 1; 164 unit/controller tests pass, all 39 existing PostgreSQL integration tests pass, and all 53 borrowing cases fail on expected behavioral assertions. Integration total is 92, with 53 failures and zero errors/skips. Overall: 203 passing existing checks and 53 new red tests. This is a successful red-phase observation, not a green build.

The missing POST /api/loans endpoint returns 404 instead of required 201/400/409/500 outcomes. Missing-reference cases reach the expected 404 status but fail the required business error code. Coordinated requests currently return two 404s rather than one creation and one conflict. Compilation, Flyway, fixtures and PostgreSQL startup succeeded. No infrastructure error is counted as expected red evidence. Future green assertions after the current failure point have not yet established working lending behavior.

The final full run includes the rollback-resistant sequence oracle. Its preliminary focused predecessor also had 53 behavioral failures/zero errors; a final trigger block terminator was made explicit and the attempt marker was added before the full run. [Structured results](els-32-red-results.json) retain the final exact suite counts and individual failure messages without raw timestamps or credentials.

Raw focused/full logs and XML reports remain in ignored target/els-32 and target/surefire-reports/target/failsafe-reports. Disposable database services and credentials are removed after verification. git diff --check is required before every commit.

## Review and ELS-33 handoff

Hussein Chalhoub must review the test oracles and observed red run. Hussein Chreif must review Komayl's minimum implementation proposal in docs/specifications/borrow-agent-plan.md before ELS-33 production code. The user authorized this test-only task; no individual review, team meeting, actual effort or implementation approval has been invented.

Keep this PR draft and preserve main's green baseline. After actual review, Komayl creates ELS-33-atomic-borrowing from the reviewed ELS-32 branch tip, including a847e09 and its evidence, then implements only borrowing. The green ELS-33 PR targets main and preserves the chronological commits. Link and close/supersede the draft ELS-32 PR once the test-first commits reach main through that green PR. ELS-32 remains in review until the named review and acceptance conditions are recorded.
