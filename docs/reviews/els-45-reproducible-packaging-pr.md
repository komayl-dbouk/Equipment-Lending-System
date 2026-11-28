# ELS-45 Packaging Review and ELS-47 Handoff

Historical preparation/review snapshot. Its original local failures and publication state are retained below; [final ELS-47 acceptance](../../reports/sprint-4/els-47-final-release-acceptance.md) records the later completed validation and current human-review gate.

Reviewer responsibility: Mostafa Younes. Assistance: Codex. Technical recommendations are not human approvals.

## Exact reviewed source and decisions

Reviewed the full 19-file [PR 22](https://github.com/komayl-dbouk/Equipment-Lending-System/pull/22) change from base `500eabef93d3d0068e8fe39c7b6337208663e94b` to final head `71944f6dbb2420c13076a59df1038efdc4076da6`, including packaging, configuration, helpers, runbook, original evidence and correction evidence. GitHub records an APPROVED review by `younes285`; merge is `6ec66befe229818ac3f0d1084a90c2f6c3b6aea1`. [Exact merge CI passed](https://github.com/komayl-dbouk/Equipment-Lending-System/actions/runs/46061249030).

ELS-46 [PR 23](https://github.com/komayl-dbouk/Equipment-Lending-System/pull/23) final head `ac339e544916b9df1ff536e6f356d28b55fd1b9f` has APPROVED review by `MaxHusseinChreif`, merge `74da749eb0e7ec8419cc2c87384dd688578972e6` and [passing exact-merge CI](https://github.com/komayl-dbouk/Equipment-Lending-System/actions/runs/46066988967). Both Jira dependencies are Done. These actual human/merge records satisfy ELS-47's dependency gate; the assistant did not approve or merge them.

Independent execution used a new detached clean tracked checkout of integrated merge `74da749`, created with git worktree, not a copy of the source workspace. No existing private configuration, Subject/, generated target or Docker volume was copied. The original checkout was clean before builds. Dockerfile, .dockerignore, reset, health and test-import helpers are unchanged from final ELS-45 head. ELS-46 deliberately adds credential forwarding/authenticated clients and production logging safeguards; those interactions were reviewed on integrated source.

## Findings and technical recommendation

No blocking source defect was found in the reviewed packaging scope. The source and successful author/current-merge CI evidence support technical acceptance of ELS-45, consistent with its recorded human approval. Independently reproduced clean Maven/PostgreSQL, helper and authenticated executable-JAR results support that recommendation. Local Docker runtime rehearsal is **blocked**, not passed; it remains an explicit final-acceptance gate.

| Classification | Location / trigger | Consequence and required action |
| --- | --- | --- |
| Blocking environment / incomplete local evidence | Runtime image build during tools/demo/verify-package.ps1, invoked at line 23; C: reached zero free space. Direct Docker diagnostic reports `write /var/lib/desktop-containerd/daemon/io.containerd.metadata.v1.bolt/meta.db: input/output error`. Image inspection also fails on a containerd blob read. | No local Docker package/start/seed/reset success is claimed. Restore host storage and Docker metadata health, then repeat the unchanged clean runtime build and authenticated smoke before final ELS-47 acceptance. This is an observed environment failure, not a demonstrated application defect. |
| Blocking cleanup limitation | Task-owned els47-tests service: Docker stop acknowledged, subsequent removal fails writing the same metadata database; inspect reports the container dead. | Remove only the identified owned dead container, anonymous volume and network after storage recovery. Do not restart/reset/prune Docker or interrupt other projects without human coordination. Resource identities are in the acceptance JSON. |
| Low historical delivery metadata | reports/sprint-4/els-45-reproducible-packaging.md final paragraph and els-45-correction-results.json limits still describe human review/merge as pending. | Treat these as historical snapshots. This review and the ELS-47 dependency record supply actual final head, approval, merge and merged CI; do not rewrite the historical test counts. |
| Optional reproducibility improvement | Dockerfile line 1 uses the maintained major-version tag eclipse-temurin:17-jre-jammy. | A future pull may select different JRE bytes. Record the actual successful image ID/digest with final acceptance; pin by digest if byte-identical rebuilds become required. No unverified image identity is substituted for the failed local build. |

The failed image pull/build did not justify altering packaging, suppressing checks or using a different product runtime. No production correction was introduced into this acceptance branch.

## Packaging and operational assessment

- .dockerignore is a deny-all allowlist for Dockerfile and the exact packaged JAR. Subject/, .env, .git, source checkout configuration and unrelated target output cannot enter this build context. No genuine secret was found in the reviewed diff; .env.example has placeholders/blank staff values.
- Docker copies the host-verified delivery JAR into a Java 17 runtime image and selects the non-root spring user. Compose publishes only the app on loopback; PostgreSQL has no published delivery port. The separate test Compose service binds its own loopback port and database.
- Flyway remains the only schema owner and Hibernate validates. Readiness combines PostgreSQL health with an HTTP UP check bounded by a cancellation deadline. Ordinary down preserves delivery data; the explicit disposable-project reset is separate.
- Seed writes require lowercase els-demo-* ownership, matching checkout/container labels, actual loopback port, internal-only database, effective database/cluster identity, matching staff configuration and empty initial tables. The private Authorization header is constructed only after target checks; redirects are refused.
- Atomic project/checkout/database-bound checkpoints prevent replay after interruption. Reruns compare exact synthetic student/equipment/loan fields, references, dates, availability and history. Reset rejects foreign/external volumes and removes a checkpoint only after successful Docker down.
- ELS-46 coordinates ELS_STAFF_USERNAME / ELS_STAFF_PASSWORD in configuration, Compose, seed, runnable API examples and validation clients. Exact health is public/sanitized; data routes are protected. Basic authentication remains a local HTTP demonstration unless remote HTTPS is separately verified. Browser CSRF/origin and distribution notices remain separate release obligations.

## Independently observed checks

Environment: Windows PowerShell, Python 3.13.7, JBR 17.0.5, Maven Wrapper 3.9.11, Spring Boot 3.5.16, Docker Engine 28.3.2, Compose 2.39.1 and PostgreSQL 17.5 in a task-owned container. Private generated credentials include spaces, # and a literal dollar; the documented Compose-backed importer was used in the Maven shell. Existing .env was not overwritten.

| Command / check | Observed result |
| --- | --- |
| mvnw.cmd --batch-mode --no-transfer-progress verify in clean checkout | Exit 0; 200 tests, zero failures/errors/skips |
| Documented import-test-environment.ps1 followed by mvnw.cmd --batch-mode --no-transfer-progress -Pdatabase-test verify | Exit 0; 200 unit/controller + 193 PostgreSQL integration tests, zero failures/errors/skips |
| python tools/demo/test_demo_helpers.py | Exit 0; 15 helper regressions passed; Docker fakes/real loopback helper checks are distinct from a real package run |
| Unchanged tools/validation/run-lending-workflow.ps1 on port 18048 | Passed: 86 authenticated HTTP requests, 651 assertions, 43 SQL evidence records, all three coordinated races with two observed equipment-lock waiters |
| Real tools/demo/verify-package.ps1 -ProjectName els-demo-els47-review -Port 18047 | Exit 1 during Docker build; containerd metadata I/O failure; no runtime-image/start/seed/reset pass claimed |
| Direct docker build diagnostic of same clean context | Exit 1; concrete containerd metadata write I/O error confirmed |
| Private-value scan of raw JAR evidence and ignored app logs | Generated staff/database passwords and complete Authorization header absent; this is not a host/proxy/database-server log audit |

Packaged JAR SHA-256: `5fdf429cba4adf6eb1c0be95db5ff16f4ce59b327e7904c0b31795fffd389b74`. The JAR used an explicitly loaded test-only Clock fixture for the existing deterministic HTTP workflow, excluded from the delivery package. The ordinary Docker package remained unexecuted because build failed. Effective workflow target: equipment_lending_workflow_test / equipment_workflow / PostgreSQL 17.5; integration target: equipment_lending_test / equipment_test, task-owned loopback port 15447. No credentials are retained here.

Observed races: borrow/borrow 409 and 201; borrow/return 409 and 200; borrow/delete 201 and 409. SQL oracles confirmed the corresponding committed history/active rows, zero orphans and no duplicate active-loan groups. The runner accepts either legitimate serial winner, not an arbitrary chosen thread. The raw result retains its original ELS-36 runner identity; its current execution context is this independent packaging review.

## Handoff and limits

[ELS-47 preparation](../../reports/sprint-4/els-47-final-release-acceptance.md) and [structured results](../../reports/sprint-4/els-47-acceptance-results.json) contain the requirement matrix, exact dependency gate and actual independent preparation results. Raw output remains ignored under target/els47-review. Obsolete task-generated JARs were removed to reclaim space after recording the verified digest. Only task-owned cleanup was attempted; Docker metadata failure prevents claiming complete container/volume/network removal. The dead resource and generated private cleanup configuration remain identified locally.

The ELS-47 plan is awaiting explicit user review before substantial validation-tool additions. Final scenario gaps, the failed Docker rehearsal and cleanup recovery remain pending. Existing green checks and dependency CI do not establish final release acceptance or a new human approval. No coverage/static-analysis rerun, remote TLS, browser acceptance, meeting, actual effort, Done or submission is inferred.

Recovery retry requested by the user: host space was freed (about 3.6 GB observed), and the unchanged JAR was recreated successfully with tests skipped solely to restore the removed artifact. The new digest is retained separately in the acceptance JSON. Docker version/container APIs still returned HTTP 500, so neither the real Docker smoke nor owned cleanup is marked recovered. The pending human ELS-47 plan decision is separate from the user's request to retry Docker.
