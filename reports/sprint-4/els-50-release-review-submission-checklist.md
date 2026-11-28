# ELS-50 Release Review, Retrospective and Submission Checklist

Owner: Hussein Chalhoub. Human reviewer: Mostafa Younes (`younes285`). Submission owner: Hussein Chreif. This record distinguishes checked repository facts from pending human and external actions. It does not mark the coursework submitted.

## Audited release baseline

The audit began from pulled `origin/main` revision `213c751`, which contains the merged ELS-49 reconciliation record (PR #26) and the ELS-48 review-record merge (PR #27, `4648a2f`). The current worktree was clean before ELS-50 documentation was created.

| Area | Checked evidence | Status |
| --- | --- | --- |
| Technical acceptance | [ELS-47 acceptance evidence](els-47-final-release-acceptance.md) records 200 unit/controller plus 193 PostgreSQL integration tests, 15 helper checks, 227 HTTP requests, 3,409 CI assertions, 154 SQL checks and three races on its final implementation scope. | Verified historical technical evidence; not a new ELS-50 execution. |
| Reproducible packaging | [ELS-45 evidence](els-45-reproducible-packaging.md) and the current [local runbook](../../docs/deployment/local-setup.md) define the loopback-only packaged workflow, synthetic data and guarded reset. | Verified as source documentation and prior evidence. |
| Access/privacy/license review | [ELS-46 evidence](els-46-access-privacy-license-review.md) records fail-closed staff access, privacy checks and the resolved dependency/license inventory. Remote HTTPS, browser acceptance and a third-party notices bundle remain separate. | Verified evidence with retained limitations. |
| Report and rehearsal gates | [ELS-48 team review](../els-48-team-review.md) and [ELS-49 rehearsal record](els-49-team-rehearsal.json) retain project-lead-confirmed four-member acceptance/rehearsal coverage without inventing individual quotes, timings or findings. PR #27 and PR #26 are merged. | Confirmed human records with stated provenance. |
| Pull-request workflow state | PR #26 and PR #27 each had a completed successful **Build and test** workflow. The pulled `213c751` main workflow ([run 46155647808](https://github.com/komayl-dbouk/Equipment-Lending-System/actions/runs/46155647808)) completed successfully. | Verified workflow result for this exact main revision. |
| Evidence navigation | README, final report, ledger, finalization checklist, maintenance plan, packaging/access runbooks and ELS-45/46/47/48/49 evidence paths were checked present in the tracked tree. | Verified paths; this is not a full remote-link or rendered-export audit. |

## Source and archive integrity

- `git status --short` was empty before this card's files were added.
- The active history contains the incremental ELS-47 acceptance merge, ELS-48 report/review merges, ELS-49 presentation/rehearsal merges and their source-specific evidence commits.
- `git ls-tree -r --name-only HEAD` found no tracked `Subject/` path. `.gitignore` does not add `Subject/`; the original assignment remains excluded from tracked/submission material by process, not by an ignore rule.
- `git diff --check` passed before ELS-50 edits. `git fsck --no-reflogs --unreachable --no-progress` completed successfully; it reported historical unreachable objects from prior branch activity, not an active-ref corruption or a tracked release artifact. They were not pruned or otherwise modified.
- Before any final archive is created, compare its manifest with `git ls-files`, exclude ignored/private `.env` and `target/` content, exclude `Subject/`, and identify the exact reviewed commit placed in the archive. No archive has been created or inspected by this card.

## Release checklist

### Checked and evidenced

- [x] Repository source, tracked release documentation and owned evidence paths are present at the audited baseline.
- [x] Required Java/PostgreSQL, packaged delivery, security/privacy and acceptance evidence is linked to the revisions that produced it.
- [x] ELS-48 report acceptance and ELS-49 rehearsal coverage are represented as project-lead-confirmed human records, not fabricated individual GitHub review events.
- [x] Hussein Chreif is the designated submission owner in the team plan and this checklist.
- [x] Local setup documents the private configuration, loopback boundary, authenticated workflow and safe task-owned reset procedure.

### Required before submission; not yet verified

- [ ] Confirm the exact submission channel, deadline time and timezone from an authoritative course/instructor source. Jira establishes only the December 1 date; it does not establish a time.
- [ ] Confirm that the instructor has the required GitHub repository and Jira-board access. The repository records this as a requirement, not as an observed grant.
- [ ] Select the final artifact format and exact reviewed commit; generate any requested archive, DOCX or PDF only from that revision and visually inspect it.
- [ ] Assemble/verify the exact third-party license and NOTICE materials if a binary or archive will be distributed. ELS-46 explicitly records that the repository does not yet assemble this bundle.
- [x] Checked the pulled `213c751` workflow; **Build and test** completed successfully.
- [ ] Record confirmed Sprint 4 effort and an actual retrospective input if supplied by the team. No Sprint 4 total, attendance duration or per-card allocation is reconstructed here.
- [ ] Hussein Chreif submits through the confirmed channel and retains the actual confirmation/receipt. Only then may a submission-complete statement be made.

## Sprint 4 evidence-based retrospective

### Observed delivery outcomes

- The release combined reproducible local packaging, fail-closed staff access, PostgreSQL-backed acceptance coverage, report traceability and deterministic presentation materials.
- The technical evidence deliberately separates prior source-specific executions from later documentation/reconciliation records, reducing the risk of presenting historical measurements as fresh checks.
- A real documentation discrepancy, ELS-48-R1, was corrected through the report/rehearsal reconciliation records. The correction preserves the difference between project-lead-relayed human confirmation and independently observed automation.

### Risks and follow-up ownership

| Risk or gap | Owner | Required next action |
| --- | --- | --- |
| Submission deadline/channel and instructor access not evidenced | Hussein Chreif, with ELS-50 coordination | Confirm against course/instructor source and record only the confirmed facts. |
| Future source changes can invalidate the audited workflow | Hussein Chalhoub | Recheck the exact workflow result for any later review/submission revision. |
| Distribution notices are not assembled | Hussein Chalhoub / submission owner | Determine whether a distributable binary/archive is required; if so, assemble exact notices from resolved artifacts. |
| Sprint 4 actual effort and retrospective inputs are absent | All members supply facts; Hussein Chalhoub records them | Record confirmed totals/reflection or leave them explicitly unavailable. |
| Submission receipt does not exist yet | Hussein Chreif | Retain the actual receipt after authorized submission; do not infer one from a GitHub merge or Jira status. |

### Process improvement

For final handoffs, record the exact source identity, environment, command/result, CI state and whether each human fact is directly observed, project-lead-confirmed or still pending. This keeps later status corrections from silently converting a planned activity into claimed evidence.

## Submission handoff for Hussein Chreif

1. Confirm the course destination and deadline time/timezone from the authoritative course/instructor source.
2. Confirm instructor repository and board access using the intended account, without exposing private account details in the repository.
3. Select the reviewed commit and artifact format, then run the archive manifest and license/NOTICE checks above.
4. Confirm the latest workflow result for the selected submission commit and retain its exact URL/commit in the release record.
5. Submit only after explicit authorization, retain the destination's receipt/confirmation, and attach or link that receipt through the approved project record.

Until step 5 is evidenced, this release is **prepared for submission, not submitted**.
