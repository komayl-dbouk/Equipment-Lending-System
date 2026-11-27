# Equipment Lending API Contract

This document describes the behavior implemented through ELS-46 for ELS-29, ELS-26, ELS-33,
ELS-34, ELS-35 and the delivery access boundary. [openapi.json](openapi.json) is the machine-readable companion. The
requirements remain authoritative for release scope; this contract records what the current
application actually exposes.

## Runtime and delivery boundary

The local base URL is `http://localhost:8080`; every data route starts with `/api` and uses
JSON, except a successful equipment deletion, which has no response body. Every `/api/**`
operation requires the single staff HTTP Basic credential supplied through
`ELS_STAFF_USERNAME` and `ELS_STAFF_PASSWORD`. Missing or invalid credentials return the same
six-field `401 UNAUTHORIZED` body and do not enter a controller. The application refuses to
start when either credential setting is missing or blank. The exact public exception is
`/actuator/health`, which returns sanitized status without component details.

The server is stateless and does not create a login session or cookie. CSRF protection is not
used for the present CLI/API delivery, which accepts JSON writes. Stateless Basic authentication
alone does not prevent browser CSRF; browser delivery requires a coordinated CSRF/origin review. Basic credentials are encoded, not encrypted: loopback HTTP is only a local
demonstration boundary. A remote deployment must provide HTTPS before sending the credential;
this repository does not terminate or verify remote TLS. See the [staff access boundary](../deployment/staff-access.md).

Requests use server-generated positive 64-bit IDs. Loan date inputs accept only JSON strings
matching `YYYY-MM-DD` with exactly four unsigned year digits and a valid calendar day. Signed,
expanded, impossible and non-string dates return `400 VALIDATION_FAILED`. Error timestamps
are UTC instants; the overdue boundary uses the injected application clock in `Asia/Beirut`.

Unknown write properties are rejected. Text properties must be JSON strings, except that
equipment `description` may be null or omitted. Numbers and booleans are not coerced to text.
Embedded U+0000 is rejected. Raw request strings must satisfy their maximum lengths before
normalization.

## Endpoint inventory

| Method | Route | Implemented success | Implemented client failures |
| --- | --- | --- | --- |
| `POST` | `/api/students` | `201 Student`; relative `Location` header | `400 VALIDATION_FAILED`; `409 DUPLICATE_EMAIL` |
| `GET` | `/api/students/{id}` | `200 Student` | `400 VALIDATION_FAILED`; `404 STUDENT_NOT_FOUND` |
| `PUT` | `/api/students/{id}` | `200 Student`; full editable-field replacement | `400 VALIDATION_FAILED`; `404 STUDENT_NOT_FOUND`; `409 DUPLICATE_EMAIL` |
| `POST` | `/api/equipment` | `201 Equipment` | `400 VALIDATION_FAILED`; `409 DUPLICATE_INVENTORY_CODE` |
| `GET` | `/api/equipment` | `200 Equipment[]` | `400 VALIDATION_FAILED` for an invalid `available` value |
| `PUT` | `/api/equipment/{id}` | `200 Equipment`; full editable-field replacement | `400 VALIDATION_FAILED`; `404 EQUIPMENT_NOT_FOUND`; `409 DUPLICATE_INVENTORY_CODE` |
| `DELETE` | `/api/equipment/{id}` | `204`, empty body | `400 VALIDATION_FAILED`; `404 EQUIPMENT_NOT_FOUND`; `409 EQUIPMENT_HAS_HISTORY` |
| `POST` | `/api/loans` | `201 Loan` after commit | `400 VALIDATION_FAILED` or `INVALID_LOAN_DATES`; `404 STUDENT_NOT_FOUND` or `EQUIPMENT_NOT_FOUND`; `409 EQUIPMENT_UNAVAILABLE` |
| `GET` | `/api/loans` | `200 Loan[]` | `400 VALIDATION_FAILED` for an invalid filter ID |
| `POST` | `/api/loans/{id}/return` | `200 Loan` after commit | `400 VALIDATION_FAILED` or `INVALID_RETURN_DATE`; `404 LOAN_NOT_FOUND`; `409 LOAN_ALREADY_RETURNED` |
| `GET` | `/api/loans/overdue` | `200 Loan[]` | No defined client-error input |

All operations may return a sanitized `500 INTERNAL_ERROR` for an unexpected failure. There
is no student list/delete endpoint, direct equipment/loan retrieval endpoint, loan edit/delete
endpoint or stored availability field.

