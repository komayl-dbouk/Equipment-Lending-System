# ELS-29 Student Management Interaction and Evidence

Owner: Mostafa Younes. Human reviewer: Komayl Dbouk. Tool: Codex; exact backend model not recorded. All student data in tests/examples is synthetic.

## Selected interactions and decisions

- The user requested only POST /api/students and GET/PUT /api/students/{id}, preservation of the merged foundation, a reviewed plan, incremental local commits, meaningful tests and evidence. Docker, container downloads, H2, automatic push/PR creation and student authentication/deletion were excluded.
- Repository inspection found a clean main branch. Fetched origin/main at c14dff1, including ELS-23 and merged ELS-25/ELS-30. Created exactly ELS-29-student-management from that revision. Only the root AGENTS.md applies. Git required a per-command safe.directory setting because the execution account differs from the repository owner; no global Git configuration was changed. Fetch and branch creation initially required access to the sandbox's read-only .git directory.
- Read AGENTS.md, README, requirements/traceability, architecture/ADRs, API contract/OpenAPI, student privacy review, shared errors, test strategy/CI, team plan, entities, V1 migration and existing tests. Proposed feature-owned DTO/controller/service/repository files, minimal StudentEntity accessors, raw-before-normalized validation, rollback-safe confirmed uniqueness handling, focused local tests and PostgreSQL integration tests for CI.
- The user replied **yes** to approve that plan before substantial implementation. This approves implementation, not human code review or final acceptance. A subsequent instruction asked the agent to resolve the JDK location independently.
- Atlassian transitioned ELS-29 from To Do to In Progress. Comment 10043 records the branch, base revision, approved scope and planned verification.

## Implementation decisions

StudentWrite validates raw maxima (name 120, email 254) and required nonblank text. The service checks raw input before trimming, lowercases email with Locale.ROOT and then validates the normalized DTO using the additional Jakarta Email group. Raw email has no default-group Email constraint, so permitted padding is accepted. The service repeats validation for non-HTTP callers. A feature-local text deserializer rejects number/boolean-to-string coercion without changing shared Jackson configuration.

The existing StudentEntity remains the persistence model. Student response records contain exactly id/name/email. PUT supplies both required editable fields and cannot change the generated ID. Controllers handle HTTP/DTOs; services normalize, validate and own transactions; repositories persist. Existing ApiExceptionHandler, ApiError and intentional exceptions provide the response contract.

V1 already creates student_email_key. No migration is required or modified. All student API writes store canonical emails; ordinary PostgreSQL uniqueness enforces uniqueness of those canonical values. This is not a claim that arbitrary direct SQL writers are forced to normalize their input. Writes use a service-owned REQUIRES_NEW TransactionTemplate so rollback finishes before conflict classification even with an ambient caller transaction. Only Hibernate constraint metadata identifying SQLSTATE 23505 and student_email_key becomes DUPLICATE_EMAIL. No SQL-message parsing, recovery query in a failed transaction, or pre-insert uniqueness lookup is used. Unrelated database failures propagate to sanitized shared 500 handling.

## Executed commands and corrections

Unless stated otherwise, Git commands used `git -c safe.directory=C:/Users/mosta/OneDrive/Desktop/genie_logiciel/Equipment-Lending-System` as their prefix. No push was executed.

