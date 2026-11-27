# Project Agent Guide

## Architecture and ownership

Use one Spring Boot application rooted at `edu.university.equipmentlending`. Feature packages are `student`, `equipment`, and `loan`; each owns controller, DTO, service and feature-specific repository code. Shared code is limited to `shared/config`, `shared/error` and common persistence conventions. Controllers use DTOs only, services own transactions and repositories do not call services or controllers.

Flyway is the sole delivery schema owner. Add sequential migrations under `src/main/resources/db/migration`; never use JPA schema creation or update. Coordinate any migration touching more than one feature with the shared-foundation owner. Preserve PostgreSQL restrictive foreign keys, normalized identifier uniqueness, date checks, history indexes and the active-loan partial unique index.

## Commands

On Windows use `mvnw.cmd`. Required foundation checks are:

```powershell
$ErrorActionPreference = 'Stop'
$env:JAVA_HOME = 'C:\path\to\jdk-17'
if (-not (Test-Path -LiteralPath .env)) { Copy-Item .env.example .env }
# Set private distinct passwords in .env before continuing; never overwrite an existing .env.
.\mvnw.cmd verify
if ($LASTEXITCODE -ne 0) { throw 'Verification failed.' }
docker compose -p els-tests -f compose.test.yaml --env-file .env up -d --wait
if ($LASTEXITCODE -ne 0) { throw 'Test database startup failed.' }
.\tools\demo\import-test-environment.ps1 -EnvFile .env -ProjectName els-tests
.\mvnw.cmd -Pdatabase-test verify
if ($LASTEXITCODE -ne 0) { throw 'Database verification failed.' }
docker compose -p els-tests -f compose.test.yaml --env-file .env down
$demoProject = 'els-demo-local'
docker compose -p $demoProject --env-file .env up --build -d --wait
if ($LASTEXITCODE -ne 0) { throw 'Demo startup failed.' }
.\tools\demo\wait-for-health.ps1 -BaseUrl http://localhost:8080
.\tools\demo\seed-demo.ps1 -ProjectName $demoProject -EnvFile .env
```

The importer uses resolved Compose test credentials/port in the same shell as Maven, including quoted values; do not print its private configuration. The delivery database has no host port and the application runs in Compose, so the old host `spring-boot:run`/localhost delivery JDBC path is not a packaging command. Use the runbook for port overrides. Stop only your demo project with `docker compose -p $demoProject --env-file .env down`; reset volumes only through the explicitly confirmed disposable-project helper. Do not commit `.env` or any credential. Use `./mvnw` on POSIX shells; demo scripts require PowerShell. Run `python tools/demo/test_demo_helpers.py` after helper changes.

## Conventions and scope

Use Java 17, Spring Boot 3.5.16, Maven Wrapper 3.9.11 and PostgreSQL 17.5. Keep text and date normalization, validation and error bodies aligned with the documented contract. Use synthetic data only. Do not add feature endpoints, UI, authentication behavior, student/equipment rules or borrowing/return behavior while working on shared foundation code unless the active card explicitly includes it.

## Testing and review

Run the narrowest relevant checks plus the documented full checks before requesting review. Database integrity tests use PostgreSQL, not H2. Record actual commands, outputs, failures and corrections in evidence; never infer a passing CI run or human approval. Inspect `git diff --check` and the explicit staged file list before every commit. Never stage, ignore, copy, or modify `Subject/` or the original assignment. Keep one card per branch, request the named human reviewer, and never self-approve or self-merge.
