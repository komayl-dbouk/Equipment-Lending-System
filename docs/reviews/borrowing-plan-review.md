# Borrowing Specification and Plan Review

Prepared for ELS-27. Assigned owner: Hussein Chreif. Assigned reviewer: Hussein Chalhoub. All four members are intended participants in the human plan review. Codex prepared the specification, paraphrased its understanding and reviewed the following design risks.

## Agent review results

| Check | Finding and correction |
| --- | --- |
| Atomicity | A separate availability lookup plus save can race; specify one service transaction with item lock, state check, insert/flush and commit |
| Persistence invariant | A service check alone can be bypassed; add database uniqueness for active equipment loans and restrictive foreign keys |
| Lock order | Borrow/return/delete can otherwise contend inconsistently; establish equipment-first ordering and recheck loan state |
| Error mapping | Catching every database exception as "unavailable" hides technical faults; classify confirmed constraint causes outside the failed transaction |
| Test oracle | A sequential duplicate test does not prove concurrent safety; require coordinated independent transactions and database row-count assertions |
| Scope | Do not add quotas, fines, student accounts or reservation logic to borrowing |
| Chronology | Preserve specification commit, red test commit/output, reviewed minimum plan, green implementation, refactoring/regression evidence |
| Environment | Resolve exact files/commands against the actual ELS-23 skeleton; do not claim a wrapper or migration already exists |

These corrections are reflected in [the specification](../specifications/borrow-equipment.md), [the agent plan](../specifications/borrow-agent-plan.md) and [ADR 002](../decisions/002-database-and-concurrency.md).

## Remaining human review and execution

| Participant or step | Record |
| --- | --- |
| Hussein Chreif | Decision/iteration/corrections not supplied |
| Mostafa Younes | Decision/iteration/corrections not supplied |
| Komayl Dbouk | Decision/iteration/corrections not supplied |
| Hussein Chalhoub | Decision/iteration/corrections not supplied |
| Test-first commit and expected failure | Not executed |
| Implementation approval | Not recorded |
| Implementation and green results | Not executed |

This provides a concrete plan to review before implementation; the Jira Done status was an administrative closure requested by the user and does not replace these records.
