CREATE TABLE inventory_reservations (
    id                     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    prescription_item_id   BIGINT NOT NULL REFERENCES prescription_items (id),
    inventory_batch_id     BIGINT NOT NULL REFERENCES inventory_batches (id),
    quantity               INTEGER NOT NULL,
    status                 VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    reserved_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at             TIMESTAMPTZ NOT NULL,
    released_at            TIMESTAMPTZ,
    CONSTRAINT chk_inventory_reservations_status CHECK (status IN ('ACTIVE','RELEASED','CONSUMED','EXPIRED')),
    CONSTRAINT chk_inventory_reservations_quantity CHECK (quantity > 0)
);

CREATE INDEX idx_inventory_reservations_sweep ON inventory_reservations (status, expires_at);
CREATE INDEX idx_inventory_reservations_prescription_item ON inventory_reservations (prescription_item_id);
CREATE INDEX idx_inventory_reservations_batch ON inventory_reservations (inventory_batch_id);

CREATE TABLE audit_logs (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    actor_id        BIGINT REFERENCES users (id),
    actor_username  VARCHAR(60),
    action          VARCHAR(80) NOT NULL,
    entity_type     VARCHAR(60) NOT NULL,
    entity_id       BIGINT,
    pharmacy_id     BIGINT REFERENCES pharmacies (id),
    details         JSONB,
    ip_address      VARCHAR(45),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_audit_logs_entity ON audit_logs (entity_type, entity_id);
CREATE INDEX idx_audit_logs_created_at ON audit_logs (created_at);
CREATE INDEX idx_audit_logs_actor_id ON audit_logs (actor_id);
