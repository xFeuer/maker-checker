# 0016. A PostgreSQL database for each service

- Status: Accepted
- Date: 2026-11-26

## Context

The services keep state. `account-service` keeps the ledger, and no entry of it is ever changed or deleted (`LED-2`). `payment-service` keeps the payments, their audit trail and every idempotency key, and a key never expires ([ADR 0012](0012-an-idempotency-key-for-every-post.md)). None of that may be gone after a restart.

Where that state lives also decides what several test problems of the [domain model](../domain.md) are worth. Two payments race for one balance, two checkers for one payment, and two payments for the last room under a limit (`LED-5`, `APR-6`, `LIM-7`). What happens then is decided by the thing that holds the data: its transactions, its locks and its constraints.

The platform has two services, so that something can go wrong between them ([ADR 0008](0008-two-services-and-a-simulator-of-the-clearing-network.md)). That holds for their data too. With one shared database, one transaction could span both.

The whole stack runs on one machine, from `deploy/compose.yaml`, and CI runs it the same way.

## Decision

- **Each service keeps its data in PostgreSQL 18.**
- **Each service has a database server of its own.** It is a container of the stack, and only its service connects to it. It has no port on the host.
- **The code reads and writes with SQL,** through the `JdbcClient` of Spring. There is no object-relational mapper.
- **The schema changes by migrations.** They are SQL files of Flyway in the service, and the service applies them when it starts. The first one comes with the first table.
- **A service is ready only when its database answers.** Its readiness probe includes the database, and the stack starts a service after its database is healthy.
- **A service has no default database.** Whoever starts it says where the database is and how to log in. The stack does that in `deploy/compose.yaml`, with credentials that are no secret: nothing outside the stack reaches the database.
- **The data lives as long as the stack.** A database has no named volume, and stopping the stack removes what it kept. Every start begins empty.
- **`deploy/compose.yaml` names the image.** The build reads it there for the tests of a service that need a database ([ADR 0017](0017-unit-tests-and-component-tests-for-a-service.md)), so the stack and those tests run the same PostgreSQL.

## Options considered

The database:

- **PostgreSQL.** Chosen. It is relational and transactional, which is what a ledger and a payment are. Its row locks, its constraints and its isolation levels are where a race is decided, and a test can run against the real engine in a container.
- **An embedded database, such as H2.** Nothing to run, and it can mimic the SQL of PostgreSQL. It does not mimic its locking, so a test of a race would prove the wrong engine.
- **State in memory.** The least to build. A restart would lose the ledger, and a key that never expires would last until the next deployment. The races would be decided by the locks of the JVM.
- **A database built for ledgers,** for `account-service` alone. It would take the double-entry rules off the service, and with them the test problems that the service exists to pose.

The version:

- **18.** Chosen. Its current minor is 18.7, and Flyway lists 18 as the newest version that it supports.
- **19.** It came out one week before this decision. Flyway does not list it yet.

The servers:

- **A server for each service.** Chosen. The official image creates one database and its owner from three environment variables, so no script is needed. The tests of a service start the same image in the same way. One database can stop or restart without the other, which is a failure that a test can then cause. It costs one container for each service.
- **One server with a database and a role for each service.** One container less. It takes an init script, and both services lose their data source together.
- **One database with a schema for each service.** The least to run. Only discipline would keep a join or a transaction from crossing the boundary of ADR 0008.

How the code reaches the data:

- **SQL through `JdbcClient`.** Chosen. The statement that takes a lock or relies on a constraint stands in the code as it runs. A record maps to a row, and nothing sits between them.
- **Spring Data JDBC.** Repositories without most of the SQL. It hides the statements in the very places where the tests of the races look.
- **JPA with Hibernate.** What most Java teams know. It puts a persistence context between the code and the database, and when a statement runs is then decided by a flush.
- **jOOQ.** SQL with types. Its code comes from the schema, so the build would need a database, or a second description of the schema.

The schema:

- **Flyway.** Chosen. A migration is a plain SQL file with a version, which fits code that is SQL anyway. Spring Boot runs it at the start of the service.
- **Liquibase.** Change sets with rollbacks, in YAML, XML or SQL. That is more machinery than a few tables need.
- **One script that creates everything.** Enough for a database that is always new. The schema of a ledger has to change without losing the ledger.

The life of the data:

- **As long as the stack.** Chosen. Every run starts from the same state. No hook wipes data ([ADR 0014](0014-testability-hooks-in-the-services-off-by-default.md)), so data that outlived a run would pile up.
- **A named volume.** The data would survive a stop. A run would then meet the customers and the idempotency keys of the runs before it.

## Consequences

- The stack grows by one container now, and by one more with `payment-service`. With the two simulators of [ADR 0013](0013-access-tokens-from-an-identity-provider-outside-the-platform.md) it will have six.
- A test never reaches a database. What it wants to know, it asks the service ([ADR 0002](0002-one-repository-with-a-black-box-boundary.md)).
- A service no longer starts by itself. It runs in the stack, or in a test that gives it a database.
- SQL is code. A migration and a statement are reviewed like Java, and each service maps its own rows.
- The migrations are plain SQL. The service that the last phase rewrites in Rust can apply the same files with a tool of its own.
- How the ledger stays right when requests race is a decision of its own. It builds on the locks and the constraints of PostgreSQL.
- Dependabot can raise the image in `deploy/compose.yaml` to a new minor. A new major is a decision: Flyway has to support it first.
- Once a service has migrations, it does not start without its database. Until then it starts and reports that it is not ready.
