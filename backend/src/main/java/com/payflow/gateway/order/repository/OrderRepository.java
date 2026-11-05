package com.payflow.gateway.order.repository;

import com.payflow.gateway.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {
    List<Order> findByMerchantIdOrderByCreatedAtDesc(UUID merchantId);
    Optional<Order> findByIdAndMerchantId(UUID id, UUID merchantId);
    boolean existsByMerchantIdAndOrderReference(UUID merchantId, String orderReference);
}

