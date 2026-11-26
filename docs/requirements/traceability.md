# Requirements Implementation and Test Traceability

Requirement IDs are stable across proposal, detailed requirements and tests. Targets remain planned unless the actual-evidence column records implementation and execution. Passing local checks do not establish PostgreSQL results or human acceptance.

| Requirement | Story | Jira | Planned implementation | Planned scenarios | Actual result and evidence |
| --- | --- | --- | --- | --- | --- |
| FR01 | US01 | ELS-29 | StudentController, StudentService, StudentRepository, StudentEntity; StudentWrite/Student DTOs | TC01-TC03, student TC24; related TC20/TC22 | PR #5 merged into main at c313ca9, including Unicode whitespace correction 18f6d6c. Focused student/shared-error run: 83 tests passed. Integrated Java/PostgreSQL CI passed: [run](https://github.com/komayl-dbouk/Equipment-Lending-System/actions/runs/45511736669). [Verification evidence](../testing/els-29-student-management.md). |
| FR02 | US02 | ELS-26 | EquipmentController, EquipmentService, EquipmentRepository | TC04-TC06, TC24 | PR #4 merged at 3edce62, including normalized-blank correction 6c7c453. Focused equipment run: 33 tests passed. Integrated Java/PostgreSQL CI passed: [run](https://github.com/komayl-dbouk/Equipment-Lending-System/actions/runs/45511736669). |
| FR03 | US03 | ELS-33 | LoanController, LoanService, LoanRepository | TC07â€“10 | ELS-32 defines PostgreSQL contract tests; final red run: 53 expected borrowing assertion failures, zero errors. Existing 203 tests pass. No borrowing implementation yet. [Red evidence](../../reports/sprint-2/els-32-test-first-evidence.md); red-test commit a847e09. ELS-33 green implementation remains pending. |
| FR04 | US03 | ELS-33 | LoanService transaction, equipment lock, active-loan unique index | TC11 | ELS-32 defines PostgreSQL contract tests; final red run: 53 expected borrowing assertion failures, zero errors. Existing 203 tests pass. No borrowing implementation yet. [Red evidence](../../reports/sprint-2/els-32-test-first-evidence.md); red-test commit a847e09. ELS-33 green implementation remains pending. |
| FR05 | US04 | ELS-34 | LoanService return transaction | TC12â€“15 | Not executed |
| FR06 | US05 | ELS-35 | Equipment/Loan query methods and injected Clock | TC16â€“18, TC25 | Not executed |
| FR07 | US02 | ELS-26, ELS-36 | EquipmentService delete guard and restrictive foreign keys | TC19-TC20 | ELS-26 history guard and active/returned-history PostgreSQL tests merged in PR #4. Integrated Java/PostgreSQL CI passed: [run](https://github.com/komayl-dbouk/Equipment-Lending-System/actions/runs/45511736669). ELS-36 lending concurrency acceptance remains separate Sprint 2 work. |
| NFR01 | US02â€“04 | ELS-33, ELS-36 | Migrations and transactional services on PostgreSQL | TC11, TC19â€“20 | Not executed |
| NFR02 | All | ELS-23, ELS-30, ELS-45 | Maven Wrapper, CI, packaging and runbook | TC23 | Not executed |
| NFR03 | US06 | ELS-25, ELS-46 | Staff security configuration, DTOs and sanitized error handling | TC21â€“22 | Not executed |
| NFR04 | All | ELS-22, ELS-41, ELS-42 | Layer responsibilities and reviewable changes | Design review and three refactoring records | Pending |
| NFR05 | All | ELS-24, ELS-32, ELS-39â€“43 | Test suite, audit, deliberate defect and GQM | Scenario catalog and actual tool results | Plan exists; execution pending |

Test definitions are in [test strategy](../testing/test-strategy.md). Track individual methods/output/commit IDs when implemented; a Jira Done status does not replace this evidence.

## ELS-33 execution update

This update supersedes the pending ELS-33 cells above. FR03 and FR04 are implemented through POST /api/loans, the equipment lock, one committed service transaction and the existing V1 active-loan index. Focused BorrowingContractIT passed 53/53 cases. The complete isolated PostgreSQL 17.5 profile passed 264/264 tests (172 unit, 92 integration), with zero failures, errors or skips. Eight new LoanServiceTest cases cover commit completion and conflict classification. NFR01 is verified for the ELS-33 borrowing portion; ELS-36 remains separate. The [ELS-32 red evidence](../../reports/sprint-2/els-32-test-first-evidence.md) and [ELS-33 green evidence](../../ai-log/els-33-atomic-borrowing.md) retain the sequence.
