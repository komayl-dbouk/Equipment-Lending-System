# ELS-39 executed verification

Owner: Mostafa Younes. Reviewer: Komayl Dbouk. Baseline: pulled origin/main bdbbd3e. Tested corrected source: **cc1997bfaea87ea7e902344800b6cf65c99cb2a9**. [Approved plan](els-39-approved-plan.md), [14-method audit matrix](els-39-test-audit.md), [targeted fault probes](els-39-probe-results.json), [machine-readable verification](els-39-verification-results.json).

## Results

| Executed command / phase | Actual outcome |
| --- | --- |
| Original-source `mvnw.cmd -Pdatabase-test verify` | Exit 0; 362 passed: 185 unit/controller, 177 PostgreSQL; zero failures/errors/skips |
| Corrected-source `mvnw.cmd -o --batch-mode --no-transfer-progress -Pdatabase-test -Dtest=DatabaseTestSafetyProcessorTest -Dit.test=LendingQueriesIT,LendingIntegrityIT,LoanIndexSelectivityIT verify` | Exit 0; 33 passed: 12 guard, 21 integration; zero failures/errors/skips |
| W1 returned-history local fault, original query class | Exit 0; 15 integration cases passed despite the dropped historical row; 12 guard cases passed |
| Same W1 fault, corrected query class | Exit 1; exactly one intended assertion failure, zero errors/skips; 12 guard cases passed |
| W2 UTC-date local fault, original query class | Exit 0; 15 integration cases passed at the old midday instant; 12 guard cases passed |
| Same W2 fault, corrected boundary fixture | Exit 1; exactly one intended overdue assertion failure, zero errors/skips; 12 guard cases passed |
| W3 wrong student-key partial index, original integrity class | Exit 0; four integration cases passed; 12 guard cases passed |
| Same W3 index, supplementary selectivity class | Exit 1; both new cases failed their intended assertions, zero errors/skips; 12 guard cases passed |
| Restored committed source: `mvnw.cmd verify` | Exit 0; 185 passed, zero failures/errors/skips |
| Restored committed source: `mvnw.cmd -Pdatabase-test verify` | Exit 0; 364 passed: 185 unit/controller, 179 PostgreSQL; zero failures/errors/skips |

Expected failures from temporary audit controls are not ordinary product regressions or a global mutation score. The exact commands, temporary diffs, index definitions and failing method names are retained in the probe JSON. The original tests' survival is established only for the named focused targets, not every test in the repository. The two added cases account for the changed full-suite count; count growth itself is not the quality result. The result is detection of concrete previously missed behavior.

## Environment and isolation

Java 17.0.21.1, Maven Wrapper 3.9.11, Spring Boot 3.5.16, PostgreSQL 17.5. Docker's engine socket was unavailable; the test strategy permits an isolated native instance of the selected database. Used cached pinned binaries and a task-owned cluster at D:/codex-els39-runtime/pgdata on 127.0.0.1:15439. Effective database equipment_lending_test, user equipment_test and server version 17.5 were confirmed independently before running tests. The configured application database name differs and no application instance/data was used. The existing database safety guard passed before/during the probes.

Setup used initdb with UTF8, locale C, SCRAM authentication and a generated private password file; pg_ctl started only the task-owned instance with loopback binding, shared_buffers=32MB and max_connections=40. Credentials were provided through private files/environment, never committed or printed. .env was not created or overwritten. Normal test fixtures and synthetic reference IDs were scoped to the guarded database.

MAVEN_OPTS: `-Xms16m -Xmx256m -XX:+UseSerialGC -XX:ReservedCodeCacheSize=64m`. JAVA_TOOL_OPTIONS: `-Xms16m -Xmx384m -XX:+UseSerialGC -XX:ReservedCodeCacheSize=64m`. Ordinary forked tests and pinned enforcer checks remained enabled. No dependency, test exclusion, skip or suppression was introduced. Raw logs and JUnit reports remain ignored under target/els39*, target/surefire-reports and target/failsafe-reports; sanitized committed reports omit execution-date metadata and secrets.

## Restoration, preservation and coordination

All src/main bytes match the pre-probe hash snapshot. LoanQueryService and LoanQueryRepository were restored in finally blocks after each source control; corrected query-test bytes were reinstated. The partial unique index definition was restored exactly to equipment_id with return_date IS NULL before final normal verification. No production fault, schema migration change or dependency update is in the task diff.

BorrowingContractIT is unchanged and its original red commit remains in ancestry. Existing meaningful borrowing/return, rollback, query, equipment/student and cross-feature integrity regressions pass in the full suite. Supplementary index tests vary student/equipment references independently, verify committed rows and assert the exact structured PostgreSQL constraint. Historical evidence is retained.

Avoided ELS-41's EquipmentService, EquipmentRepository, EquipmentServiceTest and EquipmentDesignReviewIT files. ELS-40 comment 10096 distinguishes these audit-specific controls from Chalhoub's separate experiment. ELS-41 comment 10095 records overlap boundaries. The latest pre-verification fetch still showed main bdbbd3e, with ELS-41's PR separate. Any later integration/CI result is recorded in the delivery update rather than inferred.

## Interpretation and limits

Fourteen baseline methods were inspected: 11 Meaningful and three Weak. Three distinct weak oracles were corrected: complete student history, an observable Beirut date boundary and physical-equipment index selectivity. No wrong-oracle finding, human acceptance, hours, coverage improvement, statistical mutation score or performance result was invented. Human verification of the matrix and implementation remains pending review by Komayl.

The published GQM coverage/static-analysis baseline remains historical. ELS-40/41/42/43 retain their separate acceptance criteria. Authentication and HTTPS remain ELS-46. ELS-44 has not started and requires a separate user decision plus accepted/merged dependencies. No self-approval, merge or premature Done is authorized.

## PR delivery observation

Plan commit 31a63bf, corrected-test/probe commit cc1997b and evidence commit 65ad9a3 were pushed to ELS-39-generated-test-audit. GitHub's integration rejected PR creation with HTTP 403 Resource not accessible by integration. PR creation, reviewer request and remote CI therefore remain unclaimed at this observation. The [prepared author-perspective description](../../docs/reviews/els-39-generated-test-audit-pr.md) contains the exact requested title and structure. Human creation or corrected integration access is required to finish PR delivery; subsequent PR/CI/Jira observations are recorded on ELS-39. Intended reviewer: komayl-dbouk. No ELS-44 work is included.
