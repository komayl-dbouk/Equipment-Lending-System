# Shared Validation and Errors

ELS-25 owner: Hussein Chreif. Peer review remains pending. `ApiExceptionHandler` provides the documented `ApiError` response for MVC validation/conversion errors, intentional domain errors and unexpected failures. Request DTOs must use Jakarta validation; MVC handler ID/filter parameters must use constraints such as `@Positive`. Keep email normalization and domain rules in the appropriate feature; no feature endpoint is introduced here.

Use `ResourceNotFoundException(code, safeMessage)` for a missing record and `BusinessConflictException(code, safeMessage)` for a confirmed business conflict. For intentional 400 date-rule failures use `ApiException(HttpStatus.BAD_REQUEST, code, safeMessage)`. Codes must match the API contract. Public messages must never contain rejected values, SQL or credentials. Unexpected exceptions return a generic 500; they are not automatically recast as 409. Classify database constraint failures in the service/transaction boundary before throwing an intentional API exception.

Unknown JSON write properties are rejected through application Jackson configuration. Tests use an isolated MVC fixture, the same strict deserialization option and real Jakarta validation, so they need neither PostgreSQL nor Docker. Field errors deliberately return a safe generic message rather than an annotation message that could interpolate a private value. Error paths exclude query strings. Return-value validation failures remain server errors.

Staff authentication remains ELS-46. This advice does not itself install authentication filters or promise to handle errors emitted before MVC; the security task must reuse the error shape at its entry point.
