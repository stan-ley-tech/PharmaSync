CREATE TABLE roles (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name        VARCHAR(50)  NOT NULL,
    description VARCHAR(255),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_roles_name UNIQUE (name)
);

CREATE TABLE pharmacies (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code          VARCHAR(20)  NOT NULL,
    name          VARCHAR(150) NOT NULL,
    address_line1 VARCHAR(200) NOT NULL,
    address_line2 VARCHAR(200),
    city          VARCHAR(100) NOT NULL,
    state         VARCHAR(100),
    postal_code   VARCHAR(20),
    country       VARCHAR(100) NOT NULL,
    phone         VARCHAR(30),
    is_active     BOOLEAN      NOT NULL DEFAULT true,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_pharmacies_code UNIQUE (code)
);

INSERT INTO roles (name, description) VALUES
    ('ADMIN', 'Full administrative access across all branches'),
    ('PHARMACIST', 'Validates prescriptions and dispenses medication'),
    ('INVENTORY_MANAGER', 'Manages stock, purchase orders and suppliers'),
    ('DOCTOR', 'Prescribes medication'),
    ('AUDITOR', 'Read-only access to audit trails and reports');
