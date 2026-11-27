# ELS-47 Authenticated Delivery Acceptance

Owner: Mostafa Younes. Human reviewer: Komayl Dbouk (`komayl-dbouk`). [Jira](https://equipment-lending-system.atlassian.net/browse/ELS-47) / [PR #24](https://github.com/komayl-dbouk/Equipment-Lending-System/pull/24).

## Outcome and provenance

All 28 scenario records have observed technical results and direct evidence. Technical acceptance passed; Komayl review, approval, merge, sprint closure and submission remain pending. The project lead explicitly authorized completing this PR, resolving the earlier plan gate without inventing a meeting or feedback.

[The successful implementation CI](https://github.com/komayl-dbouk/Equipment-Lending-System/actions/runs/46081222820/job/140780400923) ran branch head `e11e027` through actual merge-ref checkout `21cd9a67be2a0337a6e6538995a3f0da839258e0`. Both identities are retained. Application source, migrations, POM dependencies and existing Java assertions are unchanged from `74da749`. Wrapper content is unchanged; executable permission is tracked for Linux.

The separate clean tracked local checkout ran `dbb6f24ac11836999e68000bed7d08c3969f9f26`. Local and CI packages keep distinct digests and environments; no rebuild is relabeled as an earlier tested package. The controlled Clock is in a separate test-only fixture JAR excluded from delivery. The ordinary Docker image uses the normal application clock.

| Verification | Local observed result | Clean-runner CI result |
| --- | --- | --- |
| Full regression | 200 unit/controller + 193 PostgreSQL; no failures/errors/skips | 200 + 193; no failures/errors/skips |
| Helper safety | 15 passed | 15 passed |
| Expanded packaged workflow | 227 requests / 3406 assertions / 154 SQL records / 3 races | 227 requests / 3409 assertions / 154 SQL records / 3 races |
| Production application-log privacy | Passed after real conflicts and forced database failures | Passed |
| Ordinary image delivery | Separately verified in CI; no local app-container pass claimed | Passed build, health, rejected access/no writes, authorized seed/rerun/drift/reset/reseed, non-root/loopback/internal DB and owned cleanup |

Runtime image ID: `sha256:1623bed97aae4a250eb9a057abbf5e5e4aa6c77b1fc405bd395195d322a19dde`. Docker and expanded acceptance used the same CI JAR SHA-256 `84cbab21390b46f4439469a8afd1178a91686cea8810e954f21260809df99e91` and checkout source. Local JAR SHA-256: `c3229d6edbaa3b9af8ab8dbf88de5a2b30e151df01bd1aaa102c46387d95e2f3`. CI retains both JSON outputs with test reports; [committed structured results](els-47-acceptance-results.json) preserve requests, SQL checks, JUnit methods, races and per-scenario evidence.

## Executed scenario matrix

Structured scenarios retain preconditions, steps and expected outcomes. Evidence entries resolve into the retained run or explicit review/source artifacts. Passing rows are technical observations, not human approval.

| Scenario | Requirements | Observed technical result |
| --- | --- | --- |
| TC01 | FR01 | Normalized exact creation DTO, Location, retrieval and independent committed SQL row matched. |
| TC02 | FR01 | Self-email replacement persisted; missing GET/PUT returned STUDENT_NOT_FOUND with full state unchanged. |
| TC03 | FR01, NFR01 | POST/PUT blanks, nulls, malformed/unknown/overlength/wrong-type values and duplicate emails rejected without writes; maximum valid fields accepted. |
| TC04 | FR02 | Normalized inventory creation/list/replacement and same-record code update matched committed SQL; omitted description cleared. |
| TC05 | FR02, NFR01 | POST/PUT raw field/type boundaries, duplicate codes and missing update/delete targets returned exact codes without writes; maximum valid fields accepted. |
| TC06 | FR02 | Unused equipment returned empty 204 and independent SQL confirmed removal. |
| TC07 | FR03, NFR01 | Exact six-field borrowing DTO matched the committed borrower, equipment, dates and null return. |
| TC08 | FR03, NFR01 | Missing student/equipment references returned their distinct 404 codes with all rows unchanged. |
| TC09 | FR03, NFR01 | Equal/reversed dates and strict invalid date/ID/unknown-field inputs returned exact 400 codes with all rows unchanged. |
| TC10 | FR03, NFR01 | Active duplicate borrowing returned EQUIPMENT_UNAVAILABLE and preserved the original loan and full snapshot. |
| TC11 | FR04, NFR01 | Two HTTP lock waiters observed before release; one 201 and one 409; exact single committed winner verified. |
| TC12 | FR05, NFR01 | Return changed only returnDate in exact DTO and committed row; history and derived availability preserved. |
| TC13 | FR05, NFR01 | Repeated return preserved its original date; unmatched loan returned LOAN_NOT_FOUND; rejected snapshots unchanged. |
| TC14 | FR05, NFR01 | Earlier/malformed return dates rejected without writes; same borrowing-day return accepted and persisted. |
| TC15 | FR05, NFR01 | Reborrow created a distinct loan, preserved the exact old returned row and returned both in ordered combined history. |
| TC16 | FR06 | Complete all/available/unavailable ordered DTO sets matched independent SQL across unloaned, active and returned states; reads preserved rows. |
| TC17 | FR06 | Fixed Beirut day distinguished yesterday/today/tomorrow and returned loans; complete overdue set matched independent SQL without writes. |
| TC18 | FR06 | Single and AND filters plus valid no-match IDs returned exact ordered SQL-backed sets without writes. |
| TC19 | FR07, NFR01 | Active-history and returned-history deletions returned EQUIPMENT_HAS_HISTORY with every row unchanged. |
| TC20 | FR03, FR05, FR07, NFR01 | Real AFTER INSERT/UPDATE faults returned sanitized 500 and fully rolled back borrow/return; fault functions removed. Coordinated borrow/return and borrow/delete outcomes preserved history and zero orphans. |
| TC21 | NFR03 | All 11 operations rejected missing and wrong credentials with exact six-field 401/challenge and unchanged SQL snapshots. Authorized lifecycle, public aggregate health and absence of session cookies verified. |
| TC22 | NFR03 | Error bodies, application logs and Docker checkpoints passed secret/stack/SQL-value checks after real conflicts and faults; no host/proxy/database-server log audit claimed. |
| TC23 | NFR02 | Clean tracked build/full suites and ordinary Docker build/start/authenticated seed/rerun/drift/reset/reseed/cleanup passed; source/JAR/image identities retained. |
| TC24 | FR01, FR02, NFR01 | Normalized self-identifier updates and cross-record conflicts verified; POST/PUT raw-before-trim limits, exact maxima, blanks/types/unknown fields checked with unchanged rejection snapshots. |
| TC25 | FR06, NFR01 | Unavailable selection, complete AND/no-match sets and malformed/nonpositive/overflow filters returned exact DTOs/codes without writes. |
| AQ01 | NFR04 | Technical source/contract review confirms DTO/service/transaction/repository ownership and 11 protected API operations; product/schema/dependencies/Java assertions unchanged. Human peer review remains pending. |
| AQ02 | NFR05 | Historical audit, defect restoration, three refactorings and revision-scoped quality evidence reconciled. Five service blobs match historical quality source; fresh regression passed. No new metrics inferred. |
| AQ03 | NFR02, NFR03 | Verified local loopback/internal-database boundary and remaining TLS/browser/distribution duties explicitly separated; no remote or browser acceptance claimed. |

## Commands and oracle integrity

From a clean tracked checkout with Java 17 and Docker: `python tools/validation/run-release-acceptance.py`. It runs both Maven profiles, creates isolated databases/users and generated private process credentials, then executes the packaged JAR with a controlled Beirut Clock. CI uses `--prepared-build` after its full Maven step. SQL transport uses native psql inside only the labeled PostgreSQL container, preserving the unchanged lending snapshots, independent blocking connection, thread barrier and requirement to observe both equipment-lock waiters. Only valid serial race outcomes are accepted and committed state is checked. Fault functions/triggers are removed; tmpfs avoids persistent test volumes.

The ordinary-image command is `tools/demo/verify-package.ps1 -ProjectName els-demo-ci -Port 18045 -EvidencePath target/els47-package-results.json`. It preserves every target/fixture/checkpoint/authentication guard and records source/JAR/image evidence only after the guarded workflow and owned cleanup succeed. Raw logs stay ignored; private values and Authorization headers are not committed. [Runner instructions](../../tools/validation/README.md) distinguish JAR and ordinary-container scopes.

## Corrections and historical evidence

Mostafa's preparation results, failed storage/Docker attempt and distinct untested rebuilt JAR remain in `independentPreparation` and `preparationHistory`. They are not relabeled as final acceptance. New checks explicitly close missing targets, POST/PUT raw field limits, strict dates/IDs, real borrow/return rollback, complete SQL-backed lists and both missing/wrong credentials on all 11 operations.

The first new CI run failed the clean-checkout guard because chmod changed untracked wrapper executable metadata. Tracking the executable bit fixed this without weakening the guard. The second failed provisioning with a connection exit 2 after local-socket readiness. A temporary initialization socket can satisfy that probe; requiring the final TCP listener removes the startup ambiguity without DDL retry or relaxed assertions. Empty event ranges now use null. A duplicated task-local checkout path and UTF-8 log download were corrected; neither was counted as a successful validation run. Failed runs remain separate from the successful linked run.

Historical test audit, deliberate-defect restoration, refactoring and quality measurements keep their original revisions/counts. All five current service Git blobs match historical quality source `7fd7233`, which recorded 144/144 service lines and 37/38 branches. No new coverage/static-analysis/performance/vulnerability measurement is claimed. The technical source review checks architecture and all 11 protected operations; Komayl's human peer decision is still required.

## Cleanup, limits and handoff

Final local and CI acceptance stopped only their own Java processes and removed their labeled tmpfs PostgreSQL containers. Ordinary Docker completed owned Compose down -v, checkpoint and private-configuration cleanup. Original failed-run container/volume/network IDs are absent on the current engine. The original run did not retain an engine ID: this is not proof of cleanup on another workstation, where the original owner's recovery remains an operational handoff. No Docker restart/reset/prune or unrelated-project interruption occurred. The historical environment failure is not a demonstrated application defect and was not reused as the final environment.

Acceptance covers the backend over loopback HTTP. Remote HTTPS, browser authentication/CSRF/origin acceptance and distribution notices remain separate duties; no host/proxy/database-server log audit is claimed. ELS-48 consolidates the report, ELS-49 rehearses presentation and ELS-50 closes release/submission. No hours, meeting, feedback, human approval, Done or submission receipt is invented. Live PR/Jira records track the evidence-only head and its CI separately from the canonical implementation run.

Final evidence checks passed: 28 unique observed scenarios, every evidence reference, local/CI request-count arithmetic and event ranges, three coordinated-race waiter records, matching CI source/JAR identities, canonical Git-blob source hashes, unchanged application/test/dependency content and 64 edited-document local links. `git diff --check` passed before staging.
