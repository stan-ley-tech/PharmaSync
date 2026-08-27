CREATE TABLE suppliers (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code          VARCHAR(30)  NOT NULL,
    name          VARCHAR(150) NOT NULL,
    contact_name  VARCHAR(120),
    email         VARCHAR(150),
    phone         VARCHAR(30),
    api_base_url  VARCHAR(255),
    is_active     BOOLEAN      NOT NULL DEFAULT true,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_suppliers_code UNIQUE (code)
);

CREATE TABLE medicines (
    id                     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    sku                    VARCHAR(40)  NOT NULL,
    name                   VARCHAR(200) NOT NULL,
    generic_name           VARCHAR(200),
    form                   VARCHAR(30)  NOT NULL,
    strength               VARCHAR(50),
    manufacturer           VARCHAR(150),
    unit_of_measure        VARCHAR(20)  NOT NULL DEFAULT 'UNIT',
    requires_prescription  BOOLEAN      NOT NULL DEFAULT true,
    controlled_substance   BOOLEAN      NOT NULL DEFAULT false,
    reorder_threshold      INTEGER      NOT NULL DEFAULT 20,
    reorder_quantity       INTEGER      NOT NULL DEFAULT 100,
    default_supplier_id    BIGINT REFERENCES suppliers (id),
    unit_price             NUMERIC(12,4) NOT NULL,
    is_active              BOOLEAN      NOT NULL DEFAULT true,
    created_at             TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at             TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_medicines_sku UNIQUE (sku),
    CONSTRAINT chk_medicines_form CHECK (form IN ('TABLET','CAPSULE','SYRUP','INJECTION','OINTMENT','DROPS','INHALER','OTHER')),
    CONSTRAINT chk_medicines_unit_price CHECK (unit_price >= 0),
    CONSTRAINT chk_medicines_reorder_threshold CHECK (reorder_threshold >= 0),
    CONSTRAINT chk_medicines_reorder_quantity CHECK (reorder_quantity >= 0)
);

CREATE INDEX idx_medicines_name ON medicines (lower(name));
CREATE INDEX idx_medicines_default_supplier_id ON medicines (default_supplier_id);
