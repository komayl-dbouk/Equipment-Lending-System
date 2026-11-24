# ELS-29 Prepared Pull Request

Base: main. Head: ELS-29-student-management. Human reviewer to request: Komayl Dbouk. This is prepared text; no push or PR creation has occurred.

Title: **ELS-29: implement student registration, retrieval and updates**

## Prepared body

Implements [ELS-29](https://equipment-lending-system.atlassian.net/browse/ELS-29) / FR01: POST /api/students and GET/PUT /api/students/{id}. Requests enforce raw length limits, trim required text and normalize email with Locale.ROOT before syntax validation. PUT replaces both editable fields while preserving the generated ID; responses expose only id/name/email and POST supplies Location.

Reuses the existing StudentEntity, shared errors and V1 uniqueness constraint. Confirmed email uniqueness violations become DUPLICATE_EMAIL after transaction rollback; unrelated database failures retain sanitized 500 handling. StudentWrite rejects unknown properties and non-string text fields. No migration or dependency changes.

Validation: 79 database-free tests pass (27 service, 40 controller, 12 shared-error), and Maven Wrapper verify passes with executable JAR packaging. PostgreSQL integration tests compile and cover stored values, own/other-email updates, unchanged state after rejection and competing writes; execution awaits the existing PostgreSQL CI profile. The initial Mockito attachment and packaging-download failures, corrections and exact commands are recorded in [verification evidence](https://github.com/komayl-dbouk/Equipment-Lending-System/blob/ELS-29-student-management/docs/testing/els-29-student-management.md) and [AI log](https://github.com/komayl-dbouk/Equipment-Lending-System/blob/ELS-29-student-management/ai-log/els-29-student-management.md).

Request review from Komayl Dbouk. PostgreSQL CI and human review are required before merging; ELS-29 must not be marked Done before the reviewed merge and required passing checks.

## User-operated publication

```powershell
git push -u origin ELS-29-student-management
```

Then open [the comparison targeting main](https://github.com/komayl-dbouk/Equipment-Lending-System/compare/main...ELS-29-student-management?expand=1), paste the title/body and request Komayl Dbouk as reviewer. Preserve the incremental commit history. No automatic approval or merge is authorized.
