# ELS-49 — Equipment lending presentation

Editable speaker slides for the staff-managed backend. Advance at each `---`; the [demo runbook](els-49-demo-runbook.md) supplies commands and handovers. Evidence describes observed technical work, not an unheld team rehearsal or human sign-off.

---

## 1. Problem and release boundary — Hussein Chreif

Paper/spreadsheet records obscure which university item is available, who borrowed it, and when it is due. Staff operate this REST API; students are borrower records, not login users. The release covers student/equipment records, one active loan per item, returns, availability, overdue and history queries. There is **no frontend or student portal**. [Requirements](../requirements/requirements.md) and [report](../../reports/final-report.md) state the scope.

---

## 2. Architecture — Komayl Dbouk

One Java 17/Spring Boot 3.5.16 application has `student`, `equipment`, and `loan` feature packages. Staff Basic authentication guards `/api/**`; only aggregate health is public. Each controller accepts/returns DTOs and validates input; services normalize values, enforce rules and own transactions; repositories read/write PostgreSQL. Flyway alone migrates the schema, Hibernate validates it. Restrictive foreign keys, date checks, normalized identifiers and the active-loan partial unique index protect history and concurrency. Public errors use the documented six-field shape; rejected requests must not write. [Architecture](../design/architecture.md), [contract](../api/contract.md), [ELS-47 acceptance](../../reports/sprint-4/els-47-final-release-acceptance.md).

---

## 3. Our contributions and test oracles — all four

| Member | Actual contribution to explain | Concrete oracle |
| --- | --- | --- |
| Hussein Chreif | Borrowing specification/red tests and reviewed design/report work ([ELS-32](../../reports/sprint-2/els-32-test-first-evidence.md), [report](../../reports/final-report.md)) | Before implementation, valid borrow tests were red; final [TC07–TC11](../../reports/sprint-4/els-47-final-release-acceptance.md) assert exact loan DTO, rejected invalid/unavailable state and one winner in a real PostgreSQL race. |
| Mostafa Younes | Student and return features, generated-test audit, final acceptance ([ELS-29](../testing/els-29-student-management.md), [ELS-34](../testing/els-34-loan-returns.md), [ELS-47](../../reports/sprint-4/els-47-final-release-acceptance.md)) | [TC12–TC15](../../reports/sprint-4/els-47-final-release-acceptance.md) check exact return date, duplicate-return rejection, preserved history and reborrow; independent SQL state confirms results. |
| Komayl Dbouk | Equipment, atomic borrowing, safe refactoring, staff access ([ELS-46](../../reports/sprint-4/els-46-access-privacy-license-review.md), [ELS-42](../../reports/sprint-3/els-42-safe-refactorings.md)) | [TC21](../../reports/sprint-4/els-47-final-release-acceptance.md) checks all 11 operations reject missing/wrong credentials without committed writes; [TC11](../../reports/sprint-4/els-47-final-release-acceptance.md) proves one active loan under competing requests. |
| Hussein Chalhoub | Shared foundation/errors, query features, CI and delivery packaging ([team plan](../team-plan.md), [ELS-45](../../reports/sprint-4/els-45-reproducible-packaging.md)) | [TC16–TC18/25](../../reports/sprint-4/els-47-final-release-acceptance.md) compare availability, overdue and filtered history to independent SQL; [TC23](../../reports/sprint-4/els-47-final-release-acceptance.md) verifies packaged seed/rerun/reset. |

Each member should be able to explain another row as well as their own. The linked artifacts and merged commits/PRs are the attribution source; a planned speaker assignment is not proof a person has rehearsed.

---

## 4. Corrected AI output and engineering judgment — all four

AI-assisted output was reviewed, not accepted wholesale. A generated query implementation appeared functional but issued 25 database statements for a 24-item list; the ELS-41 repository projection correction reduced the measured count to one while preserving SQL-backed values ([ELS-43](../../reports/sprint-3/els-43-measured-quality-improvement.md)). A property-only access test missed an absent-variable literal fallback, and production SQL error logging exposed synthetic email values; ELS-46 corrected both and added packaged checks ([review](../../reports/sprint-4/els-46-access-privacy-license-review.md)). Generated tests with weak oracles were strengthened after audit ([ELS-39](../../reports/sprint-3/els-39-test-audit.md)). The team must name the original weakness, independent check, correction and remaining limit; no AI time saving or unrecorded model version is claimed.

---

## 5. Measured quality and live demo — Mostafa → Komayl → Hussein Chalhoub

Revision-scoped evidence: 24-item list **25 → 1** database statements (96% fewer statements, not a latency benchmark); SpotBugs **9 → 8** warnings (one low-priority correction, eight assessed warnings retained). ELS-47's accepted run passed **200 unit/controller + 193 PostgreSQL tests, 15 helper checks, 227 HTTP requests / 3409 CI assertions, and three coordinated races** on its stated source; these are historical ELS-47 results, not new ELS-49 measurements. Show a fresh local packaged app: public health; rejected missing/wrong staff access; authorized synthetic seed (one student, two items, active and returned loan); availability/history queries; duplicate rejection; read-only rerun; confirmed reset/reseed and owned cleanup. [Runbook](els-49-demo-runbook.md).

---

## 6. Limits and decision request — Hussein Chreif

The demonstration is loopback-only. Basic credentials need separately verified HTTPS for remote use. Browser/CSRF/origin acceptance, distribution notices, four-person rehearsal and submission receipt are not inferred from backend technical acceptance. If Docker or live HTTP fails, show source-scoped ELS-47 evidence and the command/checkpoint, label the fallback **not a live pass**, and hand over the issue to the owner. ELS-50 release closeout remains separate. Ask reviewers to inspect the evidence and identify any incorrect claim.
