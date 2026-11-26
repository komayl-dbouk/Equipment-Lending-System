# ELS-36 lending workflow and PostgreSQL integrity

Owner: Mostafa Younes. Human reviewer: Komayl Dbouk. [Approved plan and source inventory](../../docs/specifications/els-36-validation-plan.md). Plan approved in chat and recorded in Jira comment 10076; implementation acceptance remains pending human review.

## Revision and environment

Tested source: **32cda801f34abd268d16b2b26ec1e72c865db560**, based on origin/main **90dc0a1**. Plan commit: 63e1e4b. Production sources, dependencies, migrations and all prior tests are unchanged; original ELS-32 red/green history remains in ancestry. Evidence-only changes follow the tested source commit.

Windows PowerShell, Java **17.0.21.1**, Maven Wrapper **3.9.11**, Spring Boot **3.5.16**, PostgreSQL **17.5**, Python **3.13.7**. Docker's engine socket was unavailable, so verification used the test strategy's isolated-native fallback with cached pinned binaries. No dependency, enforcer or architecture override was introduced.

A task-owned cluster at D:/codex-els36-runtime/pgdata listens only on 127.0.0.1:15436. Separate generated credentials identify equipment_test on equipment_lending_test and equipment_workflow on equipment_lending_workflow_test. Independent psql queries confirmed both effective names/users and server version before the successful database run. ELS_DB_URL and ELS_TEST_DB_URL identify different databases. No existing .env or another developer's resources were used.

Native setup commands used initdb with UTF8, locale C and SCRAM authentication, then pg_ctl start with port 15436, loopback binding, shared_buffers=32MB and max_connections=40. The task-created workflow role owns its separate database. Passwords were generated locally and supplied through private files/environment, never committed. [Reusable setup and runner instructions](../../tools/validation/README.md) describe the required variables and supported database isolation.

MAVEN_OPTS: `-Xms16m -Xmx256m -XX:+UseSerialGC -XX:ReservedCodeCacheSize=64m`. JAVA_TOOL_OPTIONS: `-Xms16m -Xmx384m -XX:+UseSerialGC -XX:ReservedCodeCacheSize=64m`. These only bound runtime memory; ordinary forked Maven tests ran without exclusions or skips.

## Executed verification

| Command/check | Actual outcome |
| --- | --- |
| `mvnw.cmd --batch-mode --no-transfer-progress -Pdatabase-test -Dtest=DatabaseTestSafetyProcessorTest -Dit.test=LendingIntegrityIT,LendingQueriesIT verify` | Exit 0; 12 guard tests + 4 new integrity tests + 15 existing query IT = 31 passed, zero failures/errors/skips |
| `mvnw.cmd verify` | Exit 0; 185 unit/controller tests passed, zero failures/errors/skips |
| `mvnw.cmd -Pdatabase-test verify` | Exit 0; 362 tests passed: 185 unit/controller + 177 PostgreSQL integration, zero failures/errors/skips |
| `tools/validation/run-lending-workflow.ps1` | Exit 0 against the packaged executable JAR; 83 HTTP requests and 626 assertions passed, including independent SQL state checks |
| Runner preflight with identical app/test targets | Rejected before contacting the app; nonzero exit as expected |
| Runner preflight with nonempty workflow tables | Rejected; explicit namespace cleanup then allowed an empty-target preflight |
| Python AST / PowerShell parser checks; `git diff --check`; staged diff review | Passed; only explicit ELS-36 paths staged |

The complete suite re-executed existing borrowing (53), return (57), strict loan-date (9), lending-query (15), student/equipment persistence, shared validation and safety tests. Prior counts were not copied as evidence. The four new cases close identified gaps; no arbitrary test-count target or coverage percentage is claimed.

Sanitized [automated verification results](els-36-verification-results.json) retain per-suite counts, commands, revisions and observed race outcomes. [HTTP/SQL results](els-36-http-results.json) retain every request's method/path/status, rejected-write snapshot checks, live race outcomes and final synthetic committed rows. Raw logs/JUnit XML remain ignored under target/ to avoid committing credentials, generated build output or execution metadata.

## Acceptance mapping and independent oracles

