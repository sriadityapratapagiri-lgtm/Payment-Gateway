package com.payflow.gateway.payment.provider;

public record PaymentProviderRequest(
    String paymentId,
    Long amountMinor,
    String currency,
    String paymentMethod,
    String simulationResult
) {}

