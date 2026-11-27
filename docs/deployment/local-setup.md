# Local Packaging and Demonstration

## Scope and prerequisites

This runbook packages the current Spring Boot backend with PostgreSQL 17.5 for a local demonstration. It uses Java 17, Maven Wrapper 3.9.11, Spring Boot 3.5.16, Docker Desktop and Docker Compose. The current revision has no staff authentication: ELS-46 owns that behavior. Keep the application on the loopback boundary described below and do not expose it publicly.

Use a clean tracked checkout. Do not copy another working tree: in particular, do not carry `.env`, `target`, `.git`, Docker volumes, credentials or `Subject/` into the checkout or Docker build context.

```powershell
git clone https://github.com/komayl-dbouk/Equipment-Lending-System.git equipment-lending-system
Set-Location equipment-lending-system
git checkout <reviewed-commit>
if (Test-Path -LiteralPath .env) { throw '.env already exists; inspect it rather than overwriting it.' }
Copy-Item .env.example .env
```

Set private, distinct passwords in `.env`. Keep the supplied database names unless deliberately creating a new isolated demo project. `.env` is ignored and must never be committed.

## Build and test

Set `JAVA_HOME` to a Java 17 JDK. The first command verifies source without a database. The second provisions only the separate test database on loopback port 5433 and runs PostgreSQL integration tests.

```powershell
$env:JAVA_HOME = 'C:\path\to\jdk-17'
.\mvnw.cmd --batch-mode --no-transfer-progress verify
docker compose -p els-tests -f compose.test.yaml --env-file .env up -d --wait
Get-Content .env | ForEach-Object {
    if ($_ -match '^\s*([A-Za-z_][A-Za-z0-9_]*)=(.*)$') {
        Set-Item -Path ("Env:" + $matches[1]) -Value $matches[2]
    }
}
.\mvnw.cmd --batch-mode --no-transfer-progress -Pdatabase-test verify
docker compose -p els-tests -f compose.test.yaml --env-file .env down
```

The application and test databases are separate. Never set `ELS_TEST_DB_URL` to the application database and never reuse the `els-tests` project for the demonstration database.

## Start the packaged demonstration

Use a fresh, task-owned Compose project name beginning with `els-demo-`. Run the Maven verification above before Docker: it creates the exact delivery JAR. Docker then copies only that JAR into a Java 17 runtime image, avoiding a second build with a different Maven environment. `.dockerignore` excludes credentials, Git data, `Subject/` and generated output except the one required packaged JAR. The image runs as a non-root `spring` user.

```powershell
$demoProject = 'els-demo-local'
docker compose -p $demoProject --env-file .env up --build -d --wait
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\demo\wait-for-health.ps1 -BaseUrl http://localhost:8080
```

Only the application is published, at `127.0.0.1:<ELS_APP_PORT>` (default `8080`). PostgreSQL has no host port and is reachable only by the application on the Compose network. Compose waits for PostgreSQL, while `wait-for-health.ps1` retries the real HTTP health endpoint until it reports `UP`; together these are the local readiness condition. If port 8080 is occupied, choose an unused local value for `ELS_APP_PORT` in `.env` and use that port in every URL below.

Flyway owns the empty delivery schema and runs at application start. Hibernate validates the schema; it does not create or update it. Inspect startup status without exposing secrets:

```powershell
docker compose -p $demoProject --env-file .env logs app
```

## Synthetic seed and workflow

After readiness, create and verify a deterministic namespaced demonstration: one synthetic student, two synthetic equipment items, one active loan and one returned loan. Fixture identifiers are fixed; dates are derived from the current local day so the valid loan lifecycle remains repeatable. The script performs real HTTP POST/GET workflow calls and checks the persisted API state.

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\demo\seed-demo.ps1 -BaseUrl http://localhost:8080
Invoke-RestMethod http://localhost:8080/api/equipment
Invoke-RestMethod http://localhost:8080/api/loans
```

The script writes only `target/els45-demo-state.json`, which is ignored. A second run uses that state to verify the same database and makes no write. If the state does not match the running database, it stops rather than guessing identifiers or modifying unrelated data.

## Safe reset and shutdown

Do not reset a normal developer project. The reset helper refuses any project name not beginning with `els-demo-` and requires an explicit confirmation. It removes only that Compose project's disposable volume and this workspace's seed-state file; it does not execute SQL drops or touch other databases.

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\demo\reset-demo.ps1 -ProjectName $demoProject -ConfirmReset
docker compose -p $demoProject --env-file .env up --build -d --wait
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\demo\wait-for-health.ps1 -BaseUrl http://localhost:8080
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\demo\seed-demo.ps1 -BaseUrl http://localhost:8080
docker compose -p $demoProject --env-file .env down
```

For ordinary shutdown without resetting data, use only the final `down` command. Do not add `-v` unless resetting the dedicated disposable `els-demo-*` project.

## Common recovery

- **Application health fails:** inspect `docker compose -p $demoProject --env-file .env logs app` and verify that `.env` contains non-placeholder database credentials.
- **Port conflict:** select a different unused `ELS_APP_PORT`, then restart this same demo project.
- **Maven cannot connect to PostgreSQL:** start `els-tests`, load `.env` into the shell as shown above, and verify the URL ends in `_test`.
- **Authentication changes arrive:** follow [the ELS-46 packaging handoff](els-46-packaging-handoff.md). Current business examples are unauthenticated only until that card integrates.
