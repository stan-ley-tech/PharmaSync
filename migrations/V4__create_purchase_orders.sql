CREATE TABLE purchase_orders (
    id                       BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_number             VARCHAR(40)  NOT NULL,
    pharmacy_id              BIGINT       NOT NULL REFERENCES pharmacies (id),
    supplier_id              BIGINT       NOT NULL REFERENCES suppliers (id),
    status                   VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    created_by               BIGINT       NOT NULL REFERENCES users (id),
    submitted_at             TIMESTAMPTZ,
    expected_delivery_date   DATE,
    total_amount             NUMERIC(14,2) NOT NULL DEFAULT 0,
    supplier_reference       VARCHAR(100),
    notes                    VARCHAR(500),
    created_at               TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at               TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_purchase_orders_order_number UNIQUE (order_number),
    CONSTRAINT chk_purchase_orders_status CHECK (status IN
        ('DRAFT','SUBMITTED','ACKNOWLEDGED','PARTIALLY_RECEIVED','RECEIVED','CANCELLED','FAILED'))
);

CREATE INDEX idx_purchase_orders_pharmacy_status ON purchase_orders (pharmacy_id, status);
CREATE INDEX idx_purchase_orders_supplier_id ON purchase_orders (supplier_id);

CREATE TABLE purchase_order_items (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    purchase_order_id   BIGINT NOT NULL REFERENCES purchase_orders (id) ON DELETE CASCADE,
    medicine_id         BIGINT NOT NULL REFERENCES medicines (id),
    quantity_ordered    INTEGER NOT NULL,
    quantity_received   INTEGER NOT NULL DEFAULT 0,
    unit_price          NUMERIC(12,4) NOT NULL,
    line_total          NUMERIC(14,2) NOT NULL,
    CONSTRAINT uq_po_items_order_medicine UNIQUE (purchase_order_id, medicine_id),
    CONSTRAINT chk_po_items_quantity_ordered CHECK (quantity_ordered > 0),
    CONSTRAINT chk_po_items_quantity_received CHECK (quantity_received >= 0 AND quantity_received <= quantity_ordered)
);

CREATE INDEX idx_po_items_medicine_id ON purchase_order_items (medicine_id);
