package com.payflow.gateway.payment.controller;

import com.payflow.gateway.common.dto.ApiResponse;
import com.payflow.gateway.common.security.MerchantPrincipal;
import com.payflow.gateway.idempotency.service.IdempotencyService;
import com.payflow.gateway.payment.dto.CreatePaymentRequest;
import com.payflow.gateway.payment.dto.PaymentResponse;
import com.payflow.gateway.payment.service.PaymentService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final IdempotencyService idempotencyService;
    private final ObjectMapper objectMapper;

    public PaymentController(PaymentService paymentService,
                             IdempotencyService idempotencyService,
                             ObjectMapper objectMapper) {
        this.paymentService = paymentService;
        this.idempotencyService = idempotencyService;
        this.objectMapper = objectMapper;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PaymentResponse>> createPayment(
        @AuthenticationPrincipal MerchantPrincipal principal,
        @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
        @Valid @RequestBody CreatePaymentRequest request
    ) {
        UUID merchantId = principal.getMerchantId();

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            String requestHash = idempotencyService.computeHash(request);
            var existing = idempotencyService.checkAndReserve(merchantId, idempotencyKey, requestHash);
            if (existing.isPresent()) {
                // Return cached response
                try {
                    var cachedBody = objectMapper.readValue(existing.get().getResponseBody(),
                        objectMapper.getTypeFactory().constructParametricType(ApiResponse.class, PaymentResponse.class));
                    return ResponseEntity.status(existing.get().getResponseStatus()).body(cachedBody);
                } catch (Exception e) {
                    // Fall through to re-process
                }
            }

            try {
                var payment = paymentService.createPayment(merchantId, request);
                var response = ApiResponse.success("Payment processed successfully", PaymentResponse.from(payment));
                idempotencyService.complete(merchantId, idempotencyKey, 201, response);
                return ResponseEntity.status(HttpStatus.CREATED).body(response);
            } catch (Exception e) {
                idempotencyService.markFailed(merchantId, idempotencyKey);
                throw e;
            }
        }

        var payment = paymentService.createPayment(merchantId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Payment processed successfully", PaymentResponse.from(payment)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> getPayments(
        @AuthenticationPrincipal MerchantPrincipal principal
    ) {
        List<PaymentResponse> payments = paymentService.getPaymentsForMerchant(principal.getMerchantId())
            .stream().map(PaymentResponse::from).toList();
        return ResponseEntity.ok(ApiResponse.success(payments));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPayment(
        @AuthenticationPrincipal MerchantPrincipal principal,
        @PathVariable UUID id
    ) {
        var payment = paymentService.getPaymentForMerchant(id, principal.getMerchantId());
        return ResponseEntity.ok(ApiResponse.success(PaymentResponse.from(payment)));
    }

    @PostMapping("/{id}/capture")
    public ResponseEntity<ApiResponse<PaymentResponse>> capturePayment(
        @AuthenticationPrincipal MerchantPrincipal principal,
        @PathVariable UUID id
    ) {
        var payment = paymentService.capturePayment(id, principal.getMerchantId());
        return ResponseEntity.ok(ApiResponse.success("Payment captured successfully", PaymentResponse.from(payment)));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<PaymentResponse>> cancelPayment(
        @AuthenticationPrincipal MerchantPrincipal principal,
        @PathVariable UUID id
    ) {
        var payment = paymentService.cancelPayment(id, principal.getMerchantId());
        return ResponseEntity.ok(ApiResponse.success("Payment cancelled", PaymentResponse.from(payment)));
    }
}

