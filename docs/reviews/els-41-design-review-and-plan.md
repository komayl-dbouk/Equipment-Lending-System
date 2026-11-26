# ELS-41 Verified Design Review and Correction Plan

Owner: Hussein Chreif. Peer reviewer: Hussein Chalhoub. Baseline: merged main bdbbd3e. Branch: ELS-41-verified-design-corrections. Hussein Chreif explicitly approved both important findings and this correction plan before production edits. The approval is recorded in Jira comment 10093. Hussein Chalhoub's final implementation review remains pending.

## Review scope and classification

Inspected feature controllers, DTOs/deserializers, services, repositories/entities, shared error handling, Clock configuration, Flyway constraints, database-test guard and relevant regression suites. Checked feature ownership, service transaction boundaries, dependency direction, strict validation, privacy of response errors, query behavior and test oracles. Critical means an immediately demonstrated severe integrity/security failure; important means a verified contract/design concern worth correcting in this card; minor means a limited maintainability observation. No critical finding was demonstrated in the reviewed scope.

| Finding | Classification | Concrete evidence | Proposed decision |
| --- | --- | --- | --- |
| Equipment constraint classification searches SQL message text | Important | A test-only unique index on equipment name fails with SQLSTATE 23505 and actual constraint els41_review_name_key. A duplicated name containing equipment_inventory_code_key causes POST and PUT to return 409 DUPLICATE_INVENTORY_CODE rather than sanitized 500 INTERNAL_ERROR. Rows roll back correctly, but the client is told the wrong cause. | Accept exact structured constraint/SQLSTATE matching, following the existing borrowing/student pattern; accepted by Hussein Chreif |
| Equipment listing performs an availability query per item | Important | Hibernate statement counts are 2 for one item and 25 for 24 items, for all/available/unavailable lists. DTO values, ordering and unchanged database state pass; the number of round trips grows with inventory even when a filter returns fewer rows. | Accept repository-owned DTO projection with bounded reads and database-side availability filtering; accepted by Hussein Chreif |
| Optional Boolean parser returns null | Rejected as a defect | Null represents omission of the available filter and is deliberately consumed as list-all semantics. | Preserve behavior; a SpotBugs warning alone is not proof of a defect |
| Constructor injection exposes dependency references | Rejected as a defect | Controllers retain service references intentionally; controllers serialize DTOs, not those dependencies. | No defensive copies or architecture changes |
| LoanEntity exposes JPA associations | Modified assessment | Association access supports internal DTO mapping and persistence; controllers do not serialize JPA graphs. | No demonstrated API graph leak; leave structural refactoring to separately verified ELS-42 scope |
| Staff authentication/HTTPS absent | Deferred existing requirement | Explicitly documented and assigned to ELS-46. | Do not invent it as a new ELS-41 correction |
| Equipment uniqueness unit fixture uses plain SQLException text | Supporting test concern | The fixture asserts message-based evidence rather than the structured metadata produced by Hibernate/PostgreSQL. | Adapt the genuine-conflict fixture and add metadata-negative cases with the production correction; do not claim this as Mostafa's separate ELS-39 audit work |

The query finding is a measured database-round-trip design concern, not an invented latency SLA or an assertion that the current small demonstration is unusable.

Human approval applies to the two important corrections. Other screening assessments in this table are technical review recommendations, not separately attributed human decisions.

## Reproduction

New EquipmentDesignReviewIT uses real isolated PostgreSQL 17.5, committed synthetic fixtures, test-only Hibernate statistics and a temporary partial unique index. The original BorrowingContractIT and delivery V1 are untouched. Fixture/index cleanup runs before/after each case and targets only the ELS41 namespace.

Executed command: mvnw.cmd -o --batch-mode --no-transfer-progress -Pdatabase-test -Dtest=DatabaseTestSafetyProcessorTest -Dit.test=EquipmentDesignReviewIT -Dlogging.level.root=WARN verify.

Observed: 12 guard unit tests pass; 8 investigation cases run with 5 assertion failures, zero errors/skips. Three one-item listing cases pass with 2 statements. Three 24-item listing cases fail with 25 statements against the bounded-read oracle. Both unrelated-constraint cases fail because actual status is 409 instead of 500. These are expected reproductions, not a green acceptance claim. Local raw logs remain ignored under target/els41/red.log.

## Proposed implementation

1. Replace only equipment's message-substring classifier with exact Hibernate ConstraintViolationException metadata checks for the required SQLSTATE and constraint name. Preserve genuine duplicate/history conflict codes and keep unrelated failures technical. Use the existing sibling pattern instead of introducing a new framework or shared abstraction.
2. Move equipment-list response projection and optional availability filtering into its feature repository. Keep DTO shape, ascending ID order, null/all semantics, empty results, returned history and single-item write responses unchanged. Bound list queries independently of inventory size and assert results against independent SQL state.
3. Keep focused new integration tests separate from existing historical tests. Adapt only EquipmentServiceTest cases directly affected by the corrected repository boundary/exception fixture and add genuine/negative classification cases. No original red tests, migrations, dependency changes, authentication or business-rule additions.
4. Update architecture/API implementation notes, requirement traceability, reports/sprint-3 and AI evidence. Record accepted/modified/rejected human decisions explicitly rather than attributing agent classifications to a human.

Expected production files: EquipmentService.java and equipment/persistence/EquipmentRepository.java. Tests: EquipmentDesignReviewIT.java and directly affected EquipmentServiceTest fixtures. Coordination notice is recorded in ELS-41 comment 10092; ELS-39 should avoid those overlapping oracles and ELS-42 waits for the reviewed merge.

## Validation and delivery

Following the recorded human approval: run focused tests, mvnw.cmd verify, mvnw.cmd -Pdatabase-test verify and the packaged HTTP/SQL workflow on separate disposable test/workflow databases. Compare actual before/after query counts and constraint status/code results. Verify no original borrowing-test, migration or dependency diff, clean fixtures and remove task-owned credentials/resources. Inspect exact staged files and git diff --check before every ELS-41 commit. Push a PR with the concrete problem/change/results in the author voice and request husseinchalhoubb. Do not approve or merge the PR.
