# ELS-42 Safe Refactorings

| Field | Record |
| --- | --- |
| Request | Apply exactly three approved behavior-preserving refactorings after ELS-41 merged, verify each one, and deliver review evidence. |
| Baseline / branch | Main `c1a621d`; `ELS-42-safe-refactorings`. |
| Approval | The assistant proposed the constraint matcher, loan mapper, and validation factory; the user explicitly approved before production edits. |
| Refactoring 1 | `8ec3736` centralizes exact Hibernate cause-chain SQLSTATE/constraint matching in `shared/persistence/DatabaseConstraintMatcher`. |
| Refactoring 2 | `3d572f8` centralizes the shared six-field loan DTO construction in package-private `LoanMapper`. |
| Refactoring 3 | `b8bcd3b` adds and adopts `ApiException.validationFailed()` at the three repeated boundaries. |
| Documentation | Updated the architecture package rules and class model for `shared.persistence` and `LoanMapper`. |
| Focused tests | Matcher: 59 unit + 12 safety + 84 integration. Mapper: 8 unit + 12 safety + 110 integration. Validation: 46 unit/controller. All passed. |
| Full tests | Standard verify: 195 passed. Database profile: 195 unit/controller + 188 PostgreSQL integration passed. Zero failures/errors/skips in corrected final runs. |
| Packaged check | Delivery JAR and `docs/api/examples.ps1` passed on isolated port 18042 against local PostgreSQL 17.5. |
| Corrections | Restored a still-needed import after an initial compile error; corrected guessed DB credentials; avoided changing a stale app volume by using the verified test service; retried after a transient port collision. No assertion was weakened. |
| Preservation | No migration, dependency, endpoint, query, entity mapping, transaction/lock, business rule, or original borrowing red-test change. `Subject/` remained untouched. |
| Scope limit | No coverage/static-analysis improvement, latency gain, human effort, approval, CI result, or merge is inferred. ELS-43 remains the quality-comparison owner. |
| Jira | The connected `lending-system.atlassian.net` resource exposes only `KAN`; ELS-42 is unavailable, so no Jira state/comment is claimed. |
| Remote delivery | Branch push succeeded. GitHub PR creation returned `403 Resource not accessible by integration`; GitHub CLI was unavailable and no credential workaround was used. No PR, reviewer request, or PR-triggered CI result is claimed. |
| Review boundary | Request `MaxHusseinChreif`; do not self-approve or merge. |

The detailed narrative and structured command outcomes are in [the execution report](../reports/sprint-3/els-42-safe-refactorings.md) and [verification JSON](../reports/sprint-3/els-42-verification-results.json).
