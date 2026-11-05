package com.payflow.gateway.order.service;

import com.payflow.gateway.common.exception.BusinessRuleException;
import com.payflow.gateway.common.exception.ResourceNotFoundException;
import com.payflow.gateway.order.dto.CreateOrderRequest;
import com.payflow.gateway.order.entity.Order;
import com.payflow.gateway.order.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final Set<String> supportedCurrencies;

    public OrderService(OrderRepository orderRepository, Set<String> supportedCurrencies) {
        this.orderRepository = orderRepository;
        this.supportedCurrencies = supportedCurrencies;
    }

    @Transactional
    public Order createOrder(UUID merchantId, CreateOrderRequest request) {
        if (!supportedCurrencies.contains(request.currency().toUpperCase())) {
            throw new BusinessRuleException(
                "Currency not supported: " + request.currency() + ". Supported: " + supportedCurrencies,
                "UNSUPPORTED_CURRENCY"
            );
        }

        if (orderRepository.existsByMerchantIdAndOrderReference(merchantId, request.orderReference())) {
            throw new BusinessRuleException(
                "Order reference '" + request.orderReference() + "' already exists for this merchant",
                "DUPLICATE_ORDER_REFERENCE"
            );
        }

        Order order = new Order();
        order.setMerchantId(merchantId);
        order.setOrderReference(request.orderReference());
        order.setAmountMinor(request.amountMinor());
        order.setCurrency(request.currency().toUpperCase());
        order.setDescription(request.description());
        order.setStatus("CREATED");

        return orderRepository.save(order);
    }

    public List<Order> getOrdersForMerchant(UUID merchantId) {
        return orderRepository.findByMerchantIdOrderByCreatedAtDesc(merchantId);
    }

    public Order getOrderForMerchant(UUID orderId, UUID merchantId) {
        return orderRepository.findByIdAndMerchantId(orderId, merchantId)
            .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
    }

    @Transactional
    public Order cancelOrder(UUID orderId, UUID merchantId) {
        Order order = getOrderForMerchant(orderId, merchantId);
        if ("COMPLETED".equals(order.getStatus()) || "CANCELLED".equals(order.getStatus())) {
            throw new BusinessRuleException(
                "Cannot cancel order in status: " + order.getStatus(),
                "ORDER_INVALID_STATE"
            );
        }
        order.setStatus("CANCELLED");
        return orderRepository.save(order);
    }
}

