# ELS-46 Staff Access, Privacy and Licensing Review

## Summary

- require one environment-configured staff HTTP Basic credential for every `/api/**` operation
- fail startup for missing/blank credential configuration and return one sanitized `401` shape
- expose only aggregate `/actuator/health`, keep authentication stateless and preserve all
  authorized business contracts
- update validation clients, OpenAPI, architecture, traceability and the ELS-45 deployment handoff
- record the privacy/log review, resolved dependency licenses, obligations and maintenance findings

## Verification

- `mvnw.cmd verify`: 198 passed
- `mvnw.cmd -Pdatabase-test verify`: 198 unit/controller plus 193 integration tests passed
- packaged JAR: 86 HTTP requests, 651 assertions, 43 database checks and 3 races passed
- missing/wrong credentials and an unauthenticated mutation returned `401`; independent SQL state
  remained unchanged; the authorized workflow then passed
- OpenAPI JSON, PowerShell and Python syntax checks and `git diff --check` passed

## Boundaries and review request

Local evidence uses loopback HTTP only. Remote use requires externally terminated and verified
HTTPS. This change does not add multiple accounts, role management, UI, seed data, schema changes,
automated secret rotation, rate limiting or ELS-49 behavior. No vulnerability scan or legal
approval is claimed; release packaging still needs an exact third-party notices bundle.

Please review the fail-closed configuration, sanitized error/public-health boundary, no-write
oracles, shared-credential limitations and license obligations. Requested reviewer: Hussein Chreif
(`MaxHusseinChreif`). Do not self-approve or merge.
