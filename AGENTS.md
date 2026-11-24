# Project Agent Guide

## Architecture and ownership

Use one Spring Boot application rooted at `edu.university.equipmentlending`. Feature packages are `student`, `equipment`, and `loan`; each owns controller, DTO, service and feature-specific repository code. Shared code is limited to `shared/config`, `shared/error` and common persistence conventions. Controllers use DTOs only, services own transactions and repositories do not call services or controllers.

Flyway is the sole delivery schema owner. Add sequential migrations under `src/main/resources/db/migration`; never use JPA schema creation or update. Coordinate any migration touching more than one feature with the shared-foundation owner. Preserve PostgreSQL restrictive foreign keys, normalized identifier uniqueness, date checks, history indexes and the active-loan partial unique index.

## Commands

On Windows use `mvnw.cmd`. Required foundation checks are:

```powershell
$env:JAVA_HOME = 'C:\path\to\jdk-17'
Copy-Item .env.example .env
docker compose --env-file .env up -d --wait
$env:ELS_DB_USERNAME = 'equipment_app'
$env:ELS_DB_PASSWORD = '<your-local-secret>'
$env:ELS_DB_URL = 'jdbc:postgresql://localhost:5432/equipment_lending'
$env:ELS_TEST_DB_USERNAME = $env:ELS_DB_USERNAME
$env:ELS_TEST_DB_PASSWORD = $env:ELS_DB_PASSWORD
$env:ELS_TEST_DB_URL = $env:ELS_DB_URL
.\mvnw.cmd verify
.\mvnw.cmd -Pdatabase-test verify
.\mvnw.cmd spring-boot:run
```

Run `docker compose --env-file .env down` when finished. Do not commit `.env` or any credential. Use `./mvnw` on POSIX shells.

## Conventions and scope

Use Java 17, Spring Boot 3.5.16, Maven Wrapper 3.9.11 and PostgreSQL 17.5. Keep text and date normalization, validation and error bodies aligned with the documented contract. Use synthetic data only. Do not add feature endpoints, UI, authentication behavior, student/equipment rules or borrowing/return behavior while working on shared foundation code unless the active card explicitly includes it.

## Testing and review

Run the narrowest relevant checks plus the documented full checks before requesting review. Database integrity tests use PostgreSQL, not H2. Record actual commands, outputs, failures and corrections in evidence; never infer a passing CI run or human approval. Inspect `git diff --check` and the explicit staged file list before every commit. Never stage, ignore, copy, or modify `Subject/` or the original assignment. Keep one card per branch, request the named human reviewer, and never self-approve or self-merge.