Every operation in the table may instead return `401 UNAUTHORIZED` with message
`Staff credentials required` before request validation or business processing.

## Request and response fields

| Shape | Fields and rules |
| --- | --- |
| `StudentWrite` | Required `name` string, raw maximum 120; required `email` string, raw maximum 254. Both are trimmed; email is lowercased with `Locale.ROOT`, validated after normalization and unique. |
| `Student` | Exactly `id`, normalized `name`, normalized `email`. |
| `EquipmentWrite` | Required strings `inventoryCode` (50), `name` (120), `category` (60); optional nullable `description` (1000). Strings are trimmed; inventory code is uppercased with `Locale.ROOT`. Omitted description becomes null on create or replacement update. |
| `Equipment` | Exactly `id`, normalized editable fields, nullable `description`, and derived boolean `available`. |
| `LoanWrite` | Required integer `studentId` and `equipmentId`, both positive and within signed 64-bit range; required strict dates `borrowingDate` and `dueDate`; due date must be later. |
| `ReturnWrite` | Exactly one required strict date, `returnDate`; it must be on or after the stored borrowing date. No future-date restriction exists. |
| `Loan` | Exactly `id`, `studentId`, `equipmentId`, `borrowingDate`, `dueDate`, and nullable `returnDate`. References and original borrowing/due dates are immutable through the API. |

List results are ordered by ascending generated ID. `available=true` selects items without an
active loan; `available=false` selects items with an active loan; omission selects all items.
Only the lowercase strings `true` and `false` are accepted. Loan `studentId` and `equipmentId`
filters are optional and combine with AND. A syntactically valid unmatched filter returns
`200 []`; filters do not require the referenced row to exist. The overdue list contains only
unreturned loans whose due date is strictly before the Beirut-local current date. A loan due
today and every returned loan are excluded.

## State, locking and history

Borrowing validates date order, then performs the student lookup, pessimistic equipment lock,
active-loan check and insert in a service-owned transaction. The PostgreSQL partial unique
index on active loans is the final invariant. Return first reads the loan's immutable equipment
reference, then locks equipment before locking and rereading the loan. It changes only
`returnDate`. A second return cannot overwrite the first. Equipment deletion also locks the
equipment row and rejects every active or returned history reference.

Availability is computed from active-loan existence. It is a point-in-time view, not a
reservation. A successful return makes the item available when no other active loan exists;
reborrowing inserts a new loan and preserves the returned row.

Equipment lists derive availability and filter results in a single repository query; inventory
size does not add one database round trip per item. Database conflicts are classified by exact
constraint name and SQLSTATE metadata. An unrelated persistence failure remains a sanitized
`500 INTERNAL_ERROR`, even when its message contains a known business-constraint name.

## Error body

Every handled error uses the exact shape below. `fieldErrors` maps invalid field names to safe
messages and is empty for non-field-specific errors. Responses never echo credentials, SQL,
stack traces or rejected private values.

```json
{
  "timestamp": "2030-01-10T08:30:00Z",
  "status": 409,
  "code": "EQUIPMENT_UNAVAILABLE",
  "message": "Equipment is already on loan",
  "path": "/api/loans",
  "fieldErrors": {}
}
```

## Reproducible synthetic workflow

[examples.ps1](examples.ps1) is the runnable example. It generates unique synthetic identifiers,
captures every server-generated ID and asserts the relevant status, business code, normalized
value, filter result and state transition. It never assumes an empty database or ID `1`.

Prerequisites:

1. Configure and start PostgreSQL and the application using
   [local setup](../deployment/local-setup.md). Use an isolated disposable database because the
   API intentionally has no student/loan deletion endpoints.
2. Set `ELS_STAFF_USERNAME` and `ELS_STAFF_PASSWORD` privately in the process environment.
   Leave `ELS_API_BASE_URL` unset for `http://localhost:8080`, or set it to another loopback base
   URL. Do not put credentials in the script or command line.
3. From the repository root run:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\docs\api\examples.ps1
```

`-ExecutionPolicy Bypass` applies only to this child PowerShell process; it does not change the
machine or user execution policy.

The script covers registration, retrieval and update; equipment creation and availability;
borrowing and competing-borrow rejection; combined history filters; overdue inclusion;
return-once behavior; restored availability; reborrowing with retained history; and protected
equipment deletion. A final `PASS` line means those HTTP assertions executed successfully.
JSON examples embedded in OpenAPI are illustrative only; they are not execution evidence.