| Command/action | Observed result |
| --- | --- |
| `git status --short`, branch/remote/log inspection | Clean starting main; origin is komayl-dbouk/Equipment-Lending-System. |
| `git fetch origin` | Initial sandbox failure opening FETCH_HEAD; rerun with required access exited 0. |
| `git switch --no-track -c ELS-29-student-management origin/main` | Exited 0; exact branch created at c14dff1. |
| `java -version` with the original environment | Java 8, incompatible with the project's required Java 17. |
| Search existing JDK installations and inspect their release files | Found JDK 17.0.5 bundled with PyCharm; no JDK installation or dependency upgrade needed. A separate Java probe failed native memory allocation; removed only its generated hs_err_pid31336.log. |
| `mvnw.cmd -version` using the environment below | Exited 0; Maven Wrapper 3.9.11, Java 17.0.5. |
| `mvnw.cmd --batch-mode --no-transfer-progress "-Dtest=StudentServiceTest,StudentControllerTest,ApiExceptionHandlerTest" test` (first attempt) | Compiled application/tests. Existing shared suite: 12 passed. New suites: 67 setup errors, caused by Mockito inline agent attachment failing on this local JDK. This was an environment/mock setup failure, not a business-rule red phase. |
| Same focused command after correction | Exited 0, BUILD SUCCESS: 79 tests, zero failures/errors/skips (40 controller, 27 service, 12 existing shared errors). Output: target/els-29-focused.log and target/surefire-reports. |
| `mvnw.cmd --batch-mode --no-transfer-progress verify` | Compiled all five test source files, including StudentPersistenceIT. All 79 database-free tests passed. Packaging failed resolving the build plugin dependency net.java.dev.jna:jna:5.17.0: Connection reset. Full verify did not pass. Output: target/els-29-verify.log. |
| `mvnw.cmd --batch-mode --no-transfer-progress "-Daether.connector.connectTimeout=15000" "-Daether.connector.requestTimeout=30000" verify` with the Windows certificate-store MAVEN_OPTS below | Exited 0, BUILD SUCCESS: 79 tests, zero failures/errors/skips, executable JAR repackaged. Output: target/els-29-verify-retry.log. No dependency pin changed. |
| `mvnw.cmd --batch-mode --no-transfer-progress test-compile` after refining integration fixtures | Exited 0, BUILD SUCCESS. Integration overlength inputs now become valid if trimmed, so a missing raw-length check cannot be hidden by blank validation. Output: target/els-29-test-compile.log. This does not execute database tests. |
| `git diff --check` and `git status --short` | An attempt while the build was running failed with Git initialization memory errors; after the build ended both commands exited 0 and whitespace was clean. |
| PowerShell `ConvertFrom-Json -ErrorAction Stop` on docs/api/openapi.json, plus explicit StudentWrite schema checks | Exited 0; JSON parses, name/email remain required, additionalProperties remains false. |
| Relative Markdown link check across the changed task documents | Exited 0; all relative targets resolve. |

Process environment used for wrapper commands:

```powershell
$env:JAVA_HOME = 'C:/Program Files/JetBrains/PyCharm Community Edition 2022.3.1/jbr'
$env:JAVA_TOOL_OPTIONS = '-Xms32m -Xmx256m -XX:+UseSerialGC'
# Used for the full verification retry only:
$env:MAVEN_OPTS = '-Djavax.net.ssl.trustStoreType=Windows-ROOT -Djavax.net.ssl.trustStore=NONE'
```

Correction: both test doubles are interfaces, so use Mockito's built-in PROXY mock maker explicitly for these mocks. This avoids agent attachment and needs no dependency or global Mockito configuration change. Validator cleanup now tolerates setup failure. Replaced a deprecated JSON assertion overload with JsonCompareMode.STRICT. Successful focused feedback was obtained before adding the PostgreSQL test class.

## Pending verification and review

StudentPersistenceIT has been compiled for the existing database-test profile, but has not run locally. It checks committed persistence through MVC, independent SQL state, canonical-email uniqueness, self/other-email updates, unchanged records after rejection, ambient transaction usability and coordinated competing registrations/updates. Database-free full verify passed on retry. A documentation patch initially failed its context check without applying changes; corrected the patch and applied only task documentation updates.

Incremental commits: 5315951 implements the endpoints and 67 student service/controller tests; fd26dfc adds the PostgreSQL integration tests. Explicit file lists, staged diffs and git diff --check were inspected before each commit. Documentation updates cover README, API examples, architecture, requirements/traceability, test strategy, task evidence and prepared PR text. No Subject/ content, assignment, credentials, environment file, migration, shared configuration or equipment/loan source was staged or modified.

