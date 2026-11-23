# University Equipment Lending System Proposal

Prepared for the Software Engineering mini-project. This proposal defines a staff-managed REST API and a delivery plan for four students. Team review and proposal submission are the first Sprint 1 activity; preparation of this document does not represent instructor approval.

## Project identification

| Item | Selection |
| --- | --- |
| Team | Hussein Chreif, Mostafa Younes, Komayl Dbouk, Hussein Chalhoub |
| Application | University equipment lending backend API |
| Repository | https://github.com/komayl-dbouk/Equipment-Lending-System |
| Scrum board | https://equipment-lending-system.atlassian.net/jira/software/c/projects/ELS/boards/34 |
| Existing Kanban board | https://equipment-lending-system.atlassian.net/jira/software/c/projects/ELS/boards/1 |
| Stack | Java, Spring Boot, Maven Wrapper, Spring Data JPA, relational database, Jakarta Bean Validation |
| Tests and quality | JUnit, Spring Boot Test, controller and database tests, JaCoCo, suitable Java static analysis |
| Agent | Codex; record other tools and models actually used in the AI log |
| Collaboration | GitHub for source and peer review; Jira for scope, estimates, assignments and sprint evidence; short team check-ins and Jira comments for decisions |

The team will pin compatible supported tool versions during setup and record them in the README. The implementation baseline prepared for Sprint 1 selects PostgreSQL; [ADR 002](decisions/002-database-and-concurrency.md) records the choice and concurrency approach. An in-memory test database may support fast tests, but concurrency must also be verified against the delivery database. The project has no required paid external API or production dataset.

## Problem stakeholders and value

University staff need to know what equipment is available, which student has borrowed an item, and when it should be returned. Paper records or disconnected spreadsheets can permit inconsistent records and make overdue items difficult to track. We propose a central API that enforces lending rules and preserves borrowing history.

The primary user is the university staff member managing equipment. Students are borrowers represented in the records; they do not log in to this release. Equipment custodians benefit from reliable inventory and history. The instructor evaluates the engineering process and needs access to the repository and board. The team will initially use a credible staff workflow and explicitly recorded assumptions; it will not claim interviews or stakeholder validation that did not take place.

For staff managing university equipment, the University Equipment Lending System provides consistent inventory and lending records through a documented API. Its value is reliable availability, controlled borrowing and returns, and visible overdue loans. The project demonstrates requirements, object-oriented design, transaction integrity, testing and supervised AI use across the lifecycle.

## Scope and minimum viable product

The first usable release will support registering, retrieving and updating students; creating, listing, updating and conditionally deleting equipment; checking availability; creating loans; recording returns; listing overdue active loans; and viewing history filtered by student or equipment. One equipment record represents one physical item with a unique inventory code.

Student self-service accounts, a graphical interface, reservations, payments, fines, notifications, barcode hardware, multi-campus administration and institutional SSO are excluded. Pagination and additional search can be considered only after mandatory work is complete. A single staff access mechanism is sufficient for the delivery boundary; credentials must come from environment configuration.

The system boundary includes the API, business services and database. Staff invoke the API using documented requests or an API documentation interface. There is no dependency on a real university information system. Demonstration data will be synthetic.

## Core behavior and acceptance

| Requirement | Observable acceptance |
| --- | --- |
| FR01 Student records | Valid registration succeeds; invalid data is rejected; unknown IDs return a missing-record response; duplicate normalized email is rejected |
| FR02 Equipment inventory | Unique inventory codes are enforced; valid records can be managed; unknown records and duplicate codes are rejected |
| FR03 Borrow equipment | Existing student and item plus valid dates create one loan; unavailable equipment and invalid references/dates are rejected |
| FR04 Concurrent borrowing | Two competing requests for the same item result in one successful loan and one conflict, with one active record |
| FR05 Return equipment | A valid return preserves history and restores availability; duplicate returns and dates before borrowing are rejected |
| FR06 Overdue and history | Active loans strictly past the due date appear overdue; returned loans and loans due today do not; history filters return matching records |
| FR07 Preserve records | Equipment referenced by any loan cannot be deleted; student deletion is not exposed in the MVP |
| NFR01 Integrity | Failed operations leave no partial writes; transaction/concurrency behavior is tested on the delivery database |
| NFR02 Reproducibility | A clean clone builds, passes the full automated suite and starts using documented commands and configuration |
| NFR03 Access and privacy | Protected staff operations reject unauthenticated requests; secrets and private student data are absent from tracked artifacts and demo data |
| NFR04 Maintainability | Controllers delegate business rules to services; peer review checks layer boundaries; API examples and diagrams match implementation |
| NFR05 Test quality | Core success, failure and date-boundary rules have meaningful assertions; service line coverage target is at least 80 percent, supplemented by manual test audit |

The [detailed requirements](requirements/requirements.md) prepared for Sprint 1 define calendar dates, an injected Asia/Beirut clock, email normalization, payloads and errors. Standard responses are 201 for creation, 400 for invalid input, 404 for missing records and 409 for business conflicts. Availability is derived from the absence of an active loan rather than maintained as a separate inconsistent flag. These prepared decisions still require actual team adoption before substantial implementation.

