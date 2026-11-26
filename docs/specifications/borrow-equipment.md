# Borrow Equipment Feature Specification

Version 1. Story US03; requirements FR03, FR04 and NFR01/03/04/05. Planning card ELS-27, test-first card ELS-32, implementation card ELS-33. Owner Hussein Chreif; implementation owner Komayl Dbouk; assigned specification reviewer Hussein Chalhoub.

## Purpose users and value

Staff record a loan for a registered student using an available physical item. The feature must prevent double lending even when two staff requests arrive together. It creates a historical transaction and changes derived availability without exposing persistence entities.

## Inputs interfaces and preconditions

POST /api/loans accepts required studentId, equipmentId (positive integers), borrowingDate and dueDate (ISO dates). No ID, returnDate or availability may be supplied. Staff authentication is required in delivery configuration. Student/item must exist, dueDate must be after borrowingDate and equipment must have no active loan. Empty/unknown/malformed fields fail validation.

Use the [requirements](../requirements/requirements.md), [API contract](../api/contract.md), [architecture](../design/architecture.md) and ADR 002. No external service or real student data is needed. ELS-23 supplies build/runtime and migrations; ELS-29/26 supply borrower/item infrastructure; ELS-25 supplies errors.

## Normal flow and postconditions

1. Authenticate and validate request shape, IDs and dates.
2. Verify the referenced student exists.
3. Start the service transaction, lock the equipment row and verify it exists.
4. Check active loan state after obtaining the lock.
5. Insert one Loan with the request's immutable references/dates and null returnDate; flush and commit.
6. Return 201 with Loan DTO only after commit.

The student/item records are preserved. The item now has one active loan and availability queries exclude it. No separate stored availability flag is updated. Service transaction coverage must include the locked state check and insert; the precise implementation may begin the transaction before student lookup.

## Alternative flows and errors

| Condition | Outcome | State |
| --- | --- | --- |
| Invalid/missing/unknown field or malformed ID/date | 400 VALIDATION_FAILED | No writes |
| dueDate equal to or before borrowingDate | 400 INVALID_LOAN_DATES | No writes |
| Missing student | 404 STUDENT_NOT_FOUND | No writes |
| Missing equipment | 404 EQUIPMENT_NOT_FOUND | No writes |
| Existing active loan | 409 EQUIPMENT_UNAVAILABLE | Existing transaction unchanged |
| Concurrent requests for one initially available item | One 201 and one 409 under normal database operation | Exactly one active loan |
| Technical database failure | Sanitized 500 INTERNAL_ERROR | Rollback; no claimed success |
| Missing/invalid delivery credentials | 401 UNAUTHORIZED | No business operation |

Do not add maximum duration, borrowing quotas, fines, reservations, idempotency keys, future-date restrictions or student accounts. Do not suppress a database error by returning a successful DTO. Only confirmed active-loan violations map to the business conflict.

## Acceptance criteria and test oracles

| ID | Given and when | Then | Test |
| --- | --- | --- | --- |
| BOR01 | Existing student and available item, valid later due date | 201, positive generated ID, exact references/dates, null returnDate, one stored active loan | TC07 |
| BOR02 | Either reference is missing | Correct 404 and zero new loans | TC08 |
| BOR03 | Due date equals or precedes borrowing date | 400 and zero new loans | TC09 |
| BOR04 | Item already has active loan | 409 and original loan unchanged | TC10 |
| BOR05 | Two coordinated independent requests contend for available item | Exactly one 201, one 409, one active database row | TC11 |
| BOR06 | Validation fails or transaction rolls back | No orphan/partial writes and item not incorrectly unavailable | TC20 |
| BOR07 | Returned historical loan exists for item | New loan allowed; historical row preserved | TC15 |
| BOR08 | Unknown payload field or missing delivery authentication | 400 or 401 respectively; zero new loans | TC24, TC21 |

Assertions must independently compare expected values, inspect persisted effects and count records. Mocked repository tests are useful for service behavior but cannot establish BOR05. Use PostgreSQL with separate connections/transactions and coordinated start. The date tests use fixed values, not the machine's current clock.

## Agent understanding and proposed plan

The agent understands that this is one atomic lending operation with database protection, not a check followed by an unrelated save. The detailed proposed plan and file/command boundaries are in [agent implementation plan](borrow-agent-plan.md). The plan is prepared; a human approval record has not been supplied.

## Completion and evidence

Before implementation preserve this specification in a commit, write tests and retain the expected red run/commit, then obtain human review of the minimum implementation plan. Preserve green implementation/run, any refactoring and full regression result in chronological commits. Compare BOR01â€“08 against observed results and record met/ambiguous/missed with corrections. Keep an explicit result column in the final record; all eight are currently not executed.

## ELS-33 specification comparison

This execution update supersedes the pending-approval and not-executed statements above. The ELS-33 instruction confirms Hussein Chreif's plan approval and Hussein Chalhoub's red-test approval in Jira; PR #8 records both review decisions. The focused BorrowingContractIT run passed 53/53 cases, and the complete PostgreSQL profile passed 264/264 tests with zero failures, errors or skips. See the [execution evidence](../../ai-log/els-33-atomic-borrowing.md).

| Criterion | Observed result | Status |
| --- | --- | --- |
| BOR01 | Exact six-field response and committed active row | Met |
| BOR02 | Missing reference returns specified 404 with no new loan | Met |
| BOR03 | Equal and reversed dates fail without writes | Met |
| BOR04 | Existing active loan remains unchanged on 409 | Met |
| BOR05 | Coordinated requests yield one 201, one 409 and one active row | Met |
| BOR06 | Validation and test-only database fault leave no partial write | Met |
| BOR07 | Returned history is preserved and reborrow succeeds | Met |
| BOR08 | Unknown/malformed fields return 400 without writes | Met for request shape; authentication remains ELS-46 |

## Prohibited modifications

Do not alter unrelated student/equipment behavior, approved public contracts, subject files, security boundaries or dependencies without a justified scope decision. Do not implement return/query features in this card. Do not squash away test-first history or invent human plan approval, test execution or actual effort.

## ELS-33 date-boundary review correction

The request transport format is exactly YYYY-MM-DD with four unsigned year digits and a valid calendar day. Signed/expanded years are rejected with 400 VALIDATION_FAILED before date ordering or persistence, preventing out-of-range PostgreSQL writes. This is input-format validation, not an added business restriction on future borrowing dates or maximum loan duration.

The added LoanDateValidationIT passes nine PostgreSQL cases across both date fields, including a valid year-9999 boundary and independent persisted-state checks. The original 53 BorrowingContractIT cases remain unchanged and pass. The final full suite passes 273 tests (172 unit/controller and 101 PostgreSQL), zero failures/errors/skips; live boundary checks pass 11 requests/11 assertions. [Correction evidence](../../reports/sprint-2/els-33-date-validation-results.json) supersedes the earlier 264-test count.
