# ELS-43 Measured Quality Improvement

## Scope and source revisions

Baseline source was merged main `a703a812ff748d76db923776cd14cc8bcd86664c`. The correction source is `7fd7233`. Historical Sprint 2 measurements remain evidence for `b5e6a32`; they are not presented as measurements of either ELS-43 source revision.

Java was Temurin 17.0.21, Maven Wrapper 3.9.11, Spring Boot 3.5.16, JaCoCo Maven 0.8.11 and SpotBugs Maven plugin 4.8.3.1 / engine 4.8.3. SpotBugs used Max effort, Low threshold, all production classes, and no custom filter or suppression.

## Diagnosis and approved correction

At `a703a81`, SpotBugs reported nine warnings and zero analysis errors. The complete technical triage below distinguishes intentional framework behavior from a retained scoped risk. ELS-41 screened the optional filter and dependency/entity references; it did not disposition the constructor warning. None of the eight remaining warnings is suppressed.

The remaining `SE_NO_SERIALVERSIONID` priority-3 warning was confirmed at `StrictTextDeserializer`. It subclasses serializable Jackson infrastructure without declaring a stable serialization identifier. After approval, `7fd7233` adds `private static final long serialVersionUID = 1L;`. The change preserves text-only JSON deserialization and public API behavior. It is a small maintainability correction, not a significant functional defect.

| Rule and priority | Before / after | Locations and evidence | Technical disposition and follow-up |
| --- | --- | --- | --- |
| EI_EXPOSE_REP2, priority 2 | 4 / 4 | EquipmentController and StudentController retain service references; LoanEntity retains its student/equipment associations. Controllers use DTOs and JPA manages the internal associations. | Preserve framework/domain references; copying services or entities would alter lifecycle/identity semantics. Revisit if entities become public API payloads. |
| EI_EXPOSE_REP, priority 2 | 2 / 2 | LoanEntity association getters support internal persistence and DTO mapping; controllers do not serialize the entity. | Preserve internal relationships and DTO boundary; review any future direct entity exposure. |
| NP_BOOLEAN_RETURN_NULL, priority 2 | 1 / 1 | EquipmentController uses null to represent an omitted availability filter; EquipmentRepository explicitly supports the null/all mode. | Preserve documented three-state filtering. Existing all/true/false contract cases exercise it. |
| CT_CONSTRUCTOR_THROW, priority 2 | 1 / 1 | ApiException is extensible and rejects non-4xx status values in its constructor. The only project subclasses, BusinessConflictException and ResourceNotFoundException, are final. The hierarchy has no finalizers, resource acquisition or untrusted extension-loading path. | Retain the guard and record a scoped residual risk, not a proven false positive. Revisit if untrusted subclasses/plugins, finalizers or protected resources are introduced; peer review of this disposition is pending. |
| SE_NO_SERIALVERSIONID, priority 3 | 1 / 0 | StrictTextDeserializer inherits serializability from Jackson; the correction declares serialVersionUID. | Corrected as a minor maintainability issue; it is separate from the significant measured query improvement. |