## Architecture and design approach

The application will use controllers, request/response DTOs, services, repositories and persistence entities. Controllers handle HTTP mapping and validation; services enforce rules and transaction boundaries; repositories access data. Student and Equipment each have many Loans over time. A Loan references exactly one student and one item and remains active until its return date is recorded.

The design task will compare locking and database constraint approaches for concurrent borrowing and record the selected strategy, alternatives and consequences. Diagrams will include system context, domain and relational models, implementation classes, borrowing sequence and deployment. Architectural decisions, diagrams and agent context will evolve with the code. Microservices are unnecessary for this scope.

## Feasibility and risks

The selected Java and Spring Boot approach matches the subject's preferred stack. The small domain and absence of external integrations make a complete API feasible within the available calendar, provided the scope remains fixed and work integrates daily. The plan allows AI agents to assist with implementation while humans spend time reviewing correctness and evidence.

| Risk | Response and owner |
| --- | --- |
| Short deadline and unknown daily availability | Four two-day sprints, daily capacity checks and early scope reduction of optional work; Hussein Chreif |
| AI output looks correct but violates rules | Reviewed plans, explicit expected results, meaningful tests and peer review; all members |
| Double lending under concurrency | Early written specification and target-database concurrency test; Komayl Dbouk |
| Parallel changes conflict | Agree shared contracts first, one owner for shared foundation, short branches and daily integration; Hussein Chalhoub |
| Process evidence is lost | Record estimates/specs/tests/decisions at the time of work; preserve test-first commits; Hussein Chreif |
| Tool or Docker setup fails | Verify setup in Sprint 1; retain a documented Maven and local database execution path; Hussein Chalhoub |
| Personal data or credentials leak | Synthetic data, environment-based secrets and review of tracked files/logs; all members |
| Unreviewed dependencies or licensing | Use necessary dependencies only, inspect licenses and source attribution; Komayl Dbouk |

Learning needs include transactional concurrency, meaningful database/integration tests, static analysis, and keeping AI evidence traceable. Public hosting is optional; a reliable, rehearsed local deployment is sufficient for the proposed release. This is a university demonstration rather than a production institutional deployment.

## Team organization and oversight

Hussein Chreif coordinates scope and submission and owns test strategy and specification evidence. Mostafa Younes owns student records, returns and acceptance/test auditing. Komayl Dbouk owns equipment, domain architecture, safe borrowing and refactoring. Hussein Chalhoub owns build integration, common errors, query features, quality tooling and deployment. The detailed plan names a peer reviewer for each card. Every member writes tests, documents decisions and explains work outside their primary module.

Hussein Chreif acts as Product Owner and Hussein Chalhoub facilitates Scrum. All four remain developers and accountable authors. Codex assists technical coordination and review when invoked; it does not provide instructor approval, certify human review, or replace the team's responsibility for decisions.

Use short issue-linked branches and pull requests into the main branch. A peer reviews significant AI-generated changes. Substantial features require a written plan reviewed by a human before code generation. The substantial borrowing feature also requires failing tests committed before implementation. No blanket staging command should capture Subject files.

## Release plan

Each sprint lasts two days. Jira holds the calendar schedule and submission deadline.

| Sprint | Duration | Demonstrable increment |
| --- | --- | --- |
| 1 | Two days | Reviewed proposal/requirements/design, runnable build, tested student and equipment API |
| 2 | Two days | Borrowing, return, availability, overdue/history and verified concurrency |
| 3 | Two days | Audited tests, deliberate-defect evidence, verified design corrections, three refactorings and measured quality improvement |
| 4 | Two days | Verified packaging/access, acceptance results, final report and rehearsed presentation |
| Submission buffer | After Sprint 4 | Fix release blockers, recheck access and submit with retained receipt |

Sprint 1 has 24 provisional engineering hours, six per member across two days. Estimates include human review, tests and documentation and are planning values rather than recorded effort. Later estimates are provisional and must be refined against actual capacity and completion. Do not equate fast code generation with completed delivery.

## Engineering evidence and delivery

The completed reference lab supplies practices to adapt to this domain. Its library implementation, lab-specific tags and prior results are not project evidence. The mini-project requires continuous evidence: specification and plan review; red/green test-first history; a manual audit correcting at least three test weaknesses; a deliberate defect with restoration and regression; two important verified review findings corrected; three documented refactorings; static analysis and at least one measured correction; a GQM plan; estimates versus actuals; selected AI interactions; and tested deployment.

Each sprint retains its goal, selected cards, estimates, progress/blockers, demonstration, feedback, review, retrospective, backlog changes and actual effort. Important requirements link to stories, code, tests, results and evidence. The final reference-practice table links these artifacts directly. AI logs identify tools/models where known, instructions, plans, changes, results, human corrections and accepted/modified/rejected decisions. Prior lab evidence will not be relabeled as equipment-project work.

Deliverables are the proposal; evolving backlog; requirements and synchronized design; source/tests/context; API and user instructions; test and quality evidence; AI records; reproducible deployment and synthetic data; maintenance plan; final report with traceability; and a presentation in which every member can explain the principal decisions and their contribution.
