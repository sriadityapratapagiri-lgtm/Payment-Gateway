# PayFlow API Documentation

All endpoints adhere to the standard envelope format:

```json
{
  "success": true,
  "message": "Human readable message",
  "data": { ... }
}
```

## Authentication

### `POST /api/v1/auth/register`
Register a new merchant account.
* **Payload**:
  ```json
  {
    "businessName": "Acme Corp",
    "email": "merchant@acme.com",
    "password": "Password123"
  }
  ```
* **Response `201 Created`**:
  ```json
  {
    "success": true,
    "message": "Merchant registered successfully",
    "data": {
      "token": "jwt.bearer.token",
      "tokenType": "Bearer",
      "merchantId": "uuid",
      "email": "merchant@acme.com",
      "businessName": "Acme Corp"
    }
  }
  ```

### `POST /api/v1/auth/login`
Authenticate merchant and receive JWT.
* **Payload**:
  ```json
  {
    "email": "merchant@acme.com",
    "password": "Password123"
  }
  ```

### `GET /api/v1/auth/me`
Retrieve profile of authenticated merchant.

---

## Orders

### `POST /api/v1/orders`
Create an order.
* **Headers**: `Authorization: Bearer <token>`
* **Payload**:
  ```json
  {
    "orderReference": "ORD-1001",
    "amountMinor": 100000,
    "currency": "INR",
    "description": "Premium Subscription"
  }
  ```

### `GET /api/v1/orders`
List orders for authenticated merchant.

### `GET /api/v1/orders/{id}`
Retrieve a specific order by ID.

### `POST /api/v1/orders/{id}/cancel`
Cancel an unpaid order.

---

## Payments

### `POST /api/v1/payments`
Process simulated payment for an order.
* **Headers**:
  * `Authorization: Bearer <token>`
  * `Idempotency-Key`: Optional UUID string
* **Payload**:
  ```json
  {
    "orderId": "uuid",
    "paymentMethod": "SIMULATED_CARD",
    "simulationResult": "SUCCESS"
  }
  ```
* **Valid `simulationResult` values**: `SUCCESS`, `FAILURE`, `PENDING`.

### `GET /api/v1/payments`
List payments for authenticated merchant.

### `GET /api/v1/payments/{id}`
Retrieve payment details.

### `POST /api/v1/payments/{id}/capture`
Capture an authorized payment.

### `POST /api/v1/payments/{id}/cancel`
Cancel an uncaptured authorized payment.

---

## Refunds

### `POST /api/v1/refunds`
Initiate full or partial refund.
* **Headers**: `Authorization: Bearer <token>`
* **Payload**:
  ```json
  {
    "paymentId": "uuid",
    "amountMinor": 50000,
    "reason": "Customer cancellation"
  }
  ```

### `GET /api/v1/refunds`
List all refunds for authenticated merchant.

---

## General Ledger & Analytics

### `GET /api/v1/ledger/entries`
List double-entry journal transactions with individual debit/credit ledger lines.

### `GET /api/v1/analytics/dashboard`
Fetch volume summary, success rates, charge counts, and daily settlement data.

