package com.payflow.gateway.payment.dto;

import com.payflow.gateway.payment.entity.Payment;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PaymentResponse(
    UUID id,
    UUID orderId,
    UUID merchantId,
    Long amountMinor,
    String currency,
    String status,
    String paymentMethod,
    String providerReference,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {
    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
            payment.getId(),
            payment.getOrderId(),
            payment.getMerchantId(),
            payment.getAmountMinor(),
            payment.getCurrency(),
            payment.getStatus(),
            payment.getPaymentMethod(),
            payment.getProviderReference(),
            payment.getCreatedAt(),
            payment.getUpdatedAt()
        );
    }
}

