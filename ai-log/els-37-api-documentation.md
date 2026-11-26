# ELS-37 API documentation alignment

## Scope and review gate

Komayl Dbouk requested alignment of the implemented API, runnable examples, diagrams and
requirements traceability. The assigned human reviewer is Hussein Chalhoub
(`husseinchalhoubb`), superseding older artifact text that named Hussein Chreif. The user
approved the scoped plan before substantive edits. No application behavior, migration,
dependency or historical evidence file is changed by this card.

## Sources inspected

- Root `AGENTS.md`; no nested agent instructions exist.
- Controllers, DTOs, services, repositories, entities, configuration and Flyway V1. The branch
  began at `origin/main` `90dc0a1` and was rebased to `db8624e` after ELS-36 merged during work.
- Student, equipment, borrowing, return, query and shared-error tests.
- Requirements, API contract/OpenAPI, architecture/domain diagrams, specifications, local
  setup, traceability and feature evidence.
- The planning snapshot for ELS-36, ELS-37 and ELS-46, plus available remote branches and pull
  requests.

The connected Atlassian resource is `komayldbouk76.atlassian.net`, while repository project
links target `equipment-lending-system.atlassian.net`. ELS-37 returned 404 and project key ELS
was unavailable. Consequently, no Jira comments were read and no status or comment was changed.
This access limitation must not be mistaken for an issue state.

## Decisions and corrections

- Treat the eleven routes found in controllers as the complete implemented data API. Do not add
  student list/delete, direct equipment/loan retrieval or loan edit/delete operations.
- Make OpenAPI describe the current unauthenticated local runtime. Remove active root security
  and per-operation `401` claims. Retain the unused Basic scheme and an explicit pending ELS-46
  delivery extension so the security requirement is neither erased nor reported as verified.
- Replace fixed-ID narrative examples with a PowerShell workflow that generates unique synthetic
  values, captures response IDs and asserts status, business code, normalization, ordering,
  filters and state transitions.
- Show separate borrowing, return and query controller/service/repository responsibilities.
  Correct the Clock dependency from LoanService to LoanQueryService.
- Document the real lock order: borrowing validates dates and reads the student before locking
  equipment; equipment remains the first locked mutable resource. Return reads the immutable
  equipment reference, locks equipment, then locks/rereads the loan.
- Consolidate traceability rather than rewriting historical red/green evidence. ELS-36 evidence
  was unavailable at the initial audit, then merged as PR #12 during validation; reconcile its
  83-request/626-assertion workflow and integrity races before review. Keep ELS-46
  access/security explicitly pending.

## Validation record

The pre-edit audit established:

- Initial `git fetch origin --prune`: passed; `origin/main` was `90dc0a1`.
- OpenAPI JSON parsing and internal schema-reference check: passed before edits.
- Markdown local-link target check: passed before edits.
- Java/Maven execution on the host: unavailable because neither `java` nor `JAVA_HOME` was in
  the process environment. Validation therefore used Java 17 in a local Docker container and
  the repository's Maven Wrapper 3.9.11 bootstrap class.
- Docker server access required the approved local elevation path because the sandbox could not
  access the Windows Docker pipe.
- ELS-36 integrated evidence was unavailable at the initial audit. A final fetch found PR #12
  merged at `db8624e`; the branch was rebased and its four new integrity tests and evidence were
  incorporated into traceability.

Post-edit results:

- `npx.cmd --yes @redocly/cli lint docs/api/openapi.json`: initially failed with eleven errors
  because unauthenticated operations omitted an explicit security override. Adding root
  `security: []`, tag descriptions and removing unused response components produced exit `0`.
  Four non-blocking warnings remain: no license metadata, the intentionally local server URL,
  no artificial 4xx response on the parameterless overdue query and the deliberately reserved
  ELS-46 Basic scheme.
- OpenAPI JSON parsing and all 50 internal `$ref` resolutions: passed.
- Local Markdown targets in all changed documents: passed.
- Mermaid validation: the first browser launch stalled, and a lightweight parser exposed two
  invalid sequence-message separators. After correcting them, Mermaid CLI rendered all ten
  blocks with installed Chrome and no diagram errors.
- PowerShell parsing: passed. The first execution was blocked by the machine execution policy;
  the documented process-scoped bypass avoided changing user or machine policy. The next run
  exposed the Windows PowerShell requirement to load `System.Net.Http`; after adding `Add-Type`,
  the complete synthetic workflow passed, including normalization, conflict, filters, overdue,
  return, reborrow, retained history and protected deletion assertions.
- Final-tree Maven `verify` under Java 17 / Maven Wrapper 3.9.11: exit `0`, 185 tests, zero
  failures, errors or skips.
- Final-tree Maven `-Pdatabase-test verify` against PostgreSQL 17.5: exit `0`; 185
  unit/controller tests and 177 integration tests, zero failures, errors or skips. Flyway
  validated and applied the sole migration to the fresh test database. The earlier pre-rebase
  173-integration-test pass was superseded rather than presented as final-tree evidence.
- The first packaged-application start found that the existing app database volume had older
  credentials than the temporary ignored `.env`. The volume was left untouched; the workflow
  ran against the separate validated test database instead.
- `git diff --check`, PowerShell parser validation and final local-link checks: passed.

The temporary `.env`, Maven output and rendered diagram files are ignored or outside the
repository and are not delivery artifacts. No credential was added to version control.

## Review and delivery status

Human review, PR CI, Jira linking and CODE REVIEW transition remain pending. This log does not
claim approval, merge or Jira completion.
