# ELS-41 Verified Design Corrections

Owner: Hussein Chreif. Peer reviewer: Hussein Chalhoub. Baseline: main bdbbd3e. Tested implementation commit: dbb2b55. [Review and approved plan](../../docs/reviews/els-41-design-review-and-plan.md). Hussein Chreif approved both important findings before production edits; Jira comment 10093 records that decision. Implementation peer review and merge remain pending.

## Findings and decisions

| Finding | Severity and human decision | Before | After |
| --- | --- | --- | --- |
| Equipment persistence errors classified from SQL message text | Important; accepted by Hussein Chreif | An unrelated test-only unique index on name produces SQLSTATE 23505. Its duplicated value contains equipment_inventory_code_key, causing POST and PUT to return the wrong 409 DUPLICATE_INVENTORY_CODE. | Match exact Hibernate constraint name and SQLSTATE. Both unrelated failures return sanitized 500 INTERNAL_ERROR and preserve rows. Genuine inventory/history constraints retain business codes. |
| Equipment list adds one database round trip per item | Important; accepted by Hussein Chreif | One-item lists use 2 SQL statements; 24-item lists use 25, for all/available/unavailable modes. | One repository-owned DTO projection joins only active loans, applies the optional filter and orders by ID. All six measured size/filter combinations use 1 SQL statement. |

The full review also inspected student/loan write transactions, return lock order, strict deserializers, clock use, query filters, response privacy, feature dependency direction and database guards. No critical concern was demonstrated in that scope. Optional-null filters, controller dependency references and internal JPA association getters were screened rather than automatically “fixed” because a tool warned. Authentication/HTTPS remains the existing ELS-46 requirement. The review record distinguishes technical recommendations from actual human decisions.

No arbitrary latency target was introduced. The measured improvement is a bounded number of database statements, not a claimed wall-clock benchmark. The fix preserves DTO fields, ordering, empty results, null/all filter semantics, derived availability, returned history and existing business rules.

## Correction boundaries

Production changes are limited to EquipmentService and EquipmentRepository. EquipmentService delegates listing to a DTO projection and no longer parses exception messages. The projection's active-loan join cannot duplicate equipment under the preserved partial unique index. Single-item create/update response mapping is unchanged.

The original borrowing tests and red-test ancestry remain untouched. No dependency, delivery migration, controller, authentication or schema-owner change was made. The equipment uniqueness unit fixture now uses genuine structured Hibernate metadata; new negative cases establish that wrong/missing constraint metadata, wrong SQLSTATE and message-only evidence do not prove a business conflict. This necessary fixture adaptation is not claimed as completion of Mostafa's separate ELS-39 audit.

## Reproduction and regression evidence

[Red results](els-41-red-results.json) retain the investigation before production changes: 12 database-guard unit tests passed; 8 equipment design cases had 5 expected assertion failures, zero errors/skips. Three small-list cases passed with 2 statements; three larger-list cases failed with 25 statements; POST and PUT failed the expected-500 assertion with actual 409. No infrastructure failure was counted as a verified finding.

EquipmentDesignReviewIT now has nine passing cases:

- Six size/filter combinations compare exact response values with independent PostgreSQL queries, including active, never-borrowed and multiple-returned-history items. They check ordering, equipment/history preservation and bounded reads.
- Two unrelated-constraint cases exercise real POST/PUT requests with a temporary namespaced partial index, verify rollback and require sanitized 500 responses.
- A genuine inventory-code race holds an independent table lock until both insert requests are observed waiting, then accepts either winner. It produces one 201, one 409 DUPLICATE_INVENTORY_CODE and one committed row.

Unit tests also preserve genuine create/update duplicate and delete-history conflict classification, reject unrelated metadata/message evidence, and verify the repository filter boundary. There is no arbitrary sleep selecting a race winner. The temporary index and fixtures are removed before/after each integration case.

## Executed checks

Environment: Java 17.0.12, Maven Wrapper 3.9.11, Spring Boot 3.5.16 and PostgreSQL 17.5. Docker supplied task-owned separate test/workflow databases with generated credentials. Existing .env and unrelated resources were preserved.

| Command/check | Actual outcome |
| --- | --- |
| Focused EquipmentServiceTest and EquipmentDesignReviewIT verification | Initial focused correction run passed 22 unit cases and 8 integration cases; the positive database race was added afterward |
| `mvnw.cmd -o --batch-mode --no-transfer-progress -Pdatabase-test -Dlogging.level.root=WARN verify` | 195 unit/controller + 186 PostgreSQL integration = **381 passed**, zero failures/errors/skips |
| `mvnw.cmd -o --batch-mode --no-transfer-progress -Dlogging.level.root=WARN verify` | **195 passed**, zero failures/errors/skips |
| `tools/validation/run-lending-workflow.ps1` with isolated target/port/output overrides | **83 real HTTP requests / 626 assertions passed** on implementation commit dbb2b55 |
| Changed Mermaid class model | Rendered successfully using the cached CLI and installed Chrome |
| JSON, local links and whitespace checks | Checked before evidence commit; actual staged paths inspected |

Both complete test profiles ran against the same source committed as dbb2b55; the packaged workflow identifies that exact commit and JAR digest. The live runner uses a separately loaded test-only fixed Clock, independent container psql connections and synthetic data. Its three races observed two equipment-lock waiters each. Outcomes were borrow/borrow 409+201, borrow/return 409+200 and borrow/delete 404+204, with the corresponding committed-state assertions passing.

[Structured verification results](els-41-verification-results.json) record commands, counts, source identity and observations. Raw logs/JUnit output remain ignored under target/els41. No production correction failed verification, and no assertion was weakened to make the suite pass.

## Cleanup and handoff

Confirmed zero remaining ELS41 temporary index, equipment or student fixtures after tests. Removed the live runner's owned synthetic rows, disposable container/volumes and temporary credential file. The runner stopped its own application process. Subject remains local, untracked and unignored.

Chalhoub reviews the PR. ELS-41 remains in CODE REVIEW until human acceptance and merge. Komayl's ELS-42 implementation remains dependent on that merge. ELS-39 coordination comments identify the affected tests. The ELS-43 quality comparison must retain its original baseline and distinguish these ELS-41 corrections from any later measured correction. No new human effort, coverage percentage, static-analysis improvement, peer approval or merge is inferred.
