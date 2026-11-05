package com.payflow.gateway.payment.provider;

import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Mock payment provider for simulation purposes.
 * This is a demonstration-only implementation.
 * No real payment processing occurs.
 */
@Component
public class MockPaymentProvider implements PaymentProvider {

    @Override
    public PaymentProviderResult processPayment(PaymentProviderRequest request) {
        String providerRef = "MOCK_PAY_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();

        return switch (request.simulationResult()) {
            case "SUCCESS" -> PaymentProviderResult.success(providerRef);
            case "PENDING" -> PaymentProviderResult.pending("MOCK_PENDING_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            case "FAILURE" -> PaymentProviderResult.failure("Simulated payment failure: card declined");
            default -> PaymentProviderResult.failure("Unknown simulation result");
        };
    }

    @Override
    public PaymentProviderResult processRefund(RefundProviderRequest request) {
        String providerRef = "MOCK_REF_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        // Refunds always succeed in simulation
        return PaymentProviderResult.success(providerRef);
    }
}

