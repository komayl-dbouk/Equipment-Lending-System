# ELS-51 Validation Hardening

Owner: Hussein Chreif. Branch: ELS-51-validation-hardening. Baseline: main ea3d4bb. The user requested fixes for all three end-to-end findings on a separate branch and one Jira card assigned to themselves. This instruction authorized the implementation; no separate team approval or meeting is inferred.

## Plan and implementation

Provision an independent test service and reject unsafe test targets before Spring creates a datasource or runs Flyway. Preserve existing schema migrations. Reuse strict text deserialization across student/equipment DTOs, reject embedded NUL through DTO validation, and test safe 400 responses without repository interactions or partial persisted updates. Synchronize the runbook, environment template, agent commands, requirements and OpenAPI.

The guard is registered through test-only META-INF/spring.factories. It requires an explicit PostgreSQL database with a _test suffix and a name distinct from ELS_DB_URL, using the driver's effective database name. It does not ship in the executable JAR. compose.test.yaml, separate credentials and a distinct Compose project keep the documented application and test databases apart.

## Executed validation and corrections

Focused controller/safety regression run passed 103 tests. A clean source snapshot then passed the complete suite. An additional URL-override guard case exposed an incorrect test assumption about encoded query keys; corrected the oracle using PostgreSQL's actual parsing behavior. Final verify passed 164 unit/controller tests and 39 PostgreSQL tests, zero failures/errors/skips, using Java 17, Maven Wrapper 3.9.11 and PostgreSQL 17.5 without skipping the enforcer.

Ran the destructive suite against a disposable application-database target deliberately: guard startup failed as expected, and its protected marker remained present (1 before and 1 after). The full safe-target suite was rerun to retain a green final result. Verified that guard classes and test registration are excluded from the packaged application.

The real packaged application passed the original 220-request audit with 1,259 assertions, the saved Postman workflow with 40 assertions/15 requests and exit 0, and all 22 isolated probes. Postman cloud publication was not performed; its credential notice does not negate the local passing result. Database tests also verify rejected requests preserve stored fields and row counts.

Commands included mvnw.cmd -o clean -Pdatabase-test verify against a fresh source snapshot, mvnw.cmd -o -Pdatabase-test verify after the added guard cases, the intentionally unsafe EquipmentPersistenceIT execution, local HTTP/database checks, Postman collection run with --no-report-events, and git diff --check. Disposable services, credentials and diagnostic output stay outside tracked configuration. Reports retain baseline failures separately from successful correction results.

## Evidence and workflow

See reports/sprint-1/full-validation.md, full-validation-results.json (baseline), els-51-validation-results.json (correction) and full-validation.postman_collection.json. Raw logs remain under ignored target/els-51. The Jira card was created as High priority, assigned to Hussein Chreif and moved to In Progress when work began. Sprint 1 was not reopened and Sprint 2 was not started. A separate issue-key commit and PR preserve review before merge; no peer approval is invented. Subject remains untracked and unignored.
