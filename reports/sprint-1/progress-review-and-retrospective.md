# Sprint 1 Progress Review and Retrospective Preparation

Prepared for ELS-31. Sprint duration: two days. Responsible member: Mostafa Younes; assigned reviewer: Komayl Dbouk.

## Goal and current increment

The goal is reviewed scope/design plus a runnable, tested student and equipment API. The current demonstrable artifact increment is documentation: proposal, requirements, architecture/data models, API specification, borrowing feature specification/agent plan, testing strategy, GQM plan and review records. There is no application source/build in the inspected worktree, so a student/equipment API demonstration and passing suite cannot yet be reported.

The user confirmed the proposal step complete in chat. The user requested non-code Sprint 1 cards be marked Done; changes made outside Sprint 1 were reverted. Board status is retained, while this report distinguishes prepared artifacts from actual implementation and ceremony evidence.

## Card and estimate reconciliation

| Card | Owner | Original estimate | Observed artifact or remaining work | Actual person-hours |
| --- | --- | --- | --- | --- |
| ELS-20 | Hussein Chreif | 2h | Proposal/team plan prepared; user reported proposal step complete | Not supplied |
| ELS-21 | Mostafa Younes | 2h | Requirements, API baseline and requirement/test map prepared | Not supplied |
| ELS-22 | Komayl Dbouk | 2h | Domain/design/database/sequence/deployment models and ADRs prepared | Not supplied |
| ELS-23 | Hussein Chalhoub | 3h | Application/bootstrap/context implementation pending | Not supplied |
| ELS-24 | Hussein Chreif | 2h | Testing/GQM/reference map and evidence templates prepared | Not supplied |
| ELS-25 | Hussein Chalhoub | 2h | Shared validation/error implementation pending | Not supplied |
| ELS-26 | Komayl Dbouk | 3h | Equipment implementation pending | Not supplied |
| ELS-27 | Hussein Chreif | 2h | Borrowing specification/agent plan and agent review prepared; human decisions pending | Not supplied |
| ELS-28 | Komayl Dbouk | 1h | Student contract/privacy agent review and corrections prepared; human decisions pending | Not supplied |
| ELS-29 | Mostafa Younes | 3h | Student implementation pending | Not supplied |
| ELS-30 | Hussein Chalhoub | 1h | Build/test pipeline implementation pending | Not supplied |
| ELS-31 | Mostafa Younes | 1h | Current report and review agenda prepared; product demonstration/actual hours pending | Not supplied |

Original total: 24 engineering hours, six estimated per member. No actual-hours total, variance, velocity or productivity claim can be calculated until contributors supply real session records. Use [the effort template](../../docs/testing/evidence-templates.md).

## Demonstration agenda and acceptance record

| Scenario | Expected demonstration | Observed result |
| --- | --- | --- |
| Start system | Clean documented startup and reachable API | Not executed |
| Register and update student | Correct 201/200 data and normalization | Not executed |
| Manage equipment | Create/list/update/delete unreferenced item | Not executed |
| Reject invalid/duplicate input | 400/409 with stable errors and no partial write | Not executed |
| Run automated checks | Relevant suite passes; report revision/environment/output | Not executed |
| Inspect borrowing specification | Explain criteria and forthcoming red/green evidence | Specification available; team demonstration not recorded |

Record actual attendees, date, observed outcomes, stakeholder feedback and defects when the review occurs. No interview, stakeholder satisfaction or successful demo is invented.

## Process observations and proposed retrospective actions

Observed: the narrow backend scope and named cards make implementation boundaries clear. The earlier testing plan provided meaningful rules and evidence templates. The status-only closure exposed a gap between Jira Done and available artifacts; that gap is being reduced by creating and linking concrete documents now.

Proposed improvement: before closing an implementation card, require a linked artifact/commit, observed acceptance results and actual peer decision. Keep one shared contract to avoid conflicting agent outputs. At the daily integration check, compare source/contract and resolve drift before generating more code.

The team retrospective should record what actually helped, what caused rework, how AI affected review effort, and one agreed action with owner/iteration. The observations above are preparation based on inspected project state, not minutes of a meeting that has not been recorded.

## Blockers release planning and next work

The immediate dependency is ELS-23 setup, then shared errors/validation, parallel student/equipment work and CI. The API contract now provides a concrete baseline for these agents. Human adoption of requirements/decisions, actual plan approval and sharing/committing the baseline are still needed. The exact submission cut-off and instructor access/submission receipt remain unverified here.

Sprint 2 goal remains the complete lending lifecycle in the next two-day sprint. Refine capacity from actual member availability and Sprint 1 progress before committing. Do not move test-first borrowing implementation ahead of its specification/test-history requirements.
