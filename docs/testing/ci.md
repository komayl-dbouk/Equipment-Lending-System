# Automated Build and Test Checks

ELS-30 owner: Hussein Chreif. The Build and test workflow runs on pull requests targeting main, pushes to main and manual dispatch. It checks out the revision, selects Java 17, caches Maven dependencies, and invokes the pinned Maven Wrapper with the database-test profile. Unit tests and Failsafe integration tests share one verify lifecycle; any failure fails the job. Surefire and Failsafe reports are uploaded even when tests fail.

The PostgreSQL service runs on the GitHub-hosted runner. Its credentials are disposable test configuration for an isolated database, not deployment secrets. No production database, privileged pull-request trigger, repository write permission or secret is required. A fifteen-minute job timeout bounds execution, and superseded runs for the same ref are cancelled.

This task configures the remote PostgreSQL service but does not run Docker locally. Local validation is limited to focused database-free tests and workflow structure. No remote CI result can be observed until the user pushes and opens the PR or dispatches the workflow. Review the actual GitHub check and retained reports before merging; static workflow validation is not proof of a successful remote run.

For database-free local feedback use the documented wrapper test command with the selected test classes; for equivalent full CI verification use the database-test profile only when a configured PostgreSQL environment is available. Do not replace PostgreSQL integrity tests with H2 merely to obtain a green check.
