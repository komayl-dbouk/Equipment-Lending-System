# Sprint 1 Review and Closeout

Jira: ELS-31. Report owner: Mostafa Younes. Assigned peer reviewer: Komayl Dbouk. Sprint: ELS Sprint 1 - Foundation and inventory. Board: ELS Scrum (34). Planned duration: two days.

## Outcome and evidence boundary

The implementation increment is complete: application foundation, shared validation/errors, CI, student registration/retrieval/replacement and equipment create/list/replacement/history-safe deletion are merged into main. The integrated revision `c313ca98bd25c3c7b1393c291cc732882dbae9f7` passed the Java and PostgreSQL verification job. ELS-29 was synchronized from CODE REVIEW to Done after its reviewed merge; all 12 Sprint 1 cards are Done.

The project lead requested sprint closure after reviewing the merged work. This report records a technical review using source, PR and automated execution evidence. A synchronous four-person demonstration, attendance, stakeholder feedback and actual person-hours were not supplied. Administrative sprint closure does not establish that those activities occurred, instructor acceptance or release readiness.

## Merged increment

| Jira | Delivered work | Merged PR | Merge revision |
| --- | --- | --- | --- |
| ELS-23 | Build, PostgreSQL/Flyway foundation and agent context | [PR #2](https://github.com/komayl-dbouk/Equipment-Lending-System/pull/2) | cc43c7b |
| ELS-25, ELS-30 | Shared validation/error contract and automated verification | [PR #3](https://github.com/komayl-dbouk/Equipment-Lending-System/pull/3) | c14dff1 |
| ELS-26 | Equipment CRUD, normalization and history protection | [PR #4](https://github.com/komayl-dbouk/Equipment-Lending-System/pull/4) | 3edce62 |
| ELS-29 | Student registration, retrieval, replacement, normalization and uniqueness | [PR #5](https://github.com/komayl-dbouk/Equipment-Lending-System/pull/5) | c313ca9 |

[Passing integrated CI](https://github.com/komayl-dbouk/Equipment-Lending-System/actions/runs/45511736669/job/138860887766) executes `./mvnw --batch-mode --no-transfer-progress -Pdatabase-test verify` with Java 17 and PostgreSQL 17.5. The workflow retains Surefire/Failsafe reports as test-reports. No additional local Docker run or download was needed for this closeout.

## Card and effort reconciliation

| Card | Current owner | Original estimate | Evidence/result | Actual person-hours |
| --- | --- | --- | --- | --- |
| ELS-20 | Hussein Chreif | 2h | Proposal/team agreement artifacts; user previously confirmed proposal step complete | Not supplied |
| ELS-21 | Mostafa Younes | 2h | Requirements, API baseline and traceability available | Not supplied |
| ELS-22 | Komayl Dbouk | 2h | Architecture, domain/database models and ADRs available | Not supplied |
| ELS-23 | Hussein Chalhoub | 3h | PR #2 merged; integrated CI passes | Not supplied |
| ELS-24 | Hussein Chreif | 2h | Testing strategy, GQM and evidence templates available | Not supplied |
| ELS-25 | Hussein Chreif | 2h | PR #3 merged; integrated shared error checks pass | Not supplied |
| ELS-26 | Komayl Dbouk | 3h | PR #4 merged with validation corrections; integrated CI passes | Not supplied |
| ELS-27 | Hussein Chreif | 2h | Borrowing specification and plan available; individual implementation approvals remain unrecorded | Not supplied |
| ELS-28 | Komayl Dbouk | 1h | Contract/privacy review artifact available; student implementation reviewed before merge | Not supplied |
| ELS-29 | Mostafa Younes | 3h | PR #5 merged with Unicode whitespace correction; synchronized to Done | Not supplied |
| ELS-30 | Hussein Chreif | 1h | PR #3 merged; Java/PostgreSQL CI runs on main and PRs | Not supplied |
| ELS-31 | Mostafa Younes | 1h | Evidence-based closeout prepared at the project lead's request; meeting/effort limitations recorded | Not supplied |

The original estimate remains 24 engineering hours. ELS-25 and ELS-30 were reassigned from Hussein Chalhoub to Hussein Chreif: estimated allocation is now 9h Chreif, 3h Chalhoub, 6h Komayl and 6h Mostafa. These are estimates, not recorded work. No actual total, estimate variance, measured productivity or individual velocity is claimed. Each member should supply actual sessions using [the effort template](../../docs/testing/evidence-templates.md); AI runtime is not human effort.

## Acceptance evidence

| Scenario | Verified evidence | Limit |
| --- | --- | --- |
| Build and database setup | Integrated CI passed the database-test verify profile, including application/database integration tests | Clean local startup and live team demonstration were not repeated |
| Register, retrieve and replace a student | Student controller/service and PostgreSQL integration tests cover status, Location, normalized data, stable IDs and own-email updates | Automated evidence; no live team demo claimed |
| Manage equipment | Equipment controller/service tests cover create/list/update/delete; PostgreSQL tests cover uniqueness and deletion/history integrity | Automated evidence; no live team demo claimed |
| Invalid and conflicting input | Tests cover 400/404/409 responses, sanitized errors, rollback, duplicate emails/codes, nonpositive IDs and Unicode whitespace corrections | Passing CI does not prove every possible edge case |
| Competing student writes | PostgreSQL tests cover competing registrations/updates and preservation of losing records | Lending concurrency is separate Sprint 2 work |
| Borrowing specification | Specification and agent plan are available under docs/specifications | Preserve a genuine failing-test commit before implementation; individual plan approval is still required |

The final student correction passed 83 focused tests locally (29 service, 42 controller, 12 shared errors). The final equipment correction passed 33 focused tests locally (6 normalization, 15 controller, 12 service). These are separate focused runs, not the integrated suite count. The final main CI provides integrated verification; no uninspected report counts are inferred.

## Review findings and retrospective action

PR review found path-parameter validation behavior and Unicode whitespace gaps despite otherwise substantial tests. Corrections were committed to the respective branches before merge. The shared contract and explicit task boundaries supported independent student/equipment implementation. Earlier status-only completion and stale reports showed that Jira status alone is insufficient evidence.

The project lead's selected improvement for Sprint 2 is an evidence check before merging: each feature must link its acceptance criteria, a targeted boundary/rejection test, passing CI and a peer review decision. Include raw versus normalized input and failed-write state preservation where relevant. Hussein Chreif owns this check; the feature author supplies evidence and the peer reviewer checks it. This is a delegated process action, not fabricated unanimous meeting feedback.

## Remaining records and Sprint 2 handoff

The following records remain open even though Sprint 1 is administratively closed: actual person-hours from all four members, a live team demonstration and feedback, and individual borrowing-plan review decisions. Mostafa owns collecting the effort/review record; Komayl is its assigned peer reviewer. Do not backfill invented hours, attendance or approvals.

Sprint 2's planned two-day scope remains the lending lifecycle: test-first borrowing, returns, availability, overdue/history queries and real database integrity/concurrency. Use main as the base, one issue branch per task without a codex/ prefix, issue-key commits and reviewed PRs. Review the borrowing plan before its red phase; retain failing-test and green implementation history. Capacity remains provisional until member availability/effort is supplied. Sprint 2 has not been started by this closeout.
