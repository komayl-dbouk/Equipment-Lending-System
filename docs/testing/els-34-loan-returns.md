# ELS-34 return verification evidence

Owner: Mostafa Younes. Reviewer: Komayl Dbouk. [Reviewed implementation plan](../specifications/return-agent-plan.md).

## Baseline and red phase

Started branch ELS-34-loan-returns from fetched origin/main 6cf5e8f, retaining the previous local ELS-29 branch. The original ELS-32 red-test history remains an ancestor. No borrowing handler or existing test was changed.

Before production implementation, LoanReturnContractIT compiled and executed against the unimplemented endpoint: **57 tests, 57 expected assertion failures, zero errors or skips**. Existing LoanServiceTest passed **8/8** in the same run. Return requests received the existing route-not-found response instead of the required contract; lock-wait assertions also failed because no return handler acquired locks. Maven exited 1 as expected.

Command: `mvnw.cmd --batch-mode --no-transfer-progress -Pdatabase-test -Dtest=LoanServiceTest -Dit.test=LoanReturnContractIT -Dlogging.level.root=ERROR verify`.

Java 17.0.21.1, Maven Wrapper 3.9.11, PostgreSQL 17.5. The guarded isolated database equipment_lending_test runs on 127.0.0.1:15434 with generated temporary credentials. Docker could not start on this host; C: had about 190 MB free and JVMs hit paging-file exhaustion. Provisioned portable Java/PostgreSQL under a task-specific D: directory, then stopped task-started Docker. No application database was targeted. JVM environment settings: MAVEN_OPTS `-Xms16m -Xmx192m -XX:+UseSerialGC -XX:ReservedCodeCacheSize=32m`; JAVA_TOOL_OPTIONS `-Xms16m -Xmx256m -XX:+UseSerialGC -XX:ReservedCodeCacheSize=32m`.

Earlier attempts were infrastructure failures, not feature results: forked JVM startup failed from paging exhaustion; an in-process retry had Mockito initialization failures; an offline profile run lacked the pinned Failsafe 3.5.4 plugin. The bounded-memory forked online run above resolved those issues. Raw logs stay ignored under target/els34-*. Sanitized results are in [red results](../../reports/sprint-2/els-34-red-results.json).

## Green phase

Focused command: `mvnw.cmd -o --batch-mode --no-transfer-progress -Pdatabase-test -Dtest=LoanServiceTest -Dit.test=LoanReturnContractIT -Dlogging.level.root=ERROR verify`. Exit 0: **57/57 return integration tests and 8/8 unchanged borrowing service tests**, zero failures/errors/skips.

Required full command: `mvnw.cmd -Pdatabase-test verify`. Exit 0: **330/330 tests**, comprising **172 unit/controller** and **158 PostgreSQL integration**, zero failures/errors/skips. This includes the unchanged 53 BorrowingContractIT cases and nine LoanDateValidationIT regressions. No enforcer override, test exclusion or test-source change was needed for the green phase. Both runs use the isolated database and bounded-memory JVM environment above.

| Observable requirement | Independent evidence |
| --- | --- |
| Exact six-field committed 200; equal/later dates accepted | Six successful cases: same day, following day, due date, after due date, leap day and year 9999; JDBC row comparison changes only return_date and preserves student/equipment; equipment endpoint reports available |
| Earlier date / missing loan | Business 400 INVALID_RETURN_DATE and 404 LOAN_NOT_FOUND, no writes; date order precedes returned-state conflict as specified by shared validation order |
| Already returned | Both equal-to-borrowing and later second dates yield 409 LOAN_ALREADY_RETURNED and preserve the original stored date |
| Strict required body / IDs / dates | 21 incorrect date/type cases, 13 missing/malformed/unknown-field bodies and seven malformed/nonpositive/overflow path IDs yield 400 VALIDATION_FAILED without writes; signed/expanded years rejected before persistence |
| Atomic concurrent returns | Hold equipment lock on an independent connection, observe two actual PostgreSQL lock waiters, release it; exactly one 200 and one 409, one historical row, stored date matches winner |
| Equipment-first order and fresh state | While a request waits for equipment, update its loan with a two-second lock timeout on the blocker connection; succeeds, then waiting request returns 409 preserving the newly committed date |
| Flush and commit rollback | Separate immediate and DEFERRABLE INITIALLY DEFERRED test-only triggers prove attempted update via nontransactional sequence; both yield sanitized 500, original active row and availability retained; dropping trigger allows successful retry |
| Borrow-return-reborrow and history | Borrow through endpoint, return through new endpoint, verify available, history prevents deletion, borrow again into new row, original row retained; retrying old return preserves new active loan |

Sanitized suite counts, source hashes and commands are in [green results](../../reports/sprint-2/els-34-green-results.json). Raw logs/JUnit reports remain ignored under target/. No coverage percentage, live deployment or human implementation approval is inferred.

## Review and integration boundaries

Borrowing controller/service and all pre-existing tests remain unchanged. Separate return controller/service and DTO isolate the endpoint; shared persistence additions are explicit scalar/locking queries, mapped relationship accessors and return-date mutation. No migration, dependency, fines, date-limit rule or unrelated endpoint was added.

Fresh origin fetch after focused verification still reported main at 6cf5e8f; no open ELS-35 PR was returned. Main integration and CI are checked again during delivery. Subject/ is neither tracked nor ignored; no subject content was read/copied/modified. Credentials and execution dates are absent from document names/metadata. ELS-46 authentication, ELS-35 queries, ELS-36 broader acceptance and human peer review remain separate work.

## Delivery observation

Branch pushed with red commit cc53560 and implementation commit 756a435. Final origin fetch still shows main 6cf5e8f; ELS-32 red commit a847e09 is preserved in ancestry. GitHub rejected PR creation with HTTP 403 Resource not accessible by integration, so no PR/reviewer request is claimed. At implementation head GitHub returned no statuses/PR workflow runs; feature-branch push alone is outside the workflow triggers. The [prepared first-person PR](../reviews/els-34-loan-returns-pr.md) can be used after access correction. Jira comment 10066 records results and delivery blocker; the card remains In Progress pending PR/review.

Cleanup completed: task-created PostgreSQL stopped; its disposable data directory and generated password removed after checking the resolved D: path. Portable Java/PostgreSQL binaries remain cached on D:. Task-started Docker is stopped. Raw test logs/JUnit reports remain under ignored target/.
