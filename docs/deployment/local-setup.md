# Local Packaging and Demonstration

## Scope and prerequisites

This runbook packages the Spring Boot backend with PostgreSQL 17.5 for a local demonstration. It uses Java 17, Maven Wrapper 3.9.11, Spring Boot 3.5.16, Docker Desktop and Docker Compose. Every business request requires the private staff Basic credential. Keep the application on the loopback boundary described below; remote credential transport requires separately configured and verified HTTPS.

Use a clean tracked checkout. Do not copy another working tree: in particular, do not carry `.env`, `target`, `.git`, Docker volumes, credentials or `Subject/` into the checkout or Docker build context.

```powershell
git clone https://github.com/komayl-dbouk/Equipment-Lending-System.git equipment-lending-system
Set-Location equipment-lending-system
git checkout <reviewed-commit>
if (Test-Path -LiteralPath .env) { throw '.env already exists; inspect it rather than overwriting it.' }
Copy-Item .env.example .env
```

Set private, distinct database passwords plus nonblank `ELS_STAFF_USERNAME` and `ELS_STAFF_PASSWORD` in `.env`. Staff values are deliberately empty in the example: Compose refuses absent/empty values, and application validation also rejects absent/blank values. Never put them in command-line arguments. The importer asks Compose for its resolved test values and never parses or prints passwords; quoting, spaces and interpolation follow Compose semantics. Invoke the importer in the same PowerShell process as Maven, not through a child `powershell -File` process. If local policy blocks reviewed repository scripts, use a process-scoped policy permitted by your environment rather than changing machine policy.

Keep the supplied database names unless deliberately creating a new isolated demo project. `.env` is ignored and must never be committed.

## Build and test

Set `JAVA_HOME` to a Java 17 JDK. The first command verifies source without a database. The second provisions only the separate test database on loopback port 5433 and runs PostgreSQL integration tests.

```powershell
$ErrorActionPreference = 'Stop'
$env:JAVA_HOME = 'C:\path\to\jdk-17'
.\mvnw.cmd --batch-mode --no-transfer-progress verify
if ($LASTEXITCODE -ne 0) { throw 'Maven verification failed.' }
docker compose -p els-tests -f compose.test.yaml --env-file .env up -d --wait
if ($LASTEXITCODE -ne 0) { throw 'Test database startup failed.' }
.\tools\demo\import-test-environment.ps1 -EnvFile .env -ProjectName els-tests
.\mvnw.cmd --batch-mode --no-transfer-progress -Pdatabase-test verify
if ($LASTEXITCODE -ne 0) { throw 'PostgreSQL verification failed.' }
docker compose -p els-tests -f compose.test.yaml --env-file .env down
```

The application and test databases are separate. Never set `ELS_TEST_DB_URL` to the application database and never reuse the `els-tests` project for the demonstration database.

## Start the packaged demonstration

Use a fresh, task-owned Compose project name beginning with `els-demo-`. Run the Maven verification above before Docker: it creates the exact delivery JAR. Docker then copies only that JAR into a Java 17 runtime image, avoiding a second build with a different Maven environment. `.dockerignore` excludes credentials, Git data, `Subject/` and generated output except the one required packaged JAR. The image runs as a non-root `spring` user.

```powershell
$demoProject = 'els-demo-local'
docker compose -p $demoProject --env-file .env up --build -d --wait
if ($LASTEXITCODE -ne 0) { throw 'Demo startup failed; do not seed another application.' }
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\demo\wait-for-health.ps1 -BaseUrl http://localhost:8080
```

Only the application is published, at `127.0.0.1:<ELS_APP_PORT>` (default `8080`). PostgreSQL has no host port and is reachable only by the application on the Compose network. Compose waits for PostgreSQL, while `wait-for-health.ps1` retries the real HTTP health endpoint until it reports `UP`, with cancellation bounded by its overall timeout even for a stalled response; together these are the local readiness condition. If port 8080 is occupied, choose an unused local value for `ELS_APP_PORT` in `.env` and use that port in every URL below.

Flyway owns the empty delivery schema and runs at application start. Hibernate validates the schema; it does not create or update it. Inspect startup status without exposing secrets:

```powershell
docker compose -p $demoProject --env-file .env logs app
```

## Synthetic seed and workflow

