# Local Setup and Verification

## Pinned foundation

The project uses Java 17, Spring Boot 3.5.16, Maven Wrapper 3.9.11 and PostgreSQL 17.5. Spring Boot 3.5.16 requires Java 17 or later and supports Maven 3.6.3 or later; the wrapper pins the project's Maven distribution. Flyway migrations, rather than Hibernate DDL generation, own the delivery schema.

## Configuration

Copy `.env.example` to `.env` and replace its placeholder password with a local secret. `.env` is ignored. Export the same values to the shell before Maven commands; this prevents credentials from being committed or embedded in configuration.

## Run locally

```powershell
$env:JAVA_HOME = 'C:\path\to\jdk-17'
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

The application migrates the schema and exposes the non-business health endpoint at `http://localhost:8080/actuator/health`. The `database-test` profile verifies connectivity and the migration-created history foundation against PostgreSQL. Stop the local service with `docker compose --env-file .env down`.

`JAVA_HOME` must identify the Java 17 JDK used by the Maven Wrapper. The main Maven verification does not require a database because database integration tests run only when the explicit `database-test` profile is selected.

## Migration ownership

`V1__initial_lending_schema.sql` is the shared baseline for student, equipment and loan tables. Later changes must append a new ordered Flyway migration; never edit an applied migration. Feature owners coordinate cross-feature changes with the shared-foundation owner, especially foreign keys and loan-history constraints.
