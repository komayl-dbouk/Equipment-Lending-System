# ADR 001 Layered Application

Status: selected for the proposed implementation baseline; human adoption to be recorded. Related card: ELS-22.

## Context and alternatives

Four students must deliver a small staff API by the submission deadline. Domain boundaries are students, equipment and loans, with transactions that span equipment and loan records. Alternatives are a single controller-heavy application, a layered single application, and separately deployed microservices.

## Decision and reasons

Use one Spring Boot application with feature packages and controller, DTO, service and repository responsibilities. Keep shared exception handling and configuration small. The single deployment reduces operational overhead; service boundaries keep business rules independently testable and avoid persistence details in controllers.

## Consequences and quality attributes

The design favors maintainability, testability and reproducible delivery. It needs explicit review of layer boundaries because package names alone do not enforce architecture. Some operations legitimately use repositories across features. There is one deployment and one database, not independent service scalability. More complex infrastructure would consume time without satisfying an additional MVP need.

## Verification and maintenance

Review source dependencies, DTO exposure and transaction placement in ELS-41. Update models if responsibilities change. ELS-23 will record supported pinned Java/Spring/Maven versions; this ADR does not assert a build exists.
