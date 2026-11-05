package com.payflow.gateway.refund.service;

import com.payflow.gateway.common.exception.BusinessRuleException;
import com.payflow.gateway.common.exception.ResourceNotFoundException;
import com.payflow.gateway.ledger.service.LedgerService;
import com.payflow.gateway.payment.entity.Payment;
import com.payflow.gateway.payment.repository.PaymentRepository;
import com.payflow.gateway.payment.service.PaymentService;
import com.payflow.gateway.refund.dto.CreateRefundRequest;
import com.payflow.gateway.refund.entity.Refund;
import com.payflow.gateway.refund.repository.RefundRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class RefundService {

    private final RefundRepository refundRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;
    private final LedgerService ledgerService;

    public RefundService(RefundRepository refundRepository,
                         PaymentRepository paymentRepository,
                         PaymentService paymentService,
                         LedgerService ledgerService) {
        this.refundRepository = refundRepository;
        this.paymentRepository = paymentRepository;
        this.paymentService = paymentService;
        this.ledgerService = ledgerService;
    }

    @Transactional
    public Refund createRefund(UUID merchantId, CreateRefundRequest request) {
        Payment payment = paymentRepository.findByIdAndMerchantId(request.paymentId(), merchantId)
            .orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + request.paymentId()));

        // Validate payment is in refundable state
        if (!List.of("CAPTURED", "PARTIALLY_REFUNDED").contains(payment.getStatus())) {
            throw new BusinessRuleException(
                "Payment cannot be refunded in status: " + payment.getStatus(),
                "PAYMENT_NOT_REFUNDABLE"
            );
        }

        // Calculate total already refunded (with pessimistic lock check)
        Long totalRefunded = refundRepository.sumRefundedAmountByPaymentId(payment.getId());
        long remaining = payment.getAmountMinor() - totalRefunded;

        if (request.amountMinor() > remaining) {
            throw new BusinessRuleException(
                "Refund amount (" + request.amountMinor() + ") exceeds remaining refundable amount (" + remaining + ")",
                "REFUND_EXCEEDS_CAPTURED"
            );
        }

        // Create refund record
        Refund refund = new Refund();
        refund.setPaymentId(payment.getId());
        refund.setMerchantId(merchantId);
        refund.setAmountMinor(request.amountMinor());
        refund.setCurrency(payment.getCurrency());
        refund.setReason(request.reason());
        refund.setStatus("PROCESSING");
        refund = refundRepository.save(refund);

        // Post ledger entry for refund (within same transaction)
        ledgerService.postRefund(refund.getId(), refund.getAmountMinor(), refund.getCurrency());

        refund.setStatus("SUCCEEDED");
        refund = refundRepository.save(refund);

        // Update payment status
        long newTotalRefunded = totalRefunded + request.amountMinor();
        paymentService.updatePaymentStatusForRefund(payment, newTotalRefunded);

        return refund;
    }

    public List<Refund> getRefundsForMerchant(UUID merchantId) {
        return refundRepository.findByMerchantIdOrderByCreatedAtDesc(merchantId);
    }

    public Refund getRefundForMerchant(UUID refundId, UUID merchantId) {
        return refundRepository.findByIdAndMerchantId(refundId, merchantId)
            .orElseThrow(() -> new ResourceNotFoundException("Refund not found: " + refundId));
    }
}

