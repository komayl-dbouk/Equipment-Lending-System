# ELS-49 live workflow

Use this after setup and the 14-assertion probe in the [runbook](els-49-demo-runbook.md), in the same private environment. This deliberately changes synthetic fixture history. The seed/rerun check's exact-state guarantee remains unchanged.

```powershell
.\tools\demo\els49-live-workflow.ps1 -ProjectName els-demo-els49 -EnvFile .env -BaseUrl http://127.0.0.1:8080 -PauseBetweenSteps
```

The script verifies target ownership and the full seed before mutation. It records `rehearsal-mutating` before the first write; any interruption leaves a checkpoint that refuses ordinary seed reruns. Success records `rehearsal-complete`. Neither state means a human rehearsal happened. After inspecting the owned resources, use the runbook's explicitly confirmed reset/restart/seed steps. Never edit or delete a checkpoint to bypass the guard.

| Step | Presenter action | Observable oracle |
| --- | --- | --- |
| Debugging | Chreif asks why the active camera cannot be borrowed; Komayl traces service and index. | `409 EQUIPMENT_UNAVAILABLE`; exact committed SQL loan snapshot unchanged. |
| Return | Younes returns the original camera loan. | `200`, original ID and exact return date; camera available. Second return: `409 LOAN_ALREADY_RETURNED`, unchanged SQL loan snapshot. |
| Reborrow | Komayl borrows the camera with synthetic historical dates. | `201`, new loan ID, exact borrower/item/dates, null return; camera unavailable. |
| Overdue | Chalhoub explains the current Beirut business day. | Exactly the new unreturned past-due loan; the returned past-due laptop is excluded. |
| History | Younes returns the new camera loan and queries combined student/item filters. | Overdue list empty; both distinct camera loan IDs retained with return dates. |
| Reset | Chalhoub explains why seed rerun refuses the mutated fixture. | Confirmed guarded reset removes only owned resources/checkpoint; fresh seed restores `1/2/2`. |

Historical dates demonstrate overdue under the normal clock; there is no SQL fixture rewrite or artificial production clock. Output never includes private context/authorization. Failure stops progression and preserves the checkpoint. The assertion scope is DTOs and rejected-write loan snapshots; ELS-47 owns exhaustive acceptance and concurrency evidence.

## Debugging exercise and questions

Ask before revealing the answer; record actual answers and corrections in the [rehearsal record](../../reports/sprint-4/els-49-team-rehearsal.json).

1. **Why did borrowing return 409?** Inspect code, target IDs, active history and availability. Trace `LoanService` locking/active check and the partial unique index. This expected conflict is a working guard; do not remove the index.
2. **What if two staff borrow together?** Transaction/lock ordering plus the database invariant. ELS-47 TC11 requires one winner and one active committed row. This sequential demonstration does not rerun that race.
3. **Why historical dates?** The contract permits them; due must follow borrowing. Only unreturned loans due strictly before the Beirut day are overdue; due today is excluded.
4. **Can a second return overwrite history?** No. Explain the specific conflict and before/after SQL loan-value comparison.
5. **Why retain the old row?** Reborrow inserts a new loan; return updates only the original return date. Restrictive foreign keys protect history.
6. **Does HTTP 200 prove persistence?** No. Explain independent committed SQL oracles, exact DTO values and injected rollback checks in ELS-47.
7. **What did AI get wrong?** Each member explains one sourced ELS-39/41/46 weakness, independent check and correction; do not claim exclusive authorship.
8. **What if health fails or the port is busy?** Stop, inspect only owned resources, use historical offline evidence explicitly labeled; never attach to another application or erase its volume.
9. **What remains for remote use?** Verified HTTPS and deployment/browser acceptance; loopback Basic access does not establish them.

Record symptom, hypothesis, inspected source/SQL, expected invariant, observed result and correction/retest. Do not disable assertions or change production code to manufacture a debugging demonstration.
