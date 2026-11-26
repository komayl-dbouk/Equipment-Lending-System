# Sprint 2 Audit and Closeout

Jira: ELS-38. Owner: Hussein Chreif. The project lead waived the separate Mostafa Younes review for this closeout.

## Outcome

The lending increment is technically verified on merged main `b5e6a32ac0485e01cd3dee198785c1cdb45e636b`. Borrowing, returns, availability, overdue loans, filtered history and PostgreSQL integrity meet the Sprint 2 implementation criteria. No blocking product regression was found in this audit. ELS-32 through ELS-37 are Done; ELS-38 records the technical audit and administrative closeout. The effort limitations below remain explicit; no additional peer approval is required for this card.

The results describe automated and source-based validation. A four-person meeting, attendance, actual person-hours and team consensus are not established by automated execution. Actual hours are not recorded. The retrospective records technical observations from inspected evidence.

## Merged work and verified peer decisions

| Card | Deliverable | PR and final reviewed head | Recorded human approval |
| --- | --- | --- | --- |
| ELS-32 | Borrowing tests committed and run red before production implementation | [PR 8](https://github.com/komayl-dbouk/Equipment-Lending-System/pull/8), `b9718fa` | Hussein Chalhoub |
| ELS-33 | Atomic borrowing and strict date correction | [PR 9](https://github.com/komayl-dbouk/Equipment-Lending-System/pull/9), `a0cc76c` | Hussein Chreif |
| ELS-34 | Return once, restore availability and preserve history | [PR 10](https://github.com/komayl-dbouk/Equipment-Lending-System/pull/10), `9544a16` | Komayl Dbouk |
| ELS-35 | Availability, overdue and filtered history queries | [PR 11](https://github.com/komayl-dbouk/Equipment-Lending-System/pull/11), `6fdf52b` | Mostafa Younes |
| ELS-36 | Integrated HTTP/SQL workflow and cross-feature integrity tests | [PR 12](https://github.com/komayl-dbouk/Equipment-Lending-System/pull/12), `05317d1` | Komayl Dbouk |
| ELS-37 | API examples, OpenAPI, diagrams and traceability | [PR 13](https://github.com/komayl-dbouk/Equipment-Lending-System/pull/13), `96599b2` | Hussein Chalhoub |

GitHub review records were checked against the final feature heads. ELS-32 reached main through the green ELS-33 branch; it was not merged as a failing standalone main increment. Both `a847e09` (red tests) and `b9718fa` (red evidence) remain ancestors of main. The original BorrowingContractIT file is unchanged from its red-test commit. [Red evidence](els-32-test-first-evidence.md), [borrowing implementation/correction record](../../ai-log/els-33-atomic-borrowing.md) and [date correction results](els-33-date-validation-results.json) retain the sequence.

## Merged-main execution

Environment: Java 17.0.12, Maven Wrapper 3.9.11, Spring Boot 3.5.16 and PostgreSQL 17.5. A disposable Docker instance used separate test/workflow databases and generated credentials. No existing .env or another developer's data was changed.

| Executed check | Observed result |
| --- | --- |
| `mvnw.cmd -o --batch-mode --no-transfer-progress verify` | Exit 0; 185 unit/controller tests, zero failures/errors/skips |
| `mvnw.cmd -o --batch-mode --no-transfer-progress -Pdatabase-test verify` | Exit 0; 185 unit/controller plus 177 PostgreSQL integration tests, 362 total, zero failures/errors/skips |
| `tools/validation/run-lending-workflow.ps1` with isolated target, port and output overrides | Exit 0; 83 HTTP requests and 626 assertions; independent committed-state SQL comparisons |
| GitHub Java and PostgreSQL verification on merged main | [Passed on b5e6a32](https://github.com/komayl-dbouk/Equipment-Lending-System/actions/runs/45898662557/job/140178775118) |
| Borrowing chronology and original test preservation | Both red commits remain ancestors; original borrowing test unchanged |
| Jira board configuration | To Do, In Progress, CODE REVIEW, Done; Done is rightmost and mapped to the done category |

The live workflow uses a separately loaded test-only fixed Clock; that fixture is excluded from the delivery JAR. A local batch adapter runs the container's psql for independent SQL observations. [Structured audit results](els-38-audit-results.json) record this execution; detailed local logs remain in ignored target/sprint-2-audit. Task-owned application/container resources and disposable data were cleaned up after the successful run.

## Demonstrated workflow and integrity

| Acceptance area | Observed evidence |
| --- | --- |
| Students and equipment | Synthetic registration/retrieval/update, normalization, equipment creation/list/update and unreferenced deletion |
| Borrowing | Exact response and committed row, duplicate rejection and no partial rejected writes |
| Availability and queries | Both availability filters, ordered results, AND history filters, unmatched results and unchanged query snapshots |
| Overdue boundary | Fixed-Clock past-due inclusion; due-today, future-due and returned exclusions |
| Returns and history | First return succeeds, duplicate/invalid returns reject, availability restores, reborrow inserts a distinct loan and original history is retained |
| Deletion protection | Active and returned loan history prevent deletion; no orphan loans |
| Borrow/borrow race | One 409 and one 201; two observed equipment-lock waiters; exactly one active row |
| Borrow/return race | Borrow 409 and return 200; original history retained and no new active row |
| Borrow/delete race | Borrow 404 and delete 204; equipment deleted without an orphan |
| Constraint and rollback safety | PostgreSQL suites exercise direct unique/FK/date constraints and existing immediate/deferred rollback oracles |

The cross-feature tests accept either legitimate serial outcome; the table reports the outcome observed in this run. They do not require an arbitrary thread to win. These automated demonstrations do not claim a synchronous team demonstration occurred.

ELS-37 documentation is byte-for-byte unchanged from the independently reviewed PR head. That review executed the runnable example in Windows PowerShell and again on nonempty data, validated 50 OpenAPI references and local links, rendered all ten Mermaid blocks, and passed OpenAPI lint with four documented nonblocking warnings. [ELS-37 source/evidence record](../../ai-log/els-37-api-documentation.md) and Jira comment 10081 distinguish author evidence from independent review. No unchanged diagram or lint run is relabeled as a newly executed check in this audit.

## Effort record

Original estimates remain unchanged. Actual person-hours were not recorded, so no actual total, estimate variance or productivity ratio is reported. Human effort includes prompting, inspection, correction, testing, documentation and peer review, rather than AI elapsed runtime.

| Member | Assigned Sprint 2 cards | Original estimate | Actual person-hours |
| --- | --- | --- | --- |
| Hussein Chreif | ELS-32, ELS-38 | 4h | Not recorded |
| Komayl Dbouk | ELS-33, ELS-37 | 7h | Not recorded |
| Mostafa Younes | ELS-34, ELS-36 | 6h | Not recorded |
| Hussein Chalhoub | ELS-35 | 3h | Not recorded |
| Total | Seven cards | 20h | Not recorded |

The [effort template](../../docs/testing/evidence-templates.md) supports recording real sessions in subsequent work. No estimated allowance is entered as an actual Jira worklog.

## Technical retrospective

These findings follow from inspected technical records. Meeting attendance and member-attributed feedback are not recorded.

- Clear feature boundaries and a shared API contract allowed independent work, followed by integrated validation.
- A green pipeline alone did not establish every boundary rule. Review exposed expanded date handling and prompted targeted regressions before merge.
- Real PostgreSQL and independent SQL oracles established concurrency and rollback properties that status-code checks alone would miss.
- Integration account mismatches delayed Jira/PR synchronization. Current board state should always be reconciled with verified PR/review/CI records.
- Preserving original red-test commits made the borrowing development sequence inspectable after integration.

Sprint 3 process improvement: preserve a coverage/static-analysis baseline on the common merged revision, retain tool versions/configuration/scope, then repeat the same measurements after changes. The [baseline](quality-baseline.md) has been captured before quality changes: service line coverage 95.24%, branch coverage 89.71%, nine untriaged SpotBugs warnings and zero analysis errors. Hussein Chalhoub owns measurement collection; Hussein Chreif coordinates its use in ELS-41/42 and Mostafa reviews the evidence. This action is selected under the project lead's delegated coordination authority; it does not assert a team meeting or consensus.

## Remaining work and Sprint 3 readiness

| Item | Owner / follow-up | Effect |
| --- | --- | --- |
| Human effort and meeting records | Retain the explicit limitation | Not recorded; no additional peer review required for ELS-38 by the project lead's instruction |
| Baseline measurement before quality changes | Captured for ELS-43 handoff | [Measured baseline](quality-baseline.md) retained; final ELS-43 comparison/correction remains outstanding |
| Audit at least ten generated tests and correct at least three real weaknesses | Mostafa, ELS-39 | Planned Sprint 3 quality evidence |
| Deliberate defect detection and restoration | Hussein Chalhoub, ELS-40 | Temporary experiment only; never merge the deliberate defect |
| Verified design review and at least two important corrections | Hussein Chreif, ELS-41 | Verify concerns rather than manufacturing findings to meet a count |
| Three safe refactorings | Komayl, ELS-42 after ELS-41 | Preserve behavior and retain before/after regression evidence |
| Coverage/static analysis, measured correction and GQM interpretation | Hussein Chalhoub, ELS-43 | Baseline counters are captured; measured improvement, warning triage and after-change comparison remain outstanding |
| Quality review and numerical-criteria reconciliation | Mostafa, ELS-44 | Depends on completed quality evidence |
| Staff authentication, HTTPS and final delivery acceptance | ELS-46 and release cards | Pending release requirements; not a newly discovered Sprint 2 regression |

The future quality/delivery schedules were reconciled in Jira into nonoverlapping two-working-day windows, leaving submission time after the delivery window. Each window spans two calendar workdays rather than a continuous 48 hours. Sprint 3 remains future; no feature or quality experiment has been marked complete by capturing this baseline.

## Closeout decision

Technical increment: passed. The project lead requested administrative closeout and waived Mostafa's separate ELS-38 review. Actual hours and a team meeting remain unrecorded; these limitations do not become completed measurements. The closeout changes use the normal issue branch and PR workflow. No production change was necessary for this audit.

The administrative closeout is authorized by the project lead. Repository publication uses an ELS-38 commit and PR; merging remains a human action under AGENTS.md.
