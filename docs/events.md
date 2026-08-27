# Kafka events

All topics carry JSON payloads (`spring-kafka`'s `JsonSerializer`/`JsonDeserializer`,
type headers disabled — the consumer resolves the payload type from the listener
method signature). Keys are the primary entity id as a string, so related events for
the same medicine/prescription land on the same partition and stay ordered relative
to each other.

| Topic | Producer | Payload | Purpose |
|---|---|---|---|
| `prescription.created` | `PrescriptionService.create` | `PrescriptionCreatedEvent` | A new prescription was recorded. |
| `prescription.validated` | `PrescriptionService.validate` / `.reject` | `PrescriptionValidatedEvent` | A pharmacist approved or rejected a prescription. `approved=false` carries a `rejectionReason`. |
| `inventory.reserved` | `InventoryServiceImpl.reserve` | `InventoryReservedEvent` | Stock was committed against a prescription item pending pickup. |
| `medicine.dispensed` | `DispensingServiceImpl.dispense` | `MedicineDispensedEvent` | A dispensing transaction completed. |
| `inventory.low` | `InventoryServiceImpl` (real-time) and `LowStockSweepJob` (periodic safety net) | `InventoryLowEvent` | Available stock (on hand minus reserved) is at or below the reorder threshold. |
| `medicine.expiring` | `InventoryServiceImpl.publishExpiryWarnings` (via `ExpiryCheckJob`) | `MedicineExpiringEvent` | An active batch will expire within the configured warning window. |
| `purchase.received` | `PurchaseOrderServiceImpl.receiveDelivery` | `PurchaseReceivedEvent` | A supplier delivery was confirmed and posted to inventory. |
| `inventory.transferred` | `InventoryServiceImpl.transferStock` | `InventoryTransferredEvent` | Stock moved from one branch to another. |

## Consumers

```
inventory.low        -> NotificationEventListener   (pharmasync-notification-service)
medicine.dispensed   -> AuditEventListener           (pharmasync-audit-service)
medicine.expiring    -> ExpiryAlertListener           (pharmasync-expiry-alert-worker)
```

Each listener runs under its own consumer group id, so if this system were split
into separate deployables later (a real notification service, a real audit
service), each would keep its own offsets and receive a full copy of the
relevant topic — nothing here depends on all three being in the same process.

`AuditEventListener` writes into the same `audit_logs` table that
synchronous, in-request audit calls use, but on its own
`REQUIRES_NEW` transaction: a slow or failing consumer never blocks the
request path that produced the event, and a consumer failure is retried by
the container's error handler (`KafkaConfig`, fixed backoff, 3 attempts)
before the message is given up on.

## Why validation and reservation share one event pair

`prescription.validated` and `inventory.reserved` are published together
because they happen inside the same service call
(`PrescriptionService.validate`) and the same database transaction. A
consumer that only cares about reservations should subscribe to
`inventory.reserved` directly rather than inferring it from
`prescription.validated`.
