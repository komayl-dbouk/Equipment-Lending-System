# Team Work Plan and Sprint 1 Kickoff

Working timezone: Asia/Beirut. Jira is the live source for assignments and progress. This plan establishes responsibilities and the execution order; it does not claim the team has reviewed or completed the assigned work.

## Responsibilities

| Member | Product responsibility | Engineering responsibility | Default reviewer |
| --- | --- | --- | --- |
| Hussein Chreif | Scope and borrowing specification | Product Owner, testing strategy, traceability, verified design review, report and submission | Hussein Chalhoub |
| Mostafa Younes | Students and returns | Requirements, test audit, acceptance evidence and retrospectives | Komayl Dbouk |
| Komayl Dbouk | Equipment and safe borrowing | Architecture, database integrity, refactoring, API documentation and presentation | Hussein Chreif |
| Hussein Chalhoub | Shared foundation and query features | Scrum facilitation, CI, quality tools, access and deployment | Mostafa Younes |

Codex assists planning, technical guidance and review on request. A human team member remains responsible for each assignment, approves significant plans and verifies generated work. All four members contribute code, tests, documentation and peer review. Knowledge sharing at sprint reviews ensures the architecture and workflow are understood by everyone.

## Sprint 1

Duration: two days. Goal: agree scope and design, then demonstrate a tested student and equipment API. Planned effort is six hours per person across two days. Confirm availability in the opening check-in and replan if needed; additional available hours are buffer, not automatically extra scope.

| Member | First assignment | Next work | Estimated total |
| --- | --- | --- | --- |
| Hussein Chreif | Review proposal with team and arrange proposal submission | Testing/GQM/evidence strategy; borrowing specification and human plan review | 6 hours |
| Mostafa Younes | Define requirements and acceptance criteria | Student endpoints and tests; sprint review/retro/effort reconciliation | 6 hours |
| Komayl Dbouk | Create domain, architecture and database decisions | Peer review student contract/privacy; equipment endpoints and tests | 6 hours |
| Hussein Chalhoub | Bootstrap after scope/design agreement and create agent context | Shared errors/validation; automated build/test pipeline | 6 hours |

Opening sequence: all members review the proposal and record decisions. Submit it before substantial implementation. Mostafa and Komayl then agree requirements and architecture while Hussein Chreif prepares testing/specification artifacts. Hussein Chalhoub establishes the application and shared conventions. Student/equipment implementation begins only after the relevant criteria, architecture and shared API contract are reviewed. The plan for each substantial agent change must be reviewed before implementation.

End-of-sprint demonstration: start using documented commands, register a student, retrieve/update the record, create/list/update/delete unreferenced equipment, show invalid and duplicate requests being rejected, and run relevant automated tests. Show the prepared borrowing specification. Review feedback and actual effort determine Sprint 2 capacity.

## Working agreement

Use the GitHub repository for code and pull requests, Jira for status and evidence links, and a brief synchronous team check-in at the beginning and end of each working day. Agree the call/chat channel in the first meeting and record it; no external team message has been sent by the agent. Raise blockers immediately in the related card. The instructor must have board and repository access; verify it rather than assume it.

Keep at most one implementation card per member actively in progress. Use To Do, In Progress, In Review and Done. Change status when work actually begins or meets the relevant criteria; creating or assigning a card does not mean it is complete. Share a short API/data contract before parallel implementation. Hussein Chalhoub owns shared foundation files; coordinate changes to shared configuration and exception handling.

Branch examples use codex/ELS-123-short-description for agent-created work and issue-linked names for other branches. Commit messages identify the Jira key and intent. Pull requests link requirements and acceptance evidence. Reviewers inspect the diff, test oracle, security/privacy, layer responsibilities, dependency changes and documentation. Preserve separate test and implementation commits for borrowing; do not squash away required chronology. Main must remain runnable after integration; expected red-phase results belong to the feature branch and evidence.

When members disagree, state the requirement, compare alternatives and consequences, and timebox discussion. The responsible owner proposes a decision, the reviewer checks it, and Hussein Chreif resolves product scope. Record important architecture decisions and unresolved concerns. Instructor constraints take precedence over team convenience.

## Definition of Ready

A card has a clear purpose, named owner and reviewer, observable acceptance criteria, relevant requirement/spec links, dependencies, estimate and known assumptions. A substantial feature also has a human-reviewed agent plan and proposed tests before implementation. Resolve ambiguous business rules instead of allowing an agent to invent them.

## Definition of Done

Acceptance criteria have been checked against observed results; relevant tests pass; a peer has reviewed significant changes and corrected issues; implementation follows the agreed architecture; API/docs/diagrams/context are current; no secrets or real private data are included; and the card links commits/PRs, results and evidence. Actual effort includes prompting, review, corrections, tests, documentation and interruptions. Report unresolved limitations honestly. Done never means only that an agent generated code.

## Evidence and reporting

