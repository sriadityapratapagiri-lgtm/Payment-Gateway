package com.payflow.gateway.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

public record CreatePaymentRequest(
    @NotNull(message = "Order ID is required")
    UUID orderId,

    @NotBlank(message = "Payment method is required")
    String paymentMethod,

    @NotNull(message = "Simulation result is required for demo environment")
    @Pattern(regexp = "SUCCESS|FAILURE|PENDING", message = "simulationResult must be SUCCESS, FAILURE, or PENDING")
    String simulationResult
) {}

