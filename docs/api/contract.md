# Equipment Lending API Contract

Version 1. This is the planned interface for ELS-21, ELS-25 and feature implementation. [openapi.json](openapi.json) is the machine-readable interface source. Maintain this explanation, requirements and examples with it; a later generated runtime spec must be compared against this baseline rather than silently replace it.

## Conventions

Base URL for the local demonstration is http://localhost:8080. Prefix every data route with /api. Requests/responses use application/json, except successful equipment deletion has no response body. Delivery operations require staff Basic authentication; inject credentials through the client environment and never commit them. A 401 includes WWW-Authenticate. Remote access must use HTTPS.

Use positive integer IDs and strict ISO calendar dates. Loan date inputs must match YYYY-MM-DD with exactly four unsigned year digits and a valid calendar day; signed or expanded years return 400 VALIDATION_FAILED before persistence. StudentWrite is name and email; EquipmentWrite is inventoryCode, name, category and nullable optional description; LoanWrite is studentId, equipmentId, borrowingDate and dueDate; ReturnWrite is returnDate. No writable ID, availability or nested entity. Text properties require JSON strings (or null only where optional); numbers and booleans are rejected rather than coerced. Embedded U+0000 is rejected with 400 VALIDATION_FAILED before persistence. Required fields and maxima are defined in [requirements](../requirements/requirements.md) and OpenAPI.

Student responses contain id, name and normalized email. Equipment responses contain id, normalized inventoryCode, name, category, description and derived available. Loan responses contain id, studentId, equipmentId, borrowingDate, dueDate and nullable returnDate. Lists are arrays ordered by ascending ID.

## Operations and acceptance

| Method | Route | Success | Business and validation errors |
| --- | --- | --- | --- |
| POST | /api/students | 201 Student; Location points to /api/students/{id} | 400 invalid body; 409 duplicate normalized email |
| GET | /api/students/{id} | 200 Student | 400 invalid ID; 404 unknown student |
| PUT | /api/students/{id} | 200 Student; full replacement of editable fields | 400 invalid input; 404 missing; 409 email used by another student |
| POST | /api/equipment | 201 Equipment | 400 invalid body; 409 duplicate normalized code |
| GET | /api/equipment | 200 Equipment[] | 400 invalid boolean filter |
| PUT | /api/equipment/{id} | 200 Equipment; full replacement of editable fields | 400 invalid input; 404 missing; 409 code used by another item |
| DELETE | /api/equipment/{id} | 204 with no body | 400 invalid ID; 404 missing; 409 any loan references item |
| POST | /api/loans | 201 Loan | 400 invalid dates/body; 404 missing student/item; 409 active loan |
| GET | /api/loans | 200 Loan[] | 400 invalid filter IDs |
| POST | /api/loans/{id}/return | 200 updated Loan | 400 invalid date/body; 404 missing loan; 409 already returned |
| GET | /api/loans/overdue | 200 Loan[] | Invalid route/query handling follows shared validation |

Equipment available=true means no active loan; false means active loan; omission means all. Loan studentId and equipmentId filters combine with AND. Well-formed unmatched filters return an empty array. /overdue is a fixed route. All operations may return 401 for authentication and sanitized 500 on unexpected failures. No student-delete, loan-delete or record-edit loan route is exposed.

Validate authentication, request shape/types/fields/IDs, date order, existence and then state conflict. With multiple invalid conditions the client must fix the first reported condition and retry; validation-error order within a payload is not a public guarantee. A uniqueness race has the same conflict outcome as an ordinary duplicate.

## Error shape and codes

Error fields are timestamp (UTC instant), status (integer), code (stable identifier), message (safe summary), path and fieldErrors (object mapping field names to safe messages, empty when not field-specific). Do not echo passwords, tokens, SQL, stack traces or raw invalid private values.

| HTTP | Example code | Meaning |
| --- | --- | --- |
| 400 | VALIDATION_FAILED | Missing, malformed, unknown or invalid field/filter |
| 400 | INVALID_LOAN_DATES | dueDate is not after borrowingDate |
| 400 | INVALID_RETURN_DATE | returnDate precedes borrowingDate |
| 401 | UNAUTHORIZED | Missing or invalid staff authentication |
| 404 | STUDENT_NOT_FOUND, EQUIPMENT_NOT_FOUND, LOAN_NOT_FOUND | Required target absent |
| 409 | DUPLICATE_EMAIL, DUPLICATE_INVENTORY_CODE | Normalized identifier already used |
| 409 | EQUIPMENT_UNAVAILABLE | Active loan already exists |
| 409 | LOAN_ALREADY_RETURNED | Return recorded previously |
| 409 | EQUIPMENT_HAS_HISTORY | Deletion would destroy referenced inventory |
| 500 | INTERNAL_ERROR | Unexpected technical failure; details stay in protected logs |

## Synthetic examples

Register a borrower:

```json
{"name":"Test Student One","email":" Borrower.One@Example.test "}
```

Expected 201 body for an illustrative generated ID:

```json
{"id":1,"name":"Test Student One","email":"borrower.one@example.test"}
```

The creation response also includes `Location: /api/students/1`. `GET /api/students/1` returns 200 with the same three fields. To replace the editable fields, send `PUT /api/students/1`:

```json
{"name":" Updated Test Student ","email":" New.Borrower@Example.test "}
```

Expected 200 body, retaining the generated ID:

```json
{"id":1,"name":"Updated Test Student","email":"new.borrower@example.test"}
```

Both PUT fields are required. Omitting email returns 400 VALIDATION_FAILED and leaves the stored record unchanged; providing an id or loans property also returns 400. JSON name/email values must be strings, not numbers or booleans. Keeping this student's own email with different case/padding succeeds; using another student's normalized email returns 409 DUPLICATE_EMAIL and preserves both records. Missing positive IDs return 404 STUDENT_NOT_FOUND; malformed/nonpositive IDs return 400 VALIDATION_FAILED. Raw maxima include padding, even when trimming would shorten the value. These remain expected examples, not captured HTTP traffic. See [ELS-29 tests and execution evidence](../testing/els-29-student-management.md) for actual verification limits.

Create equipment with {"inventoryCode":"cam-001","name":"Demo Camera","category":"Camera","description":null}. Expected normalized inventoryCode is CAM-001 and available is true.

Borrow using studentId and equipmentId plus ISO-formatted borrowingDate and a later dueDate. Expected 201 Loan has the supplied references and dates, an illustrative generated ID and returnDate:null. A competing request returns 409 EQUIPMENT_UNAVAILABLE in the documented error shape, including a runtime-generated UTC timestamp.

Return with returnDate on or after borrowingDate and expect 200 Loan with that returnDate. A second return returns 409 LOAN_ALREADY_RETURNED. For overdue scenarios use a fixed test clock: an unreturned loan due yesterday is overdue, while one due today and any returned loan are excluded.

These are expected examples, not captured traffic. Generated IDs are illustrative. The API provides no idempotency key: after a network timeout inspect matching loan history before deciding whether to resubmit; never blindly retry a state-changing POST. Availability lookup is advisory; borrowing enforces the actual invariant.