At implementation readiness, listed the current Jira transitions and successfully used transition 51 to move ELS-29 to the existing **CODE REVIEW** status. The named human reviewer remains Komayl Dbouk; no human review, PR request or merge has occurred. [Prepared PR text](../docs/reviews/els-29-student-management-pr.md) includes the exact user-operated push command and comparison URL targeting main.

No Docker or PostgreSQL environment has been started. PostgreSQL execution, remote CI, Komayl's review, push/PR creation and reviewed merge remain pending. No passing integration result, measured coverage, actual human review or Jira Done status is claimed.

## Rebase conflict resolution and regression checks

The preceding entries describe the original implementation stage. The user subsequently supplied a successful push transcript for the original branch, fetched main at 3edce62 and requested resolution of their interrupted rebase. Inspection confirmed one conflict, docs/requirements/traceability.md, at the final documentation commit; no rebase commands remained. Kept ELS-29's FR01 implementation evidence and main's ELS-26 FR02 evidence, while preserving main's FR07 history entry. No production/test source changes were needed.

Inspected git diff --check, the explicit staged list, the resolved staged traceability diff and the staged documents compared with the original 11f940f revision. The only differences from that documentation revision were main's equipment evidence entries. Ran `git -c core.editor=true rebase --continue` successfully, retaining three separate commits:

| Original | Rebased | Purpose |
| --- | --- | --- |
| 5315951 | 6e02299 | Student implementation and database-free tests |
| fd26dfc | 8eec279 | PostgreSQL integration tests |
| 11f940f | 80e65bc | Documentation, combined traceability and original evidence |

`git range-diff c14dff1..11f940f origin/main..HEAD` confirmed both source/test commits are unchanged; only the documentation commit's context reflects the preserved ELS-26 traceability. Updated the live FR01 references to the rebased hashes, corrected README's equipment status and changed the prepared user push command to force-with-lease because the old history was already published. Original hashes above remain as historical evidence.

Post-rebase focused attempts first failed native JVM memory allocation: the original 256 MiB heap attempt failed during compilation; a reduced-memory forked attempt failed before any tests ran. Moved only these generated crash/replay reports into ignored target/ output. Retried without a separate Surefire JVM and with process-local memory limits:

```powershell
$env:JAVA_HOME = 'C:/Program Files/JetBrains/PyCharm Community Edition 2022.3.1/jbr'
$env:JAVA_TOOL_OPTIONS = '-Xms16m -Xmx128m -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -XX:ReservedCodeCacheSize=32m -XX:MaxMetaspaceSize=160m -Xss512k'
.\mvnw.cmd --batch-mode --no-transfer-progress '-DforkCount=0' '-Dtest=StudentServiceTest,StudentControllerTest,ApiExceptionHandlerTest' test
```

Observed: exit 0, BUILD SUCCESS, 79 tests with zero failures/errors/skips (27 student service, 40 student controller, 12 shared errors). Output: target/els-29-rebase-focused-inprocess.log. The prior failures are captured in target/els-29-rebase-focused.log and target/els-29-rebase-focused-retry.log; they do not establish failing business tests. No Docker or database profile was run, and no automatic push or PR action was taken.

Full integrated verification used the same one-JVM limits plus the already-cached Byte Buddy agent for main's equipment mocks; no dependency or test configuration was changed:

```powershell
$env:JAVA_TOOL_OPTIONS += ' -javaagent:C:/Users/mosta/.m2/repository/net/bytebuddy/byte-buddy-agent/1.17.8/byte-buddy-agent-1.17.8.jar'
$env:MAVEN_OPTS = '-Djavax.net.ssl.trustStoreType=Windows-ROOT -Djavax.net.ssl.trustStore=NONE'
.\mvnw.cmd --batch-mode --no-transfer-progress '-DforkCount=0' '-Daether.connector.connectTimeout=15000' '-Daether.connector.requestTimeout=30000' verify
```

Observed: exit 0, BUILD SUCCESS, 112 tests with zero failures/errors/skips (79 student/shared plus 33 equipment) and executable JAR packaging. Output: target/els-29-rebase-verify.log. PostgreSQL execution remains pending CI. No unrelated source or original assignment content was modified; crash reports remain only in ignored target/ output.
