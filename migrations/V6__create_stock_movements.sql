CREATE TABLE stock_movements (
    id                   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    inventory_batch_id   BIGINT NOT NULL REFERENCES inventory_batches (id),
    movement_type        VARCHAR(20) NOT NULL,
    quantity              INTEGER NOT NULL,
    quantity_before        INTEGER NOT NULL,
    quantity_after          INTEGER NOT NULL,
    reference_type          VARCHAR(30),
    reference_id             BIGINT,
    performed_by              BIGINT REFERENCES users (id),
    notes                      VARCHAR(500),
    created_at                 TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_stock_movements_type CHECK (movement_type IN
        ('PURCHASE','RECEIPT','RESERVATION','RELEASE','DISPENSE','RETURN','ADJUSTMENT','EXPIRY','TRANSFER'))
);

CREATE INDEX idx_stock_movements_batch_created ON stock_movements (inventory_batch_id, created_at);
CREATE INDEX idx_stock_movements_reference ON stock_movements (reference_type, reference_id);
