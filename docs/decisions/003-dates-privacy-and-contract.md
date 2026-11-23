# ADR 003 Dates Privacy and API Conventions

Status: proposed baseline decisions prepared for implementation review. Cards: ELS-21, ELS-25, ELS-27 and ELS-28.

## Context and alternatives

Independent agents need one date interpretation, normalization policy and error/response model. Timestamps introduce time-of-day complexity that the university lending workflow does not require. Unrestricted entity serialization exposes relationships and can leak unnecessary data.

## Decision and reasons

Use LocalDate values serialized YYYY-MM-DD for lending and return dates. Compute today with an injected Clock in Asia/Beirut. An active loan due before today is overdue; due today is not. Use UTC instants for error timestamps.

Normalize emails by trimming/lowercasing with Locale.ROOT and inventory codes by trimming/uppercasing with Locale.ROOT. Do not infer a specific university email domain. Collect only borrower name and email. Write DTOs reject unknown fields; response DTOs expose IDs rather than whole JPA relationship graphs. Calendar and normalization choices become stable contract behavior and require corresponding tests.

Use the existing /api endpoint shape. POST creation returns 201, successful GET/PUT/return returns 200, equipment deletion returns 204 with no body, invalid input returns 400, unknown record 404, business conflict 409 and unauthenticated delivery access 401. Unexpected technical errors are sanitized 500. These response semantics follow [HTTP Semantics](https://www.rfc-editor.org/rfc/rfc9110.html).

Use one staff credential from environment configuration for the release. HTTP Basic is sufficient within this small demonstration boundary with HTTPS for remote access. Local insecure transport is limited to localhost; credentials are never stored in requests/examples or committed. Authentication is a delivery task, not an invented existing feature.

## Consequences and verification

Calendar dates cannot represent hourly booking, which is outside scope. Global lowercase email comparison is a documented application policy. Limits and normalization must agree across DTOs, storage and examples. Verify date boundaries with a fixed Clock, Unicode/whitespace and duplicate normalization cases, rejection of unknown fields, correct empty responses and sanitized errors. No runtime compliance result is claimed here.
