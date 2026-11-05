package com.payflow.gateway.idempotency.repository;

import com.payflow.gateway.idempotency.entity.IdempotencyKey;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, UUID> {
    Optional<IdempotencyKey> findByMerchantIdAndIdempotencyKey(UUID merchantId, String idempotencyKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ik FROM IdempotencyKey ik WHERE ik.merchantId = :merchantId AND ik.idempotencyKey = :key")
    Optional<IdempotencyKey> findWithLock(@Param("merchantId") UUID merchantId, @Param("key") String key);
}

