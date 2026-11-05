package com.payflow.gateway.idempotency.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payflow.gateway.common.exception.IdempotencyConflictException;
import com.payflow.gateway.idempotency.entity.IdempotencyKey;
import com.payflow.gateway.idempotency.repository.IdempotencyKeyRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@Service
public class IdempotencyService {

    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final ObjectMapper objectMapper;

    public IdempotencyService(IdempotencyKeyRepository idempotencyKeyRepository, ObjectMapper objectMapper) {
        this.idempotencyKeyRepository = idempotencyKeyRepository;
        this.objectMapper = objectMapper;
    }

    public String computeHash(Object request) {
        try {
            String json = objectMapper.writeValueAsString(request);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(json.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (JsonProcessingException | NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to compute request hash", e);
        }
    }

    /**
     * Check if idempotency key exists and validate it.
     * Returns Optional.empty() if this is a new request.
     * Returns the existing response if this is a duplicate.
     * Throws IdempotencyConflictException if same key with different payload.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<IdempotencyKey> checkAndReserve(UUID merchantId, String key, String requestHash) {
        Optional<IdempotencyKey> existing = idempotencyKeyRepository
            .findByMerchantIdAndIdempotencyKey(merchantId, key);

        if (existing.isPresent()) {
            IdempotencyKey ik = existing.get();
            if (!ik.getRequestHash().equals(requestHash)) {
                throw new IdempotencyConflictException(
                    "Idempotency key '" + key + "' was used with a different request payload"
                );
            }
            // Same key, same payload - return existing
            return existing;
        }

        // New key - reserve it
        IdempotencyKey ik = new IdempotencyKey();
        ik.setMerchantId(merchantId);
        ik.setIdempotencyKey(key);
        ik.setRequestHash(requestHash);
        ik.setStatus("PROCESSING");

        try {
            idempotencyKeyRepository.saveAndFlush(ik);
        } catch (DataIntegrityViolationException e) {
            // Race condition - another request with same key was saved first
            IdempotencyKey concurrent = idempotencyKeyRepository
                .findByMerchantIdAndIdempotencyKey(merchantId, key)
                .orElseThrow(() -> new IdempotencyConflictException("Concurrent idempotency conflict"));
            if (!concurrent.getRequestHash().equals(requestHash)) {
                throw new IdempotencyConflictException("Idempotency key used with different payload");
            }
            return Optional.of(concurrent);
        }

        return Optional.empty();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void complete(UUID merchantId, String key, int responseStatus, Object responseBody) {
        idempotencyKeyRepository.findByMerchantIdAndIdempotencyKey(merchantId, key)
            .ifPresent(ik -> {
                try {
                    ik.setResponseStatus(responseStatus);
                    ik.setResponseBody(objectMapper.writeValueAsString(responseBody));
                    ik.setStatus("COMPLETED");
                    idempotencyKeyRepository.save(ik);
                } catch (JsonProcessingException e) {
                    // non-critical
                }
            });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(UUID merchantId, String key) {
        idempotencyKeyRepository.findByMerchantIdAndIdempotencyKey(merchantId, key)
            .ifPresent(ik -> {
                ik.setStatus("FAILED");
                idempotencyKeyRepository.save(ik);
            });
    }
}

