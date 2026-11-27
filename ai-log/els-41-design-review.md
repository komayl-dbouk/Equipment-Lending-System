# ELS-41 Design Review and Corrections

| Field | Record |
| --- | --- |
| Owner / reviewer | Hussein Chreif / Hussein Chalhoub |
| Tool | Codex; exact model metadata not recorded |
| Request | Complete the assigned substantial design review, verify important findings, make approved focused corrections and deliver a reviewed PR. |
| Baseline / branch | Main bdbbd3e; ELS-41-verified-design-corrections; clean working tree apart from local Subject input. |
| Inspection | Feature controllers/DTOs/deserializers, services/transactions, repositories/entities, shared errors/clock, Flyway constraints, safety guard, specifications, API/design and relevant tests. |
| Verified findings | Equipment listing issues 2 statements for one item and 25 for 24 items; unrelated name-index failure containing a known constraint name in its value is misreported as inventory conflict on POST and PUT. |
| Pre-change evidence | 12 guard tests passed; 8 new investigation cases, 5 assertion failures, zero errors/skips. Delivery source was unchanged for that run. |
| Human decision | Presented concrete evidence and a repository-projection/exact-metadata plan. Hussein Chreif explicitly approved both findings and the plan before production edits; Jira comment 10093. |
| Screening | Optional-null filters, constructor dependency references and internal JPA associations were not automatically treated as defects. Authentication/HTTPS stays ELS-46. Technical screening recommendations are not attributed as separate human decisions. |
| Implementation | dbb2b55 changes only equipment service/repository production code, adds isolated regressions and updates directly affected unit fixtures plus API/architecture/review notes. No new framework, migration or business rule. |
| Tests | Focused correction checks: 22 unit +8 integration passed. Final full database profile: 195 unit/controller +186 integration =381, zero failures/errors/skips, including a new genuine unique-index race. Separate verify:195 passed. |
| Packaged validation | Implementation commit dbb2b55:83 HTTP requests/626 assertions, independent SQL snapshots and three coordinated lending races. No cloud result publication claimed. |
| Observed improvement | All six listing size/filter combinations use one statement; unrelated constraints return sanitized500; genuine inventory collision still returns one201/one409 and one row. Statement counts do not imply a measured latency SLA. |
| Corrections during execution | No failed production correction or weakened assertion. New investigation failures were expected pre-change behavioral evidence. The genuine-positive race case was added after the initial focused green run and passed in final full verification. |
| Preservation | Original BorrowingContractIT/red ancestry, dependencies and delivery migrations unchanged. Subject never staged, ignored, copied or modified. |
| Coordination | ELS-41 scope/approval recorded in Jira; ELS-39 advised of affected test files; ELS-42 waits for reviewed merge; ELS-43 retains the pre-change baseline. |
| Cleanup | Namespaced test index/rows confirmed absent; workflow-owned fixtures, task container/volumes and temporary credential file removed; app stopped by runner-owned PID. |
| Acceptance boundary | Human approval of findings/plan is recorded. Final Chalhoub implementation review, CI and merge are separate delivery gates; no actual effort is inferred. |

[Review/plan](../docs/reviews/els-41-design-review-and-plan.md) and [execution report](../reports/sprint-3/els-41-design-corrections.md) link the evidence. PR and remote CI observations are recorded in Jira after actual delivery; no self-approval or merge is authorized.

Final evidence validation initially used the Windows default text encoding and could not read a UTF-8 quotation mark. Repeating the JSON/link checks with explicit UTF-8 passed; no application or test assertion changed.
