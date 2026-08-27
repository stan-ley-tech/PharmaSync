# Architecture

## Overview

PharmaSync is a Spring Boot service that manages pharmacy inventory and the
prescription-to-dispensing workflow across multiple branches. It is a single
deployable unit organized in layers, backed by PostgreSQL for durable state,
Redis for read-through caching, and Kafka for asynchronous fan-out to
downstream consumers.

```
                        ┌──────────────────────┐
                        │      REST API         │
                        │  (web.controller)      │
                        └──────────┬────────────┘
                                   │
                        ┌──────────▼────────────┐
                        │   Service layer        │
                        │  (service / service.impl)
                        └───┬───────────┬────────┘
                            │           │
                 ┌──────────▼──┐    ┌───▼─────────────┐
                 │ Repositories │    │  EventPublisher  │
                 │  (JPA)       │    │  (Kafka producer)│
                 └──────┬───────┘    └───┬─────────────┘
                        │                │
                 ┌──────▼───────┐   ┌────▼─────────────┐
                 │  PostgreSQL   │   │      Kafka        │
                 └───────────────┘   └────┬─────────────┘
                                           │
                                  ┌────────▼─────────┐
                                  │ Kafka consumers   │
                                  │ (kafka.consumer)  │
                                  └───────────────────┘
```

## Package layout

- `domain` — JPA entities, grouped by bounded context (`user`, `pharmacy`,
  `catalog`, `procurement`, `inventory`, `prescription`, `dispensing`,
  `audit`).
- `repository` — Spring Data JPA repositories, including the pessimistic-lock
  query methods the inventory engine relies on.
- `service` / `service.impl` — business logic. Interfaces are kept separate
  from implementations so controllers and tests depend on behavior, not
  wiring.
- `web.controller` / `web.dto` — the REST surface. Controllers are thin:
  validation, authorization annotations, and mapping to/from the service
  layer.
- `security` — JWT issuance/validation, the authentication filter, and the
  `UserDetailsService` adapter over the `User` entity.
- `kafka` — topic names, event payload records, the producer wrapper, and the
  `consumer` sub-package holding the three downstream listeners described in
  [events.md](events.md).
- `integration.supplier` — the adapter boundary to external supplier systems,
  plus an in-process simulator standing in for a real vendor API.
- `scheduler` — cron-triggered jobs for low-stock sweeps, expiry sweeps, and
  reservation TTL cleanup.
- `config` — Spring configuration and typed `@ConfigurationProperties`
  records.

## Request flow: prescription to dispensing

```
POST /api/prescriptions            create()      -> status CREATED, publish prescription.created
POST /api/prescriptions/{id}/validate  validate() -> checks & reserves stock per item,
                                                      status VALIDATED, publish prescription.validated
                                                      + inventory.reserved
POST /api/dispensing/prescriptions/{id} dispense() -> consumes the reservation batch-by-batch,
                                                       status DISPENSED/PARTIALLY_DISPENSED,
                                                       publish medicine.dispensed
```

Validation and reservation are combined into a single service call
(`PrescriptionService.validate`) because, in this domain, approving a
prescription and committing stock to it are one business decision — a
pharmacist would not want to validate a prescription that cannot actually be
filled.

## Concurrency model

Two pharmacists in the same branch can attempt to dispense the same medicine
at the same time. The inventory engine (`InventoryServiceImpl`) prevents
over-selling with pessimistic row locking:

1. Every stock-changing operation first acquires a `SELECT ... FOR UPDATE`
   lock on the `inventory` row for the (pharmacy, medicine) pair via
   `InventoryRepository.lockByPharmacyIdAndMedicineId`. This serializes all
   reservation/dispense/adjustment activity for that medicine at that branch.
2. With the inventory row locked, the relevant `inventory_batches` rows are
   locked too (`lockAvailableBatchesForDispensing` / `lockById`), so batch
   selection (FEFO — first-expiry-first-out) and quantity updates are
   consistent even under concurrent access.
3. `inventory_batches.version` and `inventory.version` also carry a JPA
   `@Version` column, so any code path that reads a batch outside the locked
   section (there isn't one on the hot path today) still fails fast on a
   stale write instead of silently corrupting stock counts.
4. Cross-branch transfers lock the two `inventory` rows in a fixed order
   (ascending pharmacy id) to avoid a classic lock-ordering deadlock between
   two transfers running in opposite directions.

## Caching

The medicine catalog is the highest-read, lowest-write dataset in the system,
so `MedicineService` caches individual lookups and evicts on write
(`RedisCacheConfig`). Search results are cached separately with a shorter TTL
and are invalidated in bulk on any catalog change, since a single write can
affect many possible search result pages.

## External supplier integration

`integration.supplier.SupplierApiClient` is the adapter boundary. The
production implementation, `HttpSupplierApiClient`, talks to whatever base
URL is configured for a given supplier over plain HTTP using Spring's
`RestClient`, wrapped in `@Retryable` with a fixed backoff. If retries are
exhausted, the failure surfaces as a `SupplierIntegrationException` (mapped
to `502 Bad Gateway`) rather than a generic 500, and the purchase order is
marked `FAILED` so it can be resubmitted.

Because there is no real vendor to integrate with in this project,
`SupplierSimulatorController` plays that role over HTTP, on the same
process, at `/simulator/supplier/**`. The client does not know or care that
the "vendor" is local — it is exercised through the same interface a real
integration would use.
