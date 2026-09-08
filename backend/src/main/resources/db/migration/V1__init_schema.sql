-- PayFlow V1 — Core relational schema
-- Simulated payment infrastructure (no real money movement)

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ========== ENUM-LIKE LOOKUP TABLES VIA CHECK CONSTRAINTS ==========

CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    full_name       VARCHAR(255) NOT NULL,
    role            VARCHAR(50)  NOT NULL,
    merchant_id     UUID,
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    last_login_at   TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_users_role CHECK (role IN ('ADMIN', 'MERCHANT_ADMIN', 'MERCHANT_USER', 'RISK_ANALYST'))
);

CREATE TABLE merchants (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_name       VARCHAR(255) NOT NULL,
    legal_name          VARCHAR(255),
    status              VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    country_code        VARCHAR(3) NOT NULL DEFAULT 'US',
    currency            VARCHAR(3) NOT NULL DEFAULT 'USD',
    settlement_status   VARCHAR(50) NOT NULL DEFAULT 'CURRENT',
    webhook_secret      VARCHAR(255),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_merchants_status CHECK (status IN ('ACTIVE', 'SUSPENDED', 'PENDING', 'CLOSED')),
    CONSTRAINT chk_merchants_settlement CHECK (settlement_status IN ('CURRENT', 'PENDING', 'OVERDUE', 'HOLD'))
);

ALTER TABLE users
    ADD CONSTRAINT fk_users_merchant
    FOREIGN KEY (merchant_id) REFERENCES merchants(id);

CREATE TABLE customers (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    merchant_id     UUID NOT NULL REFERENCES merchants(id),
    email           VARCHAR(255) NOT NULL,
    full_name       VARCHAR(255) NOT NULL,
    phone           VARCHAR(50),
    country_code    VARCHAR(3),
    status          VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    blocked         BOOLEAN NOT NULL DEFAULT FALSE,
    blocked_reason  TEXT,
    metadata        JSONB,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_customers_merchant_email UNIQUE (merchant_id, email),
    CONSTRAINT chk_customers_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'BLOCKED'))
);

CREATE TABLE payment_methods (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id     UUID NOT NULL REFERENCES customers(id),
    type            VARCHAR(50) NOT NULL,
    last_four       VARCHAR(4),
    brand           VARCHAR(50),
    expiry_month    SMALLINT,
    expiry_year     SMALLINT,
    token_ref       VARCHAR(255),
    is_default      BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_payment_methods_type CHECK (type IN ('CARD', 'UPI', 'NET_BANKING', 'WALLET'))
);

CREATE TABLE payments (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    merchant_id         UUID NOT NULL REFERENCES merchants(id),
    customer_id         UUID REFERENCES customers(id),
    payment_method_id   UUID REFERENCES payment_methods(id),
    reference           VARCHAR(64) NOT NULL UNIQUE,
    amount_cents        BIGINT NOT NULL,
    currency            VARCHAR(3) NOT NULL DEFAULT 'USD',
    status              VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    description         TEXT,
    idempotency_key     VARCHAR(128),
    checkout_session_id VARCHAR(128),
    metadata            JSONB,
    created_by          UUID REFERENCES users(id),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_payments_amount CHECK (amount_cents > 0),
    CONSTRAINT chk_payments_status CHECK (status IN (
        'PENDING', 'FRAUD_SCREENING', 'APPROVED', 'BLOCKED',
        'PROCESSING', 'COMPLETED', 'FAILED', 'CANCELLED',
        'REFUND_PENDING', 'REFUNDED', 'PARTIALLY_REFUNDED'
    )),
    CONSTRAINT uq_payments_merchant_idempotency UNIQUE (merchant_id, idempotency_key)
);

