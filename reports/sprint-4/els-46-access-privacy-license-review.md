# ELS-46 Staff Access, Privacy and Dependency Review

Owner: Komayl Dbouk. Assigned reviewer: Hussein Chreif (`MaxHusseinChreif`). Branch:
`ELS-46-staff-access-privacy`. Baseline: `500eabe`. Verified implementation commit:
`ce86f340b165de91980dc35c02aa2bb97dcf7591`.

## Outcome and acceptance mapping

ELS-46 adds a deliberately small delivery boundary without changing student, equipment, loan,
return or query behavior. One staff username and password are required from the private process
environment. Spring Security authenticates every `/api/**` request with HTTP Basic, creates no
session, and exposes only the exact sanitized `/actuator/health` route publicly. All other routes
are denied. Missing or blank configuration fails startup.

| Acceptance area | Implemented control | Observed evidence |
| --- | --- | --- |
| Missing credential | Common authentication entry point returns the six-field `401 UNAUTHORIZED` contract | MockMvc integration test and packaged request passed |
| Wrong credential | Same status, code, message and challenge as a missing credential | MockMvc integration test and packaged request passed |
| Rejected write | Security filter rejects before controller/service entry | Student count and full packaged database snapshot were unchanged |
| Authorized behavior | In-memory staff principal has only `ROLE_STAFF`; existing tests receive the configured Basic header | Existing business suites and full packaged workflow passed unchanged |
| Public health | Exact `/actuator/health` permit rule; details and components disabled | Strict body `{"status":"UP"}` passed |
| Fail closed | `@Validated` required configuration properties have no production defaults | Missing and blank property context tests passed |
| Transport | Basic is allowed for the loopback demonstration only | Remote HTTPS is required by documentation but was not configured or verified |

CSRF is disabled because the API is stateless and authenticates every protected request through
the Authorization header, not ambient browser cookies. The credential is encoded rather than
encrypted on the wire. The application hashes the configured password for in-memory comparison,
but the original value necessarily enters the process through its environment.

## Privacy, secret and log review

- No credential value, Authorization header or real personal record is tracked. Repository
  examples contain placeholders only, and all test/workflow identities are synthetic.
- The authentication entry point does not log the rejected credential or exception. Missing and
  wrong credentials receive indistinguishable public error details.
- The packaged workflow JSON records the scheme, protected path and environment source, not the
  header or credential. A targeted scan found no staff username or credential field in the JSON
  or application logs.
- The existing `database-test` profile enables DEBUG logging and therefore prints synthetic DTO
  fixture values and SQL. This is acceptable only for disposable synthetic test data. Production
  defaults do not enable that profile, but operators must not enable it with real student data.
- A shared credential cannot attribute actions to individual staff and has no automated rotation,
  lockout, rate limiting or multi-user lifecycle. Those are explicit residual limitations, not
  hidden features and not an implementation of ELS-49.
- Process environment readers with host-level access may be able to inspect the secret. Deployment
  must restrict process/host access, provision the two variables privately and restart after a
  rotation.

The ELS-45 handoff is limited to provisioning `ELS_STAFF_USERNAME` and `ELS_STAFF_PASSWORD` in the
selected private runtime mechanism, using `/actuator/health` for health checks, and ensuring remote
traffic reaches this application only through verified HTTPS. No packaging, seed or runbook work
was taken into ELS-46.

## Resolved dependency and license inventory

The inventory comes from the effective Maven runtime tree, not version guesses from the source
POM. It covers direct dependencies and the relevant runtime families that are redistributed in
the executable JAR. Test-only libraries are identified separately. This is an engineering review,
not legal advice and not a vulnerability scan.

