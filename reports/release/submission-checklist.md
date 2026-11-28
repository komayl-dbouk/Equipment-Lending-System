# Backend submission candidate and instructor handoff

Submission owner: Hussein Chreif. Release coordinator: Hussein Chalhoub. Human peer reviewer: Mostafa Younes. The selected artifact is a backend **source candidate**, not a submitted/final release or the planned Sprint 5 frontend.

## Reproducible source export

```powershell
python -B tools/release/test_prepare_submission.py
python -B tools/release/prepare_submission.py --revision <explicit-full-commit> --output target/release/<new-candidate-name>
```

Use the actual full reviewed merge commit for the eventual instructor handoff. The helper never defaults to a moving branch, never copies a developer worktree and refuses an existing/outside destination. Outputs are `equipment-lending-source.zip`, `manifest.json` and `SHA256SUMS` below ignored `target/release/`. Generated ZIP/JAR files must not be committed. A candidate from this unmerged PR is labeled with that actual source and must be regenerated after human merge; do not rename its revision in a record.

Every non-directory archive member is compared to its exact Git blob: name, bytes, SHA-256 and Git mode. The manifest records full commit/tree identities, notice inventory checksum, member count and per-file hashes. Duplicate/case-colliding names, symlinks/submodules, tracked private or forbidden build/runtime paths and byte/list discrepancies fail closed. `.env.example` and the verified Maven Wrapper are allowed; `.env`, `Subject/`, `.git`, `target/`, caches, other runtime archives/binaries and key/log files are rejected even if accidentally tracked. Untracked and dirty developer files cannot enter a Git export. These guards do not claim a general secret scan of arbitrary source text.

The helper also verifies the source/POM inventory fingerprint and every listed legal material. See the [notices scope and provenance](../../docs/deployment/third-party-notices.md). Source builds download runtime dependencies through Maven; this archive does not redistribute their JARs or container images. Original executable Git modes are recorded for POSIX users; Windows users run `mvnw.cmd`.

## Evidence and CI

The [structured closeout](../sprint-4/els-50-release-closeout.json) records the actually generated candidate and its source/hash/count. CI runs export-guard tests and creates another inspected candidate from the exact CI checkout commit, retaining it with test artifacts. A PR merge-ref candidate is not silently called the eventual main merge commit.

Before final handoff, verify `SHA256SUMS`, inspect the manifest/member list, confirm CI for the exact chosen revision and regenerate if anything changes. Keep the verified source, notices and CI together. No new report/slides export is implied: the already reviewed Markdown report and committed presentation PPTX/PDF are included as source-tree artifacts.

## Confirmed preparation

- [x] Submission owner identified; backend source candidate format chosen for review.
- [x] Exact-runtime reference inventory and original embedded materials assembled; redistributed Wrapper materials checked.
- [x] Reproducible explicit-revision export and rejection tests implemented; actual candidate inspection recorded in closeout evidence.
- [x] Report acceptance and rehearsal coverage recorded with their project-lead-confirmed provenance.
- [x] Sprint 4 approximate actual effort, lead reflection and owned improvement explicitly confirmed by the project lead.

## External steps before submission

- [ ] Confirm the instructor's official destination, required artifact format, deadline time and timezone. The project lead supplied December 1 as the deadline date; no official time/channel was supplied.
- [ ] Verify instructor GitHub/Jira access using the intended account. A public repository or a visible local board is not proof of instructor access.
- [ ] Obtain human acceptance of the final revision; regenerate and inspect that revision's source candidate and check its exact CI.
- [ ] Submit through the confirmed destination with explicit authorization and retain the actual receipt. No submission has been performed or receipt supplied.

The completed engineering handoff and the above external submission actions are separate states. An outstanding official time, access confirmation or receipt does not prevent preparing/reviewing the archive, and a successful archive check does not establish submission.
