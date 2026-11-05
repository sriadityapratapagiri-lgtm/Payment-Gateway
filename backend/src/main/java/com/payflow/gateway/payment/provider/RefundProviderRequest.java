package com.payflow.gateway.payment.provider;

public record RefundProviderRequest(
    String refundId,
    String paymentId,
    Long amountMinor,
    String currency,
    String reason
) {}

