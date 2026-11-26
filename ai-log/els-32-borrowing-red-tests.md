# ELS-32 Borrowing Red Tests

Owner: Hussein Chreif. Assigned reviewer: Hussein Chalhoub. Branch: ELS-32-borrowing-red-tests. User request: start ELS-32. Scope is test-only borrowing contract/evidence work, not production feature code.

Inspected the current specification, implementation plan, AGENTS.md, migrated schema, loan persistence, equipment locking, shared errors and guarded test database setup. Replaced obsolete missing-skeleton assumptions and committed that plan as ef2aa12 before adding tests. The user authorized this work; individual plan/test reviewer decisions remain pending.

Added BorrowingContractIT with 53 real Spring MVC/PostgreSQL cases. Oracles cover exact committed values and response shape, missing references, date order, strict types, invalid/unknown/malformed requests, active/returned history, independently coordinated competing requests and a test-only injected write failure. A sequence increment survives rollback and proves the failure injection was reached in a future green run. Cleanup owns only its prefixed fixtures and removes the trigger/function/sequence. No production code, dependency, security, API-contract or applied-migration change was made.

Docker Desktop was stopped; started it and provisioned an isolated compose.test.yaml database with disposable credentials. Focused verify compiled and produced 53 intended assertion failures, zero errors/skips. Made the trigger block terminator explicit and strengthened the injected-failure oracle before the final full profile. Final verify: 164 unit/controller and 39 existing PostgreSQL tests pass; all 53 new borrowing cases fail behavior assertions, zero test errors/skips. The missing POST endpoint gives route-not-found responses, not the required behavior; missing-reference cases fail on business code rather than treating any 404 as success.

Commands: pinned mvnw.cmd offline focused and full database-test verify, git diff --check, exact staged-file inspection and PostgreSQL fixture cleanup checks. No enforcer override, failing-test suppression or implementation stubs. The full profile exits 1 intentionally; no green build or completed borrowing capability is claimed. Observed cleanup query counts are all zero.

Red tests were committed as a847e09 after execution, before any lending code. This evidence commit retains the actual counts, result messages and plan/test/evidence chronology. See reports/sprint-2/els-32-test-first-evidence.md and els-32-red-results.json. Raw logs remain ignored. Services and credentials are removed after verification; Subject stays untracked and unignored.

Publish a draft PR for Hussein Chalhoub to review; do not merge red into main. ELS-33 must branch from the reviewed ELS-32 tip, preserve the red commit and add its own green implementation commit after Hussein Chreif's plan review. ELS-32 must not be marked Done by the agent without recorded review/acceptance. Actual person-hours were not supplied.