CREATE TABLE transactions (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payment_id          UUID NOT NULL REFERENCES payments(id),
    merchant_id         UUID NOT NULL REFERENCES merchants(id),
    customer_id         UUID REFERENCES customers(id),
    reference           VARCHAR(64) NOT NULL UNIQUE,
    amount_cents        BIGINT NOT NULL,
    currency            VARCHAR(3) NOT NULL DEFAULT 'USD',
    status              VARCHAR(50) NOT NULL,
    payment_method_type VARCHAR(50),
    location_city       VARCHAR(100),
    location_country    VARCHAR(100),
    device_info         VARCHAR(255),
    ip_address          VARCHAR(64),
    is_new_device        BOOLEAN NOT NULL DEFAULT FALSE,
    risk_score          INTEGER,
    risk_level          VARCHAR(20),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_transactions_status CHECK (status IN (
        'PENDING', 'FRAUD_SCREENING', 'APPROVED', 'BLOCKED',
        'PROCESSING', 'COMPLETED', 'FAILED', 'CANCELLED',
        'REFUND_PENDING', 'REFUNDED', 'PARTIALLY_REFUNDED'
    )),
    CONSTRAINT chk_transactions_risk_level CHECK (risk_level IS NULL OR risk_level IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL'))
);

CREATE TABLE transaction_events (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_id  UUID NOT NULL REFERENCES transactions(id),
    event_type      VARCHAR(100) NOT NULL,
    from_status     VARCHAR(50),
    to_status       VARCHAR(50),
    message         TEXT,
    metadata        JSONB,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE fraud_rules (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code            VARCHAR(100) NOT NULL UNIQUE,
    name            VARCHAR(255) NOT NULL,
    description     TEXT,
    weight          INTEGER NOT NULL,
    threshold_value NUMERIC(18, 4),
    enabled         BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE risk_assessments (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_id  UUID NOT NULL UNIQUE REFERENCES transactions(id),
    risk_score      INTEGER NOT NULL,
    risk_level      VARCHAR(20) NOT NULL,
    decision        VARCHAR(50) NOT NULL,
    factors         JSONB NOT NULL,
    assessed_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_risk_level CHECK (risk_level IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    CONSTRAINT chk_risk_decision CHECK (decision IN ('APPROVE', 'REVIEW', 'BLOCK'))
);

CREATE TABLE fraud_alerts (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_id  UUID NOT NULL REFERENCES transactions(id),
    merchant_id     UUID NOT NULL REFERENCES merchants(id),
    severity        VARCHAR(20) NOT NULL,
    title           VARCHAR(255) NOT NULL,
    description     TEXT,
    status          VARCHAR(50) NOT NULL DEFAULT 'OPEN',
    assigned_to     UUID REFERENCES users(id),
    resolved_at     TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_fraud_alerts_severity CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    CONSTRAINT chk_fraud_alerts_status CHECK (status IN ('OPEN', 'INVESTIGATING', 'RESOLVED', 'DISMISSED'))
);

CREATE TABLE ledger_accounts (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    merchant_id     UUID REFERENCES merchants(id),
    customer_id     UUID REFERENCES customers(id),
    account_type    VARCHAR(50) NOT NULL,
    currency        VARCHAR(3) NOT NULL DEFAULT 'USD',
    name            VARCHAR(255) NOT NULL,
    balance_cents   BIGINT NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_ledger_accounts_type CHECK (account_type IN (
        'MERCHANT_RECEIVABLE', 'CUSTOMER_PAYABLE', 'PLATFORM_FEE', 'SETTLEMENT', 'REFUND_RESERVE'
    ))
);

CREATE TABLE ledger_entries (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id      UUID NOT NULL REFERENCES ledger_accounts(id),
    payment_id      UUID REFERENCES payments(id),
    transaction_id  UUID REFERENCES transactions(id),
    entry_type      VARCHAR(10) NOT NULL,
    amount_cents    BIGINT NOT NULL,
    currency        VARCHAR(3) NOT NULL DEFAULT 'USD',
    description     TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_ledger_entries_type CHECK (entry_type IN ('DEBIT', 'CREDIT')),
    CONSTRAINT chk_ledger_entries_amount CHECK (amount_cents > 0)
);

CREATE TABLE refunds (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payment_id      UUID NOT NULL REFERENCES payments(id),
    transaction_id  UUID NOT NULL REFERENCES transactions(id),
    merchant_id     UUID NOT NULL REFERENCES merchants(id),
    amount_cents    BIGINT NOT NULL,
    currency        VARCHAR(3) NOT NULL DEFAULT 'USD',
    status          VARCHAR(50) NOT NULL DEFAULT 'REQUESTED',
    reason          TEXT,
    idempotency_key VARCHAR(128),
    requested_by    UUID REFERENCES users(id),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_refunds_amount CHECK (amount_cents > 0),
    CONSTRAINT chk_refunds_status CHECK (status IN ('REQUESTED', 'PROCESSING', 'COMPLETED', 'FAILED')),
    CONSTRAINT uq_refunds_idempotency UNIQUE (payment_id, idempotency_key)
);

CREATE TABLE settlements (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    merchant_id         UUID NOT NULL REFERENCES merchants(id),
    reference           VARCHAR(64) NOT NULL UNIQUE,
    gross_amount_cents  BIGINT NOT NULL,
    fee_amount_cents    BIGINT NOT NULL DEFAULT 0,
    net_amount_cents    BIGINT NOT NULL,
    currency            VARCHAR(3) NOT NULL DEFAULT 'USD',
    transaction_count   INTEGER NOT NULL DEFAULT 0,
    status              VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    settlement_date     DATE,
    processed_at        TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_settlements_status CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED'))
);

CREATE TABLE notifications (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(id),
    type            VARCHAR(100) NOT NULL,
    title           VARCHAR(255) NOT NULL,
    message         TEXT NOT NULL,
    resource_type   VARCHAR(100),
    resource_id     UUID,
    read            BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE webhooks (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    merchant_id     UUID NOT NULL REFERENCES merchants(id),
    url             VARCHAR(2048) NOT NULL,
    events          TEXT[] NOT NULL,
    secret_hash     VARCHAR(255),
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE webhook_deliveries (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    webhook_id      UUID NOT NULL REFERENCES webhooks(id),
    event_type      VARCHAR(100) NOT NULL,
    payload         JSONB NOT NULL,
    status          VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    attempt_count   INTEGER NOT NULL DEFAULT 0,
    response_code   INTEGER,
    response_body   TEXT,
    next_retry_at   TIMESTAMPTZ,
    delivered_at    TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_webhook_deliveries_status CHECK (status IN ('PENDING', 'SUCCESS', 'FAILED', 'RETRYING'))
);

CREATE TABLE api_keys (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    merchant_id     UUID NOT NULL REFERENCES merchants(id),
    name            VARCHAR(255) NOT NULL,
    key_prefix      VARCHAR(16) NOT NULL,
    key_hash        VARCHAR(255) NOT NULL,
    last_used_at    TIMESTAMPTZ,
    revoked_at      TIMESTAMPTZ,
    created_by      UUID REFERENCES users(id),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE audit_logs (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID REFERENCES users(id),
    action          VARCHAR(100) NOT NULL,
    resource_type   VARCHAR(100),
    resource_id     VARCHAR(100),
    ip_address      VARCHAR(64),
    metadata        JSONB,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE refresh_tokens (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash      VARCHAR(255) NOT NULL UNIQUE,
    expires_at      TIMESTAMPTZ NOT NULL,
    revoked         BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ========== INDEXES ==========

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_merchant ON users(merchant_id);

CREATE INDEX idx_customers_merchant ON customers(merchant_id);
CREATE INDEX idx_customers_email ON customers(email);
CREATE INDEX idx_customers_status ON customers(status);

CREATE INDEX idx_payments_merchant ON payments(merchant_id);
CREATE INDEX idx_payments_customer ON payments(customer_id);
CREATE INDEX idx_payments_status ON payments(status);
CREATE INDEX idx_payments_created ON payments(created_at DESC);
CREATE INDEX idx_payments_reference ON payments(reference);
CREATE INDEX idx_payments_idempotency ON payments(merchant_id, idempotency_key);

CREATE INDEX idx_transactions_payment ON transactions(payment_id);
CREATE INDEX idx_transactions_merchant ON transactions(merchant_id);
CREATE INDEX idx_transactions_customer ON transactions(customer_id);
CREATE INDEX idx_transactions_status ON transactions(status);
CREATE INDEX idx_transactions_created ON transactions(created_at DESC);
CREATE INDEX idx_transactions_reference ON transactions(reference);
CREATE INDEX idx_transactions_risk ON transactions(risk_level, risk_score);
CREATE INDEX idx_transactions_merchant_created ON transactions(merchant_id, created_at DESC);

CREATE INDEX idx_transaction_events_tx ON transaction_events(transaction_id, created_at);

CREATE INDEX idx_fraud_alerts_merchant ON fraud_alerts(merchant_id);
CREATE INDEX idx_fraud_alerts_status ON fraud_alerts(status);
CREATE INDEX idx_fraud_alerts_created ON fraud_alerts(created_at DESC);
CREATE INDEX idx_fraud_alerts_tx ON fraud_alerts(transaction_id);

CREATE INDEX idx_risk_assessments_tx ON risk_assessments(transaction_id);

CREATE INDEX idx_ledger_accounts_merchant ON ledger_accounts(merchant_id);
CREATE INDEX idx_ledger_entries_account ON ledger_entries(account_id, created_at DESC);
CREATE INDEX idx_ledger_entries_payment ON ledger_entries(payment_id);
CREATE INDEX idx_ledger_entries_tx ON ledger_entries(transaction_id);

CREATE INDEX idx_refunds_payment ON refunds(payment_id);
CREATE INDEX idx_refunds_merchant ON refunds(merchant_id);
CREATE INDEX idx_refunds_status ON refunds(status);

CREATE INDEX idx_settlements_merchant ON settlements(merchant_id);
CREATE INDEX idx_settlements_status ON settlements(status);
CREATE INDEX idx_settlements_date ON settlements(settlement_date DESC);

CREATE INDEX idx_notifications_user ON notifications(user_id, read, created_at DESC);

CREATE INDEX idx_webhooks_merchant ON webhooks(merchant_id);
CREATE INDEX idx_webhook_deliveries_webhook ON webhook_deliveries(webhook_id, created_at DESC);
CREATE INDEX idx_webhook_deliveries_status ON webhook_deliveries(status);

CREATE INDEX idx_api_keys_merchant ON api_keys(merchant_id);
CREATE INDEX idx_api_keys_prefix ON api_keys(key_prefix);

CREATE INDEX idx_audit_logs_user ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_action ON audit_logs(action);
CREATE INDEX idx_audit_logs_created ON audit_logs(created_at DESC);
CREATE INDEX idx_audit_logs_resource ON audit_logs(resource_type, resource_id);

CREATE INDEX idx_refresh_tokens_user ON refresh_tokens(user_id);
