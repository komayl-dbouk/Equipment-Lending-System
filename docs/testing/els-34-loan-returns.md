# ELS-34 return verification evidence

Owner: Mostafa Younes. Reviewer: Komayl Dbouk. [Reviewed implementation plan](../specifications/return-agent-plan.md).

## Baseline and red phase

Started branch ELS-34-loan-returns from fetched origin/main 6cf5e8f, retaining the previous local ELS-29 branch. The original ELS-32 red-test history remains an ancestor. No borrowing handler or existing test was changed.

Before production implementation, LoanReturnContractIT compiled and executed against the unimplemented endpoint: **57 tests, 57 expected assertion failures, zero errors or skips**. Existing LoanServiceTest passed **8/8** in the same run. Return requests received the existing route-not-found response instead of the required contract; lock-wait assertions also failed because no return handler acquired locks. Maven exited 1 as expected.

Command: `mvnw.cmd --batch-mode --no-transfer-progress -Pdatabase-test -Dtest=LoanServiceTest -Dit.test=LoanReturnContractIT -Dlogging.level.root=ERROR verify`.

Java 17.0.21.1, Maven Wrapper 3.9.11, PostgreSQL 17.5. The guarded isolated database equipment_lending_test runs on 127.0.0.1:15434 with generated temporary credentials. Docker could not start on this host; C: had about 190 MB free and JVMs hit paging-file exhaustion. Provisioned portable Java/PostgreSQL under a task-specific D: directory, then stopped task-started Docker. No application database was targeted. JVM environment settings: MAVEN_OPTS `-Xms16m -Xmx192m -XX:+UseSerialGC -XX:ReservedCodeCacheSize=32m`; JAVA_TOOL_OPTIONS `-Xms16m -Xmx256m -XX:+UseSerialGC -XX:ReservedCodeCacheSize=32m`.

Earlier attempts were infrastructure failures, not feature results: forked JVM startup failed from paging exhaustion; an in-process retry had Mockito initialization failures; an offline profile run lacked the pinned Failsafe 3.5.4 plugin. The bounded-memory forked online run above resolved those issues. Raw logs stay ignored under target/els34-*. Sanitized results are in [red results](../../reports/sprint-2/els-34-red-results.json).

## Green phase

Pending implementation and execution. No passing return result or implementation peer approval is claimed here.
