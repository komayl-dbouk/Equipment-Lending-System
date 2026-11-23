# ADR 002 PostgreSQL and Safe Concurrent Lending

Status: selected for the proposed implementation baseline; runtime verification pending. Cards: ELS-22, ELS-33 and ELS-36.

## Context and alternatives

A physical item may have many historical loans and at most one active loan. A read-then-write check without synchronization allows concurrent requests to both pass. Alternatives considered are in-memory synchronization, optimistic locking with retry, pessimistic item locking, and database uniqueness alone.

In-memory locks do not protect multiple application instances or other database writers. Optimistic locking is viable but requires careful version updates and conflict handling. Database uniqueness alone protects storage but needs deliberate exception mapping. Pessimistic locking makes the transaction ordering explicit for this small application.

## Decision and reasons

Use PostgreSQL as the delivery and integrity-test database. Apply schema migrations, restrictive foreign keys, unique normalized identifiers and date checks. Borrowing locks the equipment row, checks active state and inserts/flushed the loan in one READ COMMITTED transaction. Return and deletion use the same equipment-first lock order.

Add a unique partial index on loan.equipment_id restricted to return_date IS NULL. This is the design adaptation of PostgreSQL's documented ability to enforce uniqueness over a subset of rows. See [partial indexes](https://www.postgresql.org/docs/18/indexes-partial.html).

PostgreSQL row locking blocks competing writers/lock requests until the conflicting transaction ends; the design uses that mechanism to serialize lending of one item. See [explicit locking](https://www.postgresql.org/docs/18/explicit-locking.html). This architectural choice is not a claim that a database test has passed.

## Consequences and quality attributes

Data integrity and reliability improve at the cost of database-specific migration syntax and brief contention on the same equipment item. Requests for different items can progress independently. H2-only tests do not prove these properties. Keep transactions short and use consistent lock order to limit deadlock risk.

Only a confirmed active-loan conflict is mapped to the stable 409 business error. Preserve rollback; map outside the failed transaction. Other integrity failures are classified by their actual cause, and unexpected infrastructure failures remain sanitized technical errors.

## Verification and maintenance

Use two independent connections/transactions with coordinated request start. For one initially available item, assert one 201, one 409 and exactly one active loan under normal database operation. Verify return/reborrow, duplicate return and deletion/history races. Record PostgreSQL/tool versions and output before claiming integrity compliance.