After readiness, create and verify a deterministic namespaced demonstration: one synthetic student, two synthetic equipment items, one active loan and one returned loan. Fixture names are fixed; database IDs are server generated and dates are derived from the Beirut-local day so the valid loan lifecycle remains repeatable. The script performs real HTTP POST/GET workflow calls and checks the persisted API state.

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\demo\seed-demo.ps1 -ProjectName $demoProject -EnvFile .env -BaseUrl http://localhost:8080
. .\tools\demo\common.ps1
$demoContext = Get-ElsDemoContext $demoProject .env http://localhost:8080
Invoke-RestMethod "$($demoContext.BaseUrl)/api/equipment" -Headers $demoContext.StaffHeaders -MaximumRedirection 0
Invoke-RestMethod "$($demoContext.BaseUrl)/api/loans" -Headers $demoContext.StaffHeaders -MaximumRedirection 0
Remove-Variable demoContext
```

Before any write, the helper verifies lowercase `els-demo-*` project ownership, checkout labels, actual loopback app binding, internal-only PostgreSQL, the running app database URL/user and effective PostgreSQL database/cluster identity. It resolves private staff settings using Compose semantics, verifies they match the running app, and sends an Authorization header only after these guards. It refuses redirects and stores no credentials in checkpoints. Do not print or serialize `$demoContext`: it contains the private in-memory header. An optional BaseUrl must match that project. Initial seeding also requires empty application tables. A conflicting application on port 8080 cannot pass this target check.

The checkpoint is project-specific: `target/els45-<project-name>.json`. It is atomically written before the first mutation and after each successful step. A rerun checks its target fingerprint plus exact borrower identity, inventory fields, availability and all loan references/dates/return state, then performs only reads. Drift or an incomplete attempt stops without replaying writes. Inspect the problem and reset only this disposable project if appropriate. Legacy unbound checkpoint files are not automatically trusted or deleted.

Use `-StatePath target/custom-demo.json` on both seed and reset if overriding the checkpoint. Paths outside this checkout target directory are refused. No private credentials are stored in the checkpoint.

## Safe reset and shutdown

Do not reset a normal developer project. The reset helper refuses any project name not beginning with `els-demo-` and requires an explicit confirmation. It rejects external/non-project volumes and foreign checkout containers. It removes the selected checkpoint only after Docker down succeeds; a Docker failure preserves it and throws. A custom StatePath must be supplied consistently. It does not execute SQL drops or reset another project. The namespace and confirmation are operational ownership checks, not a substitute for checking the chosen project is yours.

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\demo\reset-demo.ps1 -ProjectName $demoProject -ConfirmReset
docker compose -p $demoProject --env-file .env up --build -d --wait
if ($LASTEXITCODE -ne 0) { throw 'Demo startup failed; do not seed another application.' }
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\demo\wait-for-health.ps1 -BaseUrl http://localhost:8080
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\demo\seed-demo.ps1 -ProjectName $demoProject -EnvFile .env -BaseUrl http://localhost:8080
docker compose -p $demoProject --env-file .env down
```

For ordinary shutdown without resetting data, use only the final `down` command. Do not add `-v` unless resetting the dedicated disposable `els-demo-*` project.

## Common recovery

- **Application health fails:** inspect `docker compose -p $demoProject --env-file .env logs app` and verify that `.env` contains non-placeholder database credentials.
- **Port conflict:** select a different unused `ELS_APP_PORT`, then restart this same demo project.
- **Maven cannot connect to PostgreSQL:** start `els-tests`, run the Compose-backed test importer in the Maven shell, and verify the URL ends in `_test`.
- **Business request receives 401:** verify the private staff values match the running app, then restart only your project after changing them. Health intentionally remains public. Follow the [staff access boundary](staff-access.md).

## Automated helper checks

Run `python tools/demo/test_demo_helpers.py` with Windows PowerShell or pwsh installed. These checks isolate Docker calls and exercise real loopback HTTP for authenticated reruns, rejected staff-target changes, fixture drift and a stalled response. CI also runs a real Docker package smoke check: missing/wrong access and no committed writes, authorized seed/rerun, production duplicate-email log privacy, reset/reseed and preserved target guards. Fake-Docker unit checks are not represented as a container rehearsal.
