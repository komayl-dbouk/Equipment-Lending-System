# ELS-36 packaged lending workflow

These tools exercise real loopback HTTP against the executable delivery JAR and compare results with PostgreSQL through independent psql connections. Python uses only its standard library. The runner requires Windows PowerShell, Java 17, Python 3, psql from PostgreSQL 17.5, and a prior Maven verification build.

The database-test profile remains the automated-test entry point. Follow [local setup](../../docs/deployment/local-setup.md) for separate app/test services and credentials. For the live runner, provision an additional **empty disposable workflow database ending in _test**, owned by a separate workflow user. It must differ from ELS_TEST_DB_URL's effective database. An isolated native PostgreSQL instance of the pinned version is supported when Docker is unavailable; never reuse another developer's databases or overwrite .env.

Supply credentials through the process environment or a private ignored/local configuration. Do not put passwords on the command line. The example names below identify separate task-owned databases; port 15436 is illustrative and must be checked for conflicts.

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

The Python preflight rejects non-loopback targets, non-_test names, URL query overrides, identical app/test database names or users, an unexpected effective name/user/version, and nonempty workflow tables. It runs before app startup. The database-test startup guard and its existing tests protect the separate integration-test target. Both controls are intentional; neither establishes authentication or HTTPS, which remain ELS-46.

The PowerShell runner checks that its port is free, that test classes are excluded from the delivery JAR, and starts only the packaged JAR. It builds a separate single-class fixture JAR from `src/test/java/elsvalidation/FixedClockConfiguration.java`, loading it explicitly with Spring Boot PropertiesLauncher and spring.main.sources. This changes the Clock only for this validation process. The fixed business day is 2030-06-15 in Asia/Beirut. HTTP error timestamps and overdue results prove that Clock is active. No production configuration/dependency/endpoint or schema is changed.

The workflow covers student/equipment maintenance, empty/ordered/filter results, borrow/duplicate/return/repeat/reborrow, history/deletion protection, strict invalid inputs and independent SQL snapshots. The three live races use a thread barrier and a separate PostgreSQL equipment lock; both requests must be observed waiting before releasing the lock. Either legitimate serial winner is accepted. No arbitrary sleep chooses the winner.

Results default to `target/els36-http-results.json`; application logs remain in target/. `-Port`, `-ExpectedDatabase`, `-Psql` and `-OutputPath` override runner defaults. The runner always stops only its own application PID and leaves synthetic rows for inspection. To rerun against the same disposable database, explicitly remove only this tool's fixtures:

```powershell
python tools/validation/lending_workflow.py --expected-database equipment_lending_workflow_test --cleanup-owned-fixtures
.\tools\validation\run-lending-workflow.ps1
```

Cleanup uses only ELS36 inventory-code and synthetic email namespaces after repeating target checks. It does not drop schema/databases. Stop/remove only the database instance or containers provisioned for your run when finished. The committed acceptance report distinguishes actual executed commands/results from these reusable instructions.
