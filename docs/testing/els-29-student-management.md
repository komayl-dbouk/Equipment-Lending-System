# ELS-29 Student Management Verification

Owner: Mostafa Younes. Human reviewer: Komayl Dbouk. Branch: ELS-29-student-management. Base: fetched origin/main at c14dff1. Scope: FR01, TC01-TC03, student TC24 and related rollback/privacy checks. No human acceptance or PostgreSQL result is inferred from local unit/controller checks.

## Implementation and test mapping

Source lives under `src/main/java/edu/university/equipmentlending/student`. Test classes live under `src/test/java/edu/university/equipmentlending/student`.

| Requirement/scenario | Concrete tests | Oracle |
| --- | --- | --- |
| FR01 / TC01 registration | StudentServiceTest.registrationTrimsNameNormalizesEmailAndCommitsBeforeReturning; StudentControllerTest.registrationReturnsCreatedLocationAndExactlyTheNormalizedDto | Literal trimmed name/lowercase email; generated ID; 201 and exact relative Location; exactly three response fields; commit before return. |
| FR01 / TC02 retrieval/replacement | Service: retrievalReturnsOnlyTheStudentDtoInAReadOnlyTransaction, updateReplacesBothEditableFieldsAndPreservesGeneratedId, missingStudentReturnsTheDocumentedFailureWithoutWriting. Controller: getReturnsExactlyTheStoredDto, putReplacesBothFieldsAndAllowsOwnEmail, missingGetAndPutReturn404WithoutSaving | Stored DTO; both editable fields replaced; ID unchanged; 200/404 and STUDENT_NOT_FOUND; no save on missing target. |
| TC03 / TC24 validation | Service: rejectedInputDoesNotStartATransactionOrTouchPersistence. Controller: invalidPostAndPutBodiesReturnSafe400WithoutPersistence, exactRawLengthBoundariesAreAccepted, invalidIdsAreRejectedForGetAndPutBeforePersistence | Raw 120/254 limits and overlength inputs that would become valid after trimming; omitted/null/blank/malformed fields; strict string types; unknown id/loans/extra; malformed/nonpositive/overflow IDs; 400 VALIDATION_FAILED with no repository/transaction interaction. |
| TC24 normalization and own email | Service: emailNormalizationUsesRootLocale, sameStudentCanKeepItsOwnNormalizedEmail. Controller: registrationReturnsCreatedLocationAndExactlyTheNormalizedDto, putReplacesBothFieldsAndAllowsOwnEmail | Padded email accepted; Locale.ROOT verified while the default locale is Turkish; own email retained successfully. No domain allowlist. |
| TC03 / TC20 conflicts and rollback | Service: confirmedEmailUniquenessFailureRollsBackBeforeBecomingAConflict, unrelatedPersistenceFailuresKeepTheirClassification, commitFailureCannotReturnASuccessfulStudent. Controller: duplicateRegistrationOrAnotherStudentsEmailReturns409, unrelatedDatabaseFailureReturnsSanitized500 | Confirmed 23505/student_email_key becomes 409 DUPLICATE_EMAIL after rollback; unrelated/missing metadata is not a duplicate; commit failure cannot produce success; no recovery query. |
| TC22 response privacy | StudentControllerTest.assertError (used by all negative cases), existing ApiExceptionHandlerTest | Exact six-field error shape, UTC timestamp, status/code/path, safe messages, fieldErrors map; no rejected email, private details or stackTrace. Student success payloads have exactly id/name/email. |
| PostgreSQL persistence / TC01-TC03, TC24 | StudentPersistenceIT.registerRetrieveAndReplacePersistTheExpectedValuesAndStableId, updatingWithOwnPaddedCaseVariedEmailSucceeds, duplicateNormalizedRegistrationReturns409AndPreservesOriginalRow, postgresqlConstraintRejectsDuplicateCanonicalEmailFromAnIndependentWriter | Spring MVC through real repositories, migrated schema and transactions; literal expected values checked by independent JdbcTemplate reads; positive ID and canonical email; unique constraint rejects independent duplicate SQL writer. **Compiled; execution pending CI.** |
| PostgreSQL rejection / TC20 | StudentPersistenceIT.anotherStudentsEmailRollsBackTheEntireReplacement, rejectedBodiesLeavePersistedRowsUnchanged, missingAndInvalidIdsDoNotCreateOrAlterStudents, aDuplicateWriteDoesNotAbortAnExistingCallerTransaction | Both original records and counts remain intact; invalid replacement cannot change name/email; caller transaction remains usable after a rejected independent write. **Compiled; execution pending CI.** |
| Competing student writes | StudentPersistenceIT.competingRegistrationsYieldOneCreatedOneConflictAndOneCanonicalRow, competingUpdatesPreserveTheLosingStudentsOriginalDetails | Two coordinated requests on separate threads; one 201/one 409 for create, one 200/one 409 for update; exactly one canonical email and unchanged losing record. **Compiled; execution pending CI.** |

Integration fixtures use only the dedicated els29.example.test domain and clean only those synthetic student rows. Tests do not wrap HTTP operations in a test-wide rollback transaction. Coordinated request starts exercise independent service transactions; they are student uniqueness checks, not the separate lending-concurrency acceptance criterion.

## Local environment and observed execution

Windows PowerShell; Maven Wrapper 3.9.11; existing JetBrains JDK 17.0.5 bundled with PyCharm. Project dependencies and wrapper pins are unchanged. The machine's original JAVA_HOME pointed to Java 8. Commands below use an installed compatible JDK and modest memory settings; adjust only the JDK path on another machine.

