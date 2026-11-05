package com.payflow.gateway.refund.controller;

import com.payflow.gateway.common.dto.ApiResponse;
import com.payflow.gateway.common.security.MerchantPrincipal;
import com.payflow.gateway.refund.dto.CreateRefundRequest;
import com.payflow.gateway.refund.dto.RefundResponse;
import com.payflow.gateway.refund.service.RefundService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/refunds")
public class RefundController {

    private final RefundService refundService;

    public RefundController(RefundService refundService) {
        this.refundService = refundService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<RefundResponse>> createRefund(
        @AuthenticationPrincipal MerchantPrincipal principal,
        @Valid @RequestBody CreateRefundRequest request
    ) {
        var refund = refundService.createRefund(principal.getMerchantId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Refund initiated successfully", RefundResponse.from(refund)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<RefundResponse>>> getRefunds(
        @AuthenticationPrincipal MerchantPrincipal principal
    ) {
        List<RefundResponse> refunds = refundService.getRefundsForMerchant(principal.getMerchantId())
            .stream().map(RefundResponse::from).toList();
        return ResponseEntity.ok(ApiResponse.success(refunds));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RefundResponse>> getRefund(
        @AuthenticationPrincipal MerchantPrincipal principal,
        @PathVariable UUID id
    ) {
        var refund = refundService.getRefundForMerchant(id, principal.getMerchantId());
        return ResponseEntity.ok(ApiResponse.success(RefundResponse.from(refund)));
    }
}

