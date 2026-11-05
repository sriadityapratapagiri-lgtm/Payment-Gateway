package com.payflow.gateway.payment.service;

import com.payflow.gateway.common.exception.BusinessRuleException;
import com.payflow.gateway.common.exception.ResourceNotFoundException;
import com.payflow.gateway.ledger.service.LedgerService;
import com.payflow.gateway.order.entity.Order;
import com.payflow.gateway.order.repository.OrderRepository;
import com.payflow.gateway.payment.dto.CreatePaymentRequest;
import com.payflow.gateway.payment.entity.Payment;
import com.payflow.gateway.payment.provider.MockPaymentProvider;
import com.payflow.gateway.payment.provider.PaymentProvider;
import com.payflow.gateway.payment.provider.PaymentProviderRequest;
import com.payflow.gateway.payment.provider.PaymentProviderResult;
import com.payflow.gateway.payment.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class PaymentService {

    private static final int FEE_RATE_BPS = 200; // 2% = 200 basis points

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final PaymentProvider paymentProvider;
    private final LedgerService ledgerService;

    public PaymentService(PaymentRepository paymentRepository,
                          OrderRepository orderRepository,
                          PaymentProvider paymentProvider,
                          LedgerService ledgerService) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.paymentProvider = paymentProvider;
        this.ledgerService = ledgerService;
    }

    @Transactional
    public Payment createPayment(UUID merchantId, CreatePaymentRequest request) {
        Order order = orderRepository.findByIdAndMerchantId(request.orderId(), merchantId)
            .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + request.orderId()));

        if (!"CREATED".equals(order.getStatus()) && !"PROCESSING".equals(order.getStatus())) {
            throw new BusinessRuleException(
                "Order cannot accept payments in status: " + order.getStatus(),
                "ORDER_INVALID_STATE"
            );
        }

        // Check if order already has a successful/captured payment
        List<Payment> existingPayments = paymentRepository.findByOrderId(request.orderId());
        boolean hasActivePayment = existingPayments.stream()
            .anyMatch(p -> List.of("AUTHORIZED", "CAPTURED", "PROCESSING").contains(p.getStatus()));
        if (hasActivePayment) {
            throw new BusinessRuleException("Order already has an active payment", "ORDER_ALREADY_PAID");
        }

        // Update order status to PROCESSING
        order.setStatus("PROCESSING");
        orderRepository.save(order);

        // Create payment record
        Payment payment = new Payment();
        payment.setOrderId(order.getId());
        payment.setMerchantId(merchantId);
        payment.setAmountMinor(order.getAmountMinor());
        payment.setCurrency(order.getCurrency());
        payment.setPaymentMethod(request.paymentMethod());
        payment.setSimulationResult(request.simulationResult());
        payment.setStatus("CREATED");
        payment = paymentRepository.save(payment);

        // Process via provider
        payment.setStatus("PROCESSING");
        payment = paymentRepository.save(payment);

        PaymentProviderResult result = paymentProvider.processPayment(new PaymentProviderRequest(
            payment.getId().toString(),
            payment.getAmountMinor(),
            payment.getCurrency(),
            payment.getPaymentMethod(),
            request.simulationResult()
        ));

        if (result.success()) {
            payment.setStatus("AUTHORIZED");
            payment.setProviderReference(result.providerReference());
            payment = paymentRepository.save(payment);
            // Auto-capture on authorization
            payment = capturePayment(payment.getId(), merchantId);
        } else if (result.pending()) {
            payment.setStatus("AUTHORIZED"); // Treat pending as authorized for MVP
            payment.setProviderReference(result.providerReference());
            payment = paymentRepository.save(payment);
        } else {
            payment.setStatus("FAILED");
            payment = paymentRepository.save(payment);
            order.setStatus("FAILED");
            orderRepository.save(order);
        }

        return payment;
    }

    @Transactional
    public Payment capturePayment(UUID paymentId, UUID merchantId) {
        Payment payment = getPaymentForMerchant(paymentId, merchantId);
        validateStateTransition(payment.getStatus(), "CAPTURED");

        // Post ledger entries within the same transaction
        long feeMinor = calculateFee(payment.getAmountMinor());
        ledgerService.postPaymentCapture(payment.getId(), payment.getAmountMinor(),
            payment.getCurrency(), feeMinor);

        payment.setStatus("CAPTURED");
        payment = paymentRepository.save(payment);

        // Update order status
        orderRepository.findByIdAndMerchantId(payment.getOrderId(), merchantId)
            .ifPresent(order -> {
                order.setStatus("COMPLETED");
                orderRepository.save(order);
            });

        return payment;
    }

    @Transactional
    public Payment cancelPayment(UUID paymentId, UUID merchantId) {
        Payment payment = getPaymentForMerchant(paymentId, merchantId);
        validateStateTransition(payment.getStatus(), "CANCELLED");
        payment.setStatus("CANCELLED");
        return paymentRepository.save(payment);
    }

    public List<Payment> getPaymentsForMerchant(UUID merchantId) {
        return paymentRepository.findByMerchantIdOrderByCreatedAtDesc(merchantId);
    }

    public Payment getPaymentForMerchant(UUID paymentId, UUID merchantId) {
        return paymentRepository.findByIdAndMerchantId(paymentId, merchantId)
            .orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + paymentId));
    }

    private void validateStateTransition(String currentStatus, String targetStatus) {
        boolean valid = switch (targetStatus) {
            case "CAPTURED" -> "AUTHORIZED".equals(currentStatus);
            case "CANCELLED" -> List.of("CREATED", "PROCESSING", "AUTHORIZED").contains(currentStatus);
            case "PARTIALLY_REFUNDED" -> "CAPTURED".equals(currentStatus);
            case "REFUNDED" -> List.of("CAPTURED", "PARTIALLY_REFUNDED").contains(currentStatus);
            default -> false;
        };

        if (!valid) {
            throw new BusinessRuleException(
                "Invalid payment state transition from " + currentStatus + " to " + targetStatus,
                "PAYMENT_INVALID_STATE"
            );
        }
    }

    public long calculateFee(long amountMinor) {
        // 2% fee (200 basis points)
        return Math.max(1, amountMinor * FEE_RATE_BPS / 10000);
    }

    public void updatePaymentStatusForRefund(Payment payment, long totalRefunded) {
        if (totalRefunded >= payment.getAmountMinor()) {
            payment.setStatus("REFUNDED");
        } else {
            payment.setStatus("PARTIALLY_REFUNDED");
        }
        paymentRepository.save(payment);
    }
}

