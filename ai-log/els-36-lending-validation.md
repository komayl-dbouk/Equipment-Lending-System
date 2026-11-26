# ELS-36 validation interaction

| Field | Observed record |
| --- | --- |
| Owner / reviewer | Mostafa Younes / Komayl Dbouk |
| Tool | Codex; exact model metadata not recorded |
| Request | Complete reproducible lending workflow/integrity validation with meaningful missing tests, real packaged HTTP and PostgreSQL state checks, actual evidence and a reviewed PR. |
| Context | AGENTS.md and nested-instruction search; ELS-36 description/comment 10070; requirements, API contract, testing strategy, existing student/equipment/borrow/return/query tests, guard/configuration, migration and service lock/transaction source. |
| Plan approval | Presented inventory/gaps/files/commands/assumptions in chat. Mostafa explicitly replied Approve the ELS-36 plan before substantial additions; recorded in Jira comment 10076 and plan commit 63e1e4b. This does not constitute peer acceptance of implementation. |
| Preservation | Clean working tree; fetched origin/main 90dc0a1 and created ELS-36-lending-validation; previous branches and original borrowing red/green ancestry retained. Subject/, .env, production sources, dependencies, migrations and existing tests unchanged. |
| Coverage judgment | Reused strong same-operation race, strict-input, fixed-Clock query and rollback tests. Added only two missing cross-feature races and two direct database-bypass invariant cases. No artificial count/coverage quota or duplicate fault-injection test. |
| Implementation | Commit 32cda80 adds LendingIntegrityIT, an explicit test-only Clock fixture outside application scan, and Python/PowerShell packaged-app HTTP/SQL tools. Fixture JAR is loaded separately through PropertiesLauncher and verified absent from delivery JAR. No production fix was needed. |
| Environment | Java 17.0.21.1 / Maven 3.9.11 / Boot 3.5.16 / PostgreSQL 17.5 / Python 3.13.7. Docker engine unavailable; native pinned cluster on loopback port 15436 with separate generated roles and _test databases. Existing guard rerun and effective database/user/version checked. No credentials committed. |
| Failures and corrections | Premature focused run had 19 DB-context errors after an incorrect startup exit-code check; corrected provisioning sequence and reran. Harmless JAVA_TOOL_OPTIONS stderr broke jar.exe fixture creation under PowerShell Stop; used .NET ZIP instead. Failure logs retained under ignored target/. Neither failure was misreported as product red/green evidence. |
| Verification | Focused: 31 passed; mvnw.cmd verify: 185 passed; mvnw.cmd -Pdatabase-test verify: 362 passed (185 unit/controller + 177 PostgreSQL), zero failures/errors/skips. Final packaged run on committed source: 83 HTTP requests/626 assertions, including SQL DTO/state oracles and three observed two-waiter races. Identical/nonempty live target preflights reject as expected. |
| Concurrency lesson | Final integration/live runs observed borrow 409 / return 200 and borrow 404 / delete 204; live borrow-vs-borrow gives one 201 / one 409. A preliminary run also observed borrow 201 / return 200. Both serial orders are valid; committed state is the oracle. |
| Evidence | reports/sprint-2/els-36-lending-validation.md, els-36-verification-results.json and els-36-http-results.json. Tested source 32cda801f34abd268d16b2b26ec1e72c865db560; later evidence-only commits do not alter implementation. |
| Acceptance limits | No authentication/HTTPS claim (ELS-46), coverage/performance metric, fabricated effort, human implementation approval or merge. PR delivery/CI/cleanup recorded separately after checking actual external state. |
