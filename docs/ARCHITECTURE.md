# PayFlow System Architecture

## 1. Overview
PayFlow is architected as a robust **Modular Monolith** designed around domain-driven service boundaries. It simulates core behaviors found in production payment service providers (PSPs) such as Stripe and Razorpay, specifically guaranteeing **idempotent payment creation** and **double-entry ledger immutability**.

```
                   +------------------------+
                   | React Frontend (Vite)  |
                   | (Port 3000 / Nginx)    |
                   +-----------+------------+
                               |
                               | REST / JSON (JWT Auth)
                               v
               +--------------------------------+
               |  Spring Boot 3.3.5 REST API   |
               |  (Modular Monolith - Port 8080)|
               +---------------+----------------+
                               |
       +-----------------------+-----------------------+
       |           |           |           |           |
       v           v           v           v           v
  +--------+  +--------+  +--------+  +--------+  +--------+
  | Auth   |  | Orders |  |Payments|  |Refunds |  | Ledger |
  | Module |  | Module |  | Engine |  | Module |  | Engine |
  +--------+  +--------+  +--------+  +--------+  +--------+
       |           |           |           |           |
       +-----------------------+-----------------------+
                               |
                               | JPA / Hibernate / Transactions
                               v
                   +------------------------+
                   |  PostgreSQL 16 Engine  |
                   |  (Flyway Migrations)   |
                   +------------------------+
```

## 2. Core Modules

### 2.1 Authentication & Merchant Boundary
* **JWT Access Tokens**: Stateless Bearer tokens containing merchant identity claims.
* **Tenant Isolation**: All queries enforce merchant ID scoping at the database and repository layers to prevent cross-merchant leakage.

### 2.2 Payment State Machine & Simulation Engine
Payment states strictly follow explicit lifecycle transitions:
`CREATED` -> `PROCESSING` -> `AUTHORIZED` -> `CAPTURED` -> (`PARTIALLY_REFUNDED` | `REFUNDED` | `FAILED` | `CANCELLED`).
State invariants prevent invalid operations (e.g., refunding an uncaptured or failed payment).

### 2.3 Idempotency Engine
* Payment creation requests accept an optional `Idempotency-Key` HTTP header.
* Uses PostgreSQL table `idempotency_keys` with unique composite index `(merchant_id, idempotency_key)`.
* When a repeat key with the identical payload is presented, the stored result is returned immediately.
* If the payload differs, an HTTP `409 Conflict` is returned.

### 2.4 Double-Entry General Ledger
Every captured payment and refund writes immutable journal lines:
* **Payment Capture**:
  * Debit: `PAYMENT_CLEARING`
  * Credit: `MERCHANT_PAYABLE` (Net)
  * Debit: `MERCHANT_PAYABLE` (Fee)
  * Credit: `GATEWAY_FEE_REVENUE`
* **Refund**:
  * Debit: `MERCHANT_PAYABLE`
  * Credit: `REFUND_CLEARING`
* **Invariant**: `∑ Debits = ∑ Credits` checked prior to saving. Rolling back a transaction rolls back both the payment and the ledger entries.

