# ELS-46 Packaging Handoff

ELS-45 packages the current unauthenticated Spring Boot API for loopback-only local demonstration. ELS-46 owns staff authentication and must not treat the present packaging result as final protected-delivery acceptance.

When staff access is added, keep credentials private in `.env` and expose their names in `.env.example` only. The proposed names are `ELS_STAFF_USERNAME` and `ELS_STAFF_PASSWORD`; ELS-46 may select different names if it updates the application, Compose environment pass-through, API examples, and this handoff together. Do not add their values to seed data, logs, reports, committed examples, or Docker image layers.

ELS-45 treats `GET /actuator/health` on the loopback-bound application port as the local readiness probe. ELS-46 must explicitly decide whether that endpoint remains anonymous or requires staff access, then update the runbook and authenticated HTTP examples. All business API calls in the current seed/runbook are unauthenticated only because ELS-46 is not yet integrated.

ELS-47 should run the packaged workflow again after ELS-46 merges: reject an unauthenticated business request, complete the same synthetic workflow with staff credentials, confirm health policy, and retain the loopback-only application binding and non-published PostgreSQL boundary.

The guarded seed now requires `-ProjectName` and uses Compose/container/database identity before requests, with project-specific checkpoints. Preserve that preflight and its strict fixture checks while adding private authenticated-request support. The readiness helper has an overall HTTP cancellation deadline. Update `verify-package.ps1` and CI smoke verification alongside any authentication/health-policy change; do not disable target guards or security checks to keep packaging green.
