# ELS-39 Audit generated tests and correct verified weaknesses

## Purpose

Strengthen three lending test oracles that passed while concrete incorrect behavior remained undetected: incomplete student history, a hidden UTC/Beirut date difference, and an active-loan index keyed by the wrong reference.

## Audit and corrections

I inspected **14 existing representative methods** across students, equipment, borrowing, returns, queries, database integrity and safety: **11 Meaningful and 3 Weak**. The [audit matrix](https://github.com/komayl-dbouk/Equipment-Lending-System/blob/ELS-39-generated-test-audit/reports/sprint-3/els-39-test-audit.md) records each exact method, requirement, actual oracle, counterexample, correction and pending human decision.

- Student history now requires exact ordered active and returned loan results, with exact DTO values, instead of merely containing one active loan.
- The fixed Clock now crosses the UTC/Beirut calendar-date boundary, making a UTC-date substitution observable while keeping tests independent of the execution day.
- Two supplementary SQL cases vary student/equipment IDs independently: a student can hold different items, while different students cannot share one active item. They verify committed state and the structured PostgreSQL uniqueness constraint.

I preserved BorrowingContractIT and its original red ancestry, retained strong existing rollback/concurrency tests, and avoided ELS-41's equipment files. No production fix, dependency or migration change is included. Coordination is recorded on ELS-40/41.

## Validation

Tested corrected source: `cc1997bfaea87ea7e902344800b6cf65c99cb2a9`, based on pulled main `bdbbd3e`; subsequent commits contain evidence only.

- Original full PostgreSQL baseline: **362 passed**, zero failures/errors/skips.
- `mvnw.cmd -o --batch-mode --no-transfer-progress -Pdatabase-test -Dtest=DatabaseTestSafetyProcessorTest -Dit.test=LendingQueriesIT,LendingIntegrityIT,LoanIndexSelectivityIT verify`: **33 passed**.
- `mvnw.cmd verify`: **185 passed**.
- `mvnw.cmd -Pdatabase-test verify`: **364 passed** (185 unit/controller, 179 PostgreSQL integration), **zero failures/errors/skips**.

Narrow audit controls demonstrate before/after discrimination:

- Dropped returned history: original query class **15 passed**; corrected class detected it with **one intended assertion failure**.
- UTC-date substitution: original query class **15 passed**; corrected Clock fixture detected it with **one intended assertion failure**.
- Wrong student-key active index: original integrity class **4 passed**; supplementary selectivity class detected it with **two intended assertion failures**.

Every probe passed 12 safety-guard tests and had zero test errors/skips. All production bytes and the original equipment index were restored before the final green checks. These targeted results are separate from the restored suite and do not claim a global mutation score or completion of ELS-40.

Evidence: [probe commands/diffs/results](https://github.com/komayl-dbouk/Equipment-Lending-System/blob/ELS-39-generated-test-audit/reports/sprint-3/els-39-probe-results.json), [execution report](https://github.com/komayl-dbouk/Equipment-Lending-System/blob/ELS-39-generated-test-audit/reports/sprint-3/els-39-verification.md), [suite counts](https://github.com/komayl-dbouk/Equipment-Lending-System/blob/ELS-39-generated-test-audit/reports/sprint-3/els-39-verification-results.json), [AI log](https://github.com/komayl-dbouk/Equipment-Lending-System/blob/ELS-39-generated-test-audit/ai-log/els-39-generated-test-audit.md).

Java 17, Maven Wrapper 3.9.11 and isolated PostgreSQL 17.5 were used. Docker's engine was unavailable, so I used the documented native fallback with generated temporary credentials. Diff/staged-path/secret checks passed. Remote CI has not yet been observed for this PR head.

## Limitations

Human review of the audit matrix and implementation remains pending. Coverage/static-analysis comparisons remain ELS-43; the separate ELS-40 experiment and ELS-41/42 corrections/refactorings are not claimed here. Authentication/HTTPS remain ELS-46. ELS-44 has not started and requires a separate decision plus accepted/merged dependencies. No human effort or coverage improvement is invented. No self-approval or merge has been performed.

## Jira

[ELS-39 — Audit generated tests and correct three weaknesses](https://equipment-lending-system.atlassian.net/browse/ELS-39)
