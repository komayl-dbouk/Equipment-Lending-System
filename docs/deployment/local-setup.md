# Local Setup and Verification

## Pinned foundation

The project uses Java 17, Spring Boot 3.5.16, Maven Wrapper 3.9.11 and PostgreSQL 17.5. Spring Boot 3.5.16 requires Java 17 or later and supports Maven 3.6.3 or later; the wrapper pins the project's Maven distribution. Flyway migrations, rather than Hibernate DDL generation, own the delivery schema.

## Configuration

Copy `.env.example` to `.env` and replace both placeholder passwords with separate local secrets. `.env` is ignored. Export the same values to the shell before Maven commands; this prevents credentials from being committed or embedded in configuration.

## Run locally

```powershell
$env:JAVA_HOME = 'C:\path\to\jdk-17'
docker compose --env-file .env up -d --wait
$env:ELS_DB_USERNAME = 'equipment_app'
$env:ELS_DB_PASSWORD = '<your-local-secret>'
$env:ELS_DB_URL = 'jdbc:postgresql://localhost:5432/equipment_lending'
$env:ELS_TEST_DB_USERNAME = 'equipment_test'
$env:ELS_TEST_DB_PASSWORD = '<your-separate-test-secret>'
$env:ELS_TEST_DB_URL = 'jdbc:postgresql://localhost:5433/equipment_lending_test'
docker compose -p els-tests -f compose.test.yaml --env-file .env up -d --wait
.\mvnw.cmd verify
.\mvnw.cmd -Pdatabase-test verify
.\mvnw.cmd spring-boot:run
```

The application migrates the schema and exposes the non-business health endpoint at `http://localhost:8080/actuator/health`. The `database-test` profile verifies connectivity and the migration-created history foundation against PostgreSQL. Stop the application database with `docker compose --env-file .env down` and the separate test database with `docker compose -p els-tests -f compose.test.yaml --env-file .env down`.

`JAVA_HOME` must identify the Java 17 JDK used by the Maven Wrapper. The main Maven verification does not require a database because database integration tests run only when the explicit `database-test` profile is selected.

## Test database safety

Integration tests delete their fixtures and require a disposable database. The standalone compose.test.yaml provisions equipment_lending_test on port 5433 with its own test credentials. Never point ELS_TEST_DB_URL at the application database. The test-only startup guard runs before Flyway or datasource creation: it requires an explicit PostgreSQL URL, a database name ending in _test and a name different from the configured application database. Host aliases and query parameters do not bypass the name check. It is registered only on the test classpath and does not alter the packaged application.

Before running the commands, ensure the password values exported in PowerShell match the corresponding values in .env. Do not reuse the same Compose project for the application and test services.

## Migration ownership

`V1__initial_lending_schema.sql` is the shared baseline for student, equipment and loan tables. Later changes must append a new ordered Flyway migration; never edit an applied migration. Feature owners coordinate cross-feature changes with the shared-foundation owner, especially foreign keys and loan-history constraints.
