package com.payflow.gateway.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateOrderRequest(
    @NotBlank(message = "Order reference is required")
    @Size(max = 255, message = "Order reference must not exceed 255 characters")
    String orderReference,

    @NotNull(message = "Amount is required")
    @Min(value = 1, message = "Amount must be positive")
    Long amountMinor,

    @NotBlank(message = "Currency is required")
    @Size(min = 3, max = 3, message = "Currency must be a 3-letter code")
    String currency,

    String description
) {}

