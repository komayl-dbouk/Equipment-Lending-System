# Current Version End-to-End Validation

## Baseline verdict

The original Sprint 1 baseline builds and its existing automated suite passes, but its full validation was **not green**. Three issues were reproduced: unsafe database-test setup, equipment JSON type coercion and unvalidated embedded NUL characters. The ELS-51 correction and successful reruns are recorded below; the original findings remain as historical evidence.

Validated revision: `ea3d4bb` (merged PR #6), using a fresh tracked-source snapshot. Production source and configuration were not changed during validation. The report and reusable Postman collection are local validation deliverables, not a claim that fixes have been implemented.

## Findings

### P1: Documented test setup can erase application data

[Local setup](../../docs/deployment/local-setup.md) sets ELS_TEST_DB_URL equal to ELS_DB_URL. [The environment example](../../.env.example) and [agent commands](../../AGENTS.md) repeat the same application/test target. [EquipmentPersistenceIT](../../src/test/java/edu/university/equipmentlending/equipment/EquipmentPersistenceIT.java) deletes every loan, equipment and student row before each test. Following the documented database-test command against an existing local application database therefore removes its records.

Reproduction used only the disposable equipment_lending_test database: insert one unrelated synthetic student marker, run the four equipment integration tests, and query the marker. Result: tests pass while marker count changes from 1 to 0. No existing application database was used for this experiment.

Required correction: provision a separate test database in the documented setup, use distinct test settings throughout the example/runbook/agent commands, and add a guard that refuses destructive integration tests against the configured application database. The CI workflow already uses a dedicated test database; it is the local setup that is unsafe.

### P2: Equipment writes accept JSON numbers and booleans as strings

[EquipmentWrite](../../src/main/java/edu/university/equipmentlending/equipment/dto/EquipmentWrite.java) declares string fields but relies on Jackson's default scalar coercion. POST and PUT accept integers and booleans in inventoryCode, name, category and description. These values are converted into strings and persisted instead of returning 400 VALIDATION_FAILED, contrary to the published JSON string types.

For example, POST /api/equipment with `{"inventoryCode":"PROBE","name":123,"category":"Camera"}` returns 201 with name `"123"`. An equivalent replacement returns 200. All 16 isolated combinations (four fields, two wrong scalar types, POST and PUT) reproduced the problem without duplicate-code interference. Student writes already reject these types using a feature-specific deserializer.

Required correction: enforce the equipment string types before normalization without relaxing other feature contracts; add controller and real-database rejection cases that verify no write occurs.

### P2: Embedded NUL text becomes a database error and HTTP 500

Both [StudentWrite](../../src/main/java/edu/university/equipmentlending/student/dto/StudentWrite.java) and EquipmentWrite permit an embedded U+0000 character in otherwise nonblank text. PostgreSQL rejects that character in its UTF-8 text representation. The API returns sanitized 500 INTERNAL_ERROR instead of rejecting the invalid field with 400 VALIDATION_FAILED.

Reproduction: POST a student with name `"Bad\u0000Name"` and a valid unique email, or equipment with the same name and otherwise valid fields. Student name and all four equipment text fields reproduced 500 independently. Error responses did not expose SQL or stack traces. Records were not partially persisted; input classification is the defect.

Required correction: reject unsupported embedded NUL characters in writable text before persistence, covering create and replacement with tests for safe errors and unchanged stored state. Do not translate every integrity failure into a client error; unrelated server failures must retain their classification.

## Executed validation

| Area | Observed result |
| --- | --- |
| Pinned tools | Java 17.0.12, Maven Wrapper 3.9.11, PostgreSQL 17.5-alpine; no enforcer override |
| Fresh source build | clean verify passed; executable Spring Boot JAR packaged |
| Unit/controller/shared errors | 116 tests passed, zero failures/errors/skips |
| Real PostgreSQL profile | 25 integration tests passed, zero failures/errors/skips; 4 equipment, 20 student, 1 schema foundation |
| Primary live HTTP harness | 220 requests; 1,247 assertions, 1,222 passed and 25 failed; failures group into the two input-validation issues above |
| Saved Postman workflow | 15 real HTTP requests, 40 assertions, 38 passed and 2 failed; both failed assertions come from the numeric-equipment-name case |
| Isolated probes | 16 coercion cases and 5 NUL cases reproduce failures; non-ASCII student name succeeds |
| Database outage/recovery | All 5 checks passed: health 503 DOWN, sanitized API 500, automatic health/API recovery, every persisted fixture row preserved |
| Application restart | Health returns UP and every fixture row survives; Flyway validates its single migration and applies no duplicate migration |
| Missing configuration | Packaged app exits unsuccessfully without required database settings |
| Test isolation experiment | Four equipment tests pass but delete an unrelated disposable marker, confirming the setup risk |
| Repository/specification checks | OpenAPI JSON parses; two feature controllers match implemented scope; Subject remains untracked and unignored; git diff --check passes |

The main build used `./mvnw --batch-mode --no-transfer-progress clean verify`; the database build used `./mvnw --batch-mode --no-transfer-progress -Pdatabase-test verify`, targeting the fresh source snapshot through Maven's -f argument. No application build settings or dependency pins changed. A disposable Compose project named els-validation used PostgreSQL on port 15432. Integration tests targeted equipment_lending_test; the packaged app used equipment_lending on localhost port 18087. All records were synthetic.

Initial offline commands encountered uncached clean/Failsafe plugin dependencies. Online retries using the Windows certificate store passed. Initial diagnostic-harness issues (actuator's valid vendor JSON media type and Windows Unicode output encoding) were corrected before reporting final results; they are not application defects.

Postman executed locally with --no-report-events. Its authenticated --output option could not be used without a Postman login; a subsequent local collection run produced the 15-request/40-assertion results in the captured CLI log. Cloud publication was not performed. Exit code 1 reflects failing assertions, and the CLI also printed a cloud-credentials notice; no cloud report is claimed.

## Passing functional coverage

Real requests and independent database checks covered student registration, Location header, retrieval, full replacement, own/other email handling, normalization, length boundaries, Unicode whitespace, invalid/missing fields, strict student text types and ID boundaries. Equipment checks covered create/list/replacement/delete, normalized unique codes, description omission, length boundaries, uppercase expansion, ordering, invalid fields/IDs and missing targets.

Additional checks exercised duplicate-write rollback, competing student/equipment creates and updates (one success/one conflict, one canonical row, unchanged losing record), active and returned loan history deletion guards, derived availability, history preservation, foreign keys, due/return date constraints and the active-loan partial unique index. Loan rows were seeded directly only to test the current inventory/history foundation; that does not validate a borrowing or return API.

The health endpoint exposes minimal status. Ordinary validation and business errors preserve the six-field error contract; observed error responses remain sanitized. Unsupported routes/content types/methods and hidden actuator endpoints were also exercised. The current reserved availability filter is rejected intentionally until its Sprint 2 implementation.

## Scope limits and follow-up

This verifies the implemented Sprint 1 backend under the recorded local environment, not every possible input or production condition. Borrowing, return endpoints, overdue/history filters and lending-specific race acceptance remain Sprint 2 work. Delivery authentication, HTTPS deployment, coverage measurement, mutation/static-analysis experiments, sustained load and external vulnerability scanning were not established by this run. The full planned OpenAPI document includes future behavior and is not proof that those features exist.

The three findings are documented for correction. No production fixes, Jira changes or Sprint 2 startup are included in this validation task. Existing tests passing is insufficient to close these newly reproduced gaps.

## Evidence

- [Structured validation results](full-validation-results.json) retain counts, failures, isolated probes and recovery/isolation outcomes without credentials or execution-date metadata.
- [Reusable Postman collection](full-validation.postman_collection.json) targets the baseUrl collection variable. Use a disposable database; the collection writes synthetic records and intentionally contains the failing type-validation regression.
- Raw build, integration, HTTP, Postman, application and recovery logs remain under ignored target/full-validation. Credentials exist only in an ignored temporary file and are removed during cleanup. Source snapshot and generated diagnostics also remain ignored.

## Cleanup

The validation application was stopped and the disposable Compose project and its volumes were removed. The temporary credential file was deleted. Only diagnostic artifacts and the source snapshot remain under ignored target/full-validation. Independent SQL checks confirmed zero persisted rows for the rejected NUL fixtures.


## ELS-51 correction and successful rerun

[ELS-51](https://equipment-lending-system.atlassian.net/browse/ELS-51), assigned to Hussein Chreif, addresses all three findings on branch ELS-51-validation-hardening based on ea3d4bb. The original result file remains the baseline failure record. [ELS-51 results](els-51-validation-results.json) record the corrected branch's passing evidence.

- A separate compose.test.yaml provisions the test database with its own port and credentials. The example environment, local setup and agent commands use that target. A test-classpath EnvironmentPostProcessor runs after configuration loading but before datasource/Flyway initialization, requires an explicit PostgreSQL database ending in _test, and rejects the configured application database name. It uses the PostgreSQL driver's effective URL parsing, including recognized database-name overrides, and is absent from the executable JAR.
- Student and equipment DTOs share a strict string deserializer. Equipment numbers and booleans now return 400 rather than being persisted as text. JSON null remains valid only where the existing contract permits it.
- DTO validation rejects embedded U+0000 in all writable text before persistence. Optional description remains nullable, and valid Unicode text is preserved. The API explanation, requirements and OpenAPI patterns reflect these guarantees.

Final full verification: 164 unit/controller tests and 39 PostgreSQL integration tests pass, zero failures/errors/skips. The new tests cover 36 rejected POST/PUT text combinations without persistence/transaction interaction, startup-target safety and database rejection cases proving rows remain unchanged. The clean snapshot build uses the pinned Java/Maven/PostgreSQL tools without an enforcer override.

The same primary live audit now passes all 1,259 assertions over 220 HTTP requests. The saved Postman collection passes all 40 assertions over 15 requests, exit 0, with no cloud result publication. All 22 isolated probes pass, including the 16 coercion cases, five NUL cases and a valid non-ASCII name.

The destructive-suite safety reproduction was reversed: point the suite at the disposable application database containing one protected marker; startup is rejected with the expected guard error and the marker remains 1. This deliberate rejection is an expected negative test, separate from the final all-green verification against the safe test database. Initial development of an extra URL-parsing test assumed encoded query keys were decoded by PostgreSQL; the driver ignores that encoded key. The test oracle was corrected to reflect the driver's actual effective database, while a recognized PGDBNAME override remains rejected.

The fix does not change migrations or planned borrowing/authentication features. Sprint 1 remains closed and Sprint 2 has not been started. Raw correction logs are under ignored target/els-51. The new database services and temporary credentials are removed after verification. Human approval and merge remain separate from these executed checks.
