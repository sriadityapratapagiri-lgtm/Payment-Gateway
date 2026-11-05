package com.payflow.gateway.payment.provider;

public record PaymentProviderResult(
    boolean success,
    boolean pending,
    String providerReference,
    String errorMessage
) {
    public static PaymentProviderResult success(String providerReference) {
        return new PaymentProviderResult(true, false, providerReference, null);
    }

    public static PaymentProviderResult pending(String providerReference) {
        return new PaymentProviderResult(false, true, providerReference, null);
    }

    public static PaymentProviderResult failure(String errorMessage) {
        return new PaymentProviderResult(false, false, null, errorMessage);
    }
}

