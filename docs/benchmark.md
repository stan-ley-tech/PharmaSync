# Benchmark

`InventoryBenchmarkTest` (tagged `benchmark`, excluded from the default
`./mvnw test` run via the Surefire `excludedGroups` config in `pom.xml`) does
two things against a real Testcontainers-provisioned PostgreSQL instance:

1. **Scale check** — bulk-inserts 12,000 medicines and their corresponding
   `inventory` rows for one branch using batched JDBC (`JdbcTemplate.batchUpdate`),
   then asserts the low-stock query and lookups still behave correctly at
   that volume. This is meant to catch the class of bug that only shows up
   once a table has real cardinality — a missing index, an N+1 query, a
   sequential scan that was invisible with ten rows.
2. **Contention check** — receives a single very large batch (1,000,000
   units) of one "hot" medicine, creates a few thousand distinct prescription
   items for it, then fires that many `reserve()` + `consumeReservations()`
   pairs concurrently from a fixed-size thread pool. It reports throughput
   and latency percentiles, then asserts the final `inventory` row is exactly
   consistent (`quantity_on_hand` reduced by precisely what was dispensed,
   `quantity_reserved` back to zero) — i.e. the pessimistic locking described
   in [architecture.md](architecture.md#concurrency-model) holds under real
   concurrent load, not just the two-thread case covered by
   `ConcurrentDispensingIntegrationTest`.

## Running it

```bash
./mvnw test -Dgroups=benchmark -Dtest=InventoryBenchmarkTest
```

Docker must be running (it needs the same Postgres/Kafka/Redis containers as
the rest of the integration suite, via `AbstractIntegrationTest`). It prints
a report to stdout, for example:

```
[benchmark] seeded 12000 inventory rows in 4230 ms
[benchmark] 2000 concurrent reserve+dispense pairs (24 workers) in 6142 ms — 325.6 ops/sec, 0 failed
[benchmark] latency p50=68.2ms p95=142.7ms p99=201.4ms max=310.5ms
```

Treat the exact numbers as illustrative rather than a guaranteed figure —
they depend on the machine running the test (CPU, disk, and whether Docker
is running natively or through a VM), not on anything this project controls.
What should hold on any machine:

- The seed step scales roughly linearly with row count, since it's a single
  batched `INSERT ... SELECT`, not 12,000 round trips.
- Throughput is bound by lock contention on the one hot `inventory` row, by
  design — every worker is fighting over the same (pharmacy, medicine) pair,
  which is deliberately the worst case for this locking strategy. Spreading
  the same request volume across many medicines (the realistic case) would
  show much higher aggregate throughput, since unrelated medicines don't
  share a lock at all.
- The failure count should be `0`: the benchmark only asks for stock that
  exists, so every request should eventually succeed once it acquires the
  lock — the test is measuring how long correctness takes to enforce under
  contention, not whether it holds.

## What this doesn't measure

This is a single-process, single-node benchmark against local containers —
it is not a substitute for a proper load test against a deployed environment
with realistic network latency, connection pool sizing, and horizontally
scaled application instances. Its purpose is narrower: proving the schema
and the locking strategy don't fall over as data volume and concurrency
grow, using the same code path production traffic would exercise.