| Dependency family | Resolved version | Scope | License finding and authoritative source |
| --- | --- | --- | --- |
| Spring Boot starters | 3.5.16 | direct/runtime | Apache-2.0; [official Boot line](https://docs.spring.io/spring-boot/3.5/) |
| Spring Framework | 6.2.19 | transitive/runtime | Apache-2.0; Spring project license metadata |
| Spring Security | 6.5.11 | direct and transitive/runtime | Apache-2.0; [official community/license page](https://docs.spring.io/spring-security/reference/community.html) |
| Spring Data JPA | 3.5.13 | transitive/runtime | Apache-2.0; Spring project license metadata |
| Hibernate ORM | 6.6.53.Final | transitive/runtime | LGPL-2.1; [official 6.6 release record](https://hibernate.org/orm/releases/6.6/) and [license guidance](https://hibernate.org/community/license/) |
| Hibernate Validator | 8.0.3.Final | transitive/runtime | Apache-2.0; [official Hibernate license guidance](https://hibernate.org/community/license/) |
| Flyway core and PostgreSQL module | 11.7.2 | direct/runtime | Apache-2.0; [official repository license](https://github.com/flyway/flyway) |
| PostgreSQL JDBC | 42.7.11 | direct/runtime | BSD-2-Clause; [official pgJDBC license](https://jdbc.postgresql.org/license/) |
| Apache Tomcat embed | 10.1.55 | transitive/runtime | Apache-2.0; [official Tomcat legal page](https://tomcat.apache.org/legal.html) |
| Jackson | 2.21.4 | transitive/runtime | Apache-2.0; [official project FAQ](https://github.com/FasterXML/jackson/wiki/FAQ) |
| HikariCP | 6.3.3 | transitive/runtime | Apache-2.0; [official repository](https://github.com/brettwooldridge/HikariCP) |
| Logback | 1.5.34 | transitive/runtime | licensee choice of EPL-2.0 or LGPL-2.1; [official license page](https://logback.qos.ch/license.html) |
| SLF4J | 2.0.18 | transitive/runtime | MIT; [official license page](https://www.slf4j.org/license.html) |
| Log4j API/SLF4J bridge | 2.24.3 | transitive/runtime | Apache-2.0; [official project](https://logging.apache.org/log4j/2.x/) |
| Micrometer | 1.15.12 | transitive/runtime | Apache-2.0; artifact/project metadata |
| SnakeYAML | 2.4 | transitive/runtime | Apache-2.0; artifact/project metadata |
| AspectJ weaver | 1.9.25.1 | transitive/runtime | EPL-2.0; artifact/project metadata |
| Byte Buddy | 1.17.8 | transitive/runtime | Apache-2.0; artifact/project metadata |
| ANTLR runtime | 4.13.2 | transitive/runtime | BSD-3-Clause; artifact/project metadata |
| Jakarta/JAXB/Angus API and runtime family | Boot-managed versions | transitive/runtime | Mixed EPL-2.0, EDL-1.0, BSD-3-Clause and GPL-2.0-with-Classpath-exception choices; preserve each JAR's embedded license/notices |
| Spring Boot test starter and Spring Security test | 3.5.16 / 6.5.11 | direct/test only | Apache-2.0; not included as production runtime dependencies |

No incompatible license was identified for the present use of unmodified libraries, provided a
redistributed application preserves the applicable licenses, copyright notices and NOTICE files.
Apache-2.0 redistribution requires the license and notices and identification of modifications.
BSD and MIT require their copyright/license text. For Hibernate ORM, distribution triggers the
LGPL obligations for the Hibernate-covered component, including corresponding source availability;
independent application code is not thereby relicensed according to Hibernate's official guidance.
For Logback, release packaging must select and comply with either EPL-2.0 or LGPL-2.1 rather than
silently omitting the dual-license terms.

Two maintenance observations remain. Hibernate ORM 6.6 is in limited-support mode, although it is
the line documented as compatible with Spring Boot 3.5. Log4j 2.24.x is end-of-maintenance; the
resolved artifacts are API/bridges rather than Log4j Core. ELS-46 does not override the managed
Spring Boot dependency set. A release owner should assess a supported-stack upgrade separately and
regenerate the inventory after any dependency change.

The repository does not yet assemble a third-party notices bundle. Before external binary
distribution, ELS-45 or the release owner must copy the exact embedded license and NOTICE materials
for the resolved artifacts into the distribution. No CVE/SCA tool was run, so this review makes no
vulnerability or supply-chain approval claim.

## Executed verification and corrections

| Command/check | Actual result |
| --- | --- |
| Focused configuration and security integration tests | 3 configuration plus 5 security tests passed |
| `mvnw.cmd verify` | 198 tests passed; zero failures, errors or skips; executable JAR built |
| `mvnw.cmd -Pdatabase-test verify` | 198 unit/controller plus 193 PostgreSQL integration tests passed; zero failures, errors or skips |
| Packaged-JAR HTTP/SQL workflow | 86 requests, 651 assertions, 43 database checks and 3 coordinated races passed against PostgreSQL 17.5 |
| Static client/contract checks | OpenAPI JSON parsed, both PowerShell scripts parsed, Python AST parsed, and `git diff --check` passed |
| Secret/privacy scan | No credential value or Authorization header found in tracked changes, packaged JSON or packaged application logs |

The packaged result tested commit `ce86f340b165de91980dc35c02aa2bb97dcf7591`; the JAR SHA-256
was `4d01a6da992b6b5601eb382ebb7ff06f4c946a151a0931aa0fe3dbaa13a22e04`.
Its first three protected calls observed missing-read `401`, wrong-read `401`, and rejected-write
`401`; the subsequent SQL snapshot was unchanged before the authorized workflow continued.

Corrections are part of the evidence:

1. The missing-property unit test initially inherited Maven's synthetic test credentials. The
   context runner now removes that property source so missing and blank configuration are real.
2. The strict health assertion initially expected generic JSON. Actuator correctly returns its
   vendor JSON media type; the test now accepts `application/*+json` while retaining an exact body.
3. The managed shell initially sent the Maven wrapper to an unwritable cache path. Verification
   was rerun with the existing user Maven cache; no build setting changed.
4. The packaged harness needed an older-compatible cryptographic RNG call and a process-only
   PowerShell execution-policy bypass.
5. A temporary batch bridge passed a simple SQL probe but could not preserve the runner's complex
   SQL argument quoting. The installed native PostgreSQL client was used instead. Only the final
   complete run is counted as acceptance evidence.

The task-owned workflow database container/network were removed after the run. The integration
database is removed at task handoff. Jira transitions/comments are intentionally left to the user.
Human review, remote CI, remote TLS and merge are not inferred from local evidence.
