# ELS-40 AI Interaction Record

| Field | Entry |
| --- | --- |
| Sprint or iteration and Jira key | Sprint 3, ELS-40 |
| Human owner and reviewer | Hussein Chalhoub; requested reviewer: Mostafa Younes (`younes285`) |
| Tool and model where known | Codex coding agent with Maven Wrapper, Docker Compose, PostgreSQL, Git, and Jira MCP |
| Initial instruction | Run one controlled lending/date defect experiment, retain red/green evidence, restore the application, and never commit the active defect. |
| Relevant context and requirement/specification | FR03 and BOR03 require `dueDate` strictly after `borrowingDate`; the API contract requires `400 INVALID_LOAN_DATES` with no write for equality or reversal. |
| Agent interpretation and assumptions | The equal-date boundary was selected because it is a documented behavior, has both service and PostgreSQL contract oracles, and can be changed and restored in one line without altering public scope. Local ignored environment configuration supplies isolated test credentials. |
| Proposed plan and tests | Establish green unit and PostgreSQL baselines; temporarily weaken the equality guard only; run `LoanServiceTest` and `BorrowingContractIT`; restore the guard; compare the source; rerun focused and full checks. |
| Files proposed for modification | Temporary only: `LoanService.java`. Final evidence: this record, the ELS-40 report, and the GQM result row. |
| Commands proposed | `mvnw.cmd verify`; `mvnw.cmd -Pdatabase-test verify`; focused Surefire and Failsafe commands; source diff check. |
| Human review decision before substantial implementation | Hussein Chalhoub approved the scoped ELS-40 plan in this chat. |
| Changes actually made | A local uncommitted guard change allowed equal dates; the exact original guard was restored. No production source, migration, dependency, or test behavior remains changed by the experiment. |
| Commands/tests actually executed and results | Baseline and final `verify` passed 185 tests. Baseline and final database-profile verification passed 185 unit/controller plus 177 PostgreSQL integration tests with zero failures, errors, or skips. With the defect, `LoanServiceTest` failed 1/8 and `BorrowingContractIT` failed 1/53 at the equal-date boundary. Restored focused checks passed 8/8 and 53/53. See the ELS-40 report for exact commands and assertions. |
| Problems identified by human review | None recorded after plan approval. |
| Corrections requested and made | The active defect was restored before evidence creation. Two focused Failsafe invocation mistakes were corrected and explicitly documented; neither was treated as test evidence. |
| Final accepted, modified or rejected decision and reason | Pending human PR review. The experiment evidence is prepared; no approval, merge, CI result, or final acceptance is claimed here. |
| Evidence links and lesson learned | [ELS-40 experiment report](../reports/sprint-3/els-40-deliberate-defect-experiment.md); equal-date validation must remain at the service boundary even though the database constraint prevents invalid persistence. |
