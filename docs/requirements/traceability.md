# Requirements Implementation and Test Traceability

Requirement IDs are stable across proposal, detailed requirements and tests. Targets below are planned elements, not claims that classes/tests exist. Fill revision and result links when actual work runs.

| Requirement | Story | Jira | Planned implementation | Planned scenarios | Actual result and evidence |
| --- | --- | --- | --- | --- | --- |
| FR01 | US01 | ELS-29 | StudentController, StudentService, StudentRepository | TC01â€“03, TC24 | Not executed |
| FR02 | US02 | ELS-26 | EquipmentController, EquipmentService, EquipmentRepository | TC04â€“06, TC24 | Implemented in local commits `953318d`, `ae7a57c`; automated execution blocked locally because Java 17 is unavailable; PostgreSQL checks await CI |
| FR03 | US03 | ELS-33 | LoanController, LoanService, LoanRepository | TC07â€“10 | Not executed |
| FR04 | US03 | ELS-33 | LoanService transaction, equipment lock, active-loan unique index | TC11 | Not executed |
| FR05 | US04 | ELS-34 | LoanService return transaction | TC12â€“15 | Not executed |
| FR06 | US05 | ELS-35 | Equipment/Loan query methods and injected Clock | TC16â€“18, TC25 | Not executed |
| FR07 | US02 | ELS-26, ELS-36 | EquipmentService delete guard and restrictive foreign keys | TC19â€“20 | ELS-26 locked delete/history guard and active/returned history integration tests implemented in local commits `953318d`, `ae7a57c`; execution awaits CI; ELS-36 concurrency work remains separate |
| NFR01 | US02â€“04 | ELS-33, ELS-36 | Migrations and transactional services on PostgreSQL | TC11, TC19â€“20 | Not executed |
| NFR02 | All | ELS-23, ELS-30, ELS-45 | Maven Wrapper, CI, packaging and runbook | TC23 | Not executed |
| NFR03 | US06 | ELS-25, ELS-46 | Staff security configuration, DTOs and sanitized error handling | TC21â€“22 | Not executed |
| NFR04 | All | ELS-22, ELS-41, ELS-42 | Layer responsibilities and reviewable changes | Design review and three refactoring records | Pending |
| NFR05 | All | ELS-24, ELS-32, ELS-39â€“43 | Test suite, audit, deliberate defect and GQM | Scenario catalog and actual tool results | Plan exists; execution pending |

Test definitions are in [test strategy](../testing/test-strategy.md). Track individual methods/output/commit IDs when implemented; a Jira Done status does not replace this evidence.
