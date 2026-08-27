CREATE TABLE prescriptions (
    id                    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    prescription_number   VARCHAR(40)  NOT NULL,
    pharmacy_id           BIGINT       NOT NULL REFERENCES pharmacies (id),
    patient_name          VARCHAR(150) NOT NULL,
    patient_identifier    VARCHAR(60),
    patient_contact       VARCHAR(60),
    prescribed_by         BIGINT REFERENCES users (id),
    prescriber_name       VARCHAR(150),
    prescriber_license    VARCHAR(60),
    status                VARCHAR(20)  NOT NULL DEFAULT 'CREATED',
    validated_by          BIGINT REFERENCES users (id),
    validated_at          TIMESTAMPTZ,
    rejection_reason      VARCHAR(500),
    issued_date           DATE         NOT NULL,
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_prescriptions_number UNIQUE (prescription_number),
    CONSTRAINT chk_prescriptions_status CHECK (status IN
        ('CREATED','VALIDATED','REJECTED','PARTIALLY_DISPENSED','DISPENSED','CANCELLED','EXPIRED'))
);

CREATE INDEX idx_prescriptions_pharmacy_status ON prescriptions (pharmacy_id, status);
CREATE INDEX idx_prescriptions_patient_identifier ON prescriptions (patient_identifier);

CREATE TABLE prescription_items (
    id                      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    prescription_id         BIGINT NOT NULL REFERENCES prescriptions (id) ON DELETE CASCADE,
    medicine_id              BIGINT NOT NULL REFERENCES medicines (id),
    quantity_prescribed        INTEGER NOT NULL,
    quantity_dispensed           INTEGER NOT NULL DEFAULT 0,
    dosage_instructions            VARCHAR(300),
    substitution_allowed              BOOLEAN NOT NULL DEFAULT true,
    CONSTRAINT uq_prescription_items_prescription_medicine UNIQUE (prescription_id, medicine_id),
    CONSTRAINT chk_prescription_items_qty_prescribed CHECK (quantity_prescribed > 0),
    CONSTRAINT chk_prescription_items_qty_dispensed CHECK (quantity_dispensed >= 0 AND quantity_dispensed <= quantity_prescribed)
);

CREATE INDEX idx_prescription_items_medicine_id ON prescription_items (medicine_id);
