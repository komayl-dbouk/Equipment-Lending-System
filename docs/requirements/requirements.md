# Equipment Lending Requirements

Version 1. Jira: ELS-21. Responsible member: Mostafa Younes. Assigned reviewer: Komayl Dbouk. This implementation baseline expands the agreed proposal into observable behavior. The decisions below are prepared for team adoption; authorship and human review must be recorded by the actual participants.

## Vision scope and elicitation

University staff need one reliable record of equipment, borrowers and loans. We use the README workflow, assignment constraints and a staff-role scenario analysis as the initial elicitation method. The staff perspective is an explicit assumption, not evidence of a university interview. Students are data subjects and borrowers; custodians need history; the instructor needs reviewable project evidence.

The boundary is a staff-managed Java/Spring Boot REST API and relational database. Include student registration/retrieval/update, equipment management, borrowing, returns, availability, overdue and filtered history. Exclude student login, frontend, reservations, fines, notifications and external institutional integration. The demonstration uses synthetic data. Staff access is introduced by ELS-46 before delivery.

## Glossary

| Term | Meaning |
| --- | --- |
| Equipment item | One physical asset identified by a unique inventory code |
| Active loan | Loan with returnDate equal to null |
| Available | Equipment has no active loan; this is derived information |
| Overdue | Active loan with dueDate strictly before today's date in Asia/Beirut |
| Returned loan | Historical transaction with a recorded returnDate |
| Staff | Authorized operator of the API; students do not authenticate |
| Conflict | Validly shaped request rejected because of current stored state |

## Input and data conventions

IDs are server-generated positive 64-bit integers. JSON uses camelCase. Dates use ISO calendar dates YYYY-MM-DD and Java LocalDate, with an injected Clock in Asia/Beirut for "today"; error timestamps are UTC instants. Borrowing and return dates are supplied explicitly. No new future-date restriction or maximum loan duration is introduced.

Raw request strings must satisfy their maximum lengths before normalization. Trim leading/trailing whitespace from text; required fields must remain nonblank. Names have a maximum of 120 characters. Student email is required, at most 254 characters, trimmed and lowercased with Locale.ROOT, then checked for valid email syntax and uniqueness. No unverified university-domain allowlist is imposed. Inventory codes have a maximum of 50 characters, are trimmed and uppercased with Locale.ROOT, and are unique after normalization. Equipment name has a maximum of 120 characters and category 60; category is free text. Optional description is null or at most 1000 characters. Updates supply all required editable fields; an omitted description becomes null. Unknown write fields are rejected with 400 to expose contract mismatches.

Student and equipment IDs cannot be changed through an update. Loan studentId, equipmentId, borrowingDate and dueDate cannot be edited through the MVP. returnDate starts null and changes once through the return endpoint. Students cannot be deleted through the API. History references prevent deletion of equipment. These limits and normalization choices are new contract decisions documented in this baseline, not claims about existing code.

## Functional requirements and acceptance

All FR requirements are Must priority for the release and have high stability except additive field-length limits, which may change with a versioned decision. Protected delivery operations assume valid staff authentication; otherwise return 401 before executing business operations.

