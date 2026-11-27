# Final Report and Submission Evidence Checklist

Owner: Hussein Chreif. ELS-48 preparation. These are acceptance gates, not prefilled proof. Verify the final revision and actual linked results before checking an item.

## Evidence available

- [x] Scope, requirements, architecture/API models and borrowing specification linked.
- [x] Borrowing red-test commit and green/correction evidence preserved.
- [x] ELS-39 audits 14 methods and corrects three distinct weaknesses with before/after evidence.
- [x] ELS-40 records detection, exact restoration and regression.
- [x] ELS-41 records two approved important findings and tested corrections.
- [x] Pre-change quality baseline and confirmed Sprint 2 approximate effort linked with scope/precision.
- [x] Required reference-practice table and four maintenance categories prepared.

## Final acceptance gates

| Gate | Owner and card | Required evidence |
| --- | --- | --- |
| Three safe refactorings | Komayl, ELS-42 | Three real smells, before/after diffs, green baselines/regressions, maintenance rationale and current models |
| Measured improvement | Chalhoub, ELS-43 | Significant confirmed concern, original result/correction, comparable rerun, coverage denominators, warning triage and GQM action |
| Quality closeout | Mostafa, ELS-44 | Numerical criteria, actual reviews/effort and remaining risks; no duplicated credit across separate quality cards |
| Final clean setup | Chalhoub, ELS-45 | Clean-clone build/start, package/configuration, synthetic setup, runbook and observed outcomes |
| Access privacy and provenance | Komayl, ELS-46 | Staff success/401 checks, remote HTTPS where used, secret/history/log and dependency/license/IP findings |
| Final acceptance | Mostafa, ELS-47 | Principal scenarios with expected/observed outcomes and final regression on the release revision |
| Team report review | Chreif and all members, ELS-48; reviewer Chalhoub | Final evidence links, accurate contributions/effort and actual review decisions/corrections |
| Rehearsal/understanding | Komayl and all members, ELS-49 | Slides, demonstrated workflow and supplied rehearsal feedback; individual explanation of required engineering choices |
| Release/submission | Chalhoub coordinates ELS-50; Chreif submits | Instructor access, final artifact/version consistency, limitations and actual submission receipt |

## Update procedure

After accepted dependencies merge, update [the report](final-report.md) and [evidence status](final-report-evidence.json) with exact source/run/PR links. Replace only the corresponding pending result. Preserve historical metrics and distinguish them from new measurements. Recheck models/ADRs/context against final source.

Record Sprint 3/4 actual effort when supplied/confirmed. Preserve original estimates and the Sprint 2 approximation classification. Add interruption/variance explanations only when known; exact model IDs require actual metadata.

Validate links/anchors, table completeness, arithmetic, pending-state consistency and revision identity after updates. If a DOCX/PDF export is requested later, generate it from the agreed source and inspect every rendered page. No export or final package is claimed by this preparation.

Keep the original assignment local, untracked and outside both .gitignore and the submission package. Use no execution-date stamps in document names/prose; business-date fixtures needed to explain tests remain distinct.
