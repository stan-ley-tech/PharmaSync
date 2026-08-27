CREATE TABLE inventory (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    pharmacy_id         BIGINT NOT NULL REFERENCES pharmacies (id),
    medicine_id         BIGINT NOT NULL REFERENCES medicines (id),
    quantity_on_hand    INTEGER NOT NULL DEFAULT 0,
    quantity_reserved   INTEGER NOT NULL DEFAULT 0,
    reorder_threshold   INTEGER,
    version             BIGINT NOT NULL DEFAULT 0,
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_inventory_pharmacy_medicine UNIQUE (pharmacy_id, medicine_id),
    CONSTRAINT chk_inventory_on_hand CHECK (quantity_on_hand >= 0),
    CONSTRAINT chk_inventory_reserved CHECK (quantity_reserved >= 0 AND quantity_reserved <= quantity_on_hand)
);

CREATE INDEX idx_inventory_medicine_id ON inventory (medicine_id);

CREATE TABLE inventory_batches (
    id                      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    inventory_id            BIGINT NOT NULL REFERENCES inventory (id),
    batch_number             VARCHAR(60) NOT NULL,
    purchase_order_item_id   BIGINT REFERENCES purchase_order_items (id),
    quantity_received        INTEGER NOT NULL,
    quantity_remaining        INTEGER NOT NULL,
    unit_cost                 NUMERIC(12,4) NOT NULL,
    manufactured_date         DATE,
    expiry_date                DATE NOT NULL,
    received_at                TIMESTAMPTZ NOT NULL DEFAULT now(),
    status                      VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    version                     BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_inventory_batches_inventory_batch_number UNIQUE (inventory_id, batch_number),
    CONSTRAINT chk_inventory_batches_status CHECK (status IN ('ACTIVE','EXPIRED','DEPLETED','RECALLED')),
    CONSTRAINT chk_inventory_batches_quantity_received CHECK (quantity_received > 0),
    CONSTRAINT chk_inventory_batches_quantity_remaining CHECK (quantity_remaining >= 0 AND quantity_remaining <= quantity_received)
);

CREATE INDEX idx_inventory_batches_fefo ON inventory_batches (inventory_id, status, expiry_date);
CREATE INDEX idx_inventory_batches_expiry_date ON inventory_batches (expiry_date);
CREATE INDEX idx_inventory_batches_po_item_id ON inventory_batches (purchase_order_item_id);