| ID | Staff need and acceptance criteria | Implementation card | Planned tests |
| --- | --- | --- | --- |
| FR01 | Register student with valid name/email: 201 and stored normalized record. Retrieve/update existing ID: 200. Missing ID: 404. Invalid/unknown fields: 400. Duplicate normalized email: 409; updating the same student with its own email succeeds | ELS-29 | TC01â€“TC03, TC24 |
| FR02 | Create equipment: 201 with unique normalized code; list/update: 200; missing update/delete target: 404; invalid payload: 400; duplicate normalized code: 409; delete unreferenced item: 204 with empty body | ELS-26 | TC04â€“TC06, TC24 |
| FR03 | Existing student and available equipment plus dueDate later than borrowingDate create exactly one loan with null returnDate: 201. Missing reference: 404. Invalid/equal/reversed dates: 400. Item already on loan: 409 | ELS-33 | TC07â€“TC10 |
| FR04 | Two competing valid requests for the same available item yield one 201 and one 409 under normal database operation and exactly one active row. No application instance can bypass the invariant | ELS-33 | TC11 |
| FR05 | Return existing active loan with returnDate on/after borrowingDate: 200 with updated loan; item becomes available. Earlier date: 400. Already returned: 409 without altering original date. Missing loan: 404. Item can be borrowed again with history retained | ELS-34 | TC12â€“TC15 |
| FR06 | Equipment available=true excludes active loans; available=false selects active-loan items; absent filter returns all. Overdue list includes active loans due before today and excludes due-today/returned loans. Loan filters studentId/equipmentId use AND when both supplied, positive ID syntax required, no matches yields [] with 200 | ELS-35 | TC16â€“TC18, TC25 |
| FR07 | Equipment referenced by any active or returned loan cannot be deleted: 409 with records preserved. Missing equipment: 404. Concurrent delete/borrow cannot leave an orphan. No student-delete or loan-delete endpoint is exposed | ELS-26, ELS-36 | TC19, TC20 |

List endpoints return JSON arrays sorted by ascending ID. No pagination is exposed for the small demonstration dataset. A syntactically valid filter ID with no matches returns [], even when no such student/item exists. Invalid boolean text, nonpositive/malformed IDs and malformed dates return 400. Direct entity GET is not added for equipment/loans; use the documented list operations for the MVP.

## Non functional requirements

| ID | Criterion and verification | Priority |
| --- | --- | --- |
| NFR01 Integrity | Foreign keys preserve references, unique normalized identifiers and one-active-loan rule hold, failed transactions leave no partial writes; verify real PostgreSQL concurrency and rollback tests | Must |
| NFR02 Reproducibility | A clean clone builds, runs full automated checks, starts and executes the principal workflow using documented pinned tools/configuration; retain commands and results | Must |
| NFR03 Access and privacy | Delivery data operations require staff credentials; invalid/missing credentials return 401; credentials stay in environment; synthetic records only in examples; response/log review finds no secrets or stack traces exposed to callers | Must |
| NFR04 Maintainability | Controllers use DTOs and delegate rules to services; services own transactions; repositories access data; peer review checks boundaries and final diagrams/contracts match source | Must |
| NFR05 Test quality | Meaningful success/error/boundary coverage for every core rule, service line coverage target at least 80 percent, branch coverage reported, generated tests audited and required quality experiments retained | Must |

Performance is not given an arbitrary unmeasured target. If workload requirements emerge, document the dataset, hardware, concurrency and measurable threshold before claiming compliance. Coverage is a team target, not evidence by itself.

## Stories and normal workflow

| Story | Who what and why | Criteria |
| --- | --- | --- |
| US01 | As staff I register and maintain students so loans use a known borrower | FR01 |
| US02 | As staff I maintain individual equipment items so the inventory is accurate | FR02, FR07 |
| US03 | As staff I borrow an available item so a student's transaction is recorded safely | FR03, FR04 |
| US04 | As staff I record return so availability is restored without losing history | FR05 |
| US05 | As staff I see availability, overdue loans and filtered history so I can manage follow-up | FR06 |
| US06 | As staff I use controlled access so borrower information is not publicly exposed | NFR03 |

Principal workflow: register borrower, add camera, create valid loan, reject competing borrowing, see unavailable item, record return, see available item, create another loan and query retained history. Each invalid operation must leave persistent state unchanged.

## Dependencies and change control

PostgreSQL is the delivery database; see ADR 002. No external service is required. ELS-23 pins the Java/Spring/Maven versions and prepares configuration. The API contract is [openapi.json](../api/openapi.json), with [human-readable conventions](../api/contract.md). Business behavior changes require updates here, to relevant scenarios/specification, to the contract and to Jira before implementation. Future decisions cannot retroactively establish earlier approval.

Version history: v1 establishes the initial baseline to fill missing Sprint 1 specification artifacts. Implementation and acceptance results remain pending.
