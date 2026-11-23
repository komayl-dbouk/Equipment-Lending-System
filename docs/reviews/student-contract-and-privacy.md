# Student Contract and Privacy Review

Prepared for ELS-28. Assigned owner: Komayl Dbouk; assigned peer reviewer: Hussein Chreif. The review below was performed by Codex against the newly prepared requirements, contract and models. It does not attribute review decisions to either student.

## Scope and method

Compare FR01, US01, StudentWrite/Student response, error semantics and the conceptual/persistence model. Check unnecessary personal data, uniqueness, updates, ambiguous limits and behavior under concurrency. This is a contract review before student implementation, not a review of application code.

## Findings and corrections applied

| ID | Concern and evidence | Assessment | Correction in current artifacts | Human decision |
| --- | --- | --- | --- | --- |
| REV01 | "University email" did not define an allowed domain | Important ambiguity | Valid email syntax with normalization; no invented institutional domain restriction | Not recorded |
| REV02 | Case/whitespace differences could produce duplicate borrowers | Important integrity concern | Trim/lowercase with Locale.ROOT, uniqueness in database, normalized duplicates return 409 | Not recorded |
| REV03 | Updating a record with its own email could be confused with another student's duplicate | Important oracle gap | Same-record email remains allowed; another record's email produces 409 | Not recorded |
| REV04 | Raw email format validation before trimming would reject a documented padded address | Important contract mismatch | Document trim/lowercase before syntax validation; implementation must validate normalized email rather than blindly apply a raw-input email validator | Not recorded |
| REV05 | No maximum lengths and inconsistent normalization order | Important implementation ambiguity | Publish raw maxima in requirements/OpenAPI; trim then reject blank values; unique normalized storage | Not recorded |
| REV06 | Entity serialization could reveal loan relationships or unrelated fields | Important privacy/design concern | Student response limited to id/name/email; loan response uses IDs; reject unknown write fields | Not recorded |
| REV07 | Deletion could remove borrower history | Important integrity concern | Do not expose student deletion; use restrictive loan references; equipment deletion rejects any history | Not recorded |
| REV08 | PUT behavior and optional fields were unclear | Minor consistency concern | Student PUT replaces editable fields; required fields must be present; generated ID remains immutable | Not recorded |

The corrected artifacts are [requirements](../requirements/requirements.md), [contract](../api/contract.md), [OpenAPI](../api/openapi.json), [architecture](../design/architecture.md) and ADR 003. No new personal field, authentication role or student portal was introduced.

## Proposed verification

TC01â€“03 cover normal create/retrieve/update, invalid data and duplicates. TC24 adds raw length limits, whitespace normalization, unknown-property rejection and self-email update. Verify at the database layer that two equivalent normalized emails cannot both be persisted. Inspect logs/error payloads for unnecessary personal values. No execution results exist yet.

## Human review record

Komayl and Hussein should record their actual review date, accepted/modified/rejected decisions and reasons against these findings. The table records genuine agent findings and artifact corrections; it is not a completed human sign-off or the later ELS-41 implementation review.