The [SpotBugs constructor-rule description](https://spotbugs.readthedocs.io/en/stable/bugDescriptions.html#ct-be-wary-of-letting-constructors-throw-exceptions-ct-constructor-throw) explains the finalizer-attack concern. The scoped-risk assessment above is based on this repository's hierarchy and exposure boundary; it is not a claim that throwing constructors are universally safe. Removing the defensive status guard, making the base class final despite its subclasses, or adding a deprecated finalizer solely to lower the warning count is not justified here.

## Significant measured improvement: bounded equipment-list queries

The significant concern was corrected in ELS-41, not by the serialVersionUID change. EquipmentService previously queried availability separately for each inventory item. With 24 items, all/available/unavailable requests issued 25 statements, even when filtering returned fewer items. ELS-41 moved response projection/filtering into the repository. The original approved correction and its red/green history remain in [the design report](els-41-design-corrections.md), [red results](els-41-red-results.json) and [verification results](els-41-verification-results.json).

For ELS-43, the identical current EquipmentDesignReviewIT listing probe was executed against pre-correction production source `bdbbd3e` in a detached temporary worktree and against current source `3f96d90`. Both used Java 17.0.12, Maven 3.9.11, PostgreSQL 17.5, the same isolated database and Hibernate prepared-statement statistics. The probe's SHA-256 is retained in the structured results. Only the test probe was supplied to the old worktree; no production correction was backported. The original test assertions were not changed.

| Inventory size | Filter | Before statements | Current statements |
| --- | --- | --- | --- |
| 1 | all | 2 | 1 |
| 1 | true | 2 | 1 |
| 1 | false | 2 | 1 |
| 24 | all | 25 | 1 |
| 24 | true | 25 | 1 |
| 24 | false | 25 | 1 |

The pre-correction run had six cases and three expected bounded-query assertion failures, zero errors/skips. The current run passed all six, zero failures/errors/skips. The 12 database-safety unit cases also passed in each invocation. Exact response values/order and unchanged equipment/history snapshots are checked against independent SQL before the query-count assertion. This demonstrates a significant database-work reduction: 96 percent fewer statements for 24 items, with one statement across the tested sizes/filters. It is not a latency benchmark or a new ELS-43 production fix. ELS-43 measures the already accepted ELS-41 correction rather than manufacturing a second defect.

## Measurements

| Measurement | Before | After | Interpretation |
| --- | --- | --- | --- |
| SpotBugs warnings | 9: eight priority 2, one priority 3; 0 errors | 8: eight priority 2; 0 errors | One low-priority serialization warning removed using identical tool, settings and class scope. |
| Service line coverage | b5e6a32: 160/168 (95.24%) | 7fd7233: 144/144 (100.00%) | Same two-JVM measurement method; the denominator changed after accepted ELS-41/42 source changes, so this is not attributed to the serialization correction. |
| Service branch coverage | b5e6a32: 61/68 (89.71%) | 7fd7233: 37/38 (97.37%) | Same two-JVM measurement method; the denominator changed after accepted source changes. |
| Service complexity covered/total | b5e6a32: 72/77 (93.51%) | 7fd7233: 57/58 (98.28%) | JaCoCo complexity counter, not an independent maintainability score. |

The final JaCoCo report analyzes 38 production classes and contains the unit and PostgreSQL integration JVM sessions. The source revision differs from Sprint 2 because accepted ELS-41/42 work changed service code, so the changed coverage denominators are explicitly retained rather than attributed to ELS-43.

## Verification and limitation

`mvnw.cmd -Dtest=StrictWriteValidationTest test` passed 36 tests. `mvnw.cmd verify` passed 195 tests with zero failures, errors and skips. `mvnw.cmd -Pdatabase-test verify` passed 195 unit/controller and 188 PostgreSQL integration tests with zero failures, errors and skips. The two-JVM JaCoCo command passed the same 383 tests.

Independent review on `3f96d90` repeated standalone verification (195 tests), the combined PostgreSQL/coverage profile (195 + 188 tests), and SpotBugs on both `a703a81` and `3f96d90`. It reproduced all reported service counters, two execution sessions, 38 XML class entries, and the 9-to-8 warning change with zero analysis errors. The review used Java 17.0.12; this is separate from the author's Java environment above. No application/test source changed in the evidence correction. Raw review logs and XML remain ignored under `target/els43-review/`.

### Reproduction commands

Use the separately provisioned test database configuration from the runbook. Start from fresh task-owned execution data; do not append an earlier measurement. The tools and scope match the historical measurement configuration:

```powershell
.\mvnw.cmd -o --batch-mode --no-transfer-progress verify
.\mvnw.cmd -o --batch-mode --no-transfer-progress org.jacoco:jacoco-maven-plugin:0.8.11:prepare-agent '-Djacoco.destFile=target/els43-review/coverage.exec' '-Djacoco.dataFile=target/els43-review/coverage.exec' '-Djacoco.append=true' -Pdatabase-test verify org.jacoco:jacoco-maven-plugin:0.8.11:report
.\mvnw.cmd -o --batch-mode --no-transfer-progress com.github.spotbugs:spotbugs-maven-plugin:4.8.3.1:spotbugs '-Dspotbugs.effort=Max' '-Dspotbugs.threshold=Low' '-Dspotbugs.xmlOutput=true'
```

For the query comparison, create a detached worktree at `bdbbd3e2ab5a31bc12b879665a59380e0e7075d7` and supply only the unchanged `EquipmentDesignReviewIT.java` from `3f96d90e9b0f13ad918b3cb034fb488a8221f76e`. Run the following command there and on current source. Expect three assertion failures on the old source and zero on current source; infrastructure errors are never valid red evidence:

```powershell
.\mvnw.cmd -o --batch-mode --no-transfer-progress -Pdatabase-test '-Dtest=DatabaseTestSafetyProcessorTest' '-Dit.test=EquipmentDesignReviewIT#listingPreservesExactSqlValuesAndUsesBoundedQueries' '-Dlogging.level.root=WARN' verify
```

Compare the six `ELS41_LIST` output rows with the table above. The probe and before-source identifiers are pinned in [the structured results](els-43-quality-results.json). Task-owned comparison databases and the temporary worktree are cleaned up after recording results; no secrets or original assignment are copied into the worktree.

The initial database attempt found PostgreSQL unavailable at localhost:5433. Docker Desktop was then made available, and the isolated `els-tests` PostgreSQL 17.5 container was started before the passing database and coverage commands. No new packaged HTTP workflow is claimed for the serialization-only correction. PR-head CI on `3f96d90` subsequently passed: [Java and PostgreSQL verification](https://github.com/komayl-dbouk/Equipment-Lending-System/actions/runs/45977536904/job/140432930619). Final peer approval and merge remain pending.

## GQM action

Retain the historical baseline and the same-configuration static-analysis comparison. The significant query improvement is attributed to ELS-41 and validated for this quality comparison; it is not relabeled as new ELS-43 production work. The remaining eight warnings have explicit dispositions and remain visible. Preserve DTO boundaries, bounded-query regressions and the constructor status guard; reconsider retained risks when their stated assumptions change. Human review and current-head CI remain required before delivery acceptance.
