# ELS-36 packaged lending workflow

These tools exercise real loopback HTTP against the executable delivery JAR and compare results with PostgreSQL through independent psql connections. Python uses only its standard library. The runner requires Windows PowerShell, Java 17, Python 3, psql from PostgreSQL 17.5, and a prior Maven verification build.

The database-test profile remains the automated-test entry point. Follow [local setup](../../docs/deployment/local-setup.md) for separate app/test services and credentials. For the live runner, provision an additional **empty disposable workflow database ending in _test**, owned by a separate workflow user. It must differ from ELS_TEST_DB_URL's effective database. An isolated native PostgreSQL instance of the pinned version is supported when Docker is unavailable; never reuse another developer's databases or overwrite .env.

Supply credentials through the process environment or a private ignored/local configuration. Do not put passwords on the command line. The example names below identify separate task-owned databases; port 15436 is illustrative and must be checked for conflicts.

The runner also requires private `ELS_STAFF_USERNAME` and `ELS_STAFF_PASSWORD` values. It sends
them only in Authorization headers for protected API calls, never to the public health check or
the JSON evidence file. It proves missing/invalid rejection and an authorized workflow over
loopback HTTP; it does not prove remote HTTPS.

```powershell
$env:JAVA_HOME = 'C:\path\to\jdk-17'
$env:ELS_TEST_DB_URL = 'jdbc:postgresql://127.0.0.1:15436/equipment_lending_test'
$env:ELS_TEST_DB_USERNAME = 'equipment_test'
# Set ELS_TEST_DB_PASSWORD through your private environment.
$env:ELS_DB_URL = 'jdbc:postgresql://127.0.0.1:15436/equipment_lending_workflow_test'
$env:ELS_DB_USERNAME = 'equipment_workflow'
# Set ELS_DB_PASSWORD separately through your private environment.
$env:ELS_PSQL = 'C:\path\to\postgresql-17.5\bin\psql.exe'

.\mvnw.cmd --batch-mode --no-transfer-progress -Pdatabase-test '-Dtest=DatabaseTestSafetyProcessorTest' '-Dit.test=LendingIntegrityIT,LendingQueriesIT' verify
.\mvnw.cmd verify
.\mvnw.cmd -Pdatabase-test verify
.\tools\validation\run-lending-workflow.ps1
```

The Python preflight rejects non-loopback targets, non-_test names, URL query overrides, identical app/test database names or users, an unexpected effective name/user/version, and nonempty workflow tables. It runs before app startup. The database-test startup guard and its existing tests protect the separate integration-test target. Staff access checks are part of the live workflow; HTTPS remains outside this loopback runner.

The PowerShell runner checks that its port is free, that test classes are excluded from the delivery JAR, and starts only the packaged JAR. It builds a separate single-class fixture JAR from `src/test/java/elsvalidation/FixedClockConfiguration.java`, loading it explicitly with Spring Boot PropertiesLauncher and spring.main.sources. This changes the Clock only for this validation process. The fixed business day is 2030-06-15 in Asia/Beirut. HTTP error timestamps and overdue results prove that Clock is active. No production configuration/dependency/endpoint or schema is changed.

The workflow covers student/equipment maintenance, empty/ordered/filter results, borrow/duplicate/return/repeat/reborrow, history/deletion protection, strict invalid inputs and independent SQL snapshots. The three live races use a thread barrier and a separate PostgreSQL equipment lock; both requests must be observed waiting before releasing the lock. Either legitimate serial winner is accepted. No arbitrary sleep chooses the winner.

Results default to `target/els36-http-results.json`; application logs remain in target/. `-Port`, `-ExpectedDatabase`, `-Psql` and `-OutputPath` override runner defaults. The runner always stops only its own application PID and leaves synthetic rows for inspection. To rerun against the same disposable database, explicitly remove only this tool's fixtures:

```powershell
python tools/validation/lending_workflow.py --expected-database equipment_lending_workflow_test --cleanup-owned-fixtures
.\tools\validation\run-lending-workflow.ps1
```

Cleanup uses only ELS36 inventory-code and synthetic email namespaces after repeating target checks. It does not drop schema/databases. Stop/remove only the database instance or containers provisioned for your run when finished. The committed acceptance report distinguishes actual executed commands/results from these reusable instructions.

## Requirement-scoped release acceptance

From a clean tracked checkout, with Java 17 in `JAVA_HOME` and a working Docker engine:

```powershell
python tools/validation/run-release-acceptance.py
```

The runner creates one uniquely labeled PostgreSQL 17.5 container using tmpfs, private generated process credentials, separate integration/workflow databases and users, and unused loopback ports. It runs both full Maven profiles, starts the delivery JAR with the explicit test-only Clock fixture, and extends the unchanged lending workflow/SQL/race oracles with requirement-scoped missing-target, raw field/date boundary, rollback and staff-access checks. It stops its own Java process and removes only its labeled container. It never loads or overwrites `.env` and does not reset/prune Docker.

Results are written to `target/els47-acceptance/results.json`; ignored per-run logs stay below that directory. The result binds the source revision, JAR/configuration hashes, effective database, exact requests, SQL checks, coordinated races, scenario events, JUnit methods and cleanup. Request and SQL ranges are one-based inclusive; an empty range is null. Forced-failure triggers are removed before completion. A failed assertion or cleanup makes the command fail and retains a failed result rather than a release pass.

CI uses `--prepared-build` immediately after its full Maven verification to reuse that job's package and JUnit results. The separate real Docker smoke runs the ordinary application image without the test Clock and retains `target/els47-package-results.json`, including its source/JAR/image identities and successful owned cleanup. Both JSON files are retained with the CI test reports. Packaged-JAR boundary evidence, ordinary Docker delivery evidence and technical architecture/historical-quality review remain distinct in the [ELS-47 acceptance report](../../reports/sprint-4/els-47-final-release-acceptance.md). Neither local HTTP nor these checks establish remote TLS, browser acceptance, human approval or submission.
