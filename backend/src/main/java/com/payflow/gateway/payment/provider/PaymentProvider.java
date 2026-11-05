package com.payflow.gateway.payment.provider;

public interface PaymentProvider {
    PaymentProviderResult processPayment(PaymentProviderRequest request);
    PaymentProviderResult processRefund(RefundProviderRequest request);
}

