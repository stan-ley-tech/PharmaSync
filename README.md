# PharmaSync

A pharmacy inventory and prescription backend. It manages medicines, suppliers,
purchase orders, batch/lot-tracked inventory, prescriptions, dispensing, and
low-stock/expiry alerts across multiple pharmacy branches.

Built with Java 21, Spring Boot, PostgreSQL, Redis, and Kafka.

## Why this exists

Most inventory demos model stock as a single mutable number
(`medicine.quantity = 100`). Real pharmacy stock isn't like that: units come
in dated batches, some of it is already promised to a validated prescription
before it physically leaves the shelf, two pharmacists can try to dispense
the last box at the same moment, and every change needs a paper trail. This
project treats the inventory ledger as the core problem, not an afterthought:
every purchase, reservation, dispense, return, adjustment, expiry, and
transfer is written to an append-only `stock_movements` table, and the hot
path is protected with row-level locking (see
[docs/architecture.md](docs/architecture.md#concurrency-model)).

## Core workflow

```
Prescription created
        |
Prescription validated  --------->  Stock reserved (FEFO batch allocation)
        |
Pharmacist dispenses  ---------->  Reservation consumed, inventory updated
        |
Dispensing event published  --->  Kafka: medicine.dispensed
        |
Audit trail recorded, notifications fan out to downstream consumers
```

## Features

- Multi-branch pharmacy management, medicine catalog, suppliers
- Purchase orders against a simulated external supplier API, with retry on failure
- Batch/lot-tracked inventory with expiry-date tracking and FEFO picking
- Full stock-movement ledger (purchase, receipt, reservation, release, dispense,
  return, adjustment, expiry, transfer)
- Prescription lifecycle: create, validate (which reserves stock), reject, cancel
- Dispensing with per-batch allocation and partial returns
- Low-stock and expiring-batch alerts, both real-time and via scheduled sweeps
- JWT authentication, role-based access control, per-client rate limiting, audit logging
- Kafka events for every state transition, with independent downstream consumers
- Redis-cached medicine catalog reads
- Flyway-versioned schema, Testcontainers-backed test suite

## Stack

| Concern | Choice |
|---|---|
| Language / framework | Java 21, Spring Boot 3.5 |
| Database | PostgreSQL 16, Flyway migrations |
| Caching | Redis 7 |
| Messaging | Apache Kafka (KRaft mode) |
| Security | Spring Security, JWT (jjwt), Bucket4j rate limiting |
| Resilience | Spring Retry |
| Testing | JUnit 5, Mockito, AssertJ, Testcontainers, Awaitility |

## Getting started

### Run everything with Docker Compose

```bash
docker compose up --build
```

This starts Postgres, Redis, a single-node Kafka broker, and the application
on `http://localhost:8080`. Flyway runs on startup and seeds one branch, an
administrator account, two suppliers, and a starter medicine catalog — see
[migrations/V10__seed_reference_data.sql](migrations/V10__seed_reference_data.sql).

Default administrator login:

```
username: admin
password: ChangeMe123!
```

Rotate this credential before using the seed data for anything but local
development.

API docs are served at `http://localhost:8080/docs` (Swagger UI) once the
app is up.

### Run locally against Docker-hosted infrastructure only

```bash
docker compose up postgres redis kafka
./mvnw spring-boot:run
```

### Configuration

All runtime configuration is environment-variable driven — see
[.env.example](.env.example) and `src/main/resources/application.yml` for the
full list. Nothing sensitive is hardcoded outside of local-development
defaults.

## Trying the API

```bash
# Log in
curl -s -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"ChangeMe123!"}'

# Use the returned accessToken as a bearer token
curl -s http://localhost:8080/api/medicines \
  -H "Authorization: Bearer $TOKEN"
```

A full prescription-to-dispensing walkthrough (purchase order, receiving,
prescribing, validating, dispensing) is exercised end to end in
[PrescriptionDispensingFlowIntegrationTest](src/test/java/com/pharmasync/web/PrescriptionDispensingFlowIntegrationTest.java),
which is the most direct executable documentation of the API.

## Testing

```bash
./mvnw test
```

Runs the full suite — unit tests, Testcontainers-backed repository tests,
REST API integration tests, a Kafka producer/consumer test, and a
concurrency test that proves two simultaneous reservation requests against
the same stock can't over-allocate it — against real PostgreSQL, Redis, and
Kafka containers. Docker must be running.

A separate, larger-scale benchmark (12,000+ seeded inventory rows,
thousands of concurrent reserve+dispense pairs) is tagged out of the default
run; see [docs/benchmark.md](docs/benchmark.md) for how to run it and what it
measures.

## Project structure

```
pharmasync/
├── src/main/java/com/pharmasync/
│   ├── domain/           entities, grouped by bounded context
│   ├── repository/       Spring Data JPA repositories
│   ├── service/           business logic (interfaces + impl)
│   ├── web/               REST controllers and DTOs
│   ├── security/          JWT, authentication filter, UserDetails adapter
│   ├── kafka/              topics, event payloads, producer, consumers
│   ├── integration/supplier/  external supplier adapter + simulator
│   ├── scheduler/          cron jobs (low stock, expiry, reservation TTL)
│   └── config/             Spring configuration and typed properties
├── src/test/java/           unit, repository, integration, and benchmark tests
├── migrations/               Flyway SQL migrations
├── docs/                      architecture, database, events, benchmark notes
├── Dockerfile
├── docker-compose.yml
└── .github/workflows/ci.yml
```

## Documentation

- [docs/architecture.md](docs/architecture.md) — layering, request flow, concurrency model
- [docs/database.md](docs/database.md) — schema, indexing, and constraint rationale
- [docs/events.md](docs/events.md) — Kafka topics, payloads, and consumers
- [docs/benchmark.md](docs/benchmark.md) — how the system behaves at scale

## Security notes for local/demo use

The JWT secret, admin password, and supplier credentials shipped in this
repository are development defaults, not production secrets. Override them
via environment variables (`JWT_SECRET`, `DB_PASSWORD`, etc.) in any
non-local deployment.
