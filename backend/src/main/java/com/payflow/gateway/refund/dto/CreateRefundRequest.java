package com.payflow.gateway.refund.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateRefundRequest(
    @NotNull(message = "Payment ID is required")
    UUID paymentId,

    @NotNull(message = "Refund amount is required")
    @Min(value = 1, message = "Refund amount must be positive")
    Long amountMinor,

    String reason
) {}

