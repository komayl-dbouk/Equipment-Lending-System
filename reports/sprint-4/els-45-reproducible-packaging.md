# ELS-45 Reproducible Packaging Evidence

Implementation commit: `e78d7a3` (`ELS-45 package local demonstration`)
Branch: `ELS-45-reproducible-packaging`

## Delivered scope

- Added a Java 17 runtime Docker image that runs the verified Spring Boot JAR as a non-root `spring` user.
- Added an application-plus-PostgreSQL 17.5 Compose stack. Only the application is bound to `127.0.0.1`; PostgreSQL has no host port.
- Kept the application and `compose.test.yaml` database-test resources separate.
- Added fixed-identity synthetic seed, health-wait, and guarded-reset scripts. The reset script accepts only `els-demo-*` project names and requires `-ConfirmReset`.
- Rewrote the local packaging runbook and prepared the ELS-46 authentication handoff at `docs/deployment/els-46-packaging-handoff.md`. No authentication behavior was implemented by ELS-45.

## Verification

All commands below completed successfully on Windows with Java 17 and Docker Desktop. Password values were supplied through ignored `.env` files and are intentionally omitted.

| Check | Actual result |
| --- | --- |
| `./mvnw.cmd --batch-mode --no-transfer-progress verify` | 195 tests; 0 failures, 0 errors, 0 skipped |
| `./mvnw.cmd --batch-mode --no-transfer-progress -Pdatabase-test verify` with isolated `els-tests` PostgreSQL | 195 unit/controller tests plus 188 PostgreSQL integration tests; 0 failures, 0 errors, 0 skipped |
| Compose configuration review | Application published only at `127.0.0.1:<ELS_APP_PORT>`; PostgreSQL is internal-only |
| Container process check | `uid=999(spring) gid=999(spring)` |
| Packaged HTTP workflow | `/actuator/health` reported `UP`; synthetic student, two equipment items, active loan and returned loan were created and verified over HTTP |
| Rerun | Seed verified saved state and made no write |
| Reset and rerun | Guarded reset removed only `els-demo-els45clean`; a fresh stack reached health `UP` and the seed recreated the workflow |

## Clean tracked checkout rehearsal

Created a disposable detached Git worktree at `e78d7a3`, copied only `.env.example` to a new private `.env`, generated separate local secrets, and selected non-conflicting loopback ports (`18045` for the application and `15453` for the test database). No source workspace, `target`, `.git`, existing `.env`, Docker volume, or `Subject/` content was copied into it.

The clean worktree completed both Maven verification commands, built the runtime image, started a new PostgreSQL volume, applied Flyway at startup, passed HTTP readiness, ran the seed and no-write rerun, confirmed loopback binding/non-root execution, and passed reset/reseed. Its named Docker projects and volume, temporary worktree, and test service were removed afterward.

No GitHub CI result is claimed here; CI must be inspected after the pull request is pushed.
