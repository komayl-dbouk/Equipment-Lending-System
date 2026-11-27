# ELS-43 Technical Review for ELS-44

Reviewer responsibility: Mostafa Younes. Assistance: Codex. Human decisions remain separate from this technical recommendation. Plan review was explicitly approved in the task conversation.

## Reviewed identity and dependency gate

Reviewed [PR 20](https://github.com/komayl-dbouk/Equipment-Lending-System/pull/20) at exact final head `ed1bc97a38475c8cc1c2d6382f0f52ccafc2efce`, base `a703a812ff748d76db923776cd14cc8bcd86664c`. Full diff: five files (one Java class, measured report, structured results, GQM and AI log). Human `younes285` submitted APPROVED review `6086522122`. The PR merged as `bd0b2758afbe635ea95bee20ab19d3fc3b25e43c`.

[Head CI](https://github.com/komayl-dbouk/Equipment-Lending-System/actions/runs/45979176208) and [merged-main push CI](https://github.com/komayl-dbouk/Equipment-Lending-System/actions/runs/45979783370) both succeeded on their respective exact SHAs. The API's PR-only run filter does not include main push events; main CI was verified through the workflow-runs collection with the exact merge SHA and push event. ELS-43 is Done in Jira. The dependency gate for ELS-44 is satisfied by these human and CI records, not by the assistant's recommendation.

## Findings and recommendation

No blocking implementation or measurement defect was found in source/evidence inspection. Independent execution results are recorded below. Technical recommendation: accept the scoped ELS-43 quality evidence, retaining its explicit residual risks and historical measurement limits. This recommendation does not approve or merge a PR on behalf of a person.

One low-severity documentation issue: [els-43-quality-results.json](../../reports/sprint-3/els-43-quality-results.json) lines 281-282 and [els-43-measured-quality-improvement.md](../../reports/sprint-3/els-43-measured-quality-improvement.md) line 77 retain `pending` approval/merge statements from the pre-delivery snapshot. Trigger: reading these historical files as current release status. Consequence: stale handoff information despite verified human approval/merge. Required reconciliation: publish the exact reviewed head, human review, merge and passing merge CI in the canonical closeout. ELS-44's [closeout record](../../reports/sprint-3/progress-review-and-retrospective.md#merged-work-and-peer-decisions) does so while preserving the original snapshot. This is nonblocking for product correctness; it must not be interpreted as evidence of missing approval now.

## Source and measurement assessment

The entire production diff adds `private static final long serialVersionUID = 1L` to [StrictTextDeserializer](../../src/main/java/edu/university/equipmentlending/shared/config/StrictTextDeserializer.java). This is an appropriate local serialization identifier correction. No tests, assertions, dependencies, queries, locks, migrations, CI configuration, warning suppressions or exclusions changed in ELS-43. The production/test/build/CI tree of `3f96d90e9b0f13ad918b3cb034fb488a8221f76e`, the earlier independent-review revision, is identical to the final merge. The original borrowing contract test remains unchanged from its red-test source; the red history remains an ancestor.

SpotBugs comparisons use Maven plugin 4.8.3.1, engine 4.8.3, Max effort and Low threshold over all production classes. The baseline is `a703a812ff748d76db923776cd14cc8bcd86664c`; the correction is `7fd7233`. Nine warnings become eight by removing SE_NO_SERIALVERSIONID (priority 3). This is a minor maintainability correction, not the significant improvement criterion. No analysis errors are hidden.

The eight priority-2 findings remain visible: four EI_EXPOSE_REP2, two EI_EXPOSE_REP, one NP_BOOLEAN_RETURN_NULL and one CT_CONSTRUCTOR_THROW. Controllers keep DTO boundaries; JPA association access and injected collaborators were not mechanically copied. Omitted availability intentionally remains null for all-equipment behavior. ApiException's constructor guards its 4xx contract. CT_CONSTRUCTOR_THROW is retained as a scoped residual risk, not declared universally false positive: project subclasses are final and no finalizers, protected-resource acquisition or untrusted extension loading were found. Revisit that disposition if the hierarchy or trust assumptions change. No suppression was added to conceal it.

The significant measured correction is ELS-41's bounded equipment availability projection, implemented in `dbb2b55314ae8370558016092ad66b37d855624f`. ELS-43 credits its origin rather than claiming a new production fix. The identical EquipmentDesignReviewIT probe (SHA-256 `4ed4130e17f8d44039142dea45e294782f17aec823dcd3633ca0dc5a58c2a72e`) compares pre-correction `bdbbd3e2ab5a31bc12b879665a59380e0e7075d7` with corrected `3f96d90e9b0f13ad918b3cb034fb488a8221f76e` for sizes 1/24 and all/true/false filters. Statements fall from 2/25 to 1 in every case. The 24-item reduction is (25-1)/25 = 96%; no latency improvement or arbitrary SLA is asserted. Exact JSON values, ordering and unchanged equipment/loan snapshots are independently compared with SQL before the query-bound assertion, so its three original-source failures cannot conceal wrong result/state oracles.

JaCoCo 0.8.11 uses fresh execution data appended across the unit and PostgreSQL test JVMs, all production classes and the same five-service aggregate: StudentService, EquipmentService, LoanService, LoanReturnService and LoanQueryService. Reported counters are lines 144/144, branches 37/38 and complexity 57/58; 38 production classes are analyzed. The historical `b5e6a32` counters (160/168, 61/68, 72/77; 31 classes) have different source denominators following ELS-41/42. The report correctly avoids attributing those percentage changes to serialVersionUID or treating coverage as oracle strength.

The [GQM record](../testing/gqm-plan.md) gives interpretation and action, credits the earlier query/design/refactoring work, retains residual warnings and defers release access/setup checks. Effort is explicitly unavailable for Sprint 3; no hours are inferred from coverage, story points or AI runtime.

## Independent checks

Execution source: merged main `bd0b2758afbe635ea95bee20ab19d3fc3b25e43c`; Java JBR 17.0.5, Maven Wrapper 3.9.11, Spring Boot 3.5.16. This is distinct from the author's Temurin version and the earlier Java 17.0.12 review. Both static sides use the same local toolchain. Task-owned logs/reports/worktrees remain under ignored `target/els44-review/`; credentials are generated privately and are never committed.

| Check | Independent result |
| --- | --- |
| `mvnw.cmd -o --batch-mode --no-transfer-progress verify` | Exit 0; 195 tests, zero failures/errors/skips |
| Identical SpotBugs 4.8.3.1 Max/Low analysis on a703a81 and merged bd0b275 | Both exit 0; 9 to 8 warnings, 38 classes each, zero analysis errors/missing classes; only priority-3 serialization finding removed |
| Fresh combined two-JVM JaCoCo/PostgreSQL profile | Exit 0; 195 unit/controller + 188 PostgreSQL tests, 383 total, zero failures/errors/skips. Two sessions, 38 XML class entries; five-service lines 144/144, branches 37/38, complexity 57/58 reproduced. JaCoCo console counts 33 classes with executable code; five additional XML entries have no counters (four repository interfaces and a normalized record). |
| Identical old/current six-case query comparison | Old source: 12 safety tests passed, 6 query cases with exactly 3 expected bounded-query assertion failures, zero errors/skips; observed statements 2/2/2/25/25/25. Merged source: 12 safety and all 6 query cases passed, zero failures/errors/skips; 1 statement in each case. Probe bytes/hash are identical. |

The exact reproduction commands are retained in the [ELS-43 report](../../reports/sprint-3/els-43-measured-quality-improvement.md#reproduction-commands). This review uses `target/els44-review/coverage.exec` for fresh combined data. The baseline query worktree supplies only the unchanged probe; production source is untouched.

Environment correction: the previously used external tool drive was unavailable and default Java was 8. An installed Java 17 JDK was located and verified. Initial portable-tool downloads stalled and were stopped after identifying only task-owned processes. Docker Desktop was started hidden and the documented compose.test.yaml PostgreSQL 17.5 service was provisioned as the separate `els44-review` project on loopback port 15444. No existing .env or other developer's database/volume was reused. Environment preparation is not test failure or passing evidence.

The first offline coverage invocation exited 1 before test execution because JaCoCo transitive dependencies were not cached. The unchanged command was rerun online, resolved those dependencies and passed. No test or source correction was required. Exact coverage command: `mvnw.cmd --batch-mode --no-transfer-progress org.jacoco:jacoco-maven-plugin:0.8.11:prepare-agent '-Djacoco.destFile=target/els44-review/coverage.exec' '-Djacoco.dataFile=target/els44-review/coverage.exec' '-Djacoco.append=true' -Pdatabase-test verify org.jacoco:jacoco-maven-plugin:0.8.11:report`.

After retaining raw logs/XML under ignored target/els44-review, the task-owned compose service/network, comparison worktrees, generated private credential file and stopped partial downloads were removed. The main working tree contains only the ELS-44 documentation changes. Subject/ remains untracked and unignored; no existing local application data was changed.

## Observable behavior and limits

The serialization identifier does not change HTTP behavior, so no new packaged HTTP result is manufactured for it. The earlier observable ELS-41 query/error correction has a real packaged HTTP/SQL workflow with 83 requests and 626 assertions, including races and committed-state SQL checks, in its [verification record](../../reports/sprint-3/els-41-verification-results.json). ELS-42 also retains a passed packaged-JAR examples run. Those executions remain attributed to their original revisions; final delivery/access verification belongs to ELS-45/46/47.

This review establishes the scoped measurements and regressions, not a universal absence of defects, a statistical mutation score, a latency guarantee, a human retrospective meeting or final release readiness. ELS-44 human inputs and Komayl's review remain separate gates.
