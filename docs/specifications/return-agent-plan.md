# ELS-34 return implementation plan

Owner: Mostafa Younes. Peer reviewer: Komayl Dbouk.

Baseline: fetched origin/main at merge 6cf5e8f. Existing local branches and work are preserved. Read AGENTS.md, the API contract/OpenAPI, requirements, borrowing specification, existing borrowing tests/service, equipment locking and guarded PostgreSQL setup.

1. Add ReturnWrite with only required returnDate using StrictLoanDateDeserializer, and a separate LoanReturnController/LoanReturnService. Reuse the exact six-field Loan DTO and shared errors; preserve borrowing handlers and tests.
2. In a service-owned transaction, select the immutable equipment ID without loading the loan, lock equipment, then lock/read the loan. Validate date order and returned state under lock, update only returnDate, flush and complete commit before returning success. Preserve history and derived availability. Do not add computed entity ID getters.
3. Capture failing return contract tests before implementation. Test equal/later/earlier dates, missing/duplicate/concurrent returns, strict IDs/body/types/dates including expanded years, rollback at flush and commit, lock order/state recheck and borrow-return-reborrow on isolated PostgreSQL.
4. Run focused tests and mvnw.cmd -Pdatabase-test verify, record actual results in evidence/traceability/AI log, commit with ELS-34, push and create a first-person PR into main requesting komayl-dbouk. Integrate newer main and rerun checks if ELS-35 merges first.

Human decision: Mostafa explicitly replied **Approve the plan** in this chat before substantial implementation. The approval and implementation start are recorded in ELS-34 Jira comment 10065. The readiness handoff in comment 10062 was not treated as approval. Implementation peer review remains pending.

Coordination: ELS-35 comment 10064 tells Chalhoub the planned shared additions (equipment-ID projection, pessimistic loan lookup, mapped relationship accessors and return-date mutation), separate return files and preserved borrowing behavior.
