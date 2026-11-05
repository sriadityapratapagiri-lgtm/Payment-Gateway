package com.payflow.gateway.analytics.dto;

import java.util.List;

public record DashboardAnalytics(
    Long totalVolumeMinor,
    Long capturedCount,
    Long failedCount,
    Long pendingCount,
    Long refundedVolumeMinor,
    Long totalRefundCount,
    Double successRate,
    List<DailyVolume> volumeByDay
) {}

