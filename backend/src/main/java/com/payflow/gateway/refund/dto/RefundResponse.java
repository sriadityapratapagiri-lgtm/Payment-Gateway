package com.payflow.gateway.refund.dto;

import com.payflow.gateway.refund.entity.Refund;

import java.time.OffsetDateTime;
import java.util.UUID;

public record RefundResponse(
    UUID id,
    UUID paymentId,
    UUID merchantId,
    Long amountMinor,
    String currency,
    String status,
    String reason,
    OffsetDateTime createdAt
) {
    public static RefundResponse from(Refund refund) {
        return new RefundResponse(
            refund.getId(),
            refund.getPaymentId(),
            refund.getMerchantId(),
            refund.getAmountMinor(),
            refund.getCurrency(),
            refund.getStatus(),
            refund.getReason(),
            refund.getCreatedAt()
        );
    }
}

