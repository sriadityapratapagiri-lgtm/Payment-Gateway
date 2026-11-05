-- V1: Initial schema for PayFlow Payment Gateway
-- All amounts stored as BIGINT minor currency units (paise for INR, cents for USD, etc.)

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- =============================================
-- MERCHANTS TABLE
-- =============================================
CREATE TABLE merchants (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    business_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_merchant_status CHECK (status IN ('ACTIVE', 'SUSPENDED', 'INACTIVE'))
);

CREATE INDEX idx_merchants_email ON merchants(email);
CREATE INDEX idx_merchants_status ON merchants(status);

-- =============================================
-- ORDERS TABLE
-- =============================================
CREATE TABLE orders (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    merchant_id UUID NOT NULL REFERENCES merchants(id),
    order_reference VARCHAR(255) NOT NULL,
    amount_minor BIGINT NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'CREATED',
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_order_amount_positive CHECK (amount_minor > 0),
    CONSTRAINT chk_order_status CHECK (status IN ('CREATED', 'PROCESSING', 'COMPLETED', 'CANCELLED', 'FAILED')),
    CONSTRAINT uq_merchant_order_reference UNIQUE (merchant_id, order_reference)
);

CREATE INDEX idx_orders_merchant_id ON orders(merchant_id);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_orders_created_at ON orders(created_at);

-- =============================================
-- PAYMENTS TABLE
-- =============================================
CREATE TABLE payments (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    order_id UUID NOT NULL REFERENCES orders(id),
    merchant_id UUID NOT NULL REFERENCES merchants(id),
    amount_minor BIGINT NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'CREATED',
    payment_method VARCHAR(100) NOT NULL,
    provider_reference VARCHAR(255),
    simulation_result VARCHAR(50),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_payment_amount_positive CHECK (amount_minor > 0),
    CONSTRAINT chk_payment_status CHECK (status IN (
        'CREATED', 'PROCESSING', 'AUTHORIZED', 'CAPTURED',
        'FAILED', 'CANCELLED', 'PARTIALLY_REFUNDED', 'REFUNDED'
    ))
);

CREATE INDEX idx_payments_merchant_id ON payments(merchant_id);
CREATE INDEX idx_payments_order_id ON payments(order_id);
CREATE INDEX idx_payments_status ON payments(status);
CREATE INDEX idx_payments_created_at ON payments(created_at);

-- =============================================
-- REFUNDS TABLE
-- =============================================
CREATE TABLE refunds (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    payment_id UUID NOT NULL REFERENCES payments(id),
    merchant_id UUID NOT NULL REFERENCES merchants(id),
    amount_minor BIGINT NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    reason TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_refund_amount_positive CHECK (amount_minor > 0),
    CONSTRAINT chk_refund_status CHECK (status IN ('PENDING', 'PROCESSING', 'SUCCEEDED', 'FAILED'))
);

CREATE INDEX idx_refunds_payment_id ON refunds(payment_id);
CREATE INDEX idx_refunds_merchant_id ON refunds(merchant_id);
CREATE INDEX idx_refunds_status ON refunds(status);
CREATE INDEX idx_refunds_created_at ON refunds(created_at);

-- =============================================
-- LEDGER ACCOUNTS TABLE
-- =============================================
CREATE TABLE ledger_accounts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    merchant_id UUID REFERENCES merchants(id),
    account_code VARCHAR(100) NOT NULL,
    account_type VARCHAR(50) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_account_type CHECK (account_type IN ('ASSET', 'LIABILITY', 'REVENUE', 'EXPENSE')),
    CONSTRAINT uq_account_code_merchant UNIQUE (account_code, merchant_id, currency)
);

CREATE INDEX idx_ledger_accounts_merchant_id ON ledger_accounts(merchant_id);
CREATE INDEX idx_ledger_accounts_code ON ledger_accounts(account_code);

-- =============================================
-- JOURNAL ENTRIES TABLE
-- =============================================
CREATE TABLE journal_entries (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    reference_type VARCHAR(50) NOT NULL,
    reference_id UUID NOT NULL,
    description TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'POSTED',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_journal_status CHECK (status IN ('POSTED', 'REVERSED'))
);

