package com.payflow.gateway.analytics.controller;

import com.payflow.gateway.analytics.dto.DashboardAnalytics;
import com.payflow.gateway.analytics.service.AnalyticsService;
import com.payflow.gateway.common.dto.ApiResponse;
import com.payflow.gateway.common.security.MerchantPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<DashboardAnalytics>> getDashboard(
        @AuthenticationPrincipal MerchantPrincipal principal
    ) {
        DashboardAnalytics analytics = analyticsService.getDashboardAnalytics(principal.getMerchantId());
        return ResponseEntity.ok(ApiResponse.success(analytics));
    }
}

