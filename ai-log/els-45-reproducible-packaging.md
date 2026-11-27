# ELS-45 AI Work Log

Card: ELS-45 — Package application and verify clean setup

## Work performed

1. Read `AGENTS.md`, started from current `origin/main`, created `ELS-45-reproducible-packaging`, and moved ELS-45 to In Progress.
2. Inspected the existing Compose/test setup and confirmed no Dockerfile or Docker ignore rules existed.
3. Added a runtime-only Docker image. An initial multi-stage image approach was rejected after the Maven-wrapper invocation in the builder image failed due to its `/root/.m2` environment behavior. The final documented approach deliberately uses the exact JAR produced by the required host Maven verification, avoiding an unverified duplicate Maven toolchain in the image build.
4. Added Compose packaging with an internal PostgreSQL service and a loopback-only application port, plus a persistent named volume for ordinary local shutdown/restart.
5. Added deterministic namespaced seed, health readiness, and guarded-reset helpers. Dates are calculated from the current day so valid loan dates remain repeatable.
6. Revised the local runbook, including process-scoped PowerShell execution-policy bypass for checked-in helper scripts, and prepared an ELS-46/ELS-47 authentication handoff without implementing authentication.
7. Ran both required Maven profiles against PostgreSQL, then repeated the complete workflow from a new detached Git worktree at `e78d7a3` with generated local-only secrets and distinct ports.

## Boundaries respected

- Did not modify, stage, copy, or inspect `Subject/`.
- Did not add authentication, change business rules, expose PostgreSQL, or publish the application beyond loopback.
- Did not self-approve or merge anything.
- Removed only named disposable Compose/test resources and the explicit temporary clean worktree after validation.

## Follow-up

Push the branch, create a pull request, request `younes285`, inspect the actual CI result, then add concrete PR/check evidence to Jira before moving ELS-45 to Code Review.

## Authorized review corrections

The project lead requested correction of reset failure handling, seed target/fixture identity, HTTP deadlines, Compose-compatible quoted credentials and stale AGENTS.md commands. Replaced unbound ID-only checkpoints with explicit disposable-project, checkout, running port/database and PostgreSQL cluster/database identity checks, exact fixture comparisons and an incomplete-seed checkpoint that blocks replayed writes. Reset now preserves its selected checkpoint when Docker fails. Health uses a monotonic deadline and per-request cancellation.

The first real credential probe exposed Compose's escaped literal-dollar representation. Decoding resolved environment values once corrected the round trip; actual isolated PostgreSQL verification then passed with spaces, # and a literal dollar in the generated private password. Native Docker stderr is handled separately from its exit status for Windows PowerShell compatibility. Focused tests also caught PowerShell array enumeration in the new request wrapper; it was corrected rather than weakening fixture assertions. A test HTTP fixture was corrected to declare JSON Content-Type. An initial multi-file patch was rejected before writes due to duplicate target operations; file edits were split.

Thirteen helper regressions pass on Windows PowerShell. Both full Maven profiles passed: 195 unit/controller and 188 PostgreSQL integration tests, zero failures/errors/skips. No business production code, Java test oracle, dependency or migration changed. Added helper checks and real Docker build/seed/rerun/drift/reset/reseed verification to CI; this new remote step is not claimed passed until observed. The earlier local Docker rehearsal was interrupted after a very slow image download; its direct-JAR fallback is recorded separately from container proof. Execution-date headings were removed. Final validation/source identities and observed CI are recorded with the follow-up delivery evidence.

The atomic checkpoint regression exposed Windows PowerShell null-string binding in File.Replace; the explicit null-string value corrected it. The restricted local execution context then denied the OS replacement operation; the complete 13-check suite passed when rerun with that operation permitted. Assertions were retained. Local verification and isolated test resources were cleaned up; no image/container success is inferred from the earlier stalled download.