CREATE INDEX idx_journal_entries_reference_id ON journal_entries(reference_id);
CREATE INDEX idx_journal_entries_status ON journal_entries(status);
CREATE INDEX idx_journal_entries_created_at ON journal_entries(created_at);

-- =============================================
-- LEDGER LINES TABLE
-- =============================================
CREATE TABLE ledger_lines (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    journal_entry_id UUID NOT NULL REFERENCES journal_entries(id),
    account_id UUID NOT NULL REFERENCES ledger_accounts(id),
    direction VARCHAR(10) NOT NULL,
    amount_minor BIGINT NOT NULL,
    currency VARCHAR(3) NOT NULL,
    CONSTRAINT chk_ledger_direction CHECK (direction IN ('DEBIT', 'CREDIT')),
    CONSTRAINT chk_ledger_amount_positive CHECK (amount_minor > 0)
);

CREATE INDEX idx_ledger_lines_journal_entry_id ON ledger_lines(journal_entry_id);
CREATE INDEX idx_ledger_lines_account_id ON ledger_lines(account_id);

-- =============================================
-- IDEMPOTENCY KEYS TABLE
-- =============================================
CREATE TABLE idempotency_keys (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    merchant_id UUID NOT NULL REFERENCES merchants(id),
    idempotency_key VARCHAR(255) NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    response_status INTEGER,
    response_body TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'PROCESSING',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_merchant_idempotency_key UNIQUE (merchant_id, idempotency_key),
    CONSTRAINT chk_idempotency_status CHECK (status IN ('PROCESSING', 'COMPLETED', 'FAILED'))
);

CREATE INDEX idx_idempotency_merchant_key ON idempotency_keys(merchant_id, idempotency_key);

-- =============================================
-- SYSTEM LEDGER ACCOUNTS (seed data)
-- =============================================
INSERT INTO ledger_accounts (id, merchant_id, account_code, account_type, currency)
VALUES
    (uuid_generate_v4(), NULL, 'PAYMENT_CLEARING', 'ASSET', 'INR'),
    (uuid_generate_v4(), NULL, 'MERCHANT_PAYABLE', 'LIABILITY', 'INR'),
    (uuid_generate_v4(), NULL, 'GATEWAY_FEE_REVENUE', 'REVENUE', 'INR'),
    (uuid_generate_v4(), NULL, 'REFUND_CLEARING', 'ASSET', 'INR'),
    (uuid_generate_v4(), NULL, 'PAYMENT_CLEARING', 'ASSET', 'USD'),
    (uuid_generate_v4(), NULL, 'MERCHANT_PAYABLE', 'LIABILITY', 'USD'),
    (uuid_generate_v4(), NULL, 'GATEWAY_FEE_REVENUE', 'REVENUE', 'USD'),
    (uuid_generate_v4(), NULL, 'REFUND_CLEARING', 'ASSET', 'USD'),
    (uuid_generate_v4(), NULL, 'PAYMENT_CLEARING', 'ASSET', 'EUR'),
    (uuid_generate_v4(), NULL, 'MERCHANT_PAYABLE', 'LIABILITY', 'EUR'),
    (uuid_generate_v4(), NULL, 'GATEWAY_FEE_REVENUE', 'REVENUE', 'EUR'),
    (uuid_generate_v4(), NULL, 'REFUND_CLEARING', 'ASSET', 'EUR'),
    (uuid_generate_v4(), NULL, 'PAYMENT_CLEARING', 'ASSET', 'GBP'),
    (uuid_generate_v4(), NULL, 'MERCHANT_PAYABLE', 'LIABILITY', 'GBP'),
    (uuid_generate_v4(), NULL, 'GATEWAY_FEE_REVENUE', 'REVENUE', 'GBP'),
    (uuid_generate_v4(), NULL, 'REFUND_CLEARING', 'ASSET', 'GBP');

