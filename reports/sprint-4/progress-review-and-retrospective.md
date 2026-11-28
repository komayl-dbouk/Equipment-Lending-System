# Sprint 4 review and retrospective

Coordinator: Hussein Chalhoub. Project lead: Hussein Chreif. [Structured closeout](els-50-release-closeout.json) retains original estimates, confirmed approximate actuals, artifact identities, checks and remaining external actions. This review does not close the sprint or claim instructor submission.

## Product review

The backend increment includes reproducible packaging, staff-only API access, PostgreSQL-backed acceptance, report traceability, presentation materials and the project-lead-confirmed explanation/rehearsal record. ELS-45/46/47 evidence retains its original tested revisions and measured scopes. ELS-48/49 report/rehearsal decisions retain their relayed-human provenance. The frontend planned for Sprint 5 is not part of this verified backend checkpoint.

ELS-50 now supplies the missing engineering handoff: [dependency materials and inventory](../../docs/deployment/third-party-notices.md), an explicit-revision [source candidate workflow](../release/submission-checklist.md), inspected member hashes and structured closeout. The candidate distributes source/documentation and the Maven Wrapper; application/JRE/container/database binaries are excluded. Runtime-library inventory is reference evidence, not binary-distribution legal clearance.

## Estimates and confirmed actual effort

Original estimates were read from Jira `timeoriginalestimate` for ELS-45 through ELS-50: 3h, 2h, 2h, 3h, 2h and 1h respectively, totaling 13h. The following actuals were explicitly approved by Hussein Chreif in the project conversation after the proposed figures were shown to him. They are confirmed reconstructed approximate member totals, including prompting, corrections, review, testing, documentation and rehearsal; they are not precise independent sessions or invented per-card allocations.

| Member | Original owned-card estimate | Confirmed approximate actual | Difference |
| --- | --- | --- | --- |
| Hussein Chreif | 3h | 8h | +5h |
| Komayl Dbouk | 4h | 5h | +1h |
| Mostafa Younes | 2h | 6h | +4h |
| Hussein Chalhoub | 4h | 7h | +3h |
| Total | 13h | 26h | +13h |

Actual/estimate is 2.0. Original estimates follow original card ownership; actuals include shared review and corrections. The lead's aggregate reflection explains additional handoffs/rework; no individual variance explanation or measured AI time saving is inferred. Original Sprint 2/3 totals and their approximation limits remain unchanged. Story points are relative scope, not hours or productivity.

## Lead-confirmed process reflection

Hussein Chreif explicitly approved this reflection on his behalf:

> Packaging, access checks and independent SQL evidence made the backend reproducible. Stale status records and incomplete release artifacts caused extra handoffs. We should deliver the revision, inspected archive, checksums, notices and CI together.

This is the project lead's confirmed reflection, not a claim that a retrospective meeting occurred or that each member independently supplied feedback. Technical review separately observed that the original ELS-50 audit deferred notices, archive inspection and structured effort/closeout. Those independent deliverables are now implemented instead of being deferred to the submission owner.

## Adopted improvement and verification

The project lead adopted the following action: **Hussein Chalhoub maintains the release handoff; Hussein Chreif verifies it before submission.** The handoff includes the exact source commit/tree, inspected archive member manifest and SHA-256 values, notices inventory and CI link.

Observable completion check: verify all exported bytes against Git blobs, confirm Wrapper identity and original notice hashes, ensure the chosen revision's CI passed, and regenerate the candidate after final human merge. The release helper and CI exercise the machine checks. Chreif's final human submission check remains explicit; it is not inferred from an automated export or this adoption record.

## Remaining instructor-facing actions

The [submission checklist](../release/submission-checklist.md) assigns official channel/format/deadline-time confirmation, instructor access and actual receipt to Chreif. The supplied deadline date remains December 1; its official time/timezone/channel were not provided. ELS-50 prepares and verifies a reviewable candidate; it does not grant authorization to publish or submit, and does not establish a receipt.
