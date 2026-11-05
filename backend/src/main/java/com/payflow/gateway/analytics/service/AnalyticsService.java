package com.payflow.gateway.analytics.service;

import com.payflow.gateway.analytics.dto.DailyVolume;
import com.payflow.gateway.analytics.dto.DashboardAnalytics;
import com.payflow.gateway.payment.repository.PaymentRepository;
import com.payflow.gateway.refund.repository.RefundRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class AnalyticsService {

    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;

    public AnalyticsService(PaymentRepository paymentRepository, RefundRepository refundRepository) {
        this.paymentRepository = paymentRepository;
        this.refundRepository = refundRepository;
    }

    public DashboardAnalytics getDashboardAnalytics(UUID merchantId) {
        Long capturedVolume = paymentRepository.sumAmountByMerchantIdAndStatus(merchantId, "CAPTURED");
        Long partialRefundedVolume = paymentRepository.sumAmountByMerchantIdAndStatus(merchantId, "PARTIALLY_REFUNDED");
        Long refundedVolume = paymentRepository.sumAmountByMerchantIdAndStatus(merchantId, "REFUNDED");

        long totalVolume = (capturedVolume != null ? capturedVolume : 0L)
            + (partialRefundedVolume != null ? partialRefundedVolume : 0L)
            + (refundedVolume != null ? refundedVolume : 0L);

        Long capturedCount = paymentRepository.countByMerchantIdAndStatus(merchantId, "CAPTURED");
        Long failedCount = paymentRepository.countByMerchantIdAndStatus(merchantId, "FAILED");
        Long pendingCount = paymentRepository.countByMerchantIdAndStatus(merchantId, "AUTHORIZED");
        Long partialCount = paymentRepository.countByMerchantIdAndStatus(merchantId, "PARTIALLY_REFUNDED");
        Long refundedCount = paymentRepository.countByMerchantIdAndStatus(merchantId, "REFUNDED");

        long totalSuccessful = (capturedCount != null ? capturedCount : 0L)
            + (partialCount != null ? partialCount : 0L)
            + (refundedCount != null ? refundedCount : 0L);
        long totalFailed = failedCount != null ? failedCount : 0L;
        long totalProcessed = totalSuccessful + totalFailed;
        double successRate = totalProcessed > 0 ? (double) totalSuccessful / totalProcessed * 100 : 0;

        // Total refunded volume
        Long totalRefundedVolume = 0L;
        var refunds = refundRepository.findByMerchantIdOrderByCreatedAtDesc(merchantId);
        long refundSum = refunds.stream()
            .filter(r -> "SUCCEEDED".equals(r.getStatus()))
            .mapToLong(r -> r.getAmountMinor())
            .sum();
        long totalRefundCount = refunds.stream()
            .filter(r -> "SUCCEEDED".equals(r.getStatus()))
            .count();

        // Build daily volume chart data (last 30 days)
        List<DailyVolume> volumeByDay = buildDailyVolumes(merchantId);

        return new DashboardAnalytics(
            totalVolume,
            totalSuccessful,
            totalFailed,
            pendingCount != null ? pendingCount : 0L,
            refundSum,
            totalRefundCount,
            Math.round(successRate * 10.0) / 10.0,
            volumeByDay
        );
    }

    private List<DailyVolume> buildDailyVolumes(UUID merchantId) {
        // Aggregate payment data into daily buckets from existing data
        var payments = paymentRepository.findByMerchantIdOrderByCreatedAtDesc(merchantId);
        var dailyMap = new java.util.TreeMap<String, long[]>();

        for (var payment : payments) {
            if (List.of("CAPTURED", "PARTIALLY_REFUNDED", "REFUNDED").contains(payment.getStatus())) {
                String day = payment.getCreatedAt().toLocalDate().toString();
                dailyMap.computeIfAbsent(day, k -> new long[]{0L, 0L});
                dailyMap.get(day)[0] += payment.getAmountMinor();
                dailyMap.get(day)[1]++;
            }
        }

        return dailyMap.entrySet().stream()
            .map(e -> new DailyVolume(e.getKey(), e.getValue()[0], e.getValue()[1]))
            .toList();
    }
}

