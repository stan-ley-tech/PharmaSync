CREATE TABLE users (
    id                     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    pharmacy_id            BIGINT REFERENCES pharmacies (id),
    username               VARCHAR(60)  NOT NULL,
    email                  VARCHAR(150) NOT NULL,
    password_hash          VARCHAR(100) NOT NULL,
    first_name             VARCHAR(80)  NOT NULL,
    last_name              VARCHAR(80)  NOT NULL,
    license_number         VARCHAR(60),
    is_active              BOOLEAN      NOT NULL DEFAULT true,
    is_locked              BOOLEAN      NOT NULL DEFAULT false,
    failed_login_attempts  INTEGER      NOT NULL DEFAULT 0,
    created_at             TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at             TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_users_username UNIQUE (username),
    CONSTRAINT uq_users_email UNIQUE (email)
);

CREATE INDEX idx_users_pharmacy_id ON users (pharmacy_id);

CREATE TABLE user_roles (
    user_id    BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role_id    BIGINT NOT NULL REFERENCES roles (id) ON DELETE CASCADE,
    granted_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, role_id)
);
