package com.payflow.gateway.order.controller;

import com.payflow.gateway.common.dto.ApiResponse;
import com.payflow.gateway.common.security.MerchantPrincipal;
import com.payflow.gateway.order.dto.CreateOrderRequest;
import com.payflow.gateway.order.dto.OrderResponse;
import com.payflow.gateway.order.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
        @AuthenticationPrincipal MerchantPrincipal principal,
        @Valid @RequestBody CreateOrderRequest request
    ) {
        var order = orderService.createOrder(principal.getMerchantId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Order created successfully", OrderResponse.from(order)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getOrders(
        @AuthenticationPrincipal MerchantPrincipal principal
    ) {
        List<OrderResponse> orders = orderService.getOrdersForMerchant(principal.getMerchantId())
            .stream().map(OrderResponse::from).toList();
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrder(
        @AuthenticationPrincipal MerchantPrincipal principal,
        @PathVariable UUID id
    ) {
        var order = orderService.getOrderForMerchant(id, principal.getMerchantId());
        return ResponseEntity.ok(ApiResponse.success(OrderResponse.from(order)));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(
        @AuthenticationPrincipal MerchantPrincipal principal,
        @PathVariable UUID id
    ) {
        var order = orderService.cancelOrder(id, principal.getMerchantId());
        return ResponseEntity.ok(ApiResponse.success("Order cancelled", OrderResponse.from(order)));
    }
}

