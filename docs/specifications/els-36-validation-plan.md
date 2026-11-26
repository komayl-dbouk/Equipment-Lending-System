# ELS-36 validation plan and coverage inventory

Owner: Mostafa Younes. Reviewer: Komayl Dbouk. Baseline: origin/main 90dc0a1, containing merged borrowing, returns and lending queries. Clean working tree; prior branches and test-first history preserved.

Mostafa explicitly answered **Approve the ELS-36 plan** in this chat before substantial implementation. Jira comment 10076 records approval, intended files and the move to In Progress. This is plan approval; implementation acceptance remains pending.

| Acceptance | Existing source coverage to rerun | Gap addressed by this card |
| --- | --- | --- |
| Student/equipment CRUD, normalization, strict fields and rejected writes | StudentControllerTest/StudentPersistenceIT, EquipmentControllerTest/EquipmentPersistenceIT, StrictWriteValidationTest | One reproducible packaged-app HTTP workflow with independent SQL snapshots |
| Borrow, duplicate active loan, missing references, dates and committed state | BorrowingContractIT, LoanDateValidationIT, LoanServiceTest | Reuse existing strong cases; run borrow-vs-borrow over actual HTTP as part of system validation |
| Return, original history, repeat/invalid returns and reborrow | LoanReturnContractIT | Verify whole workflow over actual HTTP and query retained history |
| Availability, ordered AND filters, unmatched/empty results, overdue boundaries | LendingQueriesIT with fixed Clock; LoanQueryControllerTest | Packaged-app checks with separate test-only fixed Clock fixture |
| Rollback and commit-before-success | BorrowingContractIT insert-failure trigger; LoanReturnContractIT immediate/deferred failures | Reuse existing failure tests; no mechanical duplicate |
| Cross-feature contention and integrity | Existing same-operation races; equipment history protection | Borrow-vs-return, borrow-vs-delete, SQL bypass checks for active uniqueness, restrictive references, orphan rejection and date constraints |
| Environment safety | DatabaseTestSafetyProcessorTest and guarded database-test profile | Verify effective DB/user/version before test execution; separate disposable live/test targets and credentials |

Implementation: add LendingIntegrityIT, a test-only fixed Clock configuration outside the application scan, and tools/validation scripts for packaged-app startup plus HTTP/SQL checks. Add a readable reports/sprint-2 report, sanitized machine results, traceability and ai-log entry. No new production endpoint, dependency, migration, clock setting or architecture change is planned. Reproduce a small in-scope defect before the smallest correction, if one appears.

Concurrency: coordinate two request threads with latches and hold an independent equipment row lock until PostgreSQL reports both contenders waiting. Accept either serial winner. Compare responses with committed rows, original history and derived availability; assert no orphan or extra active row. Do not use arbitrary sleeps to choose a winner.

Live validation: launch the built executable JAR with Spring Boot PropertiesLauncher and a separate fixture JAR containing only an explicit fixed-Clock configuration. Exercise normal HTTP endpoints over loopback; SQL oracles use psql with credentials from environment. Fixture business day is 2030-06-15 in Asia/Beirut, independent of execution day. Keep this fixture outside the delivery JAR and verify that fact.

Commands: focused safety/integrity/query checks, `mvnw.cmd verify`, `mvnw.cmd -Pdatabase-test verify`, then `tools/validation/run-lending-workflow.ps1` against a separate empty disposable workflow database. Use Java 17, Maven Wrapper 3.9.11 and PostgreSQL 17.5; Docker or the documented isolated-native fallback, synthetic records only. Never overwrite .env. Clean up only task-owned resources.

Delivery: audit diff/staged paths, commit with ELS-36, push ELS-36-lending-validation, create the specified PR into main, request komayl-dbouk, inspect CI for the current head, record Jira links and move to CODE REVIEW. No self-approval, merge or Done transition. Authentication/HTTPS remain ELS-46; no execution dates, credentials, Subject/ or generated build output in commits.