| Acceptance / requirement | Verified evidence |
| --- | --- |
| Register/retrieve/update student; FR01, TC01-TC03/TC24 | Live exact three-field DTO, Location, stripped/lowercase values and independent stored-row equality; duplicate/invalid update snapshots unchanged; existing controller/persistence tests rerun |
| Create/list/update/delete unreferenced equipment; FR02, TC04-TC06 | Live exact six-field DTO and normalized inventory code/name/category/description; omitted replacement description becomes null; IDs ordered independently of inventory-code order; 204 has empty body and SQL target count zero |
| Borrow/duplicate/availability; FR03-FR04, TC07-TC11/TC16 | Exact Loan DTO and committed row; duplicate active borrowing gives 409 EQUIPMENT_UNAVAILABLE with all tables unchanged; both availability filters checked against independent SQL |
| Overdue boundaries; FR06, TC17 | Existing fixed-Clock query IT plus packaged app with fixed business day 2030-06-15 in Asia/Beirut: due yesterday included; due today/tomorrow excluded; returned overdue history excluded. No execution-day dependency |
| History and empty results; FR06, TC18/TC25 | Ordered arrays, student/equipment/AND filters, mismatched combination and unmatched positive int64 IDs yield exact results or []; every returned DTO compared with SQL; valid query group leaves all rows unchanged |
| Return/repeat/reborrow/history/deletion; FR05/FR07, TC12-TC15/TC19 | Same-day return gives exact committed 200; repeated return 409 retains original date; earlier/malformed/unknown/missing returns reject without writes; new borrowing has different ID and original SQL history remains; active and returned references prevent deletion |
| Strict request/ID/date contract and no partial writes; TC20/TC22/TC24/TC25 | Live unknown fields, wrong JSON types, invalid/nonpositive/overflow IDs, strict boolean/query syntax, signed/expanded/impossible dates, missing references and normalized duplicates checked with exact error shape/code/path; every rejection compares before/after snapshots of all tables |
| Rollback / commit-before-success; NFR01, TC20 | Existing insert fault and immediate/deferred-return failure tests pass unchanged; no mechanically duplicated rollback test added |
| Database bypass protection; NFR01, FR04/FR07 | New SQL tests require SQLSTATE 23505 for second active loan, 23503 for orphan inserts and deletion of referenced student/equipment, 23514 for bad date updates; rejected writes preserve rows. Returned history permits a separate active loan |
| Reproducible system execution; NFR02, TC23 slice | Built executable JAR starts over loopback, complete HTTP/SQL script passes, separate fixture JAR excluded from delivery artifact; fresh-clone installation and final deployment remain separate acceptance work |

## Coordinated concurrency

Two independent requests wait at a latch/barrier while a separate SQL connection holds the equipment row lock. An observer must see **two PostgreSQL Lock waiters** whose queries target equipment before release. Timers only bound waits/polling; no sleep selects a winner. Tests assert outcomes together with committed rows, history, availability and no orphans/extra active loans.

| Race | Full integration outcome | Final packaged HTTP outcome | SQL state |
| --- | --- | --- | --- |
| Borrow vs borrow | Existing BorrowingContractIT passes one creation/one conflict | 409, 201 | Exactly one active row; losing code EQUIPMENT_UNAVAILABLE |
| Borrow vs return | Borrow 409, return 200 | Borrow 409, return 200 | Original loan returned, no new active row; original references/dates preserved |
| Borrow vs delete | Borrow 404, delete 204 | Borrow 404, delete 204 | Equipment absent, no loan/orphan; borrower unchanged |

The tests accept the other valid serial outcomes as well: return before borrowing allows one new active loan while retaining the returned row; borrow before delete yields 201 and 409 EQUIPMENT_HAS_HISTORY. A preliminary HTTP run actually observed borrow 201 / return 200; its smaller assertion set was superseded by the final committed-source run after adding full query-value SQL comparisons. No claim is made that every possible scheduling outcome occurred in the final run.

## Failures and corrections

1. The first native startup check read an unavailable process ExitCode property as failure even though pg_ctl reported success. A focused run was started prematurely and produced 19 context errors because equipment_lending_test had not yet been created; the 12 guard unit tests passed. This was an infrastructure failure, not a product regression. Verified server readiness, completed the roles/databases, independently checked names/users/version, then reran the focused suite successfully. Raw failure log: target/els36-focused-provisioning-failure.log.
2. PowerShell treated jar.exe's harmless JAVA_TOOL_OPTIONS stderr diagnostic as a terminating native error before the HTTP workflow started. The fixture JAR now uses standard .NET ZIP APIs, contains only the test Clock class, and leaves the application artifact unchanged. The corrected packaged run passed. Raw failure log: target/els36-live-fixture-tool-failure.log.
3. Strengthened the live runner to compare every query DTO's values with SQL and confirm that the valid query group preserves state. Re-executed the required checks and final live run on committed source. No assertion was weakened and no application correction was necessary.

## Review, cleanup and limitations

Application processes are always stopped by captured task PID. The live runner intentionally retains synthetic rows for inspection; its explicit cleanup mode only deletes ELS36 inventory/email fixtures after rechecking the target. Task-owned database/credential cleanup and PR/CI observations are recorded in the delivery update.

Authentication and HTTPS are **not implemented or accepted** by this card; ELS-46 owns them. Native Windows PostgreSQL verifies the selected database engine; remote Linux/container CI must be checked on the actual PR head. Human review, merge, final deployment, performance measurements, coverage metrics and actual human effort remain unclaimed. No Done transition, self-approval or self-merge is authorized.
