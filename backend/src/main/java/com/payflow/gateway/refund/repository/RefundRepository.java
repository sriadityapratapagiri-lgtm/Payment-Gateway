package com.payflow.gateway.refund.repository;

import com.payflow.gateway.refund.entity.Refund;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefundRepository extends JpaRepository<Refund, UUID> {
    List<Refund> findByMerchantIdOrderByCreatedAtDesc(UUID merchantId);
    Optional<Refund> findByIdAndMerchantId(UUID id, UUID merchantId);
    List<Refund> findByPaymentId(UUID paymentId);

    @Query("SELECT COALESCE(SUM(r.amountMinor), 0) FROM Refund r WHERE r.paymentId = :paymentId AND r.status IN ('PENDING', 'PROCESSING', 'SUCCEEDED')")
    Long sumRefundedAmountByPaymentId(@Param("paymentId") UUID paymentId);
}

