# ELS-39 approved test-audit plan

Owner: Mostafa Younes. Human reviewer: Komayl Dbouk. Baseline: pulled origin/main bdbbd3e. Branch: ELS-39-generated-test-audit. Existing branches and local work are preserved.

Mostafa explicitly replied **Approve the ELS-39 plan** before substantial changes. Jira comment 10094 records the decision and implementation start. ELS-41 coordination is recorded in comment 10095; audit-specific fault-probe coordination with Chalhoub is in ELS-40 comment 10096. These are plan/coordination records, not peer acceptance of the final audit.

1. Inspect at least 14 representative AI-assisted tests across students, equipment, borrowing, returns, queries, safety and database integrity. Record exact methods, requirements, classification, actual oracle, a concrete incorrect behavior, correction/evidence and supplied human decision.
2. Verify three suspected weaknesses: student history only checks one active row; a midday Clock cannot distinguish UTC from Beirut dates; the active-loan uniqueness test repeats both reference IDs and cannot prove index selectivity.
3. Strengthen LendingQueriesIT with exact active/returned student-history results and a fixed instant across the UTC/Beirut date boundary. Add supplementary LoanIndexSelectivityIT checks for two equipment items held by one student and one equipment item contested by different students. Preserve original BorrowingContractIT and its red ancestry.
4. Compare original and corrected tests against three narrowly scoped local fault probes. Temporary changes are restricted to LoanQueryService/LoanQueryRepository or the active-loan index in our own guarded disposable PostgreSQL database. Retain exact failures/survivors and restore original bytes/hashes/index before normal verification. No faulty production source, migration or index definition may be committed. These probes substantiate audit oracles; they do not complete ELS-40.
5. Run baseline/focused checks, then `mvnw.cmd verify` and `mvnw.cmd -Pdatabase-test verify` using Java 17, Maven Wrapper and PostgreSQL 17.5. Use actual results, not inherited counts. Record failures/corrections and restoration evidence under reports/sprint-3 and ai-log. Do not infer coverage, mutation score or human effort.
6. Review diffs and explicit staged paths, commit with ELS-39, push and deliver the specified PR into main requesting komayl-dbouk. Check CI on the latest head, link evidence and move to CODE REVIEW when ready. No self-approval, merge or premature Done.

Avoid EquipmentService, EquipmentRepository, EquipmentServiceTest and EquipmentDesignReviewIT, which are owned by the ongoing ELS-41 correction. Production fixes require another reviewed scope decision. Published GQM/coverage/static-analysis baselines remain historical; ELS-43 owns comparable after measurements.

ELS-44 is excluded. Do not start, implement or change its card/branch until the user is asked separately and its acceptance/merge dependencies are satisfied. Subject/, the assignment, .env, dependencies and schema migrations remain untouched. No credentials, build output or execution dates belong in committed document names/prose.