```powershell
$env:JAVA_HOME = 'C:/Program Files/JetBrains/PyCharm Community Edition 2022.3.1/jbr'
$env:JAVA_TOOL_OPTIONS = '-Xms32m -Xmx256m -XX:+UseSerialGC'
.\mvnw.cmd -version
.\mvnw.cmd --batch-mode --no-transfer-progress '-Dtest=StudentServiceTest,StudentControllerTest,ApiExceptionHandlerTest' test
```

Observed: wrapper reports Maven 3.9.11/Java 17.0.5. The corrected focused run exits 0 with BUILD SUCCESS: **79 tests, zero failures/errors/skips** (27 service, 40 controller, 12 shared errors). The first attempt compiled but had 67 Mockito setup errors; interface mocks now explicitly use PROXY mode, avoiding this JDK's agent-attachment failure. Existing shared-error tests passed in both attempts. This is not borrowing TDD red/green evidence.

```powershell
.\mvnw.cmd --batch-mode --no-transfer-progress verify
```

The initial full verification compiled StudentPersistenceIT and passed all 79 database-free tests, then failed during Spring Boot packaging because the connection reset while downloading the pinned plugin's JNA dependency. Retried with process-local certificate-store settings and bounded network timeouts:

```powershell
$env:MAVEN_OPTS = '-Djavax.net.ssl.trustStoreType=Windows-ROOT -Djavax.net.ssl.trustStore=NONE'
.\mvnw.cmd --batch-mode --no-transfer-progress '-Daether.connector.connectTimeout=15000' '-Daether.connector.requestTimeout=30000' verify
```

Observed: exit 0, BUILD SUCCESS; all 79 tests pass and executable JAR repackaging succeeds. After strengthening integration overlength fixtures so trimming would make them valid, `mvnw.cmd --batch-mode --no-transfer-progress test-compile` also exits 0. This command compiles tests and does not execute PostgreSQL checks.

Local outputs are in ignored target/els-29-focused.log, target/els-29-verify.log, target/els-29-verify-retry.log, target/els-29-test-compile.log and target/surefire-reports. They are workspace output, not uploaded CI artifacts. The [AI log](../../ai-log/els-29-student-management.md) records setup failures, corrections and Git/Jira evidence. Whitespace/staged-file checks accompany each incremental commit.

## Required remote checks and review

The existing [Build and test workflow](../../.github/workflows/build-test.yml) runs `./mvnw --batch-mode --no-transfer-progress -Pdatabase-test verify` against PostgreSQL 17.5 on a GitHub runner. That must execute StudentPersistenceIT and DatabaseSetupIT and retain Surefire/Failsafe reports. No Docker, container image, local PostgreSQL process or H2 was used for this task. The database-test command and application startup were not executed locally under the user's resource limits.

PostgreSQL execution, database-runtime/log review, remote CI, Komayl Dbouk's review and reviewed merge remain pending. SQLSTATE/constraint translation and actual rollback/concurrent-write behavior require the real database run. Canonical uniqueness assumes writers normalize email as this API does; V1 does not enforce normalization on arbitrary direct SQL. No full database verification, coverage percentage or human approval is claimed. Rerun affected checks after review changes or integration of main.

## Verification after integrating main

The user published the original branch and rebased it onto origin/main at 3edce62, which includes ELS-26. Resolved the final documentation commit's traceability conflict by keeping both FR01 student evidence and main's FR02 equipment evidence; main's FR07 history evidence also remains intact. The three ELS-29 commits remain separate: 6e02299 (implementation), 8eec279 (PostgreSQL tests), 80e65bc (documentation). Git range-diff confirms no student production/test changes from the original commits.

Two focused attempts failed JVM native memory allocation before tests ran. Retried with one JVM and process-local limits, without changing project configuration:

```powershell
$env:JAVA_HOME = 'C:/Program Files/JetBrains/PyCharm Community Edition 2022.3.1/jbr'
$env:JAVA_TOOL_OPTIONS = '-Xms16m -Xmx128m -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -XX:ReservedCodeCacheSize=32m -XX:MaxMetaspaceSize=160m -Xss512k'
.\mvnw.cmd --batch-mode --no-transfer-progress '-DforkCount=0' '-Dtest=StudentServiceTest,StudentControllerTest,ApiExceptionHandlerTest' test
```

Observed: exit 0, BUILD SUCCESS, all 79 tests pass with zero failures/errors/skips. For the full integrated check, supplied the already-cached Byte Buddy agent explicitly so main's equipment mocks can run without dynamic agent attachment:

```powershell
$env:JAVA_TOOL_OPTIONS += ' -javaagent:C:/Users/mosta/.m2/repository/net/bytebuddy/byte-buddy-agent/1.17.8/byte-buddy-agent-1.17.8.jar'
$env:MAVEN_OPTS = '-Djavax.net.ssl.trustStoreType=Windows-ROOT -Djavax.net.ssl.trustStore=NONE'
.\mvnw.cmd --batch-mode --no-transfer-progress '-DforkCount=0' '-Daether.connector.connectTimeout=15000' '-Daether.connector.requestTimeout=30000' verify
```

Observed: exit 0, BUILD SUCCESS, **112 tests** with zero failures/errors/skips, including student, equipment and shared errors; executable JAR packaging succeeds. PostgreSQL tests remain compiled but unexecuted locally. The flags change neither application behavior nor committed build/test configuration, and no tests were skipped. Outputs: target/els-29-rebase-focused-inprocess.log and target/els-29-rebase-verify.log. The [AI log](../../ai-log/els-29-student-management.md) preserves the failed attempts and resolution commands.
