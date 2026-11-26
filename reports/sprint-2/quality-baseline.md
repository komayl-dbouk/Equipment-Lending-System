# Pre-Sprint-3 Quality Baseline

Jira: ELS-38 readiness action; comparative quality improvement remains ELS-43. Source revision: `b5e6a32ac0485e01cd3dee198785c1cdb45e636b`. Production source, dependencies and schema are unchanged. [Structured measurements](quality-baseline.json) retain scope, counters, class-level data and all warnings.

## Execution and scope

Java 17.0.12, Maven Wrapper 3.9.11 and disposable PostgreSQL 17.5. Cached plugin versions are pinned explicitly: JaCoCo Maven 0.8.11; SpotBugs Maven 4.8.3.1, engine 4.8.3. No POM change, custom coverage exclusion or static-analysis suppression was introduced. Coverage includes all production classes in the report; the service aggregate uses StudentService, EquipmentService, LoanService, LoanReturnService and LoanQueryService. Generated record helpers are not service classes. JaCoCo's standard bytecode filters apply.

Run with the documented separate integration-test database configured. Remove only the previous baseline execution file before the coverage command, then accumulate both test JVMs:

```powershell
if (Test-Path -LiteralPath target/quality-baseline/jacoco.exec) {
    Remove-Item -LiteralPath target/quality-baseline/jacoco.exec
}
.\mvnw.cmd -o --batch-mode --no-transfer-progress org.jacoco:jacoco-maven-plugin:0.8.11:prepare-agent '-Djacoco.destFile=target/quality-baseline/jacoco.exec' '-Djacoco.dataFile=target/quality-baseline/jacoco.exec' '-Djacoco.append=true' -Pdatabase-test verify org.jacoco:jacoco-maven-plugin:0.8.11:report
.\mvnw.cmd -o --batch-mode --no-transfer-progress com.github.spotbugs:spotbugs-maven-plugin:4.8.3.1:spotbugs '-Dspotbugs.effort=Max' '-Dspotbugs.threshold=Low' '-Dspotbugs.xmlOutput=true'
```

Both commands exited 0. The coverage run passed 185 unit/controller and 177 PostgreSQL integration tests, zero failures/errors/skips. The final report contains two JVM sessions. An initial review-harness setting used append=false and therefore retained only the integration JVM; that incomplete report was discarded and the full run repeated with a fresh file and append=true. It is not the baseline reported here. Local detailed reports/logs remain ignored under target/quality-baseline, target/site/jacoco and target/spotbugsXml.xml. Task-owned database/container resources were removed after measurement.

## Coverage

| Service | Lines covered / total | Branches covered / total |
| --- | --- | --- |
| StudentService | 37 / 37 | 10 / 10 |
| EquipmentService | 67 / 75 | 33 / 40 |
| LoanService | 30 / 30 | 14 / 14 |
| LoanReturnService | 20 / 20 | 4 / 4 |
| LoanQueryService | 6 / 6 | No branches reported |
| Aggregate | 160 / 168, **95.24%** | 61 / 68, **89.71%** |

JaCoCo reports service cyclomatic complexity of 77 total, with 72 covered and 5 missed. This is the tool's complexity counter, not an independent maintainability score. The full production bundle contains 31 analyzed classes. Passing the service line target does not establish oracle quality or make the deliberate-defect experiment unnecessary.

## Static analysis

SpotBugs reports **nine warnings**, eight priority 2 and one priority 3, with zero analysis errors. These are tool findings awaiting human triage, not nine confirmed product defects. There are no priority-1 findings in this run.

| Warning type | Count | Affected area |
| --- | --- | --- |
| EI_EXPOSE_REP2 | 4 | Controller service references and LoanEntity constructor associations |
| EI_EXPOSE_REP | 2 | LoanEntity association getters |
| NP_BOOLEAN_RETURN_NULL | 1 | Optional equipment availability filter parser |
| CT_CONSTRUCTOR_THROW | 1 | ApiException constructor |
| SE_NO_SERIALVERSIONID | 1 | StrictTextDeserializer |

ELS-41/43 should verify context before correcting anything. In particular, null represents an omitted optional filter, and JPA association conventions may affect representation-exposure warnings. Do not suppress or rewrite behavior merely to reduce counts. The structured file identifies exact classes/methods/source lines for triage.

## Comparison requirements

Retain the same tool versions, effort/threshold, class scope and exclusions for the after measurement. State any source or configuration difference that changes the denominator. ELS-43 must still confirm a significant measured concern, make a justified correction, repeat tools/tests and interpret the comparison. This baseline does not complete ELS-39 through ELS-44 or claim test-audit, mutation or refactoring results.
