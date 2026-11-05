package com.payflow.gateway.auth.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AuthResponse(
    String token,
    String tokenType,
    UUID merchantId,
    String email,
    String businessName,
    OffsetDateTime createdAt
) {
    public AuthResponse(String token, UUID merchantId, String email, String businessName, OffsetDateTime createdAt) {
        this(token, "Bearer", merchantId, email, businessName, createdAt);
    }
}

