# Equipment Lending Goal Question Metric Plan

Version 1. Owner: Hussein Chreif. Reviewer: Hussein Chalhoub. Planning card: ELS-24. Collection and improvement card: ELS-43. Results are not yet collected. Goals apply to the equipment project; previous lab values are excluded.

## Goals questions and measurements

| Goal and viewpoint | Questions | Metrics and method | Planned interpretation and action |
| --- | --- | --- | --- |
| G1 Evaluate lending correctness for staff and borrowers | Do important rules hold, including failures and concurrency? | Core requirements with reviewed criteria and linked passing evidence / total core requirements; number of unresolved rule/integrity defects; TC07 to TC20 results on the delivery database | All core rules need passing evidence and no unresolved integrity defect; resolve gaps before release |
| G2 Evaluate test effectiveness for maintainers | Are important paths covered, and can tests detect wrong behavior? | JaCoCo service line coverage; service branch coverage reported with denominator; meaningful/weak/wrong-oracle/missing audit counts; observed deliberate-defect detection | Aim for at least 80 percent service line coverage; inspect uncovered branches and fix at least three audited weaknesses; explain surviving defects rather than treating coverage as proof |
| G3 Improve maintainability for the four developers | Are significant warnings/smells corrected safely? | Same-version/config static-analysis findings by severity before/after; confirmed issues corrected; three documented refactorings with regression results; relevant complexity changes | Correct at least one significant measured issue and at least two important verified review concerns; interpret changed counts against scope/configuration rather than hiding warnings |
| G4 Evaluate delivery predictability for the team | How do real delivery effort and rework compare with estimates? | Per-card original estimated hours and actual person-hours; difference = actual minus estimate; ratio = actual / estimate for positive estimates; review/correction/testing/documentation effort; completed scope per sprint | Explain large differences, refine upcoming work using actual capacity and preserve the original estimate; do not rank members by velocity or code volume |
| G5 Evaluate reproducibility for the instructor | Can another person build and demonstrate the delivered revision? | Clean-clone build/test/start outcomes, acceptance results, environment/runbook deviations and unresolved deployment blockers | Correct reproducibility failures and update instructions before the presentation |

## Collection and comparison rules

Collect a first real baseline as soon as code and tools exist, before quality corrections or refactoring. Record revision, time, tool/version, scope, configuration, command and report path. After a correction, repeat the same relevant measurement and the test suite. Never substitute a target for a result. Do not report zero warnings or zero defects when measurement has not run.

Coverage is covered lines or branches divided by the tool's reported total for the stated package. Report the scope and exclusions; generated/DTO exclusions must be justified and consistent. A single mutation experiment establishes detection for that defect, not a statistical mutation score for the whole system. If source/configuration changes affect comparability, explain the difference rather than attributing every count change to improvement.

Actual effort is the sum of human work by contributors, including prompting, review, corrections, tests and documentation; distinguish it from elapsed time. Do not double-count overlapping categories or manufacture hours from the card estimate. Two people pairing for one hour is two person-hours. Record agent tool/runtime separately if useful; it is not a substitute for human effort. Count a card in sprint completion only when it meets Done.

## Results record

| Measurement | Baseline revision and result | After revision and result | Interpretation | Action and evidence |
| --- | --- | --- | --- | --- |
| Core requirement acceptance and integrity defects | Not collected | Not collected | Pending | ELS-36 and ELS-47 |
| Service line and branch coverage | Not collected | Not collected | Pending | ELS-43 |
| Generated test audit and corrected weaknesses | Not collected | Not collected | Pending | ELS-39 |
| Deliberate defect detection and restoration | Not collected | Not collected | Pending | ELS-40 |
| Static analysis and measured correction | Not collected | Not collected | Pending | ELS-43 |
| Verified design findings and refactorings | Not collected | Not collected | Pending | ELS-41 and ELS-42 |
| Estimate versus actual person-hours | Actual effort not supplied | Not collected | Pending | Sprint review cards |
| Clean setup and delivery acceptance | Not collected | Not collected | Pending | ELS-45 and ELS-47 |
