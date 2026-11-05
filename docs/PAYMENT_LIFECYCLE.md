# Payment Lifecycle & State Machine

```
              +-------------------+
              |      CREATED      |
              +---------+---------+
                        |
                        v
              +-------------------+
              |    PROCESSING     |
              +----+---------+----+
                   |         |
     (Declined)    |         | (Success)
         +---------+         +---------+
         |                             |
         v                             v
  +--------------+              +--------------+
  |    FAILED    |              |  AUTHORIZED  |
  +--------------+              +-------+------+
                                        |
                         +--------------+--------------+
                         | (Auto-capture / Manual)     | (Merchant Cancel)
                         v                             v
                  +--------------+              +---------------+
                  |   CAPTURED   |              |   CANCELLED   |
                  +-------+------+              +---------------+
                          |
             +------------+------------+
             |                         |
     (Partial Refund)            (Full Refund)
             |                         |
             v                         v
   +--------------------+     +----------------+
   | PARTIALLY_REFUNDED |     |    REFUNDED    |
   +---------+----------+     +----------------+
             |                         ^
             +-------------------------+
                 (Subsequent Full)
```

## State Invariants

1. **No Over-Captures**: A payment cannot be captured more than once or after entering a terminal failure or cancellation state.
2. **Double-Entry Synchronization**: A payment enters `CAPTURED` state in the **exact same ACID transaction** that generates its balanced ledger entries:
   * `Debit PAYMENT_CLEARING`
   * `Credit MERCHANT_PAYABLE` (Gross - Gateway Fee)
   * `Debit MERCHANT_PAYABLE` (Gateway Fee)
   * `Credit GATEWAY_FEE_REVENUE`
3. **No Over-Refunding**: The sum of all successful refunds against a payment cannot exceed `payment.amount_minor`. When total refunds equal the payment amount, state becomes `REFUNDED`; otherwise `PARTIALLY_REFUNDED`.
4. **Idempotent Guarantees**: Payment submissions using the same idempotency key return the original response immediately without re-executing state machine transitions.

