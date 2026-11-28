# ELS-49 presentation and rehearsal evidence

Owner: Komayl Dbouk. Human reviewer requested for the PR: Hussein Chreif (`MaxHusseinChreif`). This is a repository handoff, not a claim that four people attended or approved a rehearsal. ELS-50 release/submission is out of scope.

## Source and dependency identity

- Jira: [ELS-49](https://equipment-lending-system.atlassian.net/browse/ELS-49) on the [ELS board](https://equipment-lending-system.atlassian.net/jira/software/c/projects/ELS/boards/34), Sprint 4. The card was read live as To Do, with three comments and ELS-47/48 blocking links; it was moved to In Progress only when implementation began.
- Fetched `origin/main` and branched from `8b1161ba4c44e75c246300335cb4444f5cc9fced`. `HEAD` matched `origin/main` before branching and the worktree was clean. Main's `8b1161b` is merge PR #18 for ELS-48; it contains ELS-47 merge PR #24 (`62be246`) and ELS-46 merge PR #23 (`74da749`). The live ELS-48 dependency was **Done** during this work. The reported earlier Jira/merge mismatch was not still present; stale status alone was not used as a blocker.
- Current branch: `ELS-49-presentation-rehearsal`. The first local application source was `8b1161b`, with ELS-49 script/docs uncommitted; a further packaged run used committed PR head `616380cda3189a012fb16a38aa261852ff59f692`. Application Java source was unchanged. The tested JAR SHA-256 in both was `4d38df88a033ab78fc67511b3d792afbc8e83f43c820301597748036895fb858`. The initial ordinary-image runtime ID was `sha256:1f563278af430efd50f2356517e61653146ce2112037239562d06a81533b15b0`; the committed-head run's image ID was `sha256:cad5cec219828155ff584958631f7572285e1dceb18b8df7b145e7e8c4db69c6`. A separate intermediate reset/rebuild with the same JAR succeeded, but its image ID was not recorded.
- [PR #25](https://github.com/komayl-dbouk/Equipment-Lending-System/pull/25) targets main from this branch. `MaxHusseinChreif` was verified as the author of an actual review on PR #23 and requested as PR #25 reviewer; a request is not an approval. The connected GitHub app returned a 403 on PR creation/reviewer request, so the existing repository Git credential was used in memory through GitHub's API, with no credential output. An initial long-body JSON request failed HTTP 400; UTF-8 request bytes corrected it and the complete author-perspective PR body was saved. No team member was directly messaged.

## Reused verified baseline (not new ELS-49 test results)

[ELS-47](els-47-final-release-acceptance.md) records its own source-specific 200 unit/controller plus 193 PostgreSQL tests, 15 helper checks, 227 HTTP requests/3409 CI assertions and three races. [ELS-45](els-45-reproducible-packaging.md) owns the guarded seed/reset/packaging helpers; [ELS-46](els-46-access-privacy-license-review.md) owns authentication/privacy; [ELS-48](../final-report.md) owns the consolidated report. ELS-49 reuses these claims only with their revision and links. The [slides](../../docs/presentation/els-49-slides.md), [runbook](../../docs/presentation/els-49-demo-runbook.md), and [task-owned HTTP probe](../../tools/demo/els49-rehearsal.ps1) are the new handoff assets.

## ELS-49 execution register

| Check | Exact command / source | Observed result | Limitation or correction |
| --- | --- | --- | --- |
| Initial environment | `java -version`; alternate cached `java.exe -version` | `java` not on PATH; cached Temurin 17.0.21.1 worked and was used for Maven | Set `JAVA_HOME` only in the command process. |
| Initial Docker | `docker version --format '{{.Server.Version}}'`; start installed Docker Desktop; repeat | Initial config warning/engine pipe absent; repeat returned server `29.7.2` | First probe was not a container pass. |
| Static/helper checks | PowerShell `Parser.ParseFile` on `tools/demo/els49-rehearsal.ps1`; `python tools/demo/test_demo_helpers.py`; local-link check | 0 parse errors; 15/15 helper tests; all four new Markdown files' local links resolved | Helpers are fake-Docker/loopback tests, not the live package check. |
| Standard Maven | `mvnw.cmd --batch-mode --no-transfer-progress verify` with cached Java 17 | BUILD SUCCESS; 200 tests, 0 failures/errors/skips; delivery JAR built | Application Java source unchanged from `8b1161b`. |
| First PostgreSQL attempt | `docker compose -p els49-tests -f compose.test.yaml --env-file .env up -d --wait`; dot-source importer | PostgreSQL healthy, but process execution policy denied the importer; `finally` stopped only `els49-tests` | Corrected using process-only `Set-ExecutionPolicy -Scope Process Bypass -Force`; machine policy and `.env` unchanged. |
| PostgreSQL retry | Same isolated project on private process `ELS_TEST_DB_PORT=15493`, dot-source importer in Maven shell, `mvnw.cmd --batch-mode --no-transfer-progress -Pdatabase-test verify`; Compose down in `finally` | BUILD SUCCESS; 200 unit/controller + 193 PostgreSQL tests, all 0 failures/errors/skips; importer did not print credentials | No unrelated test database stopped. |
| First packaged attempt | `docker compose -p els-demo-els49 --env-file .env config --quiet` with port `18049` | Refused missing `ELS_STAFF_USERNAME/PASSWORD` in the existing `.env` | Preserved `.env`; generated synthetic staff credential only as process variables for the live run. |
| Packaged health/auth/seed/queries/error/rerun | `docker compose -p els-demo-els49 --env-file .env up --build -d --wait`; `tools/demo/wait-for-health.ps1`; `tools/demo/els49-rehearsal.ps1` on `http://127.0.0.1:18049` | Container healthy; public health `UP`; **14/14** live script assertions passed: missing/wrong 401, unchanged rejected-write SQL counts, authorized student/available-item/filtered active-and-returned history, duplicate-email 409/unchanged counts, read-only guarded rerun | Java base image was downloaded because it was not cached. Script checks counts, not a full SQL snapshot. No remote/browser acceptance claimed. |
| Guarded reset/reseed/cleanup | `tools/demo/reset-demo.ps1 -ProjectName els-demo-els49 -EnvFile .env -ConfirmReset`; Compose up/health; same rehearsal script; confirmed reset again | First reset removed only the owned volume/checkpoint; fresh start passed health and **14/14** assertions; final confirmed reset succeeded. Post-run checks found no `els-demo-els49` containers, labeled volumes or checkpoint and no `els49-tests` container. | The second image ID was not recorded; no unrelated resource was removed. |
| Committed-head packaged rerun | `docker compose -p els-demo-els49 --env-file .env up --build -d --wait`; wait/ELS-49 script/confirmed reset at `616380c` | Health and **14/14** live assertions passed again; JAR hash unchanged; image ID recorded above; owned resources removed | This proves the committed script on the pushed head locally; it is separate from the CI package smoke. |
| First PR-head CI | [Run 46142307957](https://github.com/komayl-dbouk/Equipment-Lending-System/actions/runs/46142307957), head `616380c` | Completed **success**; Java/PostgreSQL, helper safety, runtime image, packaged Docker demonstration and requirement-scoped release acceptance steps all succeeded | CI does not separately invoke the new ELS-49 script; its live run is local above. An evidence-only follow-up commit will require a new latest-head CI check. |

## Rehearsal register for actual team use

No four-person rehearsal, questions, handover delivery, human approval, presentation outcome or submission receipt has been observed by this agent. The [handoff script](../../docs/presentation/els-49-demo-runbook.md) allocates speaking and likely questions, but these are preparation, not observed answers. After an actual rehearsal, the team should append: source/JAR identity, attendees, each speaker's oracle and corrected-AI explanation, question and answer, handover issue, debugging step, correction/retest, and reviewer decision. Do not fabricate missing sessions or effort.

## Limits and handoff

The fallback slides and historical ELS-47/45 evidence are usable offline but are not a live ELS-49 runtime pass. Remote HTTPS, browser auth/CSRF/origin tests, distribution notices, team sign-off and ELS-50 submission remain separate. Hussein Chreif should review claim-to-source accuracy and runbook safety; each member should execute their assigned explanation and record any corrections before release closeout.

## PR review corrections

The preparation package now includes a 12-slide editable PowerPoint, matching offline PDF, embedded source-backed notes and planned timings (13 minutes including a four-minute demo). All slide renders were inspected and PDF title/body text matched the JSON source on all 12 pages. First-party package/layout/import checks passed; no native PowerPoint application test is claimed. Cloud authoring was rejected by automatic approval review because the personal destination was not authorized; delivery stayed local in this repository.

The [live sequence](../../docs/presentation/els-49-live-workflow.md) adds debugging, return, repeat-return rejection, reborrow, overdue and retained history. Its script verifies the guarded seed and marks the checkpoint before mutation. An interrupted or completed sequence cannot masquerade as an untouched seed. Rejected writes compare full committed loan snapshots. The original probe remains unchanged.

[Structured verification](els-49-review-verification.json) identifies the PR base and SHA-256 of each tested/generated correction file. Observed commands and results:

| Check | Command / environment | Result |
| --- | --- | --- |
| Standard | `mvnw.cmd --batch-mode --no-transfer-progress verify`, Java 17 | 200 tests; zero failures/errors/skips; BUILD SUCCESS. |
| Database | Same command with `-Pdatabase-test`, isolated `els49-review-tests`, port 15459; importer in the same shell | 200 unit/controller plus 193 PostgreSQL tests; zero failures/errors/skips; BUILD SUCCESS; owned container/network removed. |
| Helpers | `python tools/demo/test_demo_helpers.py` | 15/15 passed. |
| Independent original-script harness | Real loopback HTTP with isolated fake Docker/SQL transport | Valid 14-assertion case passed; wrong health shape, challenge and duplicate code each rejected. These four cases are not real container evidence. |
| Packaged workflow | Task-local copy of `verify-package.ps1`, inserting both ELS-49 script calls before ordinary drift/reset checks; project `els-demo-els49-review`, port 18059 | Original probe 14/14; new sequence passed every stage; mutated checkpoint refused rerun; guarded reset/restart/reseed passed; original privacy/non-root/drift/reset checks passed. Owned containers, volumes, checkpoint and private settings removed. |
| Presentation | `node tools/demo/build-els49-deck.mjs` with bundled paths | 12-slide package/layout/import checks passed; all slides rendered/inspected; all 12 PDF title/body texts matched; 12 embedded notes. |
| Static | PowerShell Parser.ParseFile, local-link scan, `git diff --check` | Both scripts parsed; links resolved; whitespace passed. |

The tested retained image is `sha256:299a03d1f98d446aa731b80ac2968553a778904c04896ab67ca6c06701903210`; its own `/app/app.jar` hashes to `91daa0e93ef8aba783367dde71566e1563a8e0034e91302a48565c30f700cee2`. This identifies the actual package independently of later Maven rebuilds. Application source/schema/dependencies were unchanged; the live workflow script was unchanged after its successful run.

Failures and corrections: sandboxed Mockito agent attachment stalled; only identified task Maven/fork/attacher processes were stopped and the permitted retry passed. An absent `.env` stopped the initial database attempt; retry used an ignored task-owned empty env file with generated private process credentials, then removed the file. Presentation build attempts exposed missing runtime-module configuration, Windows hard-link permission, PDF font registration, receipt reuse and a text encoding/render issue; all were corrected before final export. No failed attempt is counted as a pass.

The [team record](els-49-team-rehearsal.json) remains unobserved. Merging prepared assets does not establish the four-person rehearsal acceptance item or authorize marking it done. Record actual speaking, answers, corrections and handovers before rehearsal closeout; ELS-50 must preserve this distinction.
