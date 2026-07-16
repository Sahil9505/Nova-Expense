-- Nova foundation schema (Phase 1)
-- UUID primary keys, snake_case naming, audit columns, foreign keys, indexes.

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE app_users (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email       VARCHAR(255) NOT NULL,
    full_name   VARCHAR(255),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_app_users_email UNIQUE (email)
);

CREATE TABLE categories (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES app_users (id) ON DELETE CASCADE,
    name        VARCHAR(120) NOT NULL,
    color       VARCHAR(32),
    icon        VARCHAR(64),
    is_system   BOOLEAN NOT NULL DEFAULT false,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_categories_user_name UNIQUE (user_id, name)
);

CREATE TABLE accounts (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES app_users (id) ON DELETE CASCADE,
    name        VARCHAR(120) NOT NULL,
    type        VARCHAR(32) NOT NULL,
    currency    VARCHAR(8) NOT NULL DEFAULT 'USD',
    balance     NUMERIC(18, 4) NOT NULL DEFAULT 0,
    is_active   BOOLEAN NOT NULL DEFAULT true,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE transactions (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID NOT NULL REFERENCES app_users (id) ON DELETE CASCADE,
    account_id   UUID REFERENCES accounts (id) ON DELETE SET NULL,
    category_id  UUID REFERENCES categories (id) ON DELETE SET NULL,
    amount       NUMERIC(18, 4) NOT NULL,
    type         VARCHAR(16) NOT NULL,
    description  VARCHAR(255),
    occurred_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE budgets (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES app_users (id) ON DELETE CASCADE,
    category_id UUID REFERENCES categories (id) ON DELETE SET NULL,
    name        VARCHAR(120) NOT NULL,
    amount      NUMERIC(18, 4) NOT NULL,
    period      VARCHAR(16) NOT NULL,
    start_date  DATE NOT NULL,
    end_date    DATE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_categories_user_id ON categories (user_id);
CREATE INDEX idx_accounts_user_id ON accounts (user_id);
CREATE INDEX idx_transactions_user_id ON transactions (user_id);
CREATE INDEX idx_transactions_account_id ON transactions (account_id);
CREATE INDEX idx_transactions_category_id ON transactions (category_id);
CREATE INDEX idx_transactions_occurred_at ON transactions (occurred_at);
CREATE INDEX idx_budgets_user_id ON budgets (user_id);
