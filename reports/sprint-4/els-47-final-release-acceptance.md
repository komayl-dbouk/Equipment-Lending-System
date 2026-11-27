# ELS-47 Authenticated Delivery Acceptance

Owner: Mostafa Younes. Reviewer: Komayl Dbouk (`komayl-dbouk`). [Jira ELS-47](https://equipment-lending-system.atlassian.net/browse/ELS-47).

## Preparation status

Latest origin/main was fetched and pulled before creating ELS-47-final-release-acceptance at `74da749eb0e7ec8419cc2c87384dd688578972e6`. ELS-45 and ELS-46 are Done, human-approved and merged. Exact head/merge/CI identities are retained in [structured results](els-47-acceptance-results.json). Final release acceptance is not yet claimed. Plan review was requested before substantial validation-tool changes and remains pending.

The plan uses the documented non-root Docker package and existing authenticated packaged-JAR HTTP/SQL runner, separate task-owned PostgreSQL 17.5 targets and generated private settings. Synthetic fixtures, exact DTO/state assertions, no-write snapshots, rollback checks and coordinated races are retained. No production rule, endpoint, schema or authentication change is authorized by this acceptance task.

## Scenario matrix

Every row is planned until its observed result and evidence are recorded. The structured counterpart carries source/environment, preconditions, steps, expected/observed results and status for each scenario. Test catalog IDs follow [the test strategy](../../docs/testing/test-strategy.md); AQ rows cover architecture, quality provenance and operational limits.

| Scenario | Requirements | Preconditions | Steps and expected outcome |
| --- | --- | --- | --- |
+| TC01 | FR01 | Empty synthetic student namespace | POST /api/students with padded name/case-varied email; GET returned ID; 201; exact three-field normalized DTO, Location and committed row; retrieved values match |
| TC02 | FR01 | Existing student and a valid unmatched ID | PUT self-email replacement; GET/PUT missing ID; 200 for self update; 404 STUDENT_NOT_FOUND for missing; complete state unchanged on rejection |
| TC03 | FR01,NFR01 | Two distinct students and SQL snapshot | POST/PUT invalid/blank/unknown/overlength/wrong-type values and normalized duplicate email; 400 VALIDATION_FAILED or 409 DUPLICATE_EMAIL; persisted snapshot unchanged |
| TC04 | FR02 | Empty inventory namespace | Create padded/case-varied codes; list and full replacement update; 201/200; exact six-field values, normalization, ascending IDs, omitted description null |
| TC05 | FR02,NFR01 | Existing items, unmatched ID and SQL snapshot | Invalid equipment fields/types/unknown/overlength; duplicate code; missing update/delete; 400/409/404 with exact business code and no committed change |
| TC06 | FR02 | Unreferenced equipment item | DELETE item and independently query SQL; 204 with empty body; row removed |
| TC07 | FR03,NFR01 | Existing student and available equipment | Borrow with strictly ordered valid dates; independently query SQL; 201 after commit; exact six-field loan with null returnDate and exact persisted row |
| TC08 | FR03,NFR01 | Available item, known student and unmatched ID | Borrow with missing student or equipment; 404 appropriate reference code; all rows unchanged |
| TC09 | FR03,NFR01 | Known references and SQL snapshot | Borrow equal/reversed dates, impossible/expanded/signed/non-string dates, invalid IDs/unknown fields; 400 INVALID_LOAN_DATES or VALIDATION_FAILED; no partial write |
| TC10 | FR03,NFR01 | Equipment already has an active loan | Borrow the unavailable item; 409 EQUIPMENT_UNAVAILABLE; original loan and all rows preserved |
| TC11 | FR04,NFR01 | Available equipment; two request threads and independent blocking connection | Coordinate competing borrow HTTP requests; observe both equipment lock waiters, then release; One 201 and one 409; exact winner row, one active loan, zero orphan or duplicate-active groups |
| TC12 | FR05,NFR01 | Active loan and committed pre-return row | Return on/after borrowing date; compare DTO and SQL; 200 exact updated Loan; only returnDate changes; history retained and derived availability restored |
| TC13 | FR05,NFR01 | Returned loan, unmatched ID and SQL snapshot | Duplicate return with a different date; return missing loan; 409 LOAN_ALREADY_RETURNED preserves original date; 404 LOAN_NOT_FOUND |
| TC14 | FR05,NFR01 | Active loan and SQL snapshot | Return earlier date; strict invalid payload/date; return same borrowing day; Earlier date 400 INVALID_RETURN_DATE; malformed 400 VALIDATION_FAILED; same-day return permitted |
| TC15 | FR05,NFR01 | Completed loan and available item | Borrow again and query combined history; Distinct new loan; exact old returned row preserved; ordered history contains both |
| TC16 | FR06 | Unloaned, active-loan and returned items | Read all/available=true/available=false and compare independent SQL; Exact sorted DTO arrays; active only in false, unloaned/returned in true; reads preserve state |
| TC17 | FR06 | Controlled Beirut clock; yesterday/today/tomorrow and returned loans | GET /api/loans/overdue; examine date boundary regressions; Only unreturned loans due strictly before Beirut-local day; due-today/future/returned excluded |
| TC18 | FR06 | Two students, distinct equipment and retained history | Query single and combined student/equipment filters, valid unmatched IDs; Exact ordered AND-filter results and [] for no matches; no writes |
| TC19 | FR07,NFR01 | Equipment with active and returned history | DELETE each referenced item; 409 EQUIPMENT_HAS_HISTORY; equipment, loans and students unchanged |
| TC20 | FR03,FR05,FR07,NFR01 | Disposable PostgreSQL; dedicated synthetic fixtures | Run rollback tests plus real coordinated borrow/return and borrow/delete HTTP races; Complete rollback; either valid serial winner; original history/active counts match; no orphan |
| TC21 | NFR03 | Private generated staff credential and pre-request SQL snapshot | Missing/wrong credentials on protected reads and mutations; authorized lifecycle; public health; Exact sanitized six-field 401 and Basic challenge; no rejected writes or session cookie; public health only status |
| TC22 | NFR03 | Real conflict/invalid/authentication requests in owned application | Inspect response schema, checkpoint and production application logs for private tokens/SQL/error details; No secrets or stack trace exposed; no private header/password in retained evidence; no student email/SQL values in app logs |
| TC23 | NFR02 | Clean tracked checkout; isolated generated settings and Docker project | Build both profiles, runtime image/start/readiness/seed/rerun/drift/reset/reseed/shutdown; Recorded package/image identities, Flyway migration, non-root/loopback/internal DB, exact persisted seed counts and owned cleanup |
| TC24 | FR01,FR02,NFR01 | Normalized records and before/after snapshots | Boundary lengths/blanks/types/unknown fields; own identifier updates and cross-record conflicts; Documented normalized self updates pass; invalid or duplicate writes leave complete state unchanged |
| TC25 | FR06,NFR01 | Known active/returned inventory and no-match IDs | Unavailable filter, AND query, malformed/nonpositive/overflow filters; Exact correct result or 400 VALIDATION_FAILED; valid unmatched IDs yield [] |
| AQ01 | NFR04 | Merged design/security/package source and exact diff | Review DTO/service/transaction/repository boundaries, API/OpenAPI consistency and migrations; Existing architecture and contract retained; no rule change or alternate schema owner |
| AQ02 | NFR05 | Historical ELS-39/40/41/42/43 quality records and current regression | Reconcile audited tests, fault detection, refactorings and revision-scoped coverage against final regression; Historical 100% service-line result identified by original source; no new coverage/static-analysis measurement inferred |
| AQ03 | NFR02,NFR03 | Documented CLI/local delivery boundary | Compare verified exposure with deployment instructions and remaining transport/distribution limits; Loopback HTTP only; remote HTTPS/browser CSRF/origin and distribution notices explicitly outside verified local acceptance |

## Evidence and scope rules

Final execution requires both Maven profiles, meaningful focused tests and real authenticated package HTTP/SQL checks on integrated source. Tests must record current actual totals; historical 362/364/381/383 results remain associated with their original revisions. The new security baseline has different totals. A clean tracked checkout is identified separately from any later validation-tool commit. Record the package SHA-256, runtime image identity, effective database identity/version, tool versions, commands, failures/corrections and owned cleanup.

The existing packaged workflow uses a separately loaded test-only fixed Clock; it is not part of the delivery JAR. The ordinary Docker package is also exercised through the actual documented staff-access/seed/reset path. Public health must remain sanitized. No credential, authorization header, real student data or raw generated log belongs in committed evidence. Local HTTP cannot establish remote HTTPS, browser origin/CSRF acceptance or an audit of host/proxy/database-server logs.

Independent ELS-45 review and preparation results will be reconciled here after execution; dependency CI and historical author evidence never substitute for Mostafa's acceptance results. Pending plan approval or execution gaps keep delivery draft. Human approval, merge, Done, sprint closure and submission remain separate actions.

## Independent packaging review and preparation results

The [ELS-45 technical review](../../docs/reviews/els-45-reproducible-packaging-pr.md) records source assessment, exact human decisions, findings and independent execution. Its recommendation supports the recorded ELS-45 acceptance, with the local Docker reproduction explicitly blocked. The following are independent review/preparation results on clean integrated source, not a blanket pass for the planned ELS-47 matrix.

| Check | Actual result |
| --- | --- |
| Clean standalone Maven verify | 200 tests; zero failures/errors/skips; exit 0 |
| Clean PostgreSQL profile after documented Compose importer | 200 unit/controller + 193 integration = 393; zero failures/errors/skips; exit 0 |
| Existing delivery-helper regressions | 15 passed; exit 0; isolated Docker fakes and real loopback helper HTTP |
| Existing authenticated executable-JAR HTTP/SQL workflow | 86 requests, 651 assertions, 43 SQL evidence records, 3 coordinated races; passed |
| Actual documented Docker package smoke | Exit 1 during runtime build; no application container workflow success claimed |
| Direct runtime-build diagnosis | Exit 1: containerd metadata database write input/output error after C: reached zero free space |
| Raw JAR evidence/ignored app-log private-value scan | Generated staff/database passwords and full Authorization header absent |

Packaged JAR SHA-256 was `5fdf429cba4adf6eb1c0be95db5ff16f4ce59b327e7904c0b31795fffd389b74`. The test-only fixed Clock was explicitly loaded only for the existing JAR validation process. Effective workflow database/user/version: equipment_lending_workflow_test / equipment_workflow / 17.5; integration database/user: equipment_lending_test / equipment_test, isolated loopback port 15447. The SQL adapter used docker exec with the container's native PostgreSQL 17.5 psql; it changed transport only, preserving the existing SQL, barriers and state assertions.

Observed races: borrow/borrow 409 + 201, borrow/return 409 + 200, borrow/delete 201 + 409. Every race observed two blocked equipment-lock waiters before release. Final SQL state had no orphan loan or duplicate-active-equipment group. Results retain the runner's ELS-36 identity and current source/package provenance in the structured preparation record. No old 362/364/381/383 result is relabeled; this security-integrated source actually ran 393 tests. No new coverage/static-analysis result is claimed.

## Remaining gates and recovery

The explicit ELS-47 environment/data/scenario/evidence plan was presented and user review requested. No plan approval has been recorded yet; no substantive tracked validation tool was changed. The 28 matrix rows cover all FR01-FR07 and NFR01-NFR05, with expected outcomes prepared and observed final-acceptance outcomes still null. Existing review checks do not fill in a planned row by assumption.

After approval, close the remaining explicit HTTP cases for missing student/update/delete targets, strict payload and normalization boundaries, wrong-credential mutation/no-write behavior, date/rollback boundaries and per-scenario evidence mapping while retaining the existing coordinated-race oracles. Production defects require an issue-linked proposed correction, not a silent business-rule change here.

Docker storage health is also a blocker. C: reached zero; after removing only obsolete task-generated JARs, some space was recovered, but Docker still failed metadata writes/blob reads. An engine reset/restart or broad pruning could affect unrelated projects and was not attempted. The user was asked to restore storage health. Repeat the unchanged real Docker build and authenticated seed/reset smoke after recovery, recording the successful runtime image identity.

Cleanup is incomplete: the task-owned PostgreSQL container `els47-tests-postgres-1` acknowledged stop and is now inspected as dead, but removal failed with the same metadata I/O error. Its anonymous volume and els47-tests_default network remain identified in the JSON. The generated private cleanup environment stays ignored locally until recovery. No successful full cleanup is claimed. The JAR process exited and no unrelated resource was restarted or removed. Final acceptance, reviewable PR status, human approval, Done and submission remain pending.

The user requested a retry after freeing host space. The retry observed 3,603,779,584 free bytes on C:, rebuilt the unchanged delivery JAR with `mvnw.cmd -o --batch-mode --no-transfer-progress -DskipTests package` (exit 0; no tests rerun), and recorded the replacement digest `92808bbe425a64a28b24447d1ae225ee06bd7b3453f37ab0d7d2ca59e1f6ce58`. This is a different package build from the earlier passing JAR workflow and is not relabeled as its tested digest. Docker version/container probes still returned HTTP 500 from dockerDesktopLinuxEngine; the real package smoke and cleanup remain blocked. No Docker restart/prune or unrelated project interruption was attempted.

## Draft publication

Preparation commit `5acaa96` is pushed on ELS-47-final-release-acceptance. GitHub integration rejected draft PR creation with HTTP 403 `Resource not accessible by integration`. [Manual draft creation](https://github.com/komayl-dbouk/Equipment-Lending-System/compare/main...ELS-47-final-release-acceptance?expand=1) is available; the prepared first-person description remains in ignored target/els47-review/pr-body.md. Required reviewer is komayl-dbouk, but no successful review request is claimed. Feature-branch pushes do not trigger this repository workflow, so no latest-head PR CI pass is inferred before a PR exists. ELS-47 remains To Do for preparation; move to In Progress when approved acceptance execution actually begins, then CODE REVIEW only when reviewable.
