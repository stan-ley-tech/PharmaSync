# Database

PostgreSQL, versioned with Flyway. Migrations live in `/migrations` at the
repository root (not `src/main/resources/db/migration`) and are copied into
the runtime image alongside the jar — see `spring.flyway.locations` in
`application.yml` and the `Dockerfile`.

## Entity groups

```
roles ──┐
        ├── user_roles ── users ── pharmacies
        │
suppliers ── medicines
        │           │
        └── purchase_orders ── purchase_order_items
                    │                    │
              pharmacies           inventory_batches
                                          │
                              inventory ──┴── stock_movements
                                   │
                         inventory_reservations ── prescription_items ── prescriptions
                                                                              │
                                                                        dispensing ── dispensing_items
                                                                              │
                                                                          pharmacies

audit_logs -> users, pharmacies (both optional FKs; denormalized actor_username survives user deletion)
```

## Why `inventory` is separate from `inventory_batches`

`inventory` is one row per (pharmacy, medicine): it holds the two numbers
every other query cares about — `quantity_on_hand` and `quantity_reserved`
— plus the optional per-branch reorder threshold override and a `version`
column for optimistic-locking defense in depth. `inventory_batches` is the
lot-level detail: every batch has its own expiry date, unit cost, and
remaining quantity, because FEFO picking and expiry tracking are inherently
per-batch, not per-medicine. Keeping the hot aggregate (`inventory`) narrow
means the pessimistic lock taken on it during reserve/dispense holds for as
short a time as possible.

## Stock movement ledger

`stock_movements` is append-only and never updated. Every mutation to a
batch's `quantity_remaining` — a purchase receipt, a reservation, a release,
a dispense, a return, a manual adjustment, an expiry write-off, or a
transfer leg — writes exactly one row here with `quantity_before` and
`quantity_after`, so the full history of any batch can be reconstructed
without touching `inventory` or `inventory_batches` at all. `reference_type`
+ `reference_id` link a movement back to whatever business record caused it
(a prescription item, a dispensing record, a transfer).

## Constraints doing real work

- `chk_inventory_reserved` (`quantity_reserved <= quantity_on_hand`) and
  `chk_inventory_batches_quantity_remaining`
  (`0 <= quantity_remaining <= quantity_received`) make the invariants the
  service layer maintains impossible to violate even from a bug or a manual
  `UPDATE`.
- `uq_inventory_pharmacy_medicine` and `uq_inventory_batches_inventory_batch_number`
  enforce the one-row-per-(pharmacy, medicine) and unique-batch-number-per-inventory
  rules at the database level, not just in application code.
- Every status column (`purchase_orders.status`, `prescriptions.status`,
  `dispensing.status`, `inventory_batches.status`,
  `inventory_reservations.status`) is a `VARCHAR` with a `CHECK` constraint
  rather than a Postgres `ENUM` type, so adding a new status only ever needs
  a migration that alters the constraint — no `ALTER TYPE ... ADD VALUE`
  transaction-boundary restrictions to work around.

## Indexing

Beyond the unique constraints (which are also indexes), the migrations add:

- `idx_inventory_batches_fefo` on `(inventory_id, status, expiry_date)` —
  exactly the shape of the query that picks batches to reserve/dispense from.
- `idx_inventory_batches_expiry_date` and the `idx_inventory_reservations_sweep`
  `(status, expires_at)` index back the two scheduled sweep jobs.
- `idx_medicines_name` on `lower(name)` for case-insensitive catalog search.
- `idx_stock_movements_batch_created` and `idx_stock_movements_reference` for
  the two access patterns on the ledger: "history of this batch" and "what
  happened for this prescription/dispensing/transfer".
- `idx_audit_logs_entity`, `idx_audit_logs_created_at`, `idx_audit_logs_actor_id`
  for the audit trail's read paths (by record, by time range, by actor).

## Locking strategy

See [architecture.md](architecture.md#concurrency-model) for how pessimistic
locking on `inventory` and `inventory_batches` combines with the `version`
columns to keep concurrent dispensing safe.
