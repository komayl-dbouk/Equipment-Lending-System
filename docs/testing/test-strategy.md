# Equipment Lending Test Strategy

Version 2. Jira: [ELS-24](https://equipment-lending-system.atlassian.net/browse/ELS-24). Owner: Hussein Chreif. Peer reviewer: Hussein Chalhoub. This strategy turns the detailed requirements into test objectives and evidence to collect throughout the four sprints. Human peer decisions and execution results remain to be recorded.

## Objectives and scope

Verify that staff can manage students and equipment, create valid loans, record returns, detect overdue loans and retain history. Protect against duplicate identifiers, double lending, partial writes and exposure of private information. Check the externally visible outcome and persisted state, not just whether a method executes.

Scope covers the backend, database, staff access configuration, documented API and reproducible local deployment. A frontend, student accounts, notifications, fines and institutional integrations are outside the release. No application or build exists yet; this document defines planned checks and reports no passing tests or measured coverage.

## Requirements and open decisions

FR01 to FR07 and NFR01 to NFR05 now refer to [the detailed requirements](../requirements/requirements.md). The [architecture](../design/architecture.md), ADRs and [API contract](../api/contract.md) define PostgreSQL, calendar-date representation, normalization, payloads, errors and filter semantics. Hussein Chalhoub's ELS-23 setup will establish exact Maven/JDK/tool commands.

Use normalized email/inventory codes, LocalDate with an injected Asia/Beirut Clock, AND-combined history filters, the documented error/response schemas and the PostgreSQL item-lock plus active-loan unique-index design. Due date must be later than borrowing date. Overdue means an active loan whose due date is strictly earlier than today; due today is not overdue. A return date cannot precede borrowing. Availability derives from active loans. Record actual human adoption of these prepared baseline decisions before substantial implementation.

## Test levels and responsibilities

| Level | Purpose and examples | Primary responsibility | Execution |
| --- | --- | --- | --- |
| Unit | Service behavior, invalid dates, return once, clock boundaries and business exceptions | Each feature owner | Every related change |
| Controller | Request validation, JSON/error contract and 201/400/404/409 mappings; sanitized unexpected errors | Feature owner with Hussein Chalhoub | Every API change |
| Database | Unique identifiers, relationships, historical deletion restriction and transaction rollback | Komayl Dbouk, with feature owners | Persistence changes |
| Integration | HTTP to controller/service/repository/database; assert returned data and persisted effects | Mostafa Younes and feature owners | Each completed workflow |
| Concurrency | Competing loan requests cannot create two active loans | Komayl Dbouk, independently reviewed by Hussein Chreif | Sprint 2 and release regression |
| System | Start packaged application, use documented endpoints and confirm configuration/data persistence | Hussein Chalhoub | Delivery milestones |
| Acceptance | Staff scenarios with explicit expected and observed outcomes | Mostafa Younes; all members demonstrate | Sprint reviews and final environment |
| Security and privacy | Unauthorized denial, authorized flow, sanitized errors/logs and synthetic data | Komayl Dbouk and Hussein Chalhoub | Access/error changes and release |
| Regression | Previously working rules still hold after integration, correction or refactoring | Change author; reviewer checks evidence | Every integration and final release |

Use isolated service tests for fast rule feedback and real persistence tests for database behavior. Controller tests may mock a service but cannot establish database integrity. In-memory database results alone cannot establish concurrency correctness for another database. Each member owns tests for their implementation; a reviewer checks the expected outcome independently.

## Environment and test data

Use pinned project dependencies, Maven Wrapper and a reproducible test profile once ELS-23 creates them. Use the selected delivery database for integrity/concurrency tests; a disposable container or isolated local database is acceptable. Record database version, schema/migration version, configuration and startup command with results. If Docker is unavailable, use an isolated local instance of the same database; record a blocked test rather than substituting unrelated evidence.

Create synthetic students such as Test Student One and borrower.one@example.test, physical items with distinct codes such as CAM-001 and LAP-001, and loans in active/returned/overdue states. No real student data or committed credentials. Reset data between independent cases and separate delivery-demo data from automated tests. Persisted data must be inspected through an independent query/assertion when state integrity is the subject of a test.

Inject a clock or equivalent controlled date source. Include the day before, on and after a due date, an equal borrowing/due date, a due date before borrowing, and a return on/before/after borrowing as relevant. Do not add unrelated calendar or length constraints unless ELS-21 explicitly requires them.

## Initial scenario catalog

These are planned cases, not implemented test names or results. Bind each to a concrete test method and requirement after implementation.

| ID | Requirement | Scenario and expected outcome |
| --- | --- | --- |
| TC01 | FR01 | Valid student registration returns 201 and stores the submitted allowed fields |
| TC02 | FR01 | Retrieve/update an existing student returns agreed fields; missing ID returns 404 |
| TC03 | FR01 | Invalid name/email returns 400 with no row written; duplicate normalized email returns 409 |
| TC04 | FR02 | Valid equipment create/list/update succeeds with a unique inventory code |
| TC05 | FR02 | Invalid item payload returns 400, duplicate code 409 and unknown update/delete ID 404 |
| TC06 | FR02 | Deleting unreferenced equipment returns 204 and removes that item |
| TC07 | FR03 | Registered student plus available item and valid dates creates one correct loan and returns 201 |
| TC08 | FR03 | Missing student or item returns 404 and creates no loan |
| TC09 | FR03 | Due date equal to or before borrowing date returns 400 and creates no loan |
| TC10 | FR03 | Borrowing an item with an active loan returns 409 and preserves the existing loan |
| TC11 | FR04, NFR01 | Two competing valid requests create exactly one active loan; one succeeds and the other returns a conflict |
| TC12 | FR05 | A valid return sets the correct return date, retains loan history and makes the item available |
| TC13 | FR05 | Returning twice returns 409 without changing the original return date; unknown loan returns 404 |
| TC14 | FR05 | A return before borrowing returns 400; a return on borrowing date is permitted by the stated rule |
| TC15 | FR05 | After return, a new loan can be created for the same item while the original history remains |
| TC16 | FR06 | available=true excludes active-loan items and includes unloaned/returned items |
| TC17 | FR06 | Active loan due yesterday is overdue; due today/tomorrow and returned loans are excluded |
| TC18 | FR06 | Student/equipment history filters return matching records; no matches returns an empty list |
| TC19 | FR07 | Deleting an item referenced by active or returned loans is rejected and history is unchanged |
| TC20 | NFR01 | Failed writes roll back fully and leave no orphaned or partially updated records |
| TC21 | NFR03 | Protected staff operations reject missing/invalid credentials and permit valid staff credentials |
| TC22 | NFR03 | Error bodies/logs avoid stack traces, secrets and unnecessary personal data |
| TC23 | NFR02 | Clean-clone build, full suite, startup and principal workflow succeed using the runbook |
| TC24 | FR01, FR02 | Normalize padded/case-varied identifiers, reject unknown fields and overlength/raw blank inputs, allow same-record self-email/code updates and reject another record's identifier |
| TC25 | FR06 | available=false selects active-loan items; combined history filters use AND; unmatched valid IDs yield []; malformed/nonpositive filters return 400 |

For TC11 use separate transactions/connections and coordinate the request start so the test exercises contention. Assert HTTP outcomes and final database row counts and active state. A sequential duplicate-request test or a mocked repository test is insufficient. If a losing request currently produces an internal server error, report and correct the contract violation rather than weakening the oracle.

## Test first development and generated test review

Use ELS-27 specification and ELS-32 tests for the substantial borrowing feature. Preserve specification and human-reviewed plan, test commit, expected failing output, implementation commit, green output, any refactoring and full regression result. The failure must relate to the intended behavior; an unrelated setup failure is not satisfactory evidence. Keep main green while preserving the feature branch history. Do not squash away the chronology needed for assessment.

At ELS-39 manually audit a representative set of at least ten AI-assisted tests. Record requirement, classification (meaningful, weak, wrong oracle, missing scenario), reason and correction. Correct at least three identified weaknesses; do not invent weaknesses or write deliberately useless tests to reach a quota. Review assertions for state, response contents and absence of side effects where relevant.

At ELS-40 establish green, temporarily introduce a genuine rule defect, run relevant tests, retain failures and unexpected passes, restore the implementation and run the complete suite. The final code must contain no deliberate defect. At ELS-42 record separate before/after evidence for at least three actual refactorings with passing regression checks.

## Entry and exit criteria

A feature is ready for testing when criteria and expected outcomes are reviewed, dependencies are understood, test data/environment are available and the agent's substantial implementation plan has human approval. A planned test may be written earlier for the designated test-first feature. Pending requirements decisions are explicit blockers to final test-oracle approval.

A feature exits when its success/error/boundary criteria pass, persisted effects match the rule, relevant regression tests pass, defects are resolved or explicitly accepted with rationale, and peer review confirms assertions and evidence. Final release requires the full automated suite and acceptance scenarios in the delivery environment, real database concurrency evidence, clean setup, staff access checks, documented unresolved limitations and no unresolved defect violating the core workflow or data integrity.

## Automation manual checks and evidence

Automate deterministic business, API, persistence and regression checks. Manually inspect AI-test quality, architecture findings, presentation workflows and human understanding. CI must build/test and report failure; an agent statement that tests pass is not a CI result. Set exact commands only after the build configuration exists; do not cite commands as executed before they run.

Each result records task/requirement/test IDs, revision, environment, command/steps, expected/observed result, pass/fail/blocked, output/artifact location, defects and reviewer. Capture red/green and metrics at the time of execution. Link evidence in Jira and the reference-practice map. Use the templates in docs/testing/evidence-templates.md and ai-log/interaction-template.md.

## Risks limitations and review

Requirements, target database design and API contract are now documented; actual team sign-off, pinned tool versions, setup and test commands remain pending. No real stakeholder validation, performance baseline, coverage or automated test result is claimed. Introduce a measurable performance check only if requirements establish a relevant target and environment; avoid an arbitrary speed claim. Update this strategy when decisions or implementation change and recheck it at each sprint review.
