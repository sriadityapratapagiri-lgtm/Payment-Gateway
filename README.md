# PayFlow – Payment Gateway MVP

> **Simulation Notice**: PayFlow is a simulated payment gateway MVP designed for demonstration, educational, and architectural evaluation. It does not interface with live card networks, banking rails, or store actual PCI card credentials.

---

## 1. Project Overview

PayFlow provides an end-to-end simulated FinTech payment platform inspired by Stripe and Razorpay. It demonstrates robust backend engineering principles:
* **Idempotent Payment Processing** (PostgreSQL-backed with payload SHA-256 validation)
* **Double-Entry Bookkeeping Engine** (`Debit PAYMENT_CLEARING`, `Credit MERCHANT_PAYABLE`, `Credit GATEWAY_FEE_REVENUE`)
* **Strict State Machine Management** (Preventing over-capture, invalid refund states, and over-refunding)
* **Tenant Isolation** (Guaranteed merchant scoping across all domain objects)
* **Single-Command Docker Orchestration** (`docker compose up --build`)

---

## 2. Technology Stack

* **Backend**: Java 21, Spring Boot 3.3.5, Spring Data JPA, Spring Security (JWT), Hibernate, Flyway
* **Frontend**: React 19, TypeScript, Vite, Tailwind CSS v4, Lucide React, Recharts, Axios
* **Database**: PostgreSQL 16
* **Build / Dev**: Maven, pnpm, Docker & Docker Compose

---

## 3. Architecture

```
                  +--------------------------------+
                  |  React + TypeScript Frontend   |
                  |     (Tailwind CSS, Vite)       |
                  +---------------+----------------+
                                  |
                                  | REST API / JWT
                                  v
                  +--------------------------------+
                  |   Spring Boot Modular Monolith |
                  |       (Port 8080)              |
                  +---------------+----------------+
                                  |
       +--------------------------+-------------------------+
       |             |            |            |            |
  +----+-----+  +----+----+  +----+-----+ +----+-----+ +----+-----+
  |   Auth   |  | Orders  |  | Payments | | Refunds  | |  Ledger  |
  +----------+  +---------+  +----------+ +----------+ +----------+
                                  |
                                  v
                  +--------------------------------+
                  |    PostgreSQL 16 Database      |
                  |   (Flyway Managed Migrations)  |
                  +--------------------------------+
```

---

## 4. Key Features

1. **Merchant Authentication & Session Security**:
   * BCrypt password hashing, stateless JWT issuance and verification.
2. **Order Lifecycle**:
   * Create unique reference payment orders with minor currency units (paise/cents).
3. **Interactive Payment Simulation**:
   * Choose simulation outcomes: `SUCCESS`, `FAILURE`, or `PENDING`.
4. **Idempotency Protection**:
   * Guarantees that network retries using the same `Idempotency-Key` never cause duplicate charges.
   * Protects against conflicting payloads with HTTP 409 Conflict.
5. **Double-Entry General Ledger**:
   * Real-time ledger lines recorded upon payment capture and refund.
   * Automatic validation: `∑ Debits = ∑ Credits`.
6. **Partial & Full Refunds**:
   * Prevent over-refunding via database validation and locking.
7. **FinTech Analytics Dashboard**:
   * Volume trends, charge conversion rates, and settlement breakdown.

---

## 5. Quick Start (Docker Compose)

### Prerequisites
* Docker and Docker Compose installed.

### Steps
1. Clone the repository and configure environment:
   ```bash
   cp .env.example .env
   ```

2. Run the complete application stack:
   ```bash
   docker-compose up --build
   # Or on modern Docker CLI:
   docker compose up --build
   ```

3. Access the applications:
   * **Frontend Dashboard**: [http://localhost:3000](http://localhost:3000) (or `http://localhost:5173` if running dev server)
   * **Backend API**: [http://localhost:8080](http://localhost:8080)
   * **Actuator Health**: [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health)

---

## 6. Local Development Setup

### Backend (Java 21 + Maven)
```bash
cd backend
mvn clean package -DskipTests
java -jar target/payment-gateway-1.0.0.jar
```

### Frontend (React + Vite)
```bash
cd frontend
pnpm install
pnpm dev
```

---

## 7. Automated Testing

Integration tests covering authentication, order idempotency, refund limits, and ledger invariants:
```bash
cd backend
mvn test
```

---

## 8. Known Limitations (MVP Scope)
* Mock payment simulation provider (does not communicate with external acquiring banks).
* In-memory/Postgres idempotency store without Redis caching tier (intentional per MVP constraints).

