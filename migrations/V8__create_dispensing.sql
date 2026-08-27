CREATE TABLE dispensing (
    id                   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    dispensing_number    VARCHAR(40)  NOT NULL,
    prescription_id      BIGINT       NOT NULL REFERENCES prescriptions (id),
    pharmacy_id          BIGINT       NOT NULL REFERENCES pharmacies (id),
    dispensed_by         BIGINT       NOT NULL REFERENCES users (id),
    status               VARCHAR(20)  NOT NULL DEFAULT 'COMPLETED',
    total_amount         NUMERIC(14,2) NOT NULL DEFAULT 0,
    dispensed_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    notes                VARCHAR(500),
    CONSTRAINT uq_dispensing_number UNIQUE (dispensing_number),
    CONSTRAINT chk_dispensing_status CHECK (status IN ('COMPLETED','PARTIALLY_RETURNED','RETURNED','CANCELLED'))
);

CREATE INDEX idx_dispensing_prescription_id ON dispensing (prescription_id);
CREATE INDEX idx_dispensing_pharmacy_dispensed_at ON dispensing (pharmacy_id, dispensed_at);

CREATE TABLE dispensing_items (
    id                     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    dispensing_id           BIGINT NOT NULL REFERENCES dispensing (id) ON DELETE CASCADE,
    prescription_item_id     BIGINT NOT NULL REFERENCES prescription_items (id),
    inventory_batch_id        BIGINT NOT NULL REFERENCES inventory_batches (id),
    quantity                    INTEGER NOT NULL,
    quantity_returned             INTEGER NOT NULL DEFAULT 0,
    unit_price                     NUMERIC(12,4) NOT NULL,
    line_total                      NUMERIC(14,2) NOT NULL,
    CONSTRAINT chk_dispensing_items_quantity CHECK (quantity > 0),
    CONSTRAINT chk_dispensing_items_quantity_returned CHECK (quantity_returned >= 0 AND quantity_returned <= quantity)
);

CREATE INDEX idx_dispensing_items_dispensing_id ON dispensing_items (dispensing_id);
CREATE INDEX idx_dispensing_items_batch_id ON dispensing_items (inventory_batch_id);
