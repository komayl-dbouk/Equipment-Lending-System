# ELS-36 Verify lending workflow and PostgreSQL integrity

## Purpose

The merged features had strong individual regression tests, but the full lending workflow needed reproducible packaged-application validation and cross-feature contention checks. This change verifies the integrated behavior against PostgreSQL without changing production code.

## Changes

- I added four PostgreSQL cases for borrow versus return, borrow versus deletion, active-loan uniqueness through direct SQL, and restrictive references/orphan/date rejection. The races observe both requests waiting on equipment locks, accept legitimate serial outcomes, and verify committed history, availability and row counts.
- I added a reusable loopback HTTP/SQL runner for student/equipment maintenance, borrowing, rejected duplicates, availability, overdue boundaries, AND history filters, returns, reborrowing and deletion protection. It checks exact DTO/error shapes, normalization, ordering, empty results, strict inputs and unchanged snapshots after rejection.
- A separately loaded test-only fixed Clock makes overdue checks independent of the execution day. The fixture is excluded from the delivery JAR. Existing rollback, query, borrowing and return tests are preserved.
- I recorded acceptance mapping, source revisions, failures/corrections and actual results in the [ELS-36 report](https://github.com/komayl-dbouk/Equipment-Lending-System/blob/ELS-36-lending-validation/reports/sprint-2/els-36-lending-validation.md), [suite evidence](https://github.com/komayl-dbouk/Equipment-Lending-System/blob/ELS-36-lending-validation/reports/sprint-2/els-36-verification-results.json), [HTTP/SQL evidence](https://github.com/komayl-dbouk/Equipment-Lending-System/blob/ELS-36-lending-validation/reports/sprint-2/els-36-http-results.json) and [AI log](https://github.com/komayl-dbouk/Equipment-Lending-System/blob/ELS-36-lending-validation/ai-log/els-36-lending-validation.md). [Runner instructions](https://github.com/komayl-dbouk/Equipment-Lending-System/blob/ELS-36-lending-validation/tools/validation/README.md) document reproduction and scoped cleanup.

## Validation

Tested source: `32cda801f34abd268d16b2b26ec1e72c865db560`, based on main `90dc0a1`; subsequent changes contain evidence only.

- `mvnw.cmd --batch-mode --no-transfer-progress -Pdatabase-test -Dtest=DatabaseTestSafetyProcessorTest -Dit.test=LendingIntegrityIT,LendingQueriesIT verify`: **31 passed**.
- `mvnw.cmd verify`: **185 passed**.
- `mvnw.cmd -Pdatabase-test verify`: **362 passed** (185 unit/controller, 177 PostgreSQL integration), **zero failures/errors/skips**.
- `tools/validation/run-lending-workflow.ps1`: **83 real HTTP requests / 626 assertions passed**, with independent SQL DTO/state oracles and three coordinated live races. Borrow versus borrow produced one 201 and one EQUIPMENT_UNAVAILABLE conflict with exactly one active row; cross-feature outcomes and final database state are retained in the evidence.
- The database safety guard passed; effective database/user/version were checked. The runner rejected identical app/test targets and nonempty workflow data. Diff, staged-path and generated-secret checks passed.

Java 17.0.21.1, Maven 3.9.11, Spring Boot 3.5.16, PostgreSQL 17.5 and Python 3.13.7 were used. Docker's engine was unavailable, so I used task-owned isolated native PostgreSQL with separate workflow/test databases and credentials. Infrastructure failures were diagnosed, retained separately and corrected before the passing runs; no assertions were weakened. Remote CI has not yet been observed for this PR head.

## Limitations

Authentication and HTTPS remain ELS-46. Human implementation review, merge and final deployment acceptance are pending. These results do not claim coverage percentages, performance measurements or human effort. Production sources, dependencies, migrations, existing meaningful tests, Subject/ and .env remain unchanged; no credentials or build output are committed. I have not approved or merged this change.

## Jira

[ELS-36 — Verify full lending workflow and database integrity](https://equipment-lending-system.atlassian.net/browse/ELS-36)
