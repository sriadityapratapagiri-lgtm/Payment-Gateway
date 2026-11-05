package com.payflow.gateway.order.dto;

import com.payflow.gateway.order.entity.Order;

import java.time.OffsetDateTime;
import java.util.UUID;

public record OrderResponse(
    UUID id,
    UUID merchantId,
    String orderReference,
    Long amountMinor,
    String currency,
    String status,
    String description,
    OffsetDateTime createdAt
) {
    public static OrderResponse from(Order order) {
        return new OrderResponse(
            order.getId(),
            order.getMerchantId(),
            order.getOrderReference(),
            order.getAmountMinor(),
            order.getCurrency(),
            order.getStatus(),
            order.getDescription(),
            order.getCreatedAt()
        );
    }
}

