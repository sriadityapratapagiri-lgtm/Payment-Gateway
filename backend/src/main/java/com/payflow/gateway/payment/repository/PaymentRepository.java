package com.payflow.gateway.payment.repository;

import com.payflow.gateway.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    List<Payment> findByMerchantIdOrderByCreatedAtDesc(UUID merchantId);
    Optional<Payment> findByIdAndMerchantId(UUID id, UUID merchantId);
    List<Payment> findByOrderId(UUID orderId);

    @Query("SELECT SUM(p.amountMinor) FROM Payment p WHERE p.merchantId = :merchantId AND p.status = :status")
    Long sumAmountByMerchantIdAndStatus(@Param("merchantId") UUID merchantId, @Param("status") String status);

    @Query("SELECT COUNT(p) FROM Payment p WHERE p.merchantId = :merchantId AND p.status = :status")
    Long countByMerchantIdAndStatus(@Param("merchantId") UUID merchantId, @Param("status") String status);
}

