# Testing and Effort Evidence Templates

Copy the relevant template into the card's evidence file when work occurs. Keep this file as a reusable schema. Empty cells mean not supplied; they do not mean zero, pass, approval or completion. Record observations at the time of work and link the resulting file or run from Jira.

## Test or acceptance execution

| Field | Recorded value |
| --- | --- |
| Sprint, executor and Jira key | |
| Requirement and test/scenario ID | |
| Revision and environment | |
| Preconditions and synthetic data | |
| Exact command or steps | |
| Expected outcome and rationale | |
| Observed response and persisted state | |
| Pass, fail or blocked | |
| Output/report link | |
| Defect, correction and rerun | |
| Reviewer and decision | |

## Estimated and actual effort

Use one row per work session; retain the original estimate in the card. Categories must not overlap. No actual hours are prefilled.

| Sprint | Jira key | Contributor | Original estimate hours | Session person-hours | Work category | AI tool and task | Interruption or variance explanation | Evidence |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| | | | | | Requirements, design, prompting, implementation, review, correction, testing or documentation | | | |

At sprint review sum sessions per card, compare with its estimate, explain material differences and update upcoming capacity. Preserve initial estimates when replanning. Do not treat AI elapsed runtime as human effort or compare individuals using velocity.

## AI generated test audit

| Test or missing scenario | Requirement | Classification | Why the oracle is meaningful or weak/wrong | Correction | Before and after evidence | Human reviewer |
| --- | --- | --- | --- | --- | --- | --- |
| | | Meaningful, Weak, Wrong oracle or Missing scenario | | | | |

## Review refactoring and measurement

| Item and affected code | Baseline revision and test/tool result | Concern and severity | Human decision and reason | Correction or refactoring | After revision and test/tool result | Interpretation |
| --- | --- | --- | --- | --- | --- | --- |
| | | | Accepted, modified or rejected | | | |

For refactorings name the actual smell and show why behavior is preserved. For metrics retain comparable tool/version/configuration and scope. For deliberate defects also record which tests fail, which unexpectedly pass, restoration revision and full regression result.
