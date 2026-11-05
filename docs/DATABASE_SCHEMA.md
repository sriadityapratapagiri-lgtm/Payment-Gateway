# PayFlow Database Schema Reference

PostgreSQL schema initialized via Flyway migration `V1__initial_schema.sql`.

## Tables

### 1. `merchants`
Stores merchant authentication and profile credentials.
* `id`: UUID (Primary Key)
* `business_name`: VARCHAR(255) NOT NULL
* `email`: VARCHAR(255) NOT NULL UNIQUE
* `password_hash`: VARCHAR(255) NOT NULL
* `status`: VARCHAR(50) NOT NULL ('ACTIVE', 'SUSPENDED', 'INACTIVE')
* `created_at`: TIMESTAMPTZ NOT NULL

### 2. `orders`
Stores customer payment intents / purchase requests.
* `id`: UUID (Primary Key)
* `merchant_id`: UUID REFERENCES merchants(id)
* `order_reference`: VARCHAR(255) NOT NULL
* `amount_minor`: BIGINT NOT NULL (CHECK > 0)
* `currency`: VARCHAR(3) NOT NULL
* `status`: VARCHAR(50) NOT NULL ('CREATED', 'PROCESSING', 'COMPLETED', 'CANCELLED', 'FAILED')
* `description`: TEXT
* `created_at`: TIMESTAMPTZ NOT NULL
* **Constraint**: `UNIQUE (merchant_id, order_reference)`

### 3. `payments`
Transactions executing against an order.
* `id`: UUID (Primary Key)
* `order_id`: UUID REFERENCES orders(id)
* `merchant_id`: UUID REFERENCES merchants(id)
* `amount_minor`: BIGINT NOT NULL (CHECK > 0)
* `currency`: VARCHAR(3) NOT NULL
* `status`: VARCHAR(50) NOT NULL ('CREATED', 'PROCESSING', 'AUTHORIZED', 'CAPTURED', 'FAILED', 'CANCELLED', 'PARTIALLY_REFUNDED', 'REFUNDED')
* `payment_method`: VARCHAR(100) NOT NULL
* `provider_reference`: VARCHAR(255)
* `simulation_result`: VARCHAR(50)
* `created_at`: TIMESTAMPTZ NOT NULL
* `updated_at`: TIMESTAMPTZ NOT NULL

### 4. `refunds`
Reversals against captured payments.
* `id`: UUID (Primary Key)
* `payment_id`: UUID REFERENCES payments(id)
* `merchant_id`: UUID REFERENCES merchants(id)
* `amount_minor`: BIGINT NOT NULL (CHECK > 0)
* `currency`: VARCHAR(3) NOT NULL
* `status`: VARCHAR(50) NOT NULL ('PENDING', 'PROCESSING', 'SUCCEEDED', 'FAILED')
* `reason`: TEXT
* `created_at`: TIMESTAMPTZ NOT NULL

### 5. `ledger_accounts`
Chart of accounts for internal accounting.
* `id`: UUID (Primary Key)
* `merchant_id`: UUID NULL (NULL for system accounts)
* `account_code`: VARCHAR(100) NOT NULL
* `account_type`: VARCHAR(50) NOT NULL ('ASSET', 'LIABILITY', 'REVENUE', 'EXPENSE')
* `currency`: VARCHAR(3) NOT NULL
* `created_at`: TIMESTAMPTZ NOT NULL
* **Constraint**: `UNIQUE (account_code, merchant_id, currency)`

### 6. `journal_entries`
Immutable header for balanced financial movements.
* `id`: UUID (Primary Key)
* `reference_type`: VARCHAR(50) NOT NULL ('PAYMENT', 'REFUND')
* `reference_id`: UUID NOT NULL
* `description`: TEXT
* `status`: VARCHAR(50) NOT NULL DEFAULT 'POSTED'
* `created_at`: TIMESTAMPTZ NOT NULL

### 7. `ledger_lines`
Double-entry line items tied to journal entries.
* `id`: UUID (Primary Key)
* `journal_entry_id`: UUID REFERENCES journal_entries(id)
* `account_id`: UUID REFERENCES ledger_accounts(id)
* `direction`: VARCHAR(10) NOT NULL ('DEBIT', 'CREDIT')
* `amount_minor`: BIGINT NOT NULL (CHECK > 0)
* `currency`: VARCHAR(3) NOT NULL

### 8. `idempotency_keys`
Guaranteeing single-execution semantics.
* `id`: UUID (Primary Key)
* `merchant_id`: UUID REFERENCES merchants(id)
* `idempotency_key`: VARCHAR(255) NOT NULL
* `request_hash`: VARCHAR(64) NOT NULL
* `response_status`: INTEGER
* `response_body`: TEXT
* `status`: VARCHAR(50) NOT NULL ('PROCESSING', 'COMPLETED', 'FAILED')
* `created_at`: TIMESTAMPTZ NOT NULL
* **Constraint**: `UNIQUE (merchant_id, idempotency_key)`

