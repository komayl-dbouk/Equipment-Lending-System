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

The original execution above preceded PR publication. PR #22 subsequently existed and its original-head CI passed; later correction evidence is recorded below rather than rewriting that history.

## Review corrections

Reviewed baseline: `9eefe40ff28d5c003d86c6708750aca44acb7d51`. Correction implementation: `f5c6920`. The project lead authorized the five packaging corrections. No lending source, Java test assertion, migration or dependency changed.

| Reproduced concern | Correction | Targeted evidence |
| --- | --- | --- |
| Reset discarded state after failed Docker | Checked native exit status before checkpoint deletion; select a project-specific checkpoint; validate disposable project, checkout and volume scope | Fake Docker exit 17 preserves the exact checkpoint; successful reset removes only its selected checkpoint |
| ID-only seed state accepted a different borrower/inventory | Verify Compose project/checkout labels, actual loopback binding, effective DB URL/user and cluster/database fingerprint before HTTP writes; require an empty initial target; compare exact fixture fields/references/dates/state | Wrong URL/database/borrower cases are rejected; a valid rerun makes only reads; an interrupted checkpoint blocks write replay |
| Stalled HTTP response exceeded the readiness timeout | Monotonic overall deadline and request cancellation | A server accepting but not answering terminates with the declared deadline failure, with process-start allowance checked separately |
| Quoted/special-character credentials diverged between Compose and Maven | Use resolved Compose test environment and decode model-rendered literal dollars once; derive the actual test port | Real isolated PostgreSQL accepted a private generated password containing spaces, # and a literal dollar; matching regression covers repeated dollar signs |
| AGENTS.md retained host-DB/host-app commands incompatible with container-only delivery DB | Synchronize AGENTS.md/runbook with the test importer and Compose-hosted app; fail fast on native startup/build errors | Syntax, helper and runbook checks; full Java/PostgreSQL profiles below |

Seed checkpoints are written before mutation and replaced atomically after steps. They contain only synthetic fixture/target identity, no password. Custom state files stay inside the checkout target directory. Drift/incomplete runs fail closed and require inspection plus an explicitly confirmed reset of the disposable project. The original unbound checkpoint is not silently trusted.

## Corrected verification

- Windows PowerShell helper regressions: **13 passed**. These isolate Docker calls and use real loopback HTTP; they are not described as container tests.
- Standard `mvnw.cmd -o --batch-mode --no-transfer-progress verify`: **195 passed**, zero failures/errors/skips.
- `mvnw.cmd -o --batch-mode --no-transfer-progress -Pdatabase-test verify`: **195 unit/controller + 188 PostgreSQL integration passed**, zero failures/errors/skips, on PostgreSQL 17.5 using the Compose-backed special-character credential import.
- PowerShell parse checks, documentation local links, explicit staged scope and `git diff --check` passed. Removed execution-date headings.
- The independent review's clean checkout and direct-JAR workflow on the original source passed, but its Docker runtime-image download stalled. That local container attempt was canceled and is not reported as a passing container rehearsal.

The correction adds CI helper regressions plus a real runtime image build and `tools/demo/verify-package.ps1`. That smoke check uses a clean runner checkout, generated private settings, an unused Compose project and isolated volume; verifies non-root execution, actual loopback/internal-DB boundaries and effective database identity; runs seed/read-only rerun, rejects fixture drift, resets/reseeds and cleans up only owned resources. Observed CI results are added after the run completes. ELS-46 authentication and ELS-47 final integrated acceptance remain separate gates; Mostafa remains the human reviewer.

## Observed container verification and delivery

[CI on correction implementation f5c6920 passed](https://github.com/komayl-dbouk/Equipment-Lending-System/actions/runs/46058992205/job/140708368667). The exact run completed Java/PostgreSQL verification, all 13 helper regressions under Linux pwsh, the delivery runtime image build and the real packaged Docker demonstration successfully. This is the container evidence for the corrections; the earlier interrupted local image download remains a limitation of that local attempt.

The real smoke check asserts the expected persisted counts (one student, two equipment items, two loans), a read-only rerun, rejection of changed borrower identity, non-root execution, live loopback/internal-DB/target identity, and successful reset/reseed. Its finalizer cleans only the unused task-owned project and private configuration. [Structured correction results](els-45-correction-results.json) retain implementation/file identities, exact scope and the observed CI URL.

The PR description and Jira record track the latest follow-up head after this evidence commit. Prior successful CI is not relabeled as a run on an unseen revision. Mostafa's human recheck, merge and ELS-45 Done remain pending; no self-approval or merge is performed.