Create docs/requirements, docs/analysis, docs/design, docs/decisions, docs/testing and docs/deployment as their artifacts are developed. Keep reports and ai-log for quality, sprint and selected agent evidence. The Jira backlog snapshot records local planning IDs, Jira keys, estimates, owners, reviewers, dependencies and acceptance criteria. Jira remains authoritative for subsequent progress; update the snapshot at milestones if needed.

For each meaningful AI interaction record task/date/tool/model where known, prompt/context, agent interpretation, proposed files/commands/plan, human decisions, changes, tests/results, requested corrections, final acceptance decision and lesson. Initial planning artifacts are AI-assisted and require team review. Do not invent human approvals, test runs, interviews or actual hours.

Subject/ and the original assignment remain local untracked inputs. Do not add them to .gitignore, stage them or copy their contents into submission artifacts. Stage only intended project files explicitly and inspect the staged file list before every commit. The prior GL-Labs repository is a read-only reference for practices; its results do not satisfy this project's evidence requirements.

## Remaining release windows

Sprint 2 runs Two days and implements the lending lifecycle, including red/green borrowing evidence and real database concurrency. Sprint 3 runs Two days and completes measured quality improvements. Sprint 4 runs Two days and verifies deployment, acceptance, documentation and presentation. The submission buffer is reserved for blocker fixes and submission. Future sprint estimates are provisional until refined; no optional feature may displace mandatory evidence or final verification.

Hussein Chreif coordinates final submission. Before it, all four rehearse explaining a requirement, architecture choice, test oracle, corrected AI result, quality improvement and personal contribution. Confirm the exact submission cut-off with the course instructions and retain the actual submission receipt.

## Sprint 1 card index

| Card | Work | Owner | Reviewer | Estimate |
| --- | --- | --- | --- | --- |
| [ELS-20](https://equipment-lending-system.atlassian.net/browse/ELS-20) | Review proposal and confirm team working agreement | Hussein Chreif | Hussein Chalhoub | 2h |
| [ELS-21](https://equipment-lending-system.atlassian.net/browse/ELS-21) | Define requirements and observable acceptance criteria | Mostafa Younes | Komayl Dbouk | 2h |
| [ELS-22](https://equipment-lending-system.atlassian.net/browse/ELS-22) | Model domain architecture and database decisions | Komayl Dbouk | Hussein Chreif | 2h |
| [ELS-23](https://equipment-lending-system.atlassian.net/browse/ELS-23) | Bootstrap build database and agent context | Hussein Chalhoub | Mostafa Younes | 3h |
| [ELS-29](https://equipment-lending-system.atlassian.net/browse/ELS-29) | Register retrieve and update student records | Mostafa Younes | Komayl Dbouk | 3h |
| [ELS-26](https://equipment-lending-system.atlassian.net/browse/ELS-26) | Create list update and delete equipment | Komayl Dbouk | Hussein Chreif | 3h |
| [ELS-24](https://equipment-lending-system.atlassian.net/browse/ELS-24) | Define testing strategy GQM goals and evidence map | Hussein Chreif | Hussein Chalhoub | 2h |
| [ELS-25](https://equipment-lending-system.atlassian.net/browse/ELS-25) | Implement shared validation and API error contract | Hussein Chalhoub | Mostafa Younes | 2h |
| [ELS-27](https://equipment-lending-system.atlassian.net/browse/ELS-27) | Specify borrowing feature and review agent plan | Hussein Chreif | Hussein Chalhoub | 2h |
| [ELS-28](https://equipment-lending-system.atlassian.net/browse/ELS-28) | Peer review student contract and domain privacy | Komayl Dbouk | Hussein Chreif | 1h |
| [ELS-30](https://equipment-lending-system.atlassian.net/browse/ELS-30) | Add automated build and test pipeline | Hussein Chalhoub | Mostafa Younes | 1h |
| [ELS-31](https://equipment-lending-system.atlassian.net/browse/ELS-31) | Run Sprint 1 review retrospective and effort reconciliation | Mostafa Younes | Komayl Dbouk | 1h |

The full proposal is also available in [ELS-20](https://equipment-lending-system.atlassian.net/browse/ELS-20) for team review before the local files are committed and pushed. All 31 work cards have original time estimates and named assignees; 58 blocking relationships represent the initial dependency plan. The project uses New Feature cards for product stories because Story is not an available issue type in the current Jira scheme. Estimates use Jira original time tracking in hours; the new board currently retains its default Story Points estimation setting. In Review is an issue status grouped under the board's In Progress column.

The [Sprint 1 artifact index](sprint-1-artifacts.md) records the documentation package prepared for Sprint 1. The [progress and review preparation](../reports/sprint-1/progress-review-and-retrospective.md) distinguishes prepared documents from product demonstrations, human sign-offs and actual effort still to be recorded. The user requested only Sprint 1 non-code cards be closed; later-sprint cards and the planning epic were restored to To Do.
