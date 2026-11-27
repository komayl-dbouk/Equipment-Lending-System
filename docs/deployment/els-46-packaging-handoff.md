# ELS-46 Packaging Integration Handoff

ELS-46 integrates the ELS-45 delivery package with the single staff Basic credential. Private `ELS_STAFF_USERNAME` and `ELS_STAFF_PASSWORD` values are required by Compose and forwarded only to the runtime application; they are blank in `.env.example`. The application maps absent values to blank and rejects them through validated properties, including startup outside Compose.

The exact `GET /actuator/health` remains public and sanitized. Every `/api/**` call requires staff authentication. Basic encodes credentials; the loopback HTTP demonstration does not establish remote TLS. See [staff access](staff-access.md) and the [runbook](local-setup.md).

The guarded seed preserves disposable-project/checkout/database/cluster checks, empty-target requirements and atomic checkpoints. It additionally verifies resolved staff settings against the running app before creating its private in-memory header. It refuses redirects; checkpoints contain synthetic fixture identity only. Do not print or serialize the context object because it includes that header.

The CI package smoke uses generated private settings, verifies missing/wrong read and write rejection plus independent SQL counts, then completes the authorized seed/rerun/drift/reset/reseed workflow. A real duplicate-email conflict also exercises production app-log privacy. Helper checks retain fake-Docker separation from this real container proof.

ELS-47 must repeat final integrated release acceptance on the human-merged source, including this protected delivery workflow. Browser authentication/CSRF/origin policy, verified remote HTTPS and the final distribution notices remain separate delivery gates; none is inferred from local Basic authentication.
