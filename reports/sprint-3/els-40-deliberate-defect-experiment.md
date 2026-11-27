# ELS-40 Deliberate Defect Detection and Restoration

## Scope and requirement

This experiment was performed on `bdbbd3e2ab5a31bc12b879665a59380e0e7075d7`, the ELS-40 branch baseline. It exercises FR03 and BOR03: a loan due date must be strictly later than its borrowing date. The API contract requires equal or earlier dates to return `400 INVALID_LOAN_DATES` without a write.

The experiment was approved by Hussein Chalhoub before the temporary change. The temporary change was made only in the local ELS-40 worktree, was never committed or pushed, and was restored before this evidence was created.

## Green baseline

Java 17 and the isolated PostgreSQL 17.5 `els-tests` container were used. Both required commands succeeded on the baseline:

```powershell
.\mvnw.cmd verify
.\mvnw.cmd -Pdatabase-test verify
```

The first command passed 185 unit/controller tests. The database-profile command passed 185 unit/controller and 177 PostgreSQL integration tests, with zero failures, errors, and skips.

Expected forced database and uniqueness errors appear in normal integration-test logs because those tests verify rollback and error handling. Their test classes passed, so those log entries are not application failures.

## Temporary defect

The original guard in `LoanService.borrow` was:

```java
if (!request.dueDate().isAfter(request.borrowingDate())) {
```

The temporary local-only change was:

```java
if (request.dueDate().isBefore(request.borrowingDate())) {
```

This preserves rejection of an earlier due date but wrongly permits an equal due date. It is a genuine violation of FR03/BOR03, not a tooling warning or a compilation fault.

## Observed detection

The focused service test command was:

```powershell
.\mvnw.cmd -Dtest=LoanServiceTest test
```

It ran 8 tests and failed one behaviorally: `LoanServiceTest.invalidDateOrderIsRejectedBeforeStartingATransaction` expected `INVALID_LOAN_DATES` but received `STUDENT_NOT_FOUND`. The equal-date request passed the broken guard and reached a student lookup, demonstrating that validation no longer happened before transaction work.

The focused PostgreSQL contract command was:

```powershell
.\mvnw.cmd -Pdatabase-test test-compile '-Dit.test=BorrowingContractIT' failsafe:integration-test failsafe:verify
```

It ran 53 tests and failed one behaviorally: `BorrowingContractIT.equalOrReversedDatesAreRejectedWithoutWrites` expected HTTP 400 but received 500 for the equal-date invocation. PostgreSQL rejected the invalid row through `chk_loan_due_after_borrowing`; that database protection did not satisfy the API requirement because the client received `INTERNAL_ERROR`, not `INVALID_LOAN_DATES`.

The earlier-date parameter of the same test remained green because the temporary guard still rejects dates before the borrowing date. This was an expected non-detecting case, not an unexpected pass. No test intended to reject an equal due date passed.

Two attempted focused integration commands supplied no test evidence: an unquoted `-Dit.test` property was parsed by PowerShell as an invalid lifecycle phase, and a subsequent `-DskipTests` command skipped Failsafe as well as Surefire. The direct Failsafe command above corrected both invocation problems and produced the recorded behavioral failure.

## Restoration and final verification

The exact original guard was restored with a narrow reverse edit. The following comparison returned success before final tests:

```powershell
git diff --exit-code -- src/main/java/edu/university/equipmentlending/loan/LoanService.java
```

Focused restored checks passed:

```powershell
.\mvnw.cmd -Dtest=LoanServiceTest test
.\mvnw.cmd -Pdatabase-test test-compile '-Dit.test=BorrowingContractIT' failsafe:integration-test failsafe:verify
```

They passed 8/8 and 53/53 tests respectively. Final required verification was then repeated:

```powershell
.\mvnw.cmd verify
.\mvnw.cmd -Pdatabase-test verify
```

The final runs passed 185 unit/controller tests and 177 PostgreSQL integration tests, with zero failures, errors, or skips. The final application source contains no deliberate defect.

## Interpretation and limitations

Both a focused service test and an HTTP/PostgreSQL contract test detected the selected equality-boundary defect. The latter also shows why database constraints are defense in depth rather than a replacement for specified application validation and error mapping.

This single controlled experiment demonstrates detection for one FR03 boundary defect only. It does not establish a project-wide mutation score, coverage level, static-analysis result, or correctness of unrelated lending rules. Those measurements remain within ELS-43.
